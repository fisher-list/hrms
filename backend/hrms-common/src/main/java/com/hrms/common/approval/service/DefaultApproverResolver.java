package com.hrms.common.approval.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hrms.common.approval.entity.ApprovalInstance;
import com.hrms.common.rbac.entity.SysUserRole;
import com.hrms.common.rbac.mapper.SysUserRoleMapper;
import com.hrms.common.user.SysUser;
import com.hrms.common.user.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Default implementation of {@link ApproverResolver}.
 *
 * <p>Supported rules:
 * <ul>
 *   <li>{@code EMP_MANAGER} — look up the applicant's manager; fallback to admin (id=1)</li>
 *   <li>{@code ROLE:<code>} — find an active user with the given role, excluding resigned</li>
 *   <li>{@code USER:<id>} — direct user id</li>
 * </ul>
 *
 * Returns {@code null} if no eligible approver is found (triggers SUSPEND).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultApproverResolver implements ApproverResolver {

    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;

    @Override
    public Long resolve(String approverRule, Long applicantId, ApprovalInstance instance) {
        if (approverRule == null || approverRule.isBlank()) {
            return null;
        }

        String rule = approverRule.trim();

        if ("EMP_MANAGER".equals(rule)) {
            return resolveEmpManager(applicantId);
        } else if (rule.startsWith("ROLE:")) {
            return resolveByRole(rule.substring("ROLE:".length()).trim());
        } else if (rule.startsWith("USER:")) {
            return Long.parseLong(rule.substring("USER:".length()).trim());
        }

        log.warn("Unknown approver rule: {}", rule);
        return null;
    }

    /**
     * Resolve by applicant's manager_id. Falls back to admin (id=1) since
     * the employee table (S04) is not yet built.
     */
    private Long resolveEmpManager(Long applicantId) {
        // TODO(S05): look up the applicant's manager via employee table
        log.debug("EMP_MANAGER: no employee table yet, falling back to admin for user {}", applicantId);
        return 1L;
    }

    /**
     * Find an active (non-resigned) user who holds the given role.
     * If multiple users match, returns the first one found.
     * If all users with the role have resigned, returns null (triggers SUSPEND).
     */
    private Long resolveByRole(String roleCode) {
        List<SysUserRole> userRoles = userRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>()
                        .inSql(SysUserRole::getRoleId,
                                "SELECT id FROM sys_role WHERE code = '" + roleCode + "' AND deleted = false AND enabled = true"));

        if (userRoles == null || userRoles.isEmpty()) {
            log.warn("No users found with role {}", roleCode);
            return null;
        }

        List<Long> userIds = userRoles.stream()
                .map(SysUserRole::getUserId)
                .distinct()
                .collect(Collectors.toList());

        List<SysUser> activeUsers = userMapper.selectList(
                new LambdaQueryWrapper<SysUser>()
                        .in(SysUser::getId, userIds)
                        .eq(SysUser::getDeleted, false));

        if (activeUsers.isEmpty()) {
            log.warn("All users with role {} have been resigned/deleted", roleCode);
            return null;
        }

        return activeUsers.get(0).getId();
    }
}
