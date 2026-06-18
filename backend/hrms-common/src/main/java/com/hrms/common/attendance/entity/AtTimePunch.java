package com.hrms.common.attendance.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hrms.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * Time punch record entity.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("at_time_punch")
public class AtTimePunch extends BaseEntity {

    private Long employeeId;

    private LocalDate punchDate;

    /** HH:mm format */
    private String clockIn;

    /** HH:mm format */
    private String clockOut;

    /** MANUAL or CSV */
    private String source;
}
