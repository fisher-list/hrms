package com.hrms.common.attendance.dto;

import lombok.Data;

/**
 * 缺勤配额生成请求DTO。
 */
@Data
public class LeaveQuotaGenerateDto {

    /** 员工ID（单个生成时必填） */
    private Long employeeId;

    /** 生成年份 */
    private Integer year;
}
