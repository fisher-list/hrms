package com.hrms.common.rbac.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.hrms.common.rbac.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Central RBAC permission lookup with Caffeine caching.
 *
 * <p>Cache layout: {@code userId -> Set<permissionCode>}, TTL 5 minutes.
 * Uses a single JOIN query to load all permission codes at once.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionService {

    private final SysUserRoleMapper userRoleMapper;

    /** Precedence map: lower ordinal = more permissive. */
    private static final Map<com.hrms.common.rbac.annotation.DataScopeType, Integer> SCOPE_PRECEDENCE;
    static {
        SCOPE_PRECEDENCE = new EnumMap<>(com.hrms.common.rbac.annotation.DataScopeType.class);
        SCOPE_PRECEDENCE.put(com.hrms.common.rbac.annotation.DataScopeType.ALL, 0);
        SCOPE_PRECEDENCE.put(com.hrms.common.rbac.annotation.DataScopeType.SUBORDINATE_TREE, 1);
        SCOPE_PRECEDENCE.put(com.hrms.common.rbac.annotation.DataScopeType.OWN_DEPT, 2);
        SCOPE_PRECEDENCE.put(com.hrms.common.rbac.annotation.DataScopeType.SELF_ONLY, 3);
    }

    /** Cache key = userId, value = all enabled permission codes for that user. */
    private final Cache<Long, Set<String>> permissionCache = Caffeine.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .maximumSize(10_000)
            .build();

    /** Cache for dominant data scope per user. */
    private final Cache<Long, com.hrms.common.rbac.annotation.DataScopeType> dataScopeCache = Caffeine.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .maximumSize(10_000)
            .build();

    /**
     * Check whether the given user holds the specified permission.
     *
     * <p>Wildcard semantics: if the user has {@code "foo:*"} they implicitly hold
     * {@code "foo:bar"}, {@code "foo:baz"}, etc.  If the user has {@code "*"} they
     * hold everything.  Exact match always wins.</p>
     */
    public boolean userHasPermission(Long userId, String permissionCode) {
        if (userId == null || permissionCode == null) {
            return false;
        }
        if (userRoleMapper.userHasAdminRole(userId)) {
            return true;
        }
        Set<String> codes = getPermissionCodes(userId);
        // exact match
        if (codes.contains(permissionCode)) {
            return true;
        }
        // wildcard: "foo:*" or "*"
        if (codes.contains("*")) {
            return true;
        }
        int lastColon = permissionCode.lastIndexOf(':');
        if (lastColon > 0) {
            String prefix = permissionCode.substring(0, lastColon + 1);
            if (codes.contains(prefix + "*")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Return all distinct permission codes for the user, consulting the cache first.
     */
    public Set<String> getPermissionCodes(Long userId) {
        Set<String> cached = permissionCache.getIfPresent(userId);
        if (cached != null) {
            return cached;
        }
        if (userRoleMapper.userHasAdminRole(userId)) {
            Set<String> adminCodes = Set.of("*");
            permissionCache.put(userId, adminCodes);
            return adminCodes;
        }
        Set<String> codes = loadPermissionCodes(userId);
        permissionCache.put(userId, codes);
        return codes;
    }

    /**
     * Evict the cache entry for a specific user (call after role/permission changes).
     */
    public void evictCache(Long userId) {
        permissionCache.invalidate(userId);
        dataScopeCache.invalidate(userId);
    }

    /**
     * Evict all cached permissions.
     */
    public void evictAllCache() {
        permissionCache.invalidateAll();
        dataScopeCache.invalidateAll();
    }

    /**
     * Determine the dominant (most permissive) data scope for a user based on
     * all their enabled roles.  Multiple roles are resolved by picking the
     * broadest scope: ALL &gt; SUBORDINATE_TREE &gt; OWN_DEPT &gt; SELF_ONLY.
     *
     * @return the dominant scope type, or {@code SELF_ONLY} if the user has no roles
     */
    public com.hrms.common.rbac.annotation.DataScopeType getDominantDataScope(Long userId) {
        if (userId == null) {
            return com.hrms.common.rbac.annotation.DataScopeType.SELF_ONLY;
        }
        com.hrms.common.rbac.annotation.DataScopeType cached = dataScopeCache.getIfPresent(userId);
        if (cached != null) {
            return cached;
        }
        com.hrms.common.rbac.annotation.DataScopeType scope = loadDominantDataScope(userId);
        dataScopeCache.put(userId, scope);
        return scope;
    }

    private com.hrms.common.rbac.annotation.DataScopeType loadDominantDataScope(Long userId) {
        Set<String> scopes = userRoleMapper.selectDataScopesByUserId(userId);
        if (scopes == null || scopes.isEmpty()) {
            return com.hrms.common.rbac.annotation.DataScopeType.SELF_ONLY;
        }
        com.hrms.common.rbac.annotation.DataScopeType best = com.hrms.common.rbac.annotation.DataScopeType.SELF_ONLY;
        int bestPrecedence = Integer.MAX_VALUE;
        for (String s : scopes) {
            try {
                com.hrms.common.rbac.annotation.DataScopeType candidate =
                        com.hrms.common.rbac.annotation.DataScopeType.valueOf(s);
                int p = SCOPE_PRECEDENCE.getOrDefault(candidate, Integer.MAX_VALUE);
                if (p < bestPrecedence) {
                    bestPrecedence = p;
                    best = candidate;
                }
            } catch (IllegalArgumentException ignored) {
                // unknown scope value in DB, skip
            }
        }
        return best;
    }

    // -- internals -------------------------------------------------------

    /**
     * Load permission codes from DB using a single JOIN query.
     * Replaces the previous 4-query approach.
     */
    private Set<String> loadPermissionCodes(Long userId) {
        Set<String> codes = userRoleMapper.selectPermissionCodesByUserId(userId);
        return codes != null ? codes : Collections.emptySet();
    }
}
