package com.hrms.common.payroll.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hrms.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 工资模拟运行实体类。
 * 用于试算工资，不影响正式数据。支持调整参数后模拟，并与正式结果对比。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("py_payroll_simulation")
public class PyPayrollSimulation extends BaseEntity {

    /** 关联的正式薪酬期间ID */
    private Long periodId;

    /** 模拟名称/描述 */
    private String name;

    /** 模拟状态：DRAFT / CALCULATED */
    private String status;

    /** 社保比例调整（JSON字符串，null表示使用正式配置） */
    private String socialInsuranceRateOverride;

    /** 公积金比例调整（JSON字符串，null表示使用正式配置） */
    private String housingFundRateOverride;

    /** 参与模拟的员工数 */
    private Integer employeeCount;

    /** 模拟总应发 */
    private BigDecimal totalGross;

    /** 模拟总实发 */
    private BigDecimal totalNet;

    /** 是否与正式运行对比过 */
    private Boolean compared;
}
