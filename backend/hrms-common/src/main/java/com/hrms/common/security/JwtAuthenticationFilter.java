package com.hrms.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * Parses the {@code Authorization: Bearer ...} header on every request, validates it via
 * {@link JwtUtil}, and populates the security context with a {@link LoginUser} that
 * carries the user's permission codes.
 *
 * <p>Refresh tokens are rejected here — only {@code access} tokens authenticate API calls.
 * Errors do <em>not</em> short-circuit the filter chain; downstream
 * {@code AuthenticationEntryPoint} renders a unified 401.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;
    private final com.hrms.common.rbac.service.PermissionService permissionService;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse resp, FilterChain chain)
            throws ServletException, IOException {

        String header = req.getHeader(HEADER);
        if (header != null && header.startsWith(PREFIX)) {
            String token = header.substring(PREFIX.length()).trim();
            try {
                Claims claims = jwtUtil.parse(token);
                String type = claims.get(JwtUtil.CLAIM_TYPE, String.class);
                if (!JwtUtil.TYPE_ACCESS.equals(type)) {
                    log.debug("ignoring non-access token type={}", type);
                } else {
                    Long uid = claims.get(JwtUtil.CLAIM_UID, Long.class);
                    Long employeeId = claims.get(JwtUtil.CLAIM_EMPLOYEE_ID, Long.class);
                    String username = claims.get(JwtUtil.CLAIM_USERNAME, String.class);
                    Set<String> permissions = permissionService.getPermissionCodes(uid);
                    LoginUser principal = new LoginUser(uid, employeeId, username, permissions);
                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (JwtException ex) {
                log.debug("invalid jwt: {}", ex.getMessage());
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(req, resp);
    }
}
