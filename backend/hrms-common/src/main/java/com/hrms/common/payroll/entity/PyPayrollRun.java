package com.hrms.common.payroll.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hrms.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * Payroll run: a calculation execution for a given period.
 * Type: NORMAL / REVERSAL
 * Status: DRAFT -> CALCULATED -> LOCKED
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("py_payroll_run")
public class PyPayrollRun extends BaseEntity {

    private Long periodId;

    /** NORMAL / REVERSAL */
    private String runType;

    /** DRAFT / CALCULATED / LOCKED */
    private String status;

    /** Non-null for REVERSAL type, points to the original run. */
    private Long reverseOfRunId;

    private Integer employeeCount;

    private BigDecimal totalGross;

    private BigDecimal totalNet;
}
