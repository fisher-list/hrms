package com.hrms.common.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * JWT properties.  {@code secret} <strong>must</strong> be supplied via the
 * {@code JWT_SECRET} environment variable; the application will fail to start if it is
 * missing or shorter than 32 bytes.
 */
@Data
@Validated
@ConfigurationProperties(prefix = "hrms.jwt")
public class JwtProperties {

    /** HMAC-SHA256 secret, sourced from the {@code JWT_SECRET} env var. */
    @NotBlank
    private String secret;

    /** Access token TTL.  Default 30 minutes per Story EP01-S01. */
    @NotNull
    private Duration accessTtl = Duration.ofMinutes(30);

    /** Refresh token TTL.  Default 7 days. */
    @NotNull
    private Duration refreshTtl = Duration.ofDays(7);

    /** Token issuer claim. */
    @NotBlank
    private String issuer = "hrms";

    /** Whether the refresh-token cookie carries the {@code Secure} flag. */
    private boolean refreshCookieSecure = false;
}
