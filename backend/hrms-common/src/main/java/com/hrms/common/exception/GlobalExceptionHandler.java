package com.hrms.common.exception;

import com.hrms.common.api.BizCode;
import com.hrms.common.api.R;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Global REST exception translator.
 *
 * <p>Always returns HTTP 200 with a populated {@link R} envelope.  Business exceptions are
 * logged at INFO; validation at WARN; and anything else at ERROR with full stacktrace.</p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public ResponseEntity<R<Void>> handleBiz(BizException ex) {
        log.info("biz exception: code={}, msg={}", ex.getCode(), ex.getMessage());
        HttpStatus status = mapBizCodeToStatus(ex.getCode());
        return ResponseEntity.status(status).body(R.fail(ex.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<R<Void>> handleArgNotValid(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("validation failed: {}", msg);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(R.fail(BizCode.BAD_REQUEST, msg));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<R<Void>> handleBind(BindException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("bind failed: {}", msg);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(R.fail(BizCode.BAD_REQUEST, msg));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<R<Void>> handleConstraint(ConstraintViolationException ex) {
        log.warn("constraint violation: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(R.fail(BizCode.BAD_REQUEST, ex.getMessage()));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<R<Void>> handleAuth(AuthenticationException ex) {
        log.info("authentication failed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(R.fail(BizCode.UNAUTHORIZED, "未登录"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<R<Void>> handleAccessDenied(AccessDeniedException ex) {
        log.info("access denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(R.fail(BizCode.FORBIDDEN, "无权限"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<R<Void>> handleUnknown(Exception ex) {
        log.error("unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(R.fail(BizCode.INTERNAL_ERROR, "服务器内部错误"));
    }

    /**
     * Map HRMS business codes to HTTP status codes per the API contract.
     * 1xxx auth, 2xxx permission, 3xxx data, 4xxx validation, 9xxx unknown.
     */
    private HttpStatus mapBizCodeToStatus(int code) {
        if (code == BizCode.TOO_MANY_REQUESTS) return HttpStatus.TOO_MANY_REQUESTS;
        if (code >= 1000 && code < 2000) return HttpStatus.UNAUTHORIZED;
        if (code >= 2000 && code < 3000) return HttpStatus.FORBIDDEN;
        if (code >= 3000 && code < 4000) return HttpStatus.NOT_FOUND;
        if (code >= 4000 && code < 5000) return HttpStatus.BAD_REQUEST;
        if (code >= 9000) return HttpStatus.INTERNAL_SERVER_ERROR;
        return HttpStatus.BAD_REQUEST;
    }
}
