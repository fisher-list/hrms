package com.hrms.common.rbac.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Set;

/**
 * VO returned by GET /api/me/permissions.
 */
@Data
@AllArgsConstructor
public class PermissionVo {

    private Set<String> permissions;
}
