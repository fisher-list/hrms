package com.hrms.common.attendance.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hrms.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 考勤异常记录实体。
 * 根据排班+打卡数据自动识别的异常类型：迟到、早退、旷工、漏打卡。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("at_attendance_anomaly")
public class AtAttendanceAnomaly extends BaseEntity {

    /** 员工ID */
    private Long employeeId;

    /** 异常日期 */
    private LocalDate anomalyDate;

    /**
     * 异常类型：
     * LATE      - 迟到（晚于排班开始时间+弹性时间）
     * EARLY     - 早退（早于排班结束时间-弹性时间）
     * ABSENT    - 旷工（有排班但无打卡记录）
     * MISSING   - 漏打卡（仅有上班卡或仅有下班卡）
     */
    private String anomalyType;

    /** 异常时长（分钟），旷工时为0 */
    private Integer durationMinutes;

    /**
     * 处理状态：
     * PENDING   - 待处理
     * HANDLED   - 已处理（补卡/说明/忽略）
     * IGNORED   - 已忽略
     */
    private String status;

    /** 处理说明（HR或管理员填写） */
    private String handleRemark;

    /** 关联的排班ID */
    private Long scheduleId;

    /** 关联的打卡记录ID */
    private Long punchId;
}
