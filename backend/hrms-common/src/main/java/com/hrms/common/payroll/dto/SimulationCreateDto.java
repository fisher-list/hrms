package com.hrms.common.payroll.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 工资模拟运行创建DTO。
 */
@Data
public class SimulationCreateDto {

    /** 关联的薪酬期间ID */
    @NotNull
    private Long periodId;

    /** 模拟名称/描述 */
    @NotBlank
    private String name;

    /** 社保个人比例覆盖值（如 0.105），null表示使用正式配置 */
    private BigDecimal socialInsuranceRateOverride;

    /** 公积金个人比例覆盖值（如 0.12），null表示使用正式配置 */
    private BigDecimal housingFundRateOverride;
}
