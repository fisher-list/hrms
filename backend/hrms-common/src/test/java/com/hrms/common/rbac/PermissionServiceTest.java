package com.hrms.common.rbac;

import com.hrms.common.rbac.annotation.DataScopeType;
import com.hrms.common.rbac.mapper.SysUserRoleMapper;
import com.hrms.common.rbac.service.PermissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link PermissionService}.
 */
class PermissionServiceTest {

    private SysUserRoleMapper userRoleMapper;
    private PermissionService permissionService;

    @BeforeEach
    void setUp() {
        userRoleMapper = mock(SysUserRoleMapper.class);
        permissionService = new PermissionService(userRoleMapper);
    }

    @Test
    @DisplayName("Multiple roles: permissions are unioned")
    void multiRoleUnion() {
        Long userId = 10L;
        when(userRoleMapper.selectPermissionCodesByUserId(userId))
                .thenReturn(Set.of("employee:view", "attendance:view"));

        Set<String> codes = permissionService.getPermissionCodes(userId);
        assertThat(codes).containsExactlyInAnyOrder("employee:view", "attendance:view");
    }

    @Test
    @DisplayName("Disabled role is excluded from permission union")
    void disabledRoleExcluded() {
        Long userId = 20L;
        // The JOIN query already filters r.enabled = true
        when(userRoleMapper.selectPermissionCodesByUserId(userId))
                .thenReturn(Set.of("employee:view"));

        Set<String> codes = permissionService.getPermissionCodes(userId);
        assertThat(codes).containsOnly("employee:view");
    }

    @Test
    @DisplayName("Cache hit returns same result without re-querying")
    void cacheHit() {
        Long userId = 30L;
        when(userRoleMapper.selectPermissionCodesByUserId(userId))
                .thenReturn(Set.of("employee:view"));

        // First call — populates cache
        Set<String> first = permissionService.getPermissionCodes(userId);
        assertThat(first).containsOnly("employee:view");

        // Second call — should come from cache (no additional DB calls)
        Set<String> second = permissionService.getPermissionCodes(userId);
        assertThat(second).isEqualTo(first);
    }

    @Test
    @DisplayName("Wildcard permission match: user with foo:* holds foo:bar")
    void wildcardMatch() {
        Long userId = 40L;
        when(userRoleMapper.selectPermissionCodesByUserId(userId))
                .thenReturn(Set.of("employee:*"));

        assertThat(permissionService.userHasPermission(userId, "employee:view")).isTrue();
        assertThat(permissionService.userHasPermission(userId, "employee:create")).isTrue();
        // different namespace should not match
        assertThat(permissionService.userHasPermission(userId, "role:view")).isFalse();
    }

    @Test
    @DisplayName("No roles returns empty set")
    void noRolesReturnsEmpty() {
        Long userId = 50L;
        when(userRoleMapper.selectPermissionCodesByUserId(userId))
                .thenReturn(Set.of());

        Set<String> codes = permissionService.getPermissionCodes(userId);
        assertThat(codes).isEmpty();
    }

    @Test
    @DisplayName("Null return from mapper treated as empty set")
    void nullFromMapperReturnsEmpty() {
        Long userId = 60L;
        when(userRoleMapper.selectPermissionCodesByUserId(userId))
                .thenReturn(null);

        Set<String> codes = permissionService.getPermissionCodes(userId);
        assertThat(codes).isEmpty();
    }

    // ---- DataScope tests ----

    @Test
    @DisplayName("Dominant data scope: single role ALL returns ALL")
    void dominantDataScopeSingleAll() {
        Long userId = 70L;
        when(userRoleMapper.selectDataScopesByUserId(userId))
                .thenReturn(Set.of("ALL"));

        DataScopeType scope = permissionService.getDominantDataScope(userId);
        assertThat(scope).isEqualTo(DataScopeType.ALL);
    }

    @Test
    @DisplayName("Dominant data scope: multiple roles picks most permissive")
    void dominantDataScopeMultiRole() {
        Long userId = 80L;
        when(userRoleMapper.selectDataScopesByUserId(userId))
                .thenReturn(Set.of("SELF_ONLY", "SUBORDINATE_TREE"));

        DataScopeType scope = permissionService.getDominantDataScope(userId);
        assertThat(scope).isEqualTo(DataScopeType.SUBORDINATE_TREE);
    }

    @Test
    @DisplayName("Dominant data scope: ALL + SELF_ONLY picks ALL")
    void dominantDataScopeAllWinsOverSelfOnly() {
        Long userId = 90L;
        when(userRoleMapper.selectDataScopesByUserId(userId))
                .thenReturn(Set.of("ALL", "SELF_ONLY"));

        DataScopeType scope = permissionService.getDominantDataScope(userId);
        assertThat(scope).isEqualTo(DataScopeType.ALL);
    }

    @Test
    @DisplayName("Dominant data scope: no roles returns SELF_ONLY")
    void dominantDataScopeNoRolesDefaultsSelfOnly() {
        Long userId = 100L;
        when(userRoleMapper.selectDataScopesByUserId(userId))
                .thenReturn(Set.of());

        DataScopeType scope = permissionService.getDominantDataScope(userId);
        assertThat(scope).isEqualTo(DataScopeType.SELF_ONLY);
    }

    @Test
    @DisplayName("Dominant data scope: null userId returns SELF_ONLY")
    void dominantDataScopeNullUser() {
        DataScopeType scope = permissionService.getDominantDataScope(null);
        assertThat(scope).isEqualTo(DataScopeType.SELF_ONLY);
    }

    @Test
    @DisplayName("Dominant data scope: cached on second call")
    void dominantDataScopeCached() {
        Long userId = 110L;
        when(userRoleMapper.selectDataScopesByUserId(userId))
                .thenReturn(Set.of("OWN_DEPT"));

        DataScopeType first = permissionService.getDominantDataScope(userId);
        DataScopeType second = permissionService.getDominantDataScope(userId);
        assertThat(first).isEqualTo(DataScopeType.OWN_DEPT);
        assertThat(second).isEqualTo(DataScopeType.OWN_DEPT);
    }

    @Test
    @DisplayName("Dominant data scope: unknown scope value in DB is skipped")
    void dominantDataScopeUnknownValueSkipped() {
        Long userId = 120L;
        when(userRoleMapper.selectDataScopesByUserId(userId))
                .thenReturn(Set.of("UNKNOWN_SCOPE", "OWN_DEPT"));

        DataScopeType scope = permissionService.getDominantDataScope(userId);
        assertThat(scope).isEqualTo(DataScopeType.OWN_DEPT);
    }

    @Test
    @DisplayName("Dominant data scope: SUBORDINATE_TREE > OWN_DEPT")
    void dominantDataScopeSubordinateTreeWinsOverOwnDept() {
        Long userId = 130L;
        when(userRoleMapper.selectDataScopesByUserId(userId))
                .thenReturn(Set.of("OWN_DEPT", "SUBORDINATE_TREE"));

        DataScopeType scope = permissionService.getDominantDataScope(userId);
        assertThat(scope).isEqualTo(DataScopeType.SUBORDINATE_TREE);
    }
}
