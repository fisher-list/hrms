package com.hrms.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * JWT helper around {@code io.jsonwebtoken} (jjwt).  HS256 only; secret is loaded from
 * {@link JwtProperties} which in turn comes from the {@code JWT_SECRET} env var.
 *
 * <p>Two token kinds are produced — {@code access} (default 30min) and {@code refresh}
 * (default 7d) — distinguished by the {@code typ} private claim.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtil {

    public static final String CLAIM_UID = "uid";
    public static final String CLAIM_EMPLOYEE_ID = "employeeId";
    public static final String CLAIM_USERNAME = "username";
    public static final String CLAIM_TYPE = "typ";
    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    private final JwtProperties props;
    private SecretKey signingKey;

    @PostConstruct
    void init() {
        byte[] bytes = props.getSecret().getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException(
                    "JWT_SECRET must be at least 32 bytes (256 bits) for HS256");
        }
        this.signingKey = Keys.hmacShaKeyFor(bytes);
        log.info("JwtUtil initialised: issuer={}, accessTtl={}, refreshTtl={}",
                props.getIssuer(), props.getAccessTtl(), props.getRefreshTtl());
    }

    public String generateAccessToken(long uid, Long employeeId, String username) {
        return buildToken(uid, employeeId, username, TYPE_ACCESS, props.getAccessTtl().toMillis());
    }

    public String generateAccessToken(long uid, String username) {
        return generateAccessToken(uid, null, username);
    }

    public String generateRefreshToken(long uid, Long employeeId, String username) {
        return buildToken(uid, employeeId, username, TYPE_REFRESH, props.getRefreshTtl().toMillis());
    }

    public String generateRefreshToken(long uid, String username) {
        return generateRefreshToken(uid, null, username);
    }

    private String buildToken(long uid, Long employeeId, String username, String type, long ttlMillis) {
        Instant now = Instant.now();
        var builder = Jwts.builder()
                .id(UUID.randomUUID().toString())
                .issuer(props.getIssuer())
                .subject(String.valueOf(uid))
                .claim(CLAIM_UID, uid)
                .claim(CLAIM_USERNAME, username)
                .claim(CLAIM_TYPE, type)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(ttlMillis)))
                .signWith(signingKey, Jwts.SIG.HS256);
        if (employeeId != null) {
            builder.claim(CLAIM_EMPLOYEE_ID, employeeId);
        }
        return builder.compact();
    }

    /**
     * Parse and validate a token.  Throws {@link JwtException} subclasses on failure;
     * callers should map them to the appropriate business code.
     */
    public Claims parse(String token) throws JwtException {
        Jws<Claims> jws = Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(props.getIssuer())
                .build()
                .parseSignedClaims(token);
        return jws.getPayload();
    }

    /** Returns true if token is non-null, well-formed, signed with our key, and unexpired. */
    public boolean isValid(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        try {
            parse(token);
            return true;
        } catch (ExpiredJwtException e) {
            return false;
        } catch (JwtException e) {
            return false;
        }
    }

    public long getAccessTtlSeconds() {
        return props.getAccessTtl().toSeconds();
    }

    public long getRefreshTtlSeconds() {
        return props.getRefreshTtl().toSeconds();
    }
}
