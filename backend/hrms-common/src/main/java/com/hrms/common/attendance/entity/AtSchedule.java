package com.hrms.common.attendance.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hrms.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * Schedule assignment entity: links an employee to a shift on a specific date.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("at_schedule")
public class AtSchedule extends BaseEntity {

    private Long employeeId;

    private LocalDate scheduleDate;

    private Long shiftId;
}
