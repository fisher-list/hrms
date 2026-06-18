package com.hrms.common.employee.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 试用期转正申请创建DTO。
 */
@Data
public class ProbationConversionCreateDto {

    /** 员工ID */
    @NotNull(message = "员工ID不能为空")
    private Long employeeId;

    /** 转正评估说明/自评 */
    private String evaluationRemark;

    /** 计划转正日期（可选，默认为试用期结束日期） */
    private LocalDate plannedConversionDate;

    /** 评估得分（可选） */
    private Integer evaluationScore;
}
