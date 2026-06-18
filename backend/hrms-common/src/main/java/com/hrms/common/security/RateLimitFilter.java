package com.hrms.common.security;

import com.hrms.common.api.BizCode;
import com.hrms.common.api.R;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Rate-limit filter for the login endpoint to prevent brute-force attacks.
 *
 * <p>Strategy: same IP may attempt at most {@value MAX_ATTEMPTS} failed logins within
 * {@code WINDOW_MINUTES} minutes.  Exceeding the limit results in HTTP 429.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 10) // Run early, before security filters
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/api/auth/login";
    private static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_MILLIS = 5 * 60 * 1000L; // 5 minutes

    private final ObjectMapper objectMapper;

    /** IP → rate-limit state. */
    private final ConcurrentMap<String, RateLimitEntry> attempts = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Only filter POST requests to the login path
        return !(LOGIN_PATH.equals(request.getRequestURI())
                && "POST".equalsIgnoreCase(request.getMethod()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String ip = getClientIp(request);

        // Check if rate limit is exceeded
        RateLimitEntry entry = attempts.get(ip);
        if (entry != null) {
            // Clean up expired entry
            if (System.currentTimeMillis() - entry.firstFailureTime > WINDOW_MILLIS) {
                attempts.remove(ip);
                entry = null;
            } else if (entry.count >= MAX_ATTEMPTS) {
                // Rate limit exceeded
                log.warn("rate limit exceeded for IP: {}", ip);
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                String body = objectMapper.writeValueAsString(
                        R.fail(BizCode.TOO_MANY_REQUESTS, "登录尝试次数过多，请5分钟后再试"));
                response.getWriter().write(body);
                response.getWriter().flush();
                return;
            }
        }

        // Wrap the response to capture the status code
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);
        filterChain.doFilter(request, wrappedResponse);

        int status = wrappedResponse.getStatus();
        if (status == HttpStatus.OK.value() || status == HttpStatus.CREATED.value()) {
            // Login successful — reset counter
            attempts.remove(ip);
            log.debug("login success for IP: {}, counter reset", ip);
        } else if (status == HttpStatus.UNAUTHORIZED.value()) {
            // Login failed — increment counter
            attempts.merge(ip, new RateLimitEntry(1, System.currentTimeMillis()),
                    (existing, newEntry) -> {
                        if (System.currentTimeMillis() - existing.firstFailureTime > WINDOW_MILLIS) {
                            return newEntry; // Reset if window expired
                        }
                        return new RateLimitEntry(existing.count + 1, existing.firstFailureTime);
                    });
            RateLimitEntry updated = attempts.get(ip);
            log.debug("login failed for IP: {}, attempts: {}/{}", ip, updated.count, MAX_ATTEMPTS);
        }

        // Copy the cached response body to the actual response
        wrappedResponse.copyBodyToResponse();
    }

    /**
     * Clear all rate-limit counters. Intended for use in tests only.
     */
    public void clearAttempts() {
        attempts.clear();
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank()) {
            // X-Forwarded-For may contain multiple IPs; take the first one
            ip = ip.split(",")[0].trim();
        }
        if (ip == null || ip.isBlank()) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isBlank()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    /** Rate-limit entry: number of failed attempts and the timestamp of the first failure in the current window. */
    private static class RateLimitEntry {
        final int count;
        final long firstFailureTime;

        RateLimitEntry(int count, long firstFailureTime) {
            this.count = count;
            this.firstFailureTime = firstFailureTime;
        }
    }
}
