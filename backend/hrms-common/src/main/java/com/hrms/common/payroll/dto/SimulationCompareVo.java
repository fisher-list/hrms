package com.hrms.common.payroll.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 模拟与正式运行对比结果VO。
 */
@Data
public class SimulationCompareVo {

    /** 员工ID */
    private Long employeeId;

    /** 员工姓名 */
    private String employeeName;

    /** 员工编号 */
    private String empNo;

    /** 模拟实发 */
    private BigDecimal simulationNetPay;

    /** 正式实发 */
    private BigDecimal formalNetPay;

    /** 差异 = simulationNetPay - formalNetPay */
    private BigDecimal diffNetPay;

    /** 差异百分比 */
    private BigDecimal diffPercentage;
}
