package com.hrms.common.rbac.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Method-level annotation that triggers the data-scope MyBatis interceptor.
 * The interceptor appends a SQL WHERE clause restricting the result set to
 * rows the current user is allowed to see.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DataScope {

    /**
     * The scope type; if omitted the aspect infers it from the user's roles.
     */
    DataScopeType type() default DataScopeType.ALL;

    /**
     * The SQL column name representing the department FK on the target table.
     */
    String deptField() default "dept_id";

    /**
     * The SQL column name representing the employee FK on the target table.
     * Used by SELF_ONLY scope to filter rows belonging to the current user.
     */
    String employeeField() default "employee_id";
}
