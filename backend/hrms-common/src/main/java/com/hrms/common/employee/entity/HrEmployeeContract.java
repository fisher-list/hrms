package com.hrms.common.employee.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hrms.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * Employee contract.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hr_employee_contract")
public class HrEmployeeContract extends BaseEntity {

    private Long employeeId;

    private String contractNo;

    /** FIXED / PERMANENT / INTERN etc. */
    private String contractType;

    private LocalDate startDate;

    private LocalDate endDate;

    private LocalDate signingDate;

    /** ACTIVE / EXPIRED / TERMINATED */
    private String status;
}
