package com.hrms.common.employee.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hrms.common.api.BizCode;
import com.hrms.common.approval.service.ApprovalService;
import com.hrms.common.employee.dto.HireFormCreateDto;
import com.hrms.common.employee.entity.HrEmployee;
import com.hrms.common.employee.entity.HrEmployeeContract;
import com.hrms.common.employee.entity.HrHireForm;
import com.hrms.common.employee.mapper.HrEmployeeContractMapper;
import com.hrms.common.employee.mapper.HrEmployeeMapper;
import com.hrms.common.employee.mapper.HrHireFormMapper;
import com.hrms.common.exception.BizException;
import com.hrms.common.org.service.PositionService;
import com.hrms.common.user.SysUser;
import com.hrms.common.user.SysUserMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.Map;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for hire form lifecycle: create, submit, and handle approval outcomes.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HireFormService {

    private final HrHireFormMapper hireFormMapper;
    private final HrEmployeeMapper employeeMapper;
    private final HrEmployeeContractMapper contractMapper;
    private final ApprovalService approvalService;
    private final PositionService positionService;
    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    /**
     * Create a new hire form with status=PENDING.
     */
    @Transactional
    public HrHireForm create(HireFormCreateDto dto) {
        HrHireForm form = new HrHireForm();
        form.setFormNo("HF" + cn.hutool.core.util.IdUtil.getSnowflakeNextIdStr());
        form.setEmployeeSnapshot(dto.getEmployeeSnapshot());
        form.setPositionId(dto.getPositionId());
        form.setDeptId(dto.getDeptId());
        form.setHireDate(dto.getHireDate());
        form.setAccountName(dto.getAccountName());
        form.setStatus("PENDING");
        hireFormMapper.insert(form);
        return form;
    }

    /**
     * Submit a hire form for approval.
     */
    @Transactional
    public void submit(Long id) {
        HrHireForm form = hireFormMapper.selectById(id);
        if (form == null) {
            throw new BizException(BizCode.BAD_REQUEST, "Hire form not found: " + id);
        }
        if (!"PENDING".equals(form.getStatus())) {
            throw new BizException(BizCode.BAD_REQUEST, "Form is not in PENDING status");
        }

        Long instanceId = approvalService.start(
                "HR_HIRE_FORM",
                form.getId().toString(),
                "入职申请: " + form.getAccountName(),
                form.getEmployeeSnapshot(),
                null); // applicantId will be resolved by the approval context

        form.setApprovalInstanceId(instanceId);
        form.setStatus("SUBMITTED");
        hireFormMapper.updateById(form);
    }

    /**
     * Called when the approval is completed successfully.
     * Creates the employee record in PROBATION status, creates a SysUser, and increments position headcount.
     *
     * @param formId the hire form ID (from businessKey)
     */
    @Transactional
    public void onApproved(Long formId) {
        HrHireForm form = hireFormMapper.selectById(formId);
        if (form == null) {
            log.warn("No hire form found for id {}", formId);
            return;
        }

        // Create employee in PROBATION status, populating from snapshot if available
        HrEmployee emp = new HrEmployee();
        emp.setName(form.getAccountName());
        emp.setDeptId(form.getDeptId());
        emp.setPositionId(form.getPositionId());
        emp.setHireDate(form.getHireDate());
        emp.setStatus("PROBATION");

        // Parse employeeSnapshot JSON to populate additional fields
        if (form.getEmployeeSnapshot() != null && !form.getEmployeeSnapshot().isBlank()) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> snapshot = objectMapper.readValue(form.getEmployeeSnapshot(), Map.class);
                if (snapshot.get("gender") != null) emp.setGender(snapshot.get("gender").toString());
                if (snapshot.get("birthDate") != null) emp.setBirthDate(java.time.LocalDate.parse(snapshot.get("birthDate").toString()));
                if (snapshot.get("email") != null) emp.setEmail(snapshot.get("email").toString());
                if (snapshot.get("idCard") != null) {
                    String idCard = snapshot.get("idCard").toString();
                    emp.setIdCardEnc(com.hrms.common.util.AesUtil.encrypt(idCard));
                    emp.setIdCardHash(com.hrms.common.util.SensitiveHashUtil.idCardHash(idCard));
                }
                if (snapshot.get("phone") != null) emp.setPhoneEnc(com.hrms.common.util.AesUtil.encrypt(snapshot.get("phone").toString()));
                if (snapshot.get("contractStart") != null) emp.setContractStart(java.time.LocalDate.parse(snapshot.get("contractStart").toString()));
                if (snapshot.get("contractEnd") != null) emp.setContractEnd(java.time.LocalDate.parse(snapshot.get("contractEnd").toString()));
                if (snapshot.get("probationEnd") != null) emp.setProbationEnd(java.time.LocalDate.parse(snapshot.get("probationEnd").toString()));
                if (snapshot.get("emergencyContact") != null) emp.setEmergencyContact(snapshot.get("emergencyContact").toString());
                if (snapshot.get("emergencyPhone") != null) emp.setEmergencyPhoneEnc(com.hrms.common.util.AesUtil.encrypt(snapshot.get("emergencyPhone").toString()));
            } catch (Exception e) {
                log.warn("Failed to parse employee snapshot for form {}: {}", form.getFormNo(), e.getMessage());
            }
        }
        employeeMapper.insert(emp);

        // Generate empNo
        emp.setEmpNo(String.format("E%06d", emp.getId()));
        employeeMapper.updateById(emp);

        // Create default contract
        HrEmployeeContract contract = new HrEmployeeContract();
        contract.setEmployeeId(emp.getId());
        contract.setContractType("FIXED");
        contract.setStartDate(form.getHireDate());
        contract.setStatus("ACTIVE");
        contractMapper.insert(contract);

        // Create system user (username = empNo, password = last 6 chars of empNo as placeholder)
        SysUser user = new SysUser();
        user.setUsername(emp.getEmpNo());
        user.setPasswordHash(passwordEncoder.encode(emp.getEmpNo().substring(emp.getEmpNo().length() - 6)));
        user.setNickname(form.getAccountName());
        user.setEmployeeId(emp.getId());
        user.setStatus("ACTIVE");
        sysUserMapper.insert(user);

        // Increment position headcount
        positionService.incrOccupied(form.getPositionId());

        form.setStatus("APPROVED");
        hireFormMapper.updateById(form);
        log.info("Hire form {} approved, employee {} created", form.getFormNo(), emp.getEmpNo());
    }

    /**
     * Called when the approval is rejected.
     *
     * @param formId the hire form ID (from businessKey)
     */
    @Transactional
    public void onRejected(Long formId) {
        HrHireForm form = hireFormMapper.selectById(formId);
        if (form == null) {
            log.warn("No hire form found for id {}", formId);
            return;
        }
        form.setStatus("REJECTED");
        hireFormMapper.updateById(form);
        log.info("Hire form {} rejected", form.getFormNo());
    }

    /**
     * List hire forms with pagination.
     */
    public IPage<HrHireForm> list(String status, Page<HrHireForm> page) {
        LambdaQueryWrapper<HrHireForm> qw = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            qw.eq(HrHireForm::getStatus, status);
        }
        qw.orderByDesc(HrHireForm::getCreatedAt);
        return hireFormMapper.selectPage(page, qw);
    }

    /**
     * Get hire form by ID.
     */
    public HrHireForm getById(Long id) {
        HrHireForm form = hireFormMapper.selectById(id);
        if (form == null) {
            throw new BizException(BizCode.BAD_REQUEST, "Hire form not found: " + id);
        }
        return form;
    }
}
