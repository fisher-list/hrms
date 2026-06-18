package com.hrms.common.performance.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hrms.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 绩效联动薪酬调整建议单。
 * <p>
 * 绩效考核完成后，根据最终得分自动关联薪酬调整建议。
 * grade: S / A / B / C / D
 * status: PENDING / APPROVED / REJECTED / EXECUTED
 * </p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pf_salary_adjustment")
public class PfSalaryAdjustment extends BaseEntity {

    /** 关联员工ID */
    private Long employeeId;

    /** 关联考核ID */
    private Long appraisalId;

    /** 关联绩效周期ID */
    private Long cycleId;

    /** 绩效等级: S / A / B / C / D */
    private String grade;

    /** 绩效最终得分 */
    private BigDecimal finalScore;

    /** 当前基本工资 */
    private BigDecimal currentBaseSalary;

    /** 建议调薪比例（如10.00表示10%） */
    private BigDecimal adjustmentPct;

    /** 建议调整后工资 */
    private BigDecimal suggestedSalary;

    /** 生效日期 */
    private LocalDate effectiveDate;

    /** PENDING / APPROVED / REJECTED / EXECUTED */
    private String status;

    /** 备注 */
    private String remark;
}
