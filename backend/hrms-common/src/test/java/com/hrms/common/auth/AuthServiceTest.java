package com.hrms.common.auth;

import com.hrms.common.api.BizCode;
import com.hrms.common.auth.dto.LoginDto;
import com.hrms.common.auth.dto.RefreshDto;
import com.hrms.common.auth.dto.TokenVo;
import com.hrms.common.exception.BizException;
import com.hrms.common.rbac.mapper.SysRoleMapper;
import com.hrms.common.rbac.mapper.SysUserRoleMapper;
import com.hrms.common.rbac.service.PermissionService;
import com.hrms.common.security.JwtProperties;
import com.hrms.common.security.JwtUtil;
import com.hrms.common.user.SysUser;
import com.hrms.common.user.SysUserService;
import com.hrms.common.user.SysUserToken;
import com.hrms.common.user.SysUserTokenMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests covering the three story-mandated cases plus several edges:
 * <ol>
 *   <li>5-th wrong password locks the account; locked + correct password still rejected</li>
 *   <li>Successful refresh rotates the token pair</li>
 *   <li>Expired refresh token is rejected with AUTH_REFRESH_INVALID</li>
 * </ol>
 */
class AuthServiceTest {

    private SysUserService userService;
    private SysUserTokenMapper tokenMapper;
    private PasswordEncoder passwordEncoder;
    private JwtUtil jwtUtil;
    private JwtProperties jwtProps;
    private MutableClock clock;
    private LoginAttemptService loginAttemptService;
    private AuthService authService;

    private final Map<String, SysUser> userByName = new HashMap<>();
    private final Map<Long, SysUser> userById = new HashMap<>();
    private final Map<String, SysUserToken> tokenByJti = new HashMap<>();
    private final AtomicLong tokenIdSeq = new AtomicLong(1000);

    @BeforeEach
    void setUp() {
        userService = mock(SysUserService.class);
        tokenMapper = mock(SysUserTokenMapper.class);
        passwordEncoder = new BCryptPasswordEncoder(10);

        jwtProps = new JwtProperties();
        jwtProps.setSecret("0123456789abcdef0123456789abcdef0123456789abcdef");
        jwtProps.setAccessTtl(Duration.ofMinutes(30));
        jwtProps.setRefreshTtl(Duration.ofDays(7));
        jwtProps.setIssuer("hrms");
        jwtUtil = new JwtUtil(jwtProps);
        ReflectionTestUtils.invokeMethod(jwtUtil, "init");

        clock = new MutableClock(Instant.parse("2026-06-14T10:00:00Z"));

        loginAttemptService = new LoginAttemptService(userService, clock);
        SysUserRoleMapper userRoleMapper = mock(SysUserRoleMapper.class);
        SysRoleMapper roleMapper = mock(SysRoleMapper.class);
        PermissionService permissionService = mock(PermissionService.class);
        authService = new AuthService(userService, tokenMapper, userRoleMapper, roleMapper, permissionService, passwordEncoder, jwtUtil, loginAttemptService, clock);

        // userService stubs
        when(userService.findByUsername(any())).thenAnswer(inv -> {
            String name = inv.getArgument(0);
            return Optional.ofNullable(userByName.get(name));
        });
        when(userService.findById(anyLong())).thenAnswer(inv ->
                Optional.ofNullable(userById.get((Long) inv.getArgument(0))));
        doAnswer(inv -> {
            SysUser u = inv.getArgument(0);
            userByName.put(u.getUsername(), u);
            userById.put(u.getId(), u);
            return null;
        }).when(userService).update(any());

        // tokenMapper stubs
        when(tokenMapper.insert(any(SysUserToken.class))).thenAnswer(inv -> {
            SysUserToken t = inv.getArgument(0);
            if (t.getId() == null) {
                t.setId(tokenIdSeq.incrementAndGet());
            }
            tokenByJti.put(t.getRefreshJti(), t);
            return 1;
        });
        when(tokenMapper.selectOne(any())).thenAnswer(inv -> {
            // crude: return first token whose jti matches the in-flight test query
            // we'll resolve by iterating; tests assume only one active token
            for (SysUserToken t : tokenByJti.values()) {
                if (Boolean.FALSE.equals(t.getRevoked()) || t.getRevoked() == null) {
                    return t;
                }
            }
            return tokenByJti.values().stream().findFirst().orElse(null);
        });
        when(tokenMapper.updateById(any(SysUserToken.class))).thenAnswer(inv -> {
            SysUserToken t = inv.getArgument(0);
            tokenByJti.put(t.getRefreshJti(), t);
            return 1;
        });
    }

    private SysUser seedUser(String username, String rawPassword) {
        SysUser u = new SysUser();
        u.setId(42L);
        u.setUsername(username);
        u.setPasswordHash(passwordEncoder.encode(rawPassword));
        u.setStatus("ACTIVE");
        u.setFailedAttempts(0);
        userByName.put(username, u);
        userById.put(u.getId(), u);
        return u;
    }

    @Test
    @DisplayName("5次错密锁定30分钟，锁定期间正确密码也拒绝")
    void wrongPasswordFiveTimesLocksAccount() {
        seedUser("alice", "Correct@Pass1");
        LoginDto wrong = new LoginDto();
        wrong.setUsername("alice");
        wrong.setPassword("WrongPass!");

        // first 4 attempts: bad creds, not yet locked
        for (int i = 1; i <= 4; i++) {
            assertThatThrownBy(() -> authService.login(wrong))
                    .isInstanceOf(BizException.class)
                    .extracting("code").isEqualTo(BizCode.AUTH_BAD_CREDENTIALS);
        }
        SysUser stored = userByName.get("alice");
        assertThat(stored.getFailedAttempts()).isEqualTo(4);
        assertThat(stored.getLockedUntil()).isNull();

        // 5th attempt: still bad creds, but now locked
        assertThatThrownBy(() -> authService.login(wrong))
                .isInstanceOf(BizException.class)
                .extracting("code").isEqualTo(BizCode.AUTH_BAD_CREDENTIALS);
        stored = userByName.get("alice");
        assertThat(stored.getFailedAttempts()).isEqualTo(5);
        assertThat(stored.getLockedUntil()).isNotNull();
        assertThat(stored.getLockedUntil()).isAfter(java.time.LocalDateTime.now(clock));

        // even correct password is rejected with ACCOUNT_LOCKED while locked
        LoginDto correct = new LoginDto();
        correct.setUsername("alice");
        correct.setPassword("Correct@Pass1");
        assertThatThrownBy(() -> authService.login(correct))
                .isInstanceOf(BizException.class)
                .extracting("code").isEqualTo(BizCode.AUTH_ACCOUNT_LOCKED);

        // after 30 minutes the lock expires and login succeeds + counters reset
        clock.advance(Duration.ofMinutes(31));
        TokenVo tokens = authService.login(correct);
        assertThat(tokens.getAccessToken()).isNotBlank();
        assertThat(tokens.getRefreshToken()).isNotBlank();
        SysUser unlocked = userByName.get("alice");
        assertThat(unlocked.getFailedAttempts()).isZero();
        assertThat(unlocked.getLockedUntil()).isNull();
    }

    @Test
    @DisplayName("refresh 成功 — 旧 refresh 被吊销并发放新 token 对")
    void refreshSucceedsAndRotatesToken() {
        seedUser("bob", "BobPass@1");
        LoginDto login = new LoginDto();
        login.setUsername("bob");
        login.setPassword("BobPass@1");
        TokenVo first = authService.login(login);

        clock.advance(Duration.ofMinutes(1));

        RefreshDto dto = new RefreshDto();
        dto.setRefreshToken(first.getRefreshToken());
        TokenVo rotated = authService.refresh(dto);

        assertThat(rotated.getAccessToken()).isNotBlank();
        assertThat(rotated.getRefreshToken()).isNotBlank().isNotEqualTo(first.getRefreshToken());

        // the old refresh-token jti must be marked revoked
        String oldJti = jwtUtil.parse(first.getRefreshToken()).getId();
        SysUserToken old = tokenByJti.get(oldJti);
        assertThat(old).isNotNull();
        assertThat(old.getRevoked()).isTrue();
    }

    @Test
    @DisplayName("refresh 过期 — 返回 AUTH_REFRESH_INVALID")
    void refreshFailsWhenTokenExpired() throws Exception {
        // Build a refresh token whose TTL is just 1 second so we don't have to wait
        // the full 7 days in a unit test.
        seedUser("carol", "Carol@123");
        JwtProperties shortLived = new JwtProperties();
        shortLived.setSecret("0123456789abcdef0123456789abcdef0123456789abcdef");
        shortLived.setAccessTtl(Duration.ofMinutes(30));
        shortLived.setRefreshTtl(Duration.ofSeconds(1));
        shortLived.setIssuer("hrms");
        JwtUtil shortJwt = new JwtUtil(shortLived);
        ReflectionTestUtils.invokeMethod(shortJwt, "init");

        SysUserRoleMapper shortUserRoleMapper = mock(SysUserRoleMapper.class);
        SysRoleMapper shortRoleMapper = mock(SysRoleMapper.class);
        PermissionService shortPermissionService = mock(PermissionService.class);
        AuthService shortAuth = new AuthService(userService, tokenMapper, shortUserRoleMapper, shortRoleMapper, shortPermissionService, passwordEncoder, shortJwt, loginAttemptService, clock);
        SysUser carol = userByName.get("carol");
        // Insert an already-known refresh token by faking a login-via-issueTokens path
        String refresh = shortJwt.generateRefreshToken(carol.getId(), carol.getUsername());
        SysUserToken t = new SysUserToken();
        t.setUserId(carol.getId());
        t.setRefreshJti(shortJwt.parse(refresh).getId());
        t.setRefreshTokenHash(refresh);
        t.setIssuedAt(java.time.LocalDateTime.now(clock));
        t.setExpiresAt(java.time.LocalDateTime.now(clock).plusSeconds(1));
        t.setRevoked(false);
        tokenMapper.insert(t);

        // wait past the 1s TTL — jjwt's parser compares against the real JVM clock
        Thread.sleep(1_200);

        RefreshDto dto = new RefreshDto();
        dto.setRefreshToken(refresh);
        assertThatThrownBy(() -> shortAuth.refresh(dto))
                .isInstanceOf(BizException.class)
                .extracting("code").isEqualTo(BizCode.AUTH_REFRESH_INVALID);
    }

    @Test
    @DisplayName("disabled 账号无法登录")
    void disabledAccountCannotLogin() {
        SysUser u = seedUser("dan", "Dan@2026");
        u.setStatus("DISABLED");
        LoginDto login = new LoginDto();
        login.setUsername("dan");
        login.setPassword("Dan@2026");
        assertThatThrownBy(() -> authService.login(login))
                .isInstanceOf(BizException.class)
                .extracting("code").isEqualTo(BizCode.AUTH_ACCOUNT_DISABLED);
    }

    @Test
    @DisplayName("用 access token 调 refresh 应被拒")
    void refreshRejectsAccessTokenType() {
        seedUser("eve", "Eve@Pass1");
        LoginDto login = new LoginDto();
        login.setUsername("eve");
        login.setPassword("Eve@Pass1");
        TokenVo t = authService.login(login);

        RefreshDto dto = new RefreshDto();
        dto.setRefreshToken(t.getAccessToken()); // wrong type on purpose
        assertThatThrownBy(() -> authService.refresh(dto))
                .isInstanceOf(BizException.class)
                .extracting("code").isEqualTo(BizCode.AUTH_REFRESH_INVALID);
    }

    /**
     * Mutable clock that tests can advance — JwtUtil uses {@code Instant.now()} from the
     * JVM clock when signing, so we wrap the JVM clock at construction time and advance
     * a delta on top of it.
     */
    private static final class MutableClock extends Clock {
        private final ZoneId zone = ZoneId.of("UTC");
        private Instant fixedNow;

        MutableClock(Instant start) {
            this.fixedNow = start;
        }

        void advance(Duration d) {
            fixedNow = fixedNow.plus(d);
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return fixedNow;
        }
    }
}
