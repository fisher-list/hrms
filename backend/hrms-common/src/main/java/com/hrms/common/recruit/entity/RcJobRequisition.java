package com.hrms.common.recruit.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hrms.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * Job requisition entity — tracks open positions.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("rc_job_requisition")
public class RcJobRequisition extends BaseEntity {

    private Long positionId;

    private String title;

    private String description;

    private Integer headcount;

    private LocalDate deadline;

    /** OPEN / CLOSED */
    private String status;
}
