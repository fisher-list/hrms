package com.hrms.common.audit.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a controller method for audit logging.
 * The {@link #action()} value describes the operation (e.g. "LOGIN", "CREATE_EMPLOYEE").
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuditLog {

    /**
     * Human-readable action description, e.g. "LOGIN", "APPROVE".
     */
    String action();
}
