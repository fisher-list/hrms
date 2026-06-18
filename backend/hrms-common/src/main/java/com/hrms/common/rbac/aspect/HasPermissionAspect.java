package com.hrms.common.rbac.aspect;

import com.hrms.common.api.BizCode;
import com.hrms.common.exception.BizException;
import com.hrms.common.rbac.annotation.HasPermission;
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

/**
 * AOP aspect that enforces {@link HasPermission} at the method level.
 *
 * <p>Delegates to {@link PermissionService#userHasPermission} — users with the
 * {@code *} wildcard permission in the database are treated as super-admins.</p>
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class HasPermissionAspect {

    private final PermissionService permissionService;

    @Around("@annotation(hasPermission)")
    public Object checkPermission(ProceedingJoinPoint joinPoint, HasPermission hasPermission) throws Throwable {
        String required = hasPermission.value();

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof LoginUser loginUser)) {
            throw new BizException(BizCode.UNAUTHORIZED, "未登录");
        }

        // All permission checks (including super-admin via "*") go through the service
        if (!permissionService.userHasPermission(loginUser.getUid(), required)) {
            log.warn("user {} denied permission {}", loginUser.getUsername(), required);
            throw new BizException(BizCode.FORBIDDEN, "无权限: " + required);
        }

        return joinPoint.proceed();
    }
}
