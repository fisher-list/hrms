package com.hrms.common.employee.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hrms.common.api.BizCode;
import com.hrms.common.approval.service.ApprovalService;
import com.hrms.common.employee.dto.TerminationFormCreateDto;
import com.hrms.common.employee.entity.HrEmployee;
import com.hrms.common.employee.entity.HrTerminationForm;
import com.hrms.common.employee.mapper.HrEmployeeMapper;
import com.hrms.common.employee.mapper.HrTerminationFormMapper;
import com.hrms.common.exception.BizException;
import com.hrms.common.org.service.PositionService;
import com.hrms.common.user.SysUser;
import com.hrms.common.user.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for termination form lifecycle: create, submit, and handle approval outcomes.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TerminationFormService {

    private final HrTerminationFormMapper terminationFormMapper;
    private final HrEmployeeMapper employeeMapper;
    private final ApprovalService approvalService;
    private final PositionService positionService;
    private final SysUserMapper sysUserMapper;

    /**
     * Create a new termination form with status=PENDING.
     */
    @Transactional
    public HrTerminationForm create(TerminationFormCreateDto dto) {
        HrEmployee emp = employeeMapper.selectById(dto.getEmployeeId());
        if (emp == null) {
            throw new BizException(BizCode.EMPLOYEE_NOT_FOUND, "Employee not found: " + dto.getEmployeeId());
        }

        HrTerminationForm form = new HrTerminationForm();
        form.setFormNo("TM" + cn.hutool.core.util.IdUtil.getSnowflakeNextIdStr());
        form.setEmployeeId(dto.getEmployeeId());
        form.setTerminationType(dto.getTerminationType());
        form.setReason(dto.getReason());
        form.setLastWorkingDay(dto.getLastWorkingDay());
        form.setStatus("PENDING");
        terminationFormMapper.insert(form);
        return form;
    }

    /**
     * Submit a termination form for approval.
     */
    @Transactional
    public void submit(Long id) {
        HrTerminationForm form = terminationFormMapper.selectById(id);
        if (form == null) {
            throw new BizException(BizCode.BAD_REQUEST, "Termination form not found: " + id);
        }
        if (!"PENDING".equals(form.getStatus())) {
            throw new BizException(BizCode.BAD_REQUEST, "Form is not in PENDING status");
        }

        Long instanceId = approvalService.start(
                "HR_TERMINATION_FORM",
                form.getId().toString(),
                "离职申请: " + form.getTerminationType(),
                null,
                null);

        form.setApprovalInstanceId(instanceId);
        form.setStatus("SUBMITTED");
        terminationFormMapper.updateById(form);
    }

    /**
     * Called when approval is completed successfully.
     * Transitions employee to TERMINATED, decrements position headcount, disables SysUser.
     *
     * @param formId the termination form ID (from businessKey)
     */
    @Transactional
    public void onApproved(Long formId) {
        HrTerminationForm form = terminationFormMapper.selectById(formId);
        if (form == null) {
            log.warn("No termination form found for id {}", formId);
            return;
        }

        HrEmployee emp = employeeMapper.selectById(form.getEmployeeId());
        if (emp == null) {
            log.warn("Employee {} not found for termination form {}", form.getEmployeeId(), form.getFormNo());
            return;
        }

        // Validate state transition
        EmployeeStateMachine.validate(emp.getStatus(), "TERMINATED");
        emp.setStatus("TERMINATED");
        employeeMapper.updateById(emp);

        // Decrement position headcount
        positionService.decrOccupied(emp.getPositionId());

        // Disable SysUser
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, emp.getEmpNo()));
        if (user != null) {
            user.setStatus("DISABLED");
            sysUserMapper.updateById(user);
        }

        form.setStatus("APPROVED");
        terminationFormMapper.updateById(form);
        log.info("Termination form {} approved, employee {} terminated", form.getFormNo(), emp.getEmpNo());
    }

    /**
     * Called when approval is rejected.
     *
     * @param formId the termination form ID (from businessKey)
     */
    @Transactional
    public void onRejected(Long formId) {
        HrTerminationForm form = terminationFormMapper.selectById(formId);
        if (form == null) {
            log.warn("No termination form found for id {}", formId);
            return;
        }
        form.setStatus("REJECTED");
        terminationFormMapper.updateById(form);
        log.info("Termination form {} rejected", form.getFormNo());
    }

    /**
     * List termination forms with pagination.
     */
    public IPage<HrTerminationForm> list(String status, Page<HrTerminationForm> page) {
        LambdaQueryWrapper<HrTerminationForm> qw = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            qw.eq(HrTerminationForm::getStatus, status);
        }
        qw.orderByDesc(HrTerminationForm::getCreatedAt);
        return terminationFormMapper.selectPage(page, qw);
    }

    /**
     * Get termination form by ID.
     */
    public HrTerminationForm getById(Long id) {
        HrTerminationForm form = terminationFormMapper.selectById(id);
        if (form == null) {
            throw new BizException(BizCode.BAD_REQUEST, "Termination form not found: " + id);
        }
        return form;
    }
}
