package com.hrms.common.attendance.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 加班转调休 DTO。
 * 将已审批的加班申请转换为调休余额。
 */
@Data
public class OvertimeToCompLeaveDto {

    /** 已审批通过的加班申请ID */
    @NotNull
    private Long overtimeRequestId;

    /**
     * 转换倍率，如1.5表示加班1天转1.5天调休。
     * 默认为1.0。
     */
    @NotNull
    private BigDecimal convertRate;

    /** 备注 */
    @Size(max = 500)
    private String remark;
}
