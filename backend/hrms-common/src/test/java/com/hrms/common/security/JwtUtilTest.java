package com.hrms.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    private JwtProperties props;

    @BeforeEach
    void setUp() {
        props = new JwtProperties();
        props.setSecret("test-secret-test-secret-test-secret-1234");
        props.setAccessTtl(Duration.ofMinutes(30));
        props.setRefreshTtl(Duration.ofDays(7));
        props.setIssuer("hrms");
        jwtUtil = new JwtUtil(props);
        ReflectionTestUtils.invokeMethod(jwtUtil, "init");
    }

    @Test
    @DisplayName("短于 32 字节的 secret 启动失败")
    void rejectsShortSecret() {
        JwtProperties weak = new JwtProperties();
        weak.setSecret("short");
        weak.setAccessTtl(Duration.ofMinutes(1));
        weak.setRefreshTtl(Duration.ofDays(1));
        weak.setIssuer("hrms");
        JwtUtil bad = new JwtUtil(weak);
        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(bad, "init"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 bytes");
    }

    @Test
    @DisplayName("access token 含 uid/username/typ=access 且可被 parse 出原值")
    void accessTokenRoundTrip() {
        String token = jwtUtil.generateAccessToken(99L, "user99");
        Claims claims = jwtUtil.parse(token);
        assertThat(claims.get(JwtUtil.CLAIM_UID, Long.class)).isEqualTo(99L);
        assertThat(claims.get(JwtUtil.CLAIM_USERNAME, String.class)).isEqualTo("user99");
        assertThat(claims.get(JwtUtil.CLAIM_TYPE, String.class)).isEqualTo(JwtUtil.TYPE_ACCESS);
    }

    @Test
    @DisplayName("refresh token 携带 typ=refresh")
    void refreshTokenHasRefreshType() {
        String token = jwtUtil.generateRefreshToken(7L, "user7");
        Claims claims = jwtUtil.parse(token);
        assertThat(claims.get(JwtUtil.CLAIM_TYPE, String.class)).isEqualTo(JwtUtil.TYPE_REFRESH);
    }

    @Test
    @DisplayName("篡改 token 签名 → JwtException")
    void tamperedSignatureRejected() {
        String token = jwtUtil.generateAccessToken(1L, "u");
        // flip a character in the signature segment
        String[] parts = token.split("\\.");
        String tampered = parts[0] + "." + parts[1] + "." + parts[2].substring(0, parts[2].length() - 2) + "AB";
        assertThatThrownBy(() -> jwtUtil.parse(tampered))
                .isInstanceOf(JwtException.class);
        assertThat(jwtUtil.isValid(tampered)).isFalse();
    }

    @Test
    @DisplayName("null/空 token → isValid=false")
    void blankTokensAreInvalid() {
        assertThat(jwtUtil.isValid(null)).isFalse();
        assertThat(jwtUtil.isValid("")).isFalse();
        assertThat(jwtUtil.isValid("   ")).isFalse();
    }

    @Test
    @DisplayName("过期 token 抛出 ExpiredJwtException")
    void expiredTokenThrows() {
        JwtProperties shortLived = new JwtProperties();
        shortLived.setSecret("test-secret-test-secret-test-secret-1234");
        shortLived.setAccessTtl(Duration.ofMillis(1));
        shortLived.setRefreshTtl(Duration.ofMillis(1));
        shortLived.setIssuer("hrms");
        JwtUtil shortUtil = new JwtUtil(shortLived);
        ReflectionTestUtils.invokeMethod(shortUtil, "init");

        String token = shortUtil.generateAccessToken(1L, "u");
        try {
            Thread.sleep(50);
        } catch (InterruptedException ignored) {
        }
        assertThatThrownBy(() -> shortUtil.parse(token))
                .isInstanceOf(ExpiredJwtException.class);
    }
}
