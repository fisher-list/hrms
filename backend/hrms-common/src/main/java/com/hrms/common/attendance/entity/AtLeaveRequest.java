package com.hrms.common.attendance.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hrms.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Leave request entity.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("at_leave_request")
public class AtLeaveRequest extends BaseEntity {

    private Long employeeId;

    private Long leaveTypeId;

    private LocalDate startDate;

    private LocalDate endDate;

    /** Total leave days (supports 0.5 increments) */
    private BigDecimal days;

    private String reason;

    /** PENDING / SUBMITTED / APPROVED / REJECTED / CANCELLED */
    private String status;

    /** Reference to approval_instance.id after submission */
    private Long approvalInstanceId;
}
