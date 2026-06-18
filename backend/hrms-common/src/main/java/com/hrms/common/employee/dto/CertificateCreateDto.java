package com.hrms.common.employee.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 证明开具申请DTO。
 */
@Data
public class CertificateCreateDto {

    /** 证明类型：EMPLOYMENT / INCOME */
    @NotBlank
    private String type;

    /** 用途说明 */
    @NotBlank
    private String purpose;

    /** 需要份数 */
    @NotNull
    private Integer copies;

    /** 收入证明专用：起始日期 */
    private LocalDate incomeStartDate;

    /** 收入证明专用：截止日期 */
    private LocalDate incomeEndDate;
}
