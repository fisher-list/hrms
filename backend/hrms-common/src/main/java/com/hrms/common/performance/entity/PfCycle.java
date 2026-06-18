package com.hrms.common.performance.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hrms.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * Performance cycle: name, date range, status, and scope.
 * Status: DRAFT / ACTIVE / CLOSED
 * Scope: ALL / DEPT
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pf_cycle")
public class PfCycle extends BaseEntity {

    private String name;

    private LocalDate startDate;

    private LocalDate endDate;

    /** DRAFT / ACTIVE / CLOSED */
    private String status;

    /** ALL / DEPT */
    private String scopeType;

    /** Comma-separated department IDs when scopeType=DEPT */
    private String scopeDepts;
}
