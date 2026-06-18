package com.hrms.common.performance.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 薪酬调整建议VO。
 */
@Data
public class SalaryAdjustmentVo {

    private Long id;
    private Long employeeId;
    private String employeeName;
    private String empNo;
    private Long appraisalId;
    private Long cycleId;
    private String grade;
    private BigDecimal finalScore;
    private BigDecimal currentBaseSalary;
    private BigDecimal adjustmentPct;
    private BigDecimal suggestedSalary;
    private LocalDate effectiveDate;
    private String status;
    private String remark;
}
