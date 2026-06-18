package com.hrms.common.report.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 薪资汇总报表VO
 * 包含部门薪资对比、薪资结构分析、年度薪资趋势等数据
 */
@Data
public class PayrollSummaryVo {

    /** 部门ID */
    private Long deptId;

    /** 部门名称 */
    private String deptName;

    /** 月份, 格式: YYYY-MM */
    private String month;

    /** 该部门员工数 */
    private Integer employeeCount;

    /** 该部门基本薪资合计 */
    private BigDecimal totalBaseSalary;

    /** 该部门岗位工资合计 */
    private BigDecimal totalPositionSalary;

    /** 该部门绩效工资合计 */
    private BigDecimal totalPerformance;

    /** 该部门津贴合计 */
    private BigDecimal totalAllowance;

    /** 该部门应发工资合计 */
    private BigDecimal totalGrossPay;

    /** 该部门社保合计 */
    private BigDecimal totalSocialInsurance;

    /** 该部门公积金合计 */
    private BigDecimal totalHousingFund;

    /** 该部门个税合计 */
    private BigDecimal totalIit;

    /** 该部门实发工资合计 */
    private BigDecimal totalNetPay;

    /** 该部门人均薪资 */
    private BigDecimal avgGrossPay;
}
