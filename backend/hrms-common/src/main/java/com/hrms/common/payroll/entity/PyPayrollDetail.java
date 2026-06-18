package com.hrms.common.payroll.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hrms.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * Payroll detail: one row per employee per run.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("py_payroll_detail")
public class PyPayrollDetail extends BaseEntity {

    private Long runId;

    private Long employeeId;

    private String employeeName;

    private String empNo;

    private BigDecimal grossPay;

    private BigDecimal socialInsurance;

    private BigDecimal housingFund;

    private BigDecimal iit;

    private BigDecimal netPay;

    private Boolean isException;
}
