package com.hrms.common.rbac.aspect;

import com.hrms.common.rbac.annotation.DataScope;
import com.hrms.common.rbac.annotation.DataScopeType;
import com.hrms.common.rbac.datascope.DataScopeContext;
import com.hrms.common.rbac.datascope.DepartmentTreeService;
import com.hrms.common.rbac.service.PermissionService;
import com.hrms.common.security.LoginUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * AOP aspect that sets up a {@link DataScopeContext} before the target method runs
 * and clears it afterwards.
 *
 * <p>The effective scope type is determined by:
 * <ol>
 *   <li>The user's <em>dominant</em> role data_scope (most permissive across all roles):
 *     <ul>
 *       <li>ALL &rarr; no row filter</li>
 *       <li>SUBORDINATE_TREE &rarr; dept + sub-depts</li>
 *       <li>OWN_DEPT &rarr; own department only</li>
 *       <li>SELF_ONLY &rarr; own rows only</li>
 *     </ul>
 *   </li>
 *   <li>If the {@link DataScope} annotation specifies a <em>more restrictive</em> scope,
 *       the annotation value wins.</li>
 * </ol>
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class DataScopeAspect {

    private final PermissionService permissionService;
    private final DepartmentTreeService departmentTreeService;

    @Around("@annotation(dataScope)")
    public Object applyDataScope(ProceedingJoinPoint joinPoint, DataScope dataScope) throws Throwable {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof LoginUser loginUser)) {
            return joinPoint.proceed();
        }

        Long userId = loginUser.getUid();

        DataScopeContext ctx = new DataScopeContext();
        ctx.setUserId(userId);
        ctx.setDeptField(dataScope.deptField());
        ctx.setEmployeeField(dataScope.employeeField());

        // Resolve effective scope: annotation cannot grant broader access than role allows
        DataScopeType resolved = resolveScopeType(userId, dataScope.type());
        ctx.setScopeType(resolved);

        Long employeeId = loginUser.getEmployeeId();
        ctx.setEmployeeId(employeeId);

        Long deptId = resolveDeptId(employeeId);
        ctx.setDeptId(deptId);

        // Populate subordinate dept ids for SUBORDINATE_TREE scope
        if (resolved == DataScopeType.SUBORDINATE_TREE && deptId != null) {
            List<Long> subIds = departmentTreeService.getSubordinateDeptIds(deptId);
            ctx.setSubordinateDeptIds(subIds);
        }
        if (mustDenyAll(resolved, employeeId, deptId, ctx.getSubordinateDeptIds())) {
            ctx.setDenyAll(true);
        }

        DataScopeContext.set(ctx);
        try {
            return joinPoint.proceed();
        } finally {
            DataScopeContext.clear();
        }
    }

    /**
     * Resolve the effective scope type.
     *
     * <p>Algorithm:
     * <ol>
     *   <li>Get the user's dominant (most permissive) scope from their roles.</li>
     *   <li>If the annotation specifies a more restrictive scope, use the annotation value.</li>
     *   <li>Otherwise, use the role-based scope.</li>
     * </ol>
     *
     * <p>Comparison uses explicit precedence: ALL(0) &gt; SUBORDINATE_TREE(1) &gt; OWN_DEPT(2) &gt; SELF_ONLY(3).
     * A higher precedence value means more restrictive.
     */
    private DataScopeType resolveScopeType(Long userId, DataScopeType annotated) {
        DataScopeType roleBased = permissionService.getDominantDataScope(userId);

        // If annotation is more restrictive than role-based, use annotation
        if (precedence(annotated) > precedence(roleBased)) {
            return annotated;
        }
        // Otherwise, role-based scope is the effective limit
        return roleBased;
    }

    /**
     * Resolve the department ID for the given user by looking up their employee record.
     * Returns null if no employee record is found (e.g. admin without an employee profile).
     *
     * <p>Uses a direct SQL query to avoid circular service dependencies.</p>
     */
    private Long resolveDeptId(Long employeeId) {
        // This is intentionally a lightweight mapper lookup to avoid injecting
        // the full EmployeeService and creating a circular dependency.
        try {
            return departmentTreeService.getDeptIdForEmployee(employeeId);
        } catch (Exception e) {
            log.debug("Could not resolve dept_id for employee {}: {}", employeeId, e.getMessage());
            return null;
        }
    }

    private int precedence(DataScopeType type) {
        return switch (type) {
            case ALL -> 0;
            case SUBORDINATE_TREE -> 1;
            case OWN_DEPT -> 2;
            case SELF_ONLY -> 3;
        };
    }

    private boolean mustDenyAll(DataScopeType scope, Long employeeId, Long deptId, List<Long> subordinateDeptIds) {
        return switch (scope) {
            case ALL -> false;
            case SELF_ONLY -> employeeId == null;
            case OWN_DEPT -> deptId == null;
            case SUBORDINATE_TREE -> deptId == null || subordinateDeptIds == null || subordinateDeptIds.isEmpty();
        };
    }
}
