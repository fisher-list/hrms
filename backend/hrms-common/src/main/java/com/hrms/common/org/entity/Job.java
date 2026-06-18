package com.hrms.common.org.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hrms.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * Job entity mapped to job table.
 *
 * <p>A job represents a grade / rank within a sequence (M/P/T).</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("job")
public class Job extends BaseEntity {

    private String code;

    private String name;

    /** Sequence: M (management), P (professional), T (technical) */
    private String sequence;

    private Integer grade;

    private BigDecimal minSalary;

    private BigDecimal maxSalary;
}
