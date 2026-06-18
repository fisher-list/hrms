package com.hrms.common.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serializable;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Lightweight authenticated principal placed into
 * {@link org.springframework.security.core.context.SecurityContextHolder}
 * after a JWT is validated.  No password is held.
 *
 * <p>Carries a set of permission codes for downstream RBAC checks.</p>
 */
@Getter
public class LoginUser implements UserDetails, Serializable {

    private static final long serialVersionUID = 1L;

    private final Long uid;
    private final Long employeeId;
    private final String username;
    private final Set<String> permissions;

    public LoginUser(Long uid, String username) {
        this(uid, null, username, Set.of());
    }

    public LoginUser(Long uid, String username, Set<String> permissions) {
        this(uid, null, username, permissions);
    }

    public LoginUser(Long uid, Long employeeId, String username, Set<String> permissions) {
        this.uid = uid;
        this.employeeId = employeeId;
        this.username = username;
        this.permissions = permissions != null ? permissions : Set.of();
    }

    public Long requireEmployeeId() {
        if (employeeId == null) {
            throw new IllegalStateException("当前账号未绑定员工档案");
        }
        return employeeId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return permissions.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
