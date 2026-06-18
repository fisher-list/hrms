package com.hrms.common.employee.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hrms.common.employee.dto.*;
import com.hrms.common.employee.entity.HrEmployee;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Facade for employee operations.
 * Delegates to {@link EmployeeMasterService} and {@link EmployeeSubDataService}.
 *
 * <p>Kept for backward compatibility with existing injection points.</p>
 */
@Service
@RequiredArgsConstructor
public class HrEmployeeService {

    private final EmployeeMasterService masterService;

    public IPage<EmployeeListVo> list(String keyword, String status, Page<HrEmployee> page) {
        return masterService.list(keyword, status, page);
    }

    public EmployeeDetailVo getDetailById(Long id) {
        return masterService.getDetailById(id);
    }

    public EmployeeDetailVo getDetailByIdForUser(Long id, Long userId, Long employeeId) {
        return masterService.getDetailByIdForUser(id, userId, employeeId);
    }

    /**
     * @deprecated Use {@link #getDetailById(Long)} for type-safe return.
     */
    @Deprecated
    public java.util.Map<String, Object> getById(Long id) {
        EmployeeDetailVo vo = masterService.getDetailById(id);
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("employee", vo.getEmployee());
        result.put("educations", vo.getEducations());
        result.put("workExps", vo.getWorkExps());
        result.put("family", vo.getFamily());
        result.put("contracts", vo.getContracts());
        result.put("bankAccounts", vo.getBankAccounts());
        result.put("addresses", vo.getAddresses());
        return result;
    }

    public HrEmployee create(EmployeeCreateDto dto) {
        return masterService.create(dto);
    }

    public HrEmployee update(Long id, EmployeeUpdateDto dto) {
        return masterService.update(id, dto);
    }

    public void terminate(Long id) {
        masterService.terminate(id);
    }
}
