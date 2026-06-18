package com.hrms.common.payroll.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hrms.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Compensation master record for an employee.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("py_compensation_master")
public class PyCompensationMaster extends BaseEntity {

    private Long employeeId;

    private BigDecimal baseSalary;

    private BigDecimal positionSalary;

    private BigDecimal performanceBase;

    private BigDecimal allowance;

    private LocalDate effectiveDate;
}
