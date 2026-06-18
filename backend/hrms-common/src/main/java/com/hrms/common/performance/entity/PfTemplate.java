package com.hrms.common.performance.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hrms.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Performance template: name, description.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pf_template")
public class PfTemplate extends BaseEntity {

    private String name;

    private String description;
}
