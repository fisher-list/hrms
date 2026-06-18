package com.hrms.common.org.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hrms.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Position entity mapped to position table.
 *
 * <p>A position is a concrete seat within a department that belongs to a job family.</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("position")
public class Position extends BaseEntity {

    private Long deptId;

    private Long jobId;

    private String name;

    private String code;

    private Integer headcount;

    private Integer occupied;
}
