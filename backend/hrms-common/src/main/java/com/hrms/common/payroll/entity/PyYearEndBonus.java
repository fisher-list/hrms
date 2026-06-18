package com.hrms.common.payroll.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hrms.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 年终奖实体类。
 * 支持三种模式：
 * - STANDALONE: 单独计税（年终奖除以12找税率档）
 * - MERGED: 并入综合所得计税
 * - THIRTEENTH: 十三薪（固定一个月基本工资）
 *
 * 计算公式：年终奖 = 基本工资 × 奖励月数 × 出勤系数
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("py_year_end_bonus")
public class PyYearEndBonus extends BaseEntity {

    /** 员工ID */
    private Long employeeId;

    /** 员工姓名（冗余，便于查询） */
    private String employeeName;

    /** 员工编号（冗余，便于查询） */
    private String empNo;

    /** 所属年度，如 2026 */
    private Integer bonusYear;

    /** 基本工资（取自薪酬档案） */
    private BigDecimal baseSalary;

    /** 奖励月数，如 1.0 表示一个月 */
    private BigDecimal bonusMonths;

    /** 出勤系数，范围 0~1，默认 1.0 */
    private BigDecimal attendanceCoefficient;

    /** 计算出的应发奖金金额 = baseSalary * bonusMonths * attendanceCoefficient */
    private BigDecimal bonusAmount;

    /** 计税方式：STANDALONE / MERGED / THIRTEENTH */
    private String taxMethod;

    /** 个人所得税金额 */
    private BigDecimal taxAmount;

    /** 税后实发金额 = bonusAmount - taxAmount */
    private BigDecimal netAmount;

    /** 状态：DRAFT / CALCULATED / PAID */
    private String status;
}
