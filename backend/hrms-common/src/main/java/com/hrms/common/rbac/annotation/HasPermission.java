package com.hrms.common.rbac.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Method-level annotation that guards a controller/service method by checking
 * whether the current user holds the specified permission code.
 *
 * <p>Wildcard semantics: a user with {@code role:*} implicitly holds
 * {@code role:view}, {@code role:create}, etc.  A user with {@code *} holds
 * every permission (admin shortcut).</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface HasPermission {

    /**
     * Permission code to check, e.g. {@code "role:view"} or {@code "role:*"}.
     */
    String value();
}
