package com.hrms.common.employee.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hrms.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * Hire form: captures new employee data for approval workflow.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hr_hire_form")
public class HrHireForm extends BaseEntity {

    private String formNo;

    /** JSON snapshot of hire data. */
    private String employeeSnapshot;

    private Long positionId;

    private Long deptId;

    private LocalDate hireDate;

    private String accountName;

    /** PENDING / SUBMITTED / APPROVED / REJECTED */
    private String status;

    private Long approvalInstanceId;
}
