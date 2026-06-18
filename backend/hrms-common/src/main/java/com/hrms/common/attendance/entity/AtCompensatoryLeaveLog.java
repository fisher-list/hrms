package com.hrms.common.attendance.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 调休余额变动日志实体。
 * 记录每次调休余额的增减变动。
 */
@Data
@TableName("at_compensatory_leave_log")
public class AtCompensatoryLeaveLog {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 调休余额ID */
    private Long compLeaveId;

    /**
     * 变动类型：
     * CONVERT   - 加班转调休（增加）
     * DEDUCT    - 调休申请扣减（减少）
     * REFUND    - 调休退回（增加）
     */
    private String changeType;

    /** 变动值（正数=增加，负数=减少） */
    private BigDecimal changeValue;

    /** 转换倍率（如1.5表示加班1天转1.5天调休） */
    private BigDecimal convertRate;

    /** 关联的加班申请ID */
    private Long overtimeRequestId;

    /** 关联的调休请假申请ID */
    private Long leaveRequestId;

    /** 备注 */
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;
}
