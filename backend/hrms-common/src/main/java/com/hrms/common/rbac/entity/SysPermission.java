package com.hrms.common.rbac.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hrms.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Permission entity mapped to sys_permission.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_permission")
public class SysPermission extends BaseEntity {

    private String code;

    private String name;

    /** MENU / BUTTON / API */
    private String type;

    private Long parentId;

    private Integer sortNo;
}
