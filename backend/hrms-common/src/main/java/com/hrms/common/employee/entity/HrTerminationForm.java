package com.hrms.common.employee.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hrms.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * Termination form: captures employee offboarding for approval workflow.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hr_termination_form")
public class HrTerminationForm extends BaseEntity {

    private String formNo;

    private Long employeeId;

    /** RESIGNATION / RETIREMENT / DISMISSAL */
    private String terminationType;

    private String reason;

    private LocalDate lastWorkingDay;

    /** PENDING / SUBMITTED / APPROVED / REJECTED */
    private String status;

    private Long approvalInstanceId;
}
