package com.hrms.common.rbac.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hrms.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Role entity mapped to sys_role.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role")
public class SysRole extends BaseEntity {

    private String code;

    private String name;

    private String description;

    private Boolean builtin;

    private Boolean enabled;

    /** Data scope: ALL, OWN_DEPT, SUBORDINATE_TREE, SELF_ONLY */
    private String dataScope;
}
