package com.hrms.common.employee.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hrms.common.api.BizCode;
import com.hrms.common.approval.service.ApprovalService;
import com.hrms.common.employee.dto.TransferFormCreateDto;
import com.hrms.common.employee.entity.HrEmployee;
import com.hrms.common.employee.entity.HrTransferForm;
import com.hrms.common.employee.mapper.HrEmployeeMapper;
import com.hrms.common.employee.mapper.HrTransferFormMapper;
import com.hrms.common.exception.BizException;
import com.hrms.common.org.service.PositionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for transfer form lifecycle: create, submit, and handle approval outcomes.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransferFormService {

    private final HrTransferFormMapper transferFormMapper;
    private final HrEmployeeMapper employeeMapper;
    private final ApprovalService approvalService;
    private final PositionService positionService;

    /**
     * Create a new transfer form with status=PENDING.
     */
    @Transactional
    public HrTransferForm create(TransferFormCreateDto dto) {
        HrEmployee emp = employeeMapper.selectById(dto.getEmployeeId());
        if (emp == null) {
            throw new BizException(BizCode.EMPLOYEE_NOT_FOUND, "Employee not found: " + dto.getEmployeeId());
        }

        HrTransferForm form = new HrTransferForm();
        form.setFormNo("TF" + cn.hutool.core.util.IdUtil.getSnowflakeNextIdStr());
        form.setEmployeeId(dto.getEmployeeId());
        form.setFromDeptId(emp.getDeptId());
        form.setFromPositionId(emp.getPositionId());
        form.setToDeptId(dto.getToDeptId());
        form.setToPositionId(dto.getToPositionId());
        form.setTransferType(dto.getTransferType());
        form.setEffectiveDate(dto.getEffectiveDate());
        form.setReason(dto.getReason());
        form.setStatus("PENDING");
        transferFormMapper.insert(form);
        return form;
    }

    /**
     * Submit a transfer form for approval.
     */
    @Transactional
    public void submit(Long id) {
        HrTransferForm form = transferFormMapper.selectById(id);
        if (form == null) {
            throw new BizException(BizCode.BAD_REQUEST, "Transfer form not found: " + id);
        }
        if (!"PENDING".equals(form.getStatus())) {
            throw new BizException(BizCode.BAD_REQUEST, "Form is not in PENDING status");
        }

        Long instanceId = approvalService.start(
                "HR_TRANSFER_FORM",
                form.getId().toString(),
                "调动申请: " + form.getTransferType(),
                null,
                null);

        form.setApprovalInstanceId(instanceId);
        form.setStatus("SUBMITTED");
        transferFormMapper.updateById(form);
    }

    /**
     * Called when approval is completed successfully.
     * Updates employee dept/position, and adjusts position headcounts.
     *
     * @param formId the transfer form ID (from businessKey)
     */
    @Transactional
    public void onApproved(Long formId) {
        HrTransferForm form = transferFormMapper.selectById(formId);
        if (form == null) {
            log.warn("No transfer form found for id {}", formId);
            return;
        }

        HrEmployee emp = employeeMapper.selectById(form.getEmployeeId());
        if (emp == null) {
            log.warn("Employee {} not found for transfer form {}", form.getEmployeeId(), form.getFormNo());
            return;
        }

        // Update employee dept/position
        if (form.getToDeptId() != null) {
            emp.setDeptId(form.getToDeptId());
        }
        if (form.getToPositionId() != null) {
            // Decrement old position, increment new position
            positionService.decrOccupied(emp.getPositionId());
            positionService.incrOccupied(form.getToPositionId());
            emp.setPositionId(form.getToPositionId());
        }
        employeeMapper.updateById(emp);

        form.setStatus("APPROVED");
        transferFormMapper.updateById(form);
        log.info("Transfer form {} approved, employee {} updated", form.getFormNo(), emp.getEmpNo());
    }

    /**
     * Called when approval is rejected.
     *
     * @param formId the transfer form ID (from businessKey)
     */
    @Transactional
    public void onRejected(Long formId) {
        HrTransferForm form = transferFormMapper.selectById(formId);
        if (form == null) {
            log.warn("No transfer form found for id {}", formId);
            return;
        }
        form.setStatus("REJECTED");
        transferFormMapper.updateById(form);
        log.info("Transfer form {} rejected", form.getFormNo());
    }

    /**
     * List transfer forms with pagination.
     */
    public IPage<HrTransferForm> list(String status, Page<HrTransferForm> page) {
        LambdaQueryWrapper<HrTransferForm> qw = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            qw.eq(HrTransferForm::getStatus, status);
        }
        qw.orderByDesc(HrTransferForm::getCreatedAt);
        return transferFormMapper.selectPage(page, qw);
    }

    /**
     * Get transfer form by ID.
     */
    public HrTransferForm getById(Long id) {
        HrTransferForm form = transferFormMapper.selectById(id);
        if (form == null) {
            throw new BizException(BizCode.BAD_REQUEST, "Transfer form not found: " + id);
        }
        return form;
    }
}
