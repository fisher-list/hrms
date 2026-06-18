package com.hrms.common.user;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hrms.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * System user entity (see data-model §7.1.1).  Only fields needed by the auth flow
 * are populated for this story; remaining columns (real_name / email / phone / etc.)
 * are left for the RBAC story.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseEntity {

    private String username;

    private String passwordHash;

    private String nickname;

    private String email;

    private String phone;

    private Long employeeId;

    /** {@code ACTIVE} / {@code LOCKED} / {@code DISABLED}. */
    private String status;

    private LocalDateTime lastLoginAt;

    private String lastLoginIp;

    private Integer failedAttempts;

    private LocalDateTime lockedUntil;

    private LocalDateTime passwordChangedAt;
}
