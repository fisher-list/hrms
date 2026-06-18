package com.hrms.common.auth;

import com.hrms.common.api.BizCode;
import com.hrms.common.api.R;
import com.hrms.common.audit.annotation.AuditLog;
import com.hrms.common.auth.dto.ChangePasswordDto;
import com.hrms.common.auth.dto.CurrentUserVo;
import com.hrms.common.auth.dto.LoginDto;
import com.hrms.common.auth.dto.RefreshDto;
import com.hrms.common.auth.dto.TokenVo;
import com.hrms.common.exception.BizException;
import com.hrms.common.rbac.dto.PermissionVo;
import com.hrms.common.security.LoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@Tag(name = "Auth", description = "Authentication endpoints")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthController {

    private static final String REFRESH_COOKIE_NAME = "refreshToken";
    private static final long REFRESH_COOKIE_MAX_AGE = 7 * 24 * 60 * 60; // 7 days
    private static final String AUTH_PATH = "/api/auth";

    private final AuthService authService;
    private final com.hrms.common.security.JwtProperties jwtProperties;

    @Operation(summary = "Login with username/password")
    @PostMapping("/auth/login")
    @AuditLog(action = "LOGIN")
    public R<TokenVo> login(@Valid @RequestBody LoginDto dto, HttpServletResponse response) {
        TokenVo tokenVo = authService.login(dto);
        setRefreshCookie(response, tokenVo.getRefreshToken());
        tokenVo.setRefreshToken(null); // refresh token 通过 HttpOnly cookie 返回，不在 body 中暴露
        return R.ok(tokenVo);
    }

    @Operation(summary = "Exchange a refresh token for a new access token")
    @PostMapping("/auth/refresh")
    public R<TokenVo> refresh(@CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken,
                              HttpServletResponse response) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BizException(BizCode.AUTH_REFRESH_INVALID, "refresh token 缺失");
        }
        TokenVo tokenVo = authService.refresh(new RefreshDto(refreshToken));
        setRefreshCookie(response, tokenVo.getRefreshToken());
        tokenVo.setRefreshToken(null);
        return R.ok(tokenVo);
    }

    @Operation(summary = "Revoke the supplied refresh token")
    @PostMapping("/auth/logout")
    @AuditLog(action = "LOGOUT")
    public R<Void> logout(@CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken,
                           HttpServletResponse response) {
        authService.logout(refreshToken);
        clearRefreshCookie(response);
        return R.ok();
    }

    @Operation(summary = "Change the current user's password")
    @PostMapping("/auth/change-password")
    @AuditLog(action = "CHANGE_PASSWORD")
    public R<Void> changePassword(@AuthenticationPrincipal LoginUser principal,
                                   @Valid @RequestBody ChangePasswordDto dto) {
        authService.changePassword(principal.getUid(), dto);
        return R.ok();
    }

    @Operation(summary = "Return the currently authenticated user")
    @GetMapping("/me")
    public R<CurrentUserVo> me(@AuthenticationPrincipal LoginUser principal) {
        return R.ok(authService.me(principal.getUid()));
    }

    @Operation(summary = "Return the current user's permission codes")
    @GetMapping("/me/permissions")
    public R<PermissionVo> mePermissions(@AuthenticationPrincipal LoginUser principal) {
        Set<String> permissions = authService.myPermissions(principal.getUid());
        return R.ok(new PermissionVo(permissions));
    }

    private void setRefreshCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(jwtProperties.isRefreshCookieSecure())
                .sameSite("Lax")
                .path("/")
                .maxAge(REFRESH_COOKIE_MAX_AGE)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(jwtProperties.isRefreshCookieSecure())
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
