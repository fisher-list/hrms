package com.hrms.common.employee.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hrms.common.api.BizCode;
import com.hrms.common.employee.dto.*;
import com.hrms.common.employee.entity.*;
import com.hrms.common.employee.mapper.HrEmployeeMapper;
import com.hrms.common.exception.BizException;
import com.hrms.common.org.service.PositionService;
import com.hrms.common.rbac.annotation.DataScopeType;
import com.hrms.common.rbac.datascope.DepartmentTreeService;
import com.hrms.common.rbac.service.PermissionService;
import com.hrms.common.util.AesUtil;
import com.hrms.common.util.MaskUtil;
import com.hrms.common.util.SensitiveHashUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;

/**
 * Service for employee master table CRUD and lifecycle operations.
 * Sub-table operations are delegated to {@link EmployeeSubDataService}.
 */
@Service
@RequiredArgsConstructor
public class EmployeeMasterService {

    private static final Logger log = LoggerFactory.getLogger(EmployeeMasterService.class);

    private final HrEmployeeMapper employeeMapper;
    private final EmployeeSubDataService subDataService;
    private final PositionService positionService;
    private final PermissionService permissionService;
    private final DepartmentTreeService departmentTreeService;

    /**
     * List employees with pagination, keyword, and status filter.
     */
    public IPage<EmployeeListVo> list(String keyword, String status, Page<HrEmployee> page) {
        IPage<HrEmployee> result = employeeMapper.selectPageWithDept(page, keyword, status);
        return result.convert(this::toEmployeeListVo);
    }

    /**
     * Get employee detail with all sub-tables (type-safe VO).
     */
    public EmployeeDetailVo getDetailById(Long id) {
        HrEmployee emp = employeeMapper.selectById(id);
        if (emp == null) {
            throw new BizException(BizCode.EMPLOYEE_NOT_FOUND, "Employee not found: " + id);
        }
        return buildDetailVo(emp);
    }

    public EmployeeDetailVo getDetailByIdForUser(Long id, Long userId, Long employeeId) {
        HrEmployee emp = employeeMapper.selectById(id);
        if (emp == null) {
            throw new BizException(BizCode.EMPLOYEE_NOT_FOUND, "Employee not found: " + id);
        }
        if (!canViewEmployee(emp, userId, employeeId)) {
            throw new BizException(BizCode.FORBIDDEN, "无权限访问该员工档案");
        }
        return buildDetailVo(emp);
    }

    /**
     * Build the full employee detail VO by loading sub-table data.
     * 
     * <p>[P1-DB3 FIX] The 6 sub-table queries run in parallel using CompletableFuture
     * since they query independent tables and cannot be merged into a single JOIN.
     * Wall-clock time drops from ~6 sequential round-trips to ~1 (the slowest query).</p>
     */
    private EmployeeDetailVo buildDetailVo(HrEmployee emp) {
        Long id = emp.getId();

        // Launch all 6 sub-table queries in parallel
        CompletableFuture<List<HrEmployeeEducation>> eduFuture = CompletableFuture.supplyAsync(
                () -> subDataService.listEducations(id), ForkJoinPool.commonPool());
        CompletableFuture<List<HrEmployeeWorkExp>> workFuture = CompletableFuture.supplyAsync(
                () -> subDataService.listWorkExps(id), ForkJoinPool.commonPool());
        CompletableFuture<List<HrEmployeeFamily>> famFuture = CompletableFuture.supplyAsync(
                () -> subDataService.listFamily(id), ForkJoinPool.commonPool());
        CompletableFuture<List<HrEmployeeContract>> contFuture = CompletableFuture.supplyAsync(
                () -> subDataService.listContracts(id), ForkJoinPool.commonPool());
        CompletableFuture<List<HrEmployeeBankAccount>> bankFuture = CompletableFuture.supplyAsync(
                () -> subDataService.listBankAccounts(id), ForkJoinPool.commonPool());
        CompletableFuture<List<HrEmployeeAddress>> addrFuture = CompletableFuture.supplyAsync(
                () -> subDataService.listAddresses(id), ForkJoinPool.commonPool());

        // Wait for all to complete
        CompletableFuture.allOf(eduFuture, workFuture, famFuture, contFuture, bankFuture, addrFuture).join();

        // Assemble the VO
        EmployeeDetailVo vo = new EmployeeDetailVo();
        vo.setEmployee(emp);
        vo.setEducations(getOrDefault(eduFuture));
        vo.setWorkExps(getOrDefault(workFuture));
        vo.setFamily(getOrDefault(famFuture));
        vo.setContracts(getOrDefault(contFuture));
        vo.setBankAccounts(getOrDefault(bankFuture));
        // Mask bank account numbers for security
        if (vo.getBankAccounts() != null) {
            for (var bank : vo.getBankAccounts()) {
                if (bank.getAccountNoEnc() != null) {
                    try {
                        String decrypted = AesUtil.decrypt(bank.getAccountNoEnc());
                        bank.setAccountNoEnc(MaskUtil.maskAccountNo(decrypted));
                    } catch (Exception e) {
                        bank.setAccountNoEnc("****");
                        log.warn("Failed to decrypt/mask bank account for employee {}, account id {}", id, bank.getId());
                    }
                }
            }
        }
        vo.setAddresses(getOrDefault(addrFuture));
        return vo;
    }

    /**
     * Safely extract the result from a CompletableFuture, returning null on failure.
     */
    private <T> T getOrDefault(CompletableFuture<T> future) {
        try {
            return future.get();
        } catch (Exception e) {
            log.error("Failed to load sub-table data in parallel", e);
            return null;
        }
    }

    private boolean canViewEmployee(HrEmployee target, Long userId, Long employeeId) {
        DataScopeType scope = permissionService.getDominantDataScope(userId);
        if (scope == DataScopeType.ALL) {
            return true;
        }
        if (employeeId == null) {
            return false;
        }
        if (scope == DataScopeType.SELF_ONLY) {
            return target.getId().equals(employeeId);
        }
        Long ownDeptId = departmentTreeService.getDeptIdForEmployee(employeeId);
        if (ownDeptId == null || target.getDeptId() == null) {
            return false;
        }
        if (scope == DataScopeType.OWN_DEPT) {
            return target.getDeptId().equals(ownDeptId);
        }
        return departmentTreeService.getSubordinateDeptIds(ownDeptId).contains(target.getDeptId());
    }

    /**
     * Create a new employee with sub-table data.
     */
    @Transactional
    public HrEmployee create(EmployeeCreateDto dto) {
        String idCardHash = SensitiveHashUtil.idCardHash(dto.getIdCard());
        if (idCardHash != null) {
            long count = employeeMapper.selectCount(
                    new LambdaQueryWrapper<HrEmployee>()
                            .eq(HrEmployee::getIdCardHash, idCardHash));
            if (count > 0) {
                throw new BizException(BizCode.EMPLOYEE_DUPLICATE_ID_CARD, "Duplicate ID card number");
            }
        }

        HrEmployee emp = new HrEmployee();
        emp.setName(dto.getName());
        emp.setGender(dto.getGender());
        emp.setBirthDate(dto.getBirthDate());
        emp.setIdCardEnc(AesUtil.encrypt(dto.getIdCard()));
        emp.setIdCardHash(idCardHash);
        emp.setPhoneEnc(AesUtil.encrypt(dto.getPhone()));
        emp.setEmail(dto.getEmail());
        emp.setDeptId(dto.getDeptId());
        emp.setPositionId(dto.getPositionId());
        emp.setHireDate(dto.getHireDate());
        emp.setStatus("PENDING_HIRE");
        emp.setContractStart(dto.getContractStart());
        emp.setContractEnd(dto.getContractEnd());
        emp.setProbationEnd(dto.getProbationEnd());
        emp.setEmergencyContact(dto.getEmergencyContact());
        emp.setEmergencyPhoneEnc(AesUtil.encrypt(dto.getEmergencyPhone()));

        employeeMapper.insert(emp);
        emp.setEmpNo(String.format("E%06d", emp.getId()));
        employeeMapper.updateById(emp);

        subDataService.insertSubTables(emp.getId(), dto);
        return emp;
    }

    /**
     * Update employee master data and sub-tables.
     */
    @Transactional
    public HrEmployee update(Long id, EmployeeUpdateDto dto) {
        HrEmployee emp = employeeMapper.selectById(id);
        if (emp == null) {
            throw new BizException(BizCode.EMPLOYEE_NOT_FOUND, "Employee not found: " + id);
        }

        if (dto.getName() != null) emp.setName(dto.getName());
        if (dto.getGender() != null) emp.setGender(dto.getGender());
        if (dto.getBirthDate() != null) emp.setBirthDate(dto.getBirthDate());
        if (dto.getIdCard() != null) {
            String idCardHash = SensitiveHashUtil.idCardHash(dto.getIdCard());
            if (idCardHash != null) {
                long count = employeeMapper.selectCount(
                        new LambdaQueryWrapper<HrEmployee>()
                                .eq(HrEmployee::getIdCardHash, idCardHash)
                                .ne(HrEmployee::getId, id));
                if (count > 0) {
                    throw new BizException(BizCode.EMPLOYEE_DUPLICATE_ID_CARD, "Duplicate ID card number");
                }
            }
            emp.setIdCardEnc(AesUtil.encrypt(dto.getIdCard()));
            emp.setIdCardHash(idCardHash);
        }
        if (dto.getPhone() != null) emp.setPhoneEnc(AesUtil.encrypt(dto.getPhone()));
        if (dto.getEmail() != null) emp.setEmail(dto.getEmail());
        if (dto.getDeptId() != null) emp.setDeptId(dto.getDeptId());
        if (dto.getPositionId() != null) emp.setPositionId(dto.getPositionId());
        if (dto.getHireDate() != null) emp.setHireDate(dto.getHireDate());
        if (dto.getContractStart() != null) emp.setContractStart(dto.getContractStart());
        if (dto.getContractEnd() != null) emp.setContractEnd(dto.getContractEnd());
        if (dto.getProbationEnd() != null) emp.setProbationEnd(dto.getProbationEnd());
        if (dto.getEmergencyContact() != null) emp.setEmergencyContact(dto.getEmergencyContact());
        if (dto.getEmergencyPhone() != null) emp.setEmergencyPhoneEnc(AesUtil.encrypt(dto.getEmergencyPhone()));

        employeeMapper.updateById(emp);

        // Delegate sub-table updates using diff strategy
        subDataService.updateSubTables(id, dto);
        return emp;
    }

    /**
     * Terminate an employee.
     */
    @Transactional
    public void terminate(Long id) {
        HrEmployee emp = employeeMapper.selectById(id);
        if (emp == null) {
            throw new BizException(BizCode.EMPLOYEE_NOT_FOUND, "Employee not found: " + id);
        }
        EmployeeStateMachine.validate(emp.getStatus(), "TERMINATED");
        emp.setStatus("TERMINATED");
        employeeMapper.updateById(emp);
        positionService.decrOccupied(emp.getPositionId());
    }

    private EmployeeListVo toEmployeeListVo(HrEmployee emp) {
        EmployeeListVo vo = new EmployeeListVo();
        vo.setId(emp.getId());
        vo.setEmpNo(emp.getEmpNo());
        vo.setName(emp.getName());
        vo.setGender(emp.getGender());
        vo.setBirthDate(emp.getBirthDate());
        vo.setEmail(emp.getEmail());
        vo.setDeptId(emp.getDeptId());
        vo.setPositionId(emp.getPositionId());
        vo.setHireDate(emp.getHireDate());
        vo.setStatus(emp.getStatus());
        vo.setContractStart(emp.getContractStart());
        vo.setContractEnd(emp.getContractEnd());
        vo.setProbationEnd(emp.getProbationEnd());
        vo.setEmergencyContact(emp.getEmergencyContact());
        vo.setCreatedAt(emp.getCreatedAt());

        try {
            if (emp.getIdCardEnc() != null) {
                vo.setIdCardMasked(MaskUtil.maskIdCard(AesUtil.decrypt(emp.getIdCardEnc())));
            }
        } catch (Exception e) {
            vo.setIdCardMasked("***");
        }
        try {
            if (emp.getPhoneEnc() != null) {
                vo.setPhoneMasked(MaskUtil.maskPhone(AesUtil.decrypt(emp.getPhoneEnc())));
            }
        } catch (Exception e) {
            vo.setPhoneMasked("***");
        }
        return vo;
    }
}
