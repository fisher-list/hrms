package com.hrms.common.attendance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hrms.common.api.BizCode;
import com.hrms.common.attendance.dto.LeaveRequestCreateDto;
import com.hrms.common.attendance.entity.AtLeaveBalance;
import com.hrms.common.attendance.entity.AtLeaveRequest;
import com.hrms.common.attendance.mapper.AtLeaveRequestMapper;
import com.hrms.common.approval.service.ApprovalService;
import com.hrms.common.employee.entity.HrEmployee;
import com.hrms.common.employee.mapper.HrEmployeeMapper;
import com.hrms.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Service for leave request lifecycle: create, submit, approve/reject, list.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LeaveRequestService {

    private final AtLeaveRequestMapper leaveRequestMapper;
    private final HrEmployeeMapper employeeMapper;
    private final LeaveBalanceService leaveBalanceService;
    private final ApprovalService approvalService;

    /**
     * Create a leave request with full validation.
     * Status is set to PENDING (not yet submitted for approval).
     */
    @Transactional
    public AtLeaveRequest create(LeaveRequestCreateDto dto, Long employeeId) {
        // 1. Validate employee status
        HrEmployee employee = employeeMapper.selectById(employeeId);
        if (employee == null) {
            throw new BizException(BizCode.EMPLOYEE_NOT_FOUND, "员工不存在: " + employeeId);
        }
        if (!"ACTIVE".equals(employee.getStatus()) && !"ON_LEAVE".equals(employee.getStatus())) {
            throw new BizException(BizCode.BAD_REQUEST,
                    "员工状态不允许请假: " + employee.getStatus());
        }

        // 2. Validate dates
        if (dto.getStartDate().isAfter(dto.getEndDate())) {
            throw new BizException(BizCode.LEAVE_DATE_INVALID, "起始日期不能晚于结束日期");
        }

        // 3. Validate contract end date
        if (employee.getContractEnd() != null && dto.getEndDate().isAfter(employee.getContractEnd())) {
            throw new BizException(BizCode.LEAVE_DATE_INVALID,
                    "请假结束日期超过合同到期日: " + employee.getContractEnd());
        }

        // 4. Check balance
        int currentYear = LocalDate.now().getYear();
        AtLeaveBalance balance = leaveBalanceService.getBalanceForEmployeeAndType(
                employeeId, dto.getLeaveTypeId(), currentYear);
        if (balance == null) {
            throw new BizException(BizCode.LEAVE_BALANCE_INSUFFICIENT, "未找到对应假期余额记录");
        }
        if (balance.getRemaining().compareTo(dto.getDays()) < 0) {
            throw new BizException(BizCode.LEAVE_BALANCE_INSUFFICIENT,
                    "假期余额不足，剩余: " + balance.getRemaining() + "，申请: " + dto.getDays());
        }

        // 5. Check date conflict with existing approved requests
        List<AtLeaveRequest> conflicts = leaveRequestMapper.selectList(
                new LambdaQueryWrapper<AtLeaveRequest>()
                        .eq(AtLeaveRequest::getEmployeeId, employeeId)
                        .eq(AtLeaveRequest::getStatus, "APPROVED")
                        .le(AtLeaveRequest::getStartDate, dto.getEndDate())
                        .ge(AtLeaveRequest::getEndDate, dto.getStartDate()));
        if (!conflicts.isEmpty()) {
            throw new BizException(BizCode.LEAVE_DATE_CONFLICT,
                    "与已有已批准请假单时间冲突");
        }

        // 6. Create request
        AtLeaveRequest request = new AtLeaveRequest();
        request.setEmployeeId(employeeId);
        request.setLeaveTypeId(dto.getLeaveTypeId());
        request.setStartDate(dto.getStartDate());
        request.setEndDate(dto.getEndDate());
        request.setDays(dto.getDays());
        request.setReason(dto.getReason());
        request.setStatus("PENDING");
        leaveRequestMapper.insert(request);

        log.info("Leave request created: id={}, employeeId={}", request.getId(), employeeId);
        return request;
    }

    /**
     * Submit a leave request for approval.
     */
    @Transactional
    public void submit(Long id) {
        AtLeaveRequest request = leaveRequestMapper.selectById(id);
        if (request == null) {
            throw new BizException(BizCode.BAD_REQUEST, "请假单不存在: " + id);
        }
        if (!"PENDING".equals(request.getStatus())) {
            throw new BizException(BizCode.BAD_REQUEST, "当前状态不允许提交: " + request.getStatus());
        }

        Long instanceId = approvalService.start(
                "AT_LEAVE_REQUEST", request.getId().toString(), "请假申请", null, null);
        request.setApprovalInstanceId(instanceId);
        request.setStatus("SUBMITTED");
        leaveRequestMapper.updateById(request);

        log.info("Leave request submitted: id={}, instanceId={}", id, instanceId);
    }

    /**
     * Called by the approval event listener when the request is approved.
     * Deducts the leave balance.
     */
    @Transactional
    public void onApproved(Long formId) {
        AtLeaveRequest request = leaveRequestMapper.selectById(formId);
        if (request == null) {
            log.warn("Leave request not found for approval: {}", formId);
            return;
        }

        int year = request.getStartDate().getYear();
        AtLeaveBalance balance = leaveBalanceService.getBalanceForEmployeeAndType(
                request.getEmployeeId(), request.getLeaveTypeId(), year);
        if (balance != null) {
            leaveBalanceService.deduct(balance.getId(), request.getDays(), request.getId());
        }

        request.setStatus("APPROVED");
        leaveRequestMapper.updateById(request);
        log.info("Leave request approved: id={}", formId);
    }

    /**
     * Called by the approval event listener when the request is rejected.
     */
    @Transactional
    public void onRejected(Long formId) {
        AtLeaveRequest request = leaveRequestMapper.selectById(formId);
        if (request == null) {
            log.warn("Leave request not found for rejection: {}", formId);
            return;
        }

        request.setStatus("REJECTED");
        leaveRequestMapper.updateById(request);
        log.info("Leave request rejected: id={}", formId);
    }

    /**
     * List leave requests with optional filters.
     */
    public IPage<AtLeaveRequest> list(Long employeeId, String status, Page<AtLeaveRequest> page) {
        LambdaQueryWrapper<AtLeaveRequest> wrapper = new LambdaQueryWrapper<>();
        if (employeeId != null) {
            wrapper.eq(AtLeaveRequest::getEmployeeId, employeeId);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(AtLeaveRequest::getStatus, status);
        }
        wrapper.orderByDesc(AtLeaveRequest::getCreatedAt);
        return leaveRequestMapper.selectPage(page, wrapper);
    }

    /**
     * Get a single leave request by ID.
     */
    public AtLeaveRequest getById(Long id) {
        return leaveRequestMapper.selectById(id);
    }
}
