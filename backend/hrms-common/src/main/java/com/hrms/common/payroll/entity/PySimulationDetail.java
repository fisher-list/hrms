package com.hrms.common.payroll.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hrms.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 工资模拟明细实体类。
 * 每行代表一个员工在模拟运行中的计算结果。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("py_simulation_detail")
public class PySimulationDetail extends BaseEntity {

    /** 关联的模拟运行ID */
    private Long simulationId;

    /** 员工ID */
    private Long employeeId;

    /** 员工姓名 */
    private String employeeName;

    /** 员工编号 */
    private String empNo;

    /** 模拟应发工资 */
    private BigDecimal grossPay;

    /** 模拟社保 */
    private BigDecimal socialInsurance;

    /** 模拟公积金 */
    private BigDecimal housingFund;

    /** 模拟个税 */
    private BigDecimal iit;

    /** 模拟实发工资 */
    private BigDecimal netPay;

    /** 对比正式运行的差异（netPay - formalNetPay），null表示未对比 */
    private BigDecimal diffNetPay;
}
