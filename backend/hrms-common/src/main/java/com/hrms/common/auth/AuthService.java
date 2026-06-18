package com.hrms.common.auth;

import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hrms.common.api.BizCode;
import com.hrms.common.auth.dto.ChangePasswordDto;
import com.hrms.common.auth.dto.CurrentUserVo;
import com.hrms.common.auth.dto.LoginDto;
import com.hrms.common.auth.dto.RefreshDto;
import com.hrms.common.auth.dto.TokenVo;
import com.hrms.common.exception.BizException;
import com.hrms.common.rbac.entity.SysRole;
import com.hrms.common.rbac.entity.SysUserRole;
import com.hrms.common.rbac.mapper.SysRoleMapper;
import com.hrms.common.rbac.mapper.SysUserRoleMapper;
import com.hrms.common.rbac.service.PermissionService;
import com.hrms.common.security.JwtUtil;
import com.hrms.common.user.SysUser;
import com.hrms.common.user.SysUserService;
import com.hrms.common.user.SysUserToken;
import com.hrms.common.user.SysUserTokenMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Authentication orchestration: login, refresh, logout, change-password, "/api/me" lookup.
 *
 * <p>Lock-out policy (Story EP01-S01 red-line #6): 5 failed attempts within an unlocked
 * window lock the account for 30 minutes; a successful login resets the counter; while
 * locked, even a correct password is rejected with {@code AUTH_ACCOUNT_LOCKED}.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_DISABLED = "DISABLED";

    private final SysUserService userService;
    private final SysUserTokenMapper tokenMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;
    private final PermissionService permissionService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final LoginAttemptService loginAttemptService;
    private final Clock clock;

    /** Process a login attempt; on success return a fresh access + refresh pair. */
    @Transactional
    public TokenVo login(LoginDto dto) {
        SysUser user = userService.findByUsername(dto.getUsername())
                .orElseThrow(() -> new BizException(BizCode.AUTH_BAD_CREDENTIALS, "用户名或密码错误"));

        ensureNotDisabled(user);
        ensureNotLocked(user);

        boolean passwordOk = passwordEncoder.matches(dto.getPassword(), user.getPasswordHash());
        if (!passwordOk) {
            loginAttemptService.recordFailedAttempt(user);
            throw new BizException(BizCode.AUTH_BAD_CREDENTIALS, "用户名或密码错误");
        }

        // success — reset counters, refresh last-login bookkeeping
        loginAttemptService.resetFailedAttempts(user);
        user.setLastLoginAt(now());
        userService.update(user);

        TokenVo tokenVo = issueTokens(user);
        tokenVo.setForceChangePassword(user.getPasswordChangedAt() == null);
        return tokenVo;
    }

    /** Exchange a non-expired non-revoked refresh token for a new access token. */
    @Transactional
    public TokenVo refresh(RefreshDto dto) {
        Claims claims;
        try {
            claims = jwtUtil.parse(dto.getRefreshToken());
        } catch (ExpiredJwtException ex) {
            throw new BizException(BizCode.AUTH_REFRESH_INVALID, "refresh token 已过期");
        } catch (JwtException ex) {
            throw new BizException(BizCode.AUTH_REFRESH_INVALID, "refresh token 无效");
        }

        if (!JwtUtil.TYPE_REFRESH.equals(claims.get(JwtUtil.CLAIM_TYPE, String.class))) {
            throw new BizException(BizCode.AUTH_REFRESH_INVALID, "token 类型错误");
        }

        String jti = claims.getId();
        SysUserToken record = tokenMapper.selectOne(new LambdaQueryWrapper<SysUserToken>()
                .eq(SysUserToken::getRefreshJti, jti));
        if (record == null || Boolean.TRUE.equals(record.getRevoked())) {
            throw new BizException(BizCode.AUTH_REFRESH_INVALID, "refresh token 已失效");
        }
        if (record.getExpiresAt() != null && record.getExpiresAt().isBefore(now())) {
            throw new BizException(BizCode.AUTH_REFRESH_INVALID, "refresh token 已过期");
        }

        Long uid = claims.get(JwtUtil.CLAIM_UID, Long.class);
        SysUser user = userService.findById(uid)
                .orElseThrow(() -> new BizException(BizCode.AUTH_REFRESH_INVALID, "用户不存在"));
        ensureNotDisabled(user);
        ensureNotLocked(user);

        // rotate: revoke old refresh, issue a new pair
        record.setRevoked(true);
        record.setRevokedAt(now());
        tokenMapper.updateById(record);

        TokenVo tokenVo = issueTokens(user);
        tokenVo.setForceChangePassword(user.getPasswordChangedAt() == null);
        return tokenVo;
    }

    /** Revoke the supplied refresh token if known.  Idempotent. */
    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        try {
            Claims claims = jwtUtil.parse(refreshToken);
            String jti = claims.getId();
            SysUserToken record = tokenMapper.selectOne(new LambdaQueryWrapper<SysUserToken>()
                    .eq(SysUserToken::getRefreshJti, jti));
            if (record != null && !Boolean.TRUE.equals(record.getRevoked())) {
                record.setRevoked(true);
                record.setRevokedAt(now());
                tokenMapper.updateById(record);
            }
        } catch (JwtException ignored) {
            // logout is best-effort
        }
    }

    /**
     * Change the current user's password.
     *
     * <ol>
     *   <li>Verify the old password</li>
     *   <li>Validate the new password strength</li>
     *   <li>Check the new password differs from the old one</li>
     *   <li>BCrypt-encode and persist</li>
     *   <li>Revoke all refresh tokens for this user</li>
     * </ol>
     */
    @Transactional
    public void changePassword(Long uid, ChangePasswordDto dto) {
        // 1. Confirm passwords match
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            throw new BizException(BizCode.BAD_REQUEST, "两次输入的密码不一致");
        }

        SysUser user = userService.findById(uid)
                .orElseThrow(() -> new BizException(BizCode.UNAUTHORIZED, "用户不存在"));

        // 2. Verify old password
        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPasswordHash())) {
            throw new BizException(BizCode.AUTH_OLD_PASSWORD_WRONG, "旧密码错误");
        }

        // 3. Password strength validation
        PasswordValidator.Result result = PasswordValidator.validate(dto.getNewPassword(), user.getUsername());
        if (!result.valid()) {
            throw new BizException(BizCode.AUTH_PASSWORD_WEAK,
                    String.join("；", result.errors()));
        }

        // 4. New password must differ from old
        if (passwordEncoder.matches(dto.getNewPassword(), user.getPasswordHash())) {
            throw new BizException(BizCode.AUTH_PASSWORD_SAME, "新密码不能与旧密码相同");
        }

        // 5. Persist new password hash
        user.setPasswordHash(passwordEncoder.encode(dto.getNewPassword()));
        user.setPasswordChangedAt(now());
        user.setFailedAttempts(0);
        user.setLockedUntil(null);
        userService.update(user);

        // 6. Revoke all refresh tokens for this user (force re-login)
        revokeAllTokensForUser(uid);

        log.info("password changed for user uid={}", uid);
    }

    public CurrentUserVo me(Long uid) {
        SysUser user = userService.findById(uid)
                .orElseThrow(() -> new BizException(BizCode.UNAUTHORIZED, "未登录"));

        // Load role codes for this user
        List<SysUserRole> userRoles = userRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, uid));
        Set<String> roleCodes = new HashSet<>();
        if (userRoles != null && !userRoles.isEmpty()) {
            List<Long> roleIds = userRoles.stream()
                    .map(SysUserRole::getRoleId)
                    .collect(Collectors.toList());
            List<SysRole> roles = roleMapper.selectList(
                    new LambdaQueryWrapper<SysRole>().in(SysRole::getId, roleIds));
            roleCodes = roles.stream()
                    .map(SysRole::getCode)
                    .collect(Collectors.toSet());
        }

        return new CurrentUserVo(user.getId(), user.getEmployeeId(), user.getUsername(),
                user.getNickname(), roleCodes, user.getPasswordChangedAt() == null);
    }

    /**
     * Return the current user's permission codes (union across all enabled roles).
     */
    public Set<String> myPermissions(Long uid) {
        return permissionService.getPermissionCodes(uid);
    }

    // -- internals -------------------------------------------------------

    private void ensureNotDisabled(SysUser user) {
        if (STATUS_DISABLED.equalsIgnoreCase(user.getStatus())) {
            throw new BizException(BizCode.AUTH_ACCOUNT_DISABLED, "账号已停用");
        }
    }

    private void ensureNotLocked(SysUser user) {
        LocalDateTime lockedUntil = user.getLockedUntil();
        if (lockedUntil != null && lockedUntil.isAfter(now())) {
            throw new BizException(BizCode.AUTH_ACCOUNT_LOCKED,
                    "账号已锁定，请于 " + lockedUntil + " 后重试");
        }
    }

    private TokenVo issueTokens(SysUser user) {
        String access = jwtUtil.generateAccessToken(user.getId(), user.getEmployeeId(), user.getUsername());
        String refresh = jwtUtil.generateRefreshToken(user.getId(), user.getEmployeeId(), user.getUsername());

        Claims refreshClaims = jwtUtil.parse(refresh);
        SysUserToken token = new SysUserToken();
        token.setUserId(user.getId());
        token.setRefreshJti(refreshClaims.getId());
        token.setRefreshTokenHash(SecureUtil.sha256(refresh));
        token.setIssuedAt(now());
        token.setExpiresAt(LocalDateTime.ofInstant(
                refreshClaims.getExpiration().toInstant(), clock.getZone()));
        token.setRevoked(false);
        tokenMapper.insert(token);

        return new TokenVo(access, refresh, jwtUtil.getAccessTtlSeconds(), "Bearer",
                user.getId(), user.getEmployeeId(), user.getUsername());
    }

    /**
     * Revoke every non-revoked refresh token belonging to the given user.
     */
    private void revokeAllTokensForUser(Long userId) {
        List<SysUserToken> tokens = tokenMapper.selectList(
                new LambdaQueryWrapper<SysUserToken>()
                        .eq(SysUserToken::getUserId, userId)
                        .eq(SysUserToken::getRevoked, false));
        for (SysUserToken t : tokens) {
            t.setRevoked(true);
            t.setRevokedAt(now());
            tokenMapper.updateById(t);
        }
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
