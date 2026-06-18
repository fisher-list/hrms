package com.hrms.common.payroll.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 年终奖创建/计算DTO。
 * 支持单个员工或批量计算。
 */
@Data
public class YearEndBonusCreateDto {

    /** 员工ID列表（批量计算时使用） */
    private List<Long> employeeIds;

    /** 单个员工ID（与employeeIds二选一） */
    private Long employeeId;

    /** 奖励年度 */
    @NotNull
    private Integer bonusYear;

    /** 奖励月数，默认 1.0 */
    private BigDecimal bonusMonths;

    /** 出勤系数，默认 1.0 */
    private BigDecimal attendanceCoefficient;

    /**
     * 计税方式：
     * STANDALONE - 单独计税
     * MERGED - 并入综合所得
     * THIRTEENTH - 十三薪
     */
    @NotNull
    private String taxMethod;
}
