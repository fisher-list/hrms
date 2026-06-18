package com.hrms.common.attendance.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hrms.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Shift template entity.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("at_shift")
public class AtShift extends BaseEntity {

    private String shiftName;

    /** HH:mm format */
    private String startTime;

    /** HH:mm format */
    private String endTime;

    /** Flex tolerance in minutes */
    private Integer flexMinutes;

    private Boolean isNightShift;

    private String remark;
}
