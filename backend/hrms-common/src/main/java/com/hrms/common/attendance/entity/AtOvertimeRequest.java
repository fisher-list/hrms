package com.hrms.common.attendance.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hrms.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Overtime request entity.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("at_overtime_request")
public class AtOvertimeRequest extends BaseEntity {

    private Long employeeId;

    private LocalDate overtimeDate;

    /** Overtime hours */
    private BigDecimal hours;

    private String reason;

    /** PENDING / SUBMITTED / APPROVED / REJECTED / CANCELLED */
    private String status;

    /** Reference to approval_instance.id after submission */
    private Long approvalInstanceId;
}
