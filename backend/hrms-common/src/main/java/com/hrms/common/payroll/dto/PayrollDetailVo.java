package com.hrms.common.payroll.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * VO for payroll detail / payslip.
 */
@Data
public class PayrollDetailVo {

    private String employeeName;

    private String empNo;

    private BigDecimal grossPay;

    private BigDecimal socialInsurance;

    private BigDecimal housingFund;

    private BigDecimal iit;

    private BigDecimal netPay;

    private Boolean isException;
}
