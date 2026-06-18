package com.hrms.common.rbac;

import com.hrms.common.api.BizCode;
import com.hrms.common.exception.BizException;
import com.hrms.common.rbac.annotation.HasPermission;
import com.hrms.common.rbac.aspect.HasPermissionAspect;
import com.hrms.common.rbac.service.PermissionService;
import com.hrms.common.security.LoginUser;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link HasPermissionAspect}.
 */
class HasPermissionAspectTest {

    private PermissionService permissionService;
    private HasPermissionAspect aspect;
    private ProceedingJoinPoint joinPoint;
    private HasPermission hasPermission;

    @BeforeEach
    void setUp() {
        permissionService = mock(PermissionService.class);
        aspect = new HasPermissionAspect(permissionService);
        joinPoint = mock(ProceedingJoinPoint.class);
        hasPermission = mock(HasPermission.class);

        // Clear security context
        SecurityContextHolder.clearContext();
    }

    private void setSecurityContext(Long uid, String username) {
        LoginUser loginUser = new LoginUser(uid, username, Set.of());
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(loginUser);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("User with required permission passes check")
    void userWithPermission_passesCheck() throws Throwable {
        setSecurityContext(10L, "alice");
        when(hasPermission.value()).thenReturn("role:view");
        when(permissionService.userHasPermission(10L, "role:view")).thenReturn(true);
        when(joinPoint.proceed()).thenReturn("ok");

        Object result = aspect.checkPermission(joinPoint, hasPermission);
        assertThat(result).isEqualTo("ok");
    }

    @Test
    @DisplayName("User without required permission throws 403")
    void userWithoutPermission_throws403() throws Throwable {
        setSecurityContext(10L, "alice");
        when(hasPermission.value()).thenReturn("role:delete");
        when(permissionService.userHasPermission(10L, "role:delete")).thenReturn(false);

        assertThatThrownBy(() -> aspect.checkPermission(joinPoint, hasPermission))
                .isInstanceOf(BizException.class)
                .extracting("code")
                .isEqualTo(BizCode.FORBIDDEN);
    }

    @Test
    @DisplayName("Admin with wildcard permission passes check via service")
    void adminWithWildcard_passesCheck() throws Throwable {
        setSecurityContext(1L, "admin");
        when(hasPermission.value()).thenReturn("anything:anything");
        when(permissionService.userHasPermission(1L, "anything:anything")).thenReturn(true);
        when(joinPoint.proceed()).thenReturn("admin-ok");

        Object result = aspect.checkPermission(joinPoint, hasPermission);
        assertThat(result).isEqualTo("admin-ok");
    }

    @Test
    @DisplayName("No authentication context throws UNAUTHORIZED")
    void noAuth_throwsUnauthorized() throws Throwable {
        SecurityContextHolder.clearContext();
        when(hasPermission.value()).thenReturn("role:view");

        assertThatThrownBy(() -> aspect.checkPermission(joinPoint, hasPermission))
                .isInstanceOf(BizException.class)
                .extracting("code")
                .isEqualTo(BizCode.UNAUTHORIZED);
    }
}
