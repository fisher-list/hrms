package com.hrms.common.audit.aspect;

import com.hrms.common.audit.annotation.AuditLog;
import com.hrms.common.audit.entity.SysAuditLog;
import com.hrms.common.audit.service.AuditLogService;
import com.hrms.common.security.LoginUser;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

/**
 * AOP aspect that intercepts methods annotated with {@link AuditLog}
 * and asynchronously persists an audit record via {@link AuditLogService}.
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {

    private final AuditLogService auditLogService;

    @Around("@annotation(com.hrms.common.audit.annotation.AuditLog)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        Object result;
        int statusCode = 200;

        try {
            result = joinPoint.proceed();
        } catch (Throwable ex) {
            statusCode = 500;
            throw ex;
        } finally {
            try {
                AuditLog auditAnnotation = resolveAnnotation(joinPoint);
                if (auditAnnotation != null) {
                    SysAuditLog auditLog = buildAuditLog(joinPoint, auditAnnotation, statusCode);
                    auditLogService.saveAsync(auditLog);
                }
            } catch (Exception e) {
                log.warn("Failed to build audit log: {}", e.getMessage(), e);
            }
        }
        return result;
    }

    private AuditLog resolveAnnotation(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        return method.getAnnotation(AuditLog.class);
    }

    private SysAuditLog buildAuditLog(ProceedingJoinPoint joinPoint,
                                       AuditLog annotation,
                                       int statusCode) {
        SysAuditLog auditLog = new SysAuditLog();
        auditLog.setAction(annotation.action());
        auditLog.setCreatedAt(LocalDateTime.now());
        auditLog.setStatusCode(statusCode);

        // Resource info from the target class / method
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        auditLog.setResourceType(className);
        auditLog.setResourceId(methodName);

        // Current authenticated user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof LoginUser loginUser) {
            auditLog.setUserId(loginUser.getUid());
            auditLog.setUsername(loginUser.getUsername());
        }

        // HTTP request details
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            auditLog.setIpAddress(getClientIp(request));
            auditLog.setUserAgent(request.getHeader("User-Agent"));
            auditLog.setRequestMethod(request.getMethod());
            auditLog.setRequestUri(request.getRequestURI());
        }

        return auditLog;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank()) {
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
}
