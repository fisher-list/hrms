package com.hrms.common.payroll.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hrms.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Payroll period (monthly), e.g. "2026-06".
 * Status: DRAFT -> OPEN -> LOCKED
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("py_payroll_period")
public class PyPayrollPeriod extends BaseEntity {

    /** Period month in "YYYY-MM" format. */
    private String periodMonth;

    /** DRAFT / OPEN / LOCKED */
    private String status;
}
