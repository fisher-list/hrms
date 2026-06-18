package com.hrms.common.org.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.hrms.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Department entity mapped to department table.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("department")
public class Department extends BaseEntity {

    private Long companyId;

    private Long parentId;

    private String name;

    private String code;

    /** Materialized path, e.g. /1/2/3 */
    private String path;

    private Integer level;

    private Long headId;

    @TableField("sort_no")
    private Integer sortOrder;
}
