package com.hrms.common.payroll.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hrms.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * Cumulative tax withholding ledger per employee per year.
 * Used for the progressive withholding method.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("py_cumulative_tax_ledger")
public class PyCumulativeTaxLedger extends BaseEntity {

    private Long employeeId;

    private Integer taxYear;

    private BigDecimal cumulativeGross;

    private BigDecimal cumulativeSocial;

    private BigDecimal cumulativeHousingFund;

    private BigDecimal cumulativeIit;
}
