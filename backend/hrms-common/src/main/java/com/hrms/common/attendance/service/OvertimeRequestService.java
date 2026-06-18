package com.hrms.common.attendance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hrms.common.api.BizCode;
import com.hrms.common.attendance.dto.OvertimeRequestCreateDto;
import com.hrms.common.attendance.entity.AtOvertimeRequest;
import com.hrms.common.attendance.mapper.AtOvertimeRequestMapper;
import com.hrms.common.approval.service.ApprovalService;
import com.hrms.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for overtime request lifecycle: create, submit, approve/reject, list.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OvertimeRequestService {

    private final AtOvertimeRequestMapper overtimeRequestMapper;
    private final ApprovalService approvalService;

    /**
     * Create an overtime request. Status is set to PENDING.
     */
    @Transactional
    public AtOvertimeRequest create(OvertimeRequestCreateDto dto, Long employeeId) {
        AtOvertimeRequest request = new AtOvertimeRequest();
        request.setEmployeeId(employeeId);
        request.setOvertimeDate(dto.getOvertimeDate());
        request.setHours(dto.getHours());
        request.setReason(dto.getReason());
        request.setStatus("PENDING");
        overtimeRequestMapper.insert(request);

        log.info("Overtime request created: id={}, employeeId={}", request.getId(), employeeId);
        return request;
    }

    /**
     * Submit an overtime request for approval.
     */
    @Transactional
    public void submit(Long id) {
        AtOvertimeRequest request = overtimeRequestMapper.selectById(id);
        if (request == null) {
            throw new BizException(BizCode.BAD_REQUEST, "加班单不存在: " + id);
        }
        if (!"PENDING".equals(request.getStatus())) {
            throw new BizException(BizCode.BAD_REQUEST, "当前状态不允许提交: " + request.getStatus());
        }

        Long instanceId = approvalService.start(
                "AT_OVERTIME_REQUEST", request.getId().toString(), "加班申请", null, null);
        request.setApprovalInstanceId(instanceId);
        request.setStatus("SUBMITTED");
        overtimeRequestMapper.updateById(request);

        log.info("Overtime request submitted: id={}, instanceId={}", id, instanceId);
    }

    /**
     * Called by the approval event listener when the request is approved.
     * Only records status; no balance deduction.
     */
    @Transactional
    public void onApproved(Long formId) {
        AtOvertimeRequest request = overtimeRequestMapper.selectById(formId);
        if (request == null) {
            log.warn("Overtime request not found for approval: {}", formId);
            return;
        }

        request.setStatus("APPROVED");
        overtimeRequestMapper.updateById(request);
        log.info("Overtime request approved: id={}", formId);
    }

    /**
     * Called by the approval event listener when the request is rejected.
     */
    @Transactional
    public void onRejected(Long formId) {
        AtOvertimeRequest request = overtimeRequestMapper.selectById(formId);
        if (request == null) {
            log.warn("Overtime request not found for rejection: {}", formId);
            return;
        }

        request.setStatus("REJECTED");
        overtimeRequestMapper.updateById(request);
        log.info("Overtime request rejected: id={}", formId);
    }

    /**
     * List overtime requests with optional filters.
     */
    public IPage<AtOvertimeRequest> list(Long employeeId, String status, Page<AtOvertimeRequest> page) {
        LambdaQueryWrapper<AtOvertimeRequest> wrapper = new LambdaQueryWrapper<>();
        if (employeeId != null) {
            wrapper.eq(AtOvertimeRequest::getEmployeeId, employeeId);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(AtOvertimeRequest::getStatus, status);
        }
        wrapper.orderByDesc(AtOvertimeRequest::getCreatedAt);
        return overtimeRequestMapper.selectPage(page, wrapper);
    }
}
