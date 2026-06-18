package com.hrms.common.attendance.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 考勤异常处理 DTO。
 * HR或管理员对异常记录进行处理/忽略操作。
 */
@Data
public class AnomalyHandleDto {

    /** 异常记录ID */
    @NotNull
    private Long anomalyId;

    /** 处理结果：HANDLED 或 IGNORED */
    @NotNull
    private String action;

    /** 处理说明 */
    @Size(max = 500)
    private String remark;
}
