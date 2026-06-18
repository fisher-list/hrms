package com.hrms.common.rbac.datascope;

import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import com.hrms.common.rbac.annotation.DataScopeType;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.springframework.stereotype.Component;

/**
 * MyBatis-Plus {@link InnerInterceptor} that appends a data-scope WHERE clause
 * to SELECT statements when a {@link DataScopeContext} is present in ThreadLocal.
 *
 * <p>Scope behaviour:
 * <ul>
 *   <li>{@code ALL} — no filter appended</li>
 *   <li>{@code OWN_DEPT} — {@code AND <deptField> = <deptId>}</li>
 *   <li>{@code SUBORDINATE_TREE} — {@code AND <deptField> IN (<subIds>)}</li>
 *   <li>{@code SELF_ONLY} — {@code AND <employeeField> = <employeeId>}</li>
 * </ul>
 *
 * <p>When the resolved scope is not {@code ALL} but required context is missing,
 * the interceptor fails closed with {@code AND 1 = 0}.</p>
 */
@Slf4j
@Component
public class DataScopeInterceptor implements InnerInterceptor {

    @Override
    public void beforeQuery(Executor executor, MappedStatement ms, Object parameter,
                            RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) {
        DataScopeContext ctx = DataScopeContext.get();
        if (ctx == null || ctx.getScopeType() == null || ctx.getScopeType() == DataScopeType.ALL) {
            return;
        }

        String appendSql = buildAppendSql(ctx);
        if (appendSql == null) {
            return; // nothing to append (e.g. required params are null)
        }

        String originalSql = boundSql.getSql();
        String wrappedSql = "SELECT _ds_inner.* FROM (" + originalSql + ") _ds_inner WHERE 1=1 " + appendSql;

        // Replace the BoundSql's sql field via reflection
        try {
            var field = BoundSql.class.getDeclaredField("sql");
            field.setAccessible(true);
            field.set(boundSql, wrappedSql);
        } catch (Exception e) {
            log.warn("DataScopeInterceptor: failed to rewrite SQL: {}", e.getMessage());
        }
    }

    /**
     * Build the SQL fragment to append.  Returns null if the scope cannot be
     * applied (e.g. required params are null).
     */
    private String buildAppendSql(DataScopeContext ctx) {
        if (ctx.isDenyAll()) {
            return "AND 1 = 0";
        }

        String deptField = ctx.getDeptField() != null ? ctx.getDeptField() : "dept_id";
        String employeeField = ctx.getEmployeeField() != null ? ctx.getEmployeeField() : "employee_id";

        return switch (ctx.getScopeType()) {
            case OWN_DEPT -> {
                if (ctx.getDeptId() == null) {
                    yield "AND 1 = 0";
                }
                yield "AND " + deptField + " = " + ctx.getDeptId();
            }
            case SUBORDINATE_TREE -> {
                var subIds = ctx.getSubordinateDeptIds();
                if (subIds == null || subIds.isEmpty()) {
                    yield "AND 1 = 0";
                }
                String csv = subIds.stream()
                        .map(String::valueOf)
                        .reduce((a, b) -> a + "," + b)
                        .orElse("");
                yield "AND " + deptField + " IN (" + csv + ")";
            }
            case SELF_ONLY -> {
                if (ctx.getEmployeeId() == null) {
                    yield "AND 1 = 0";
                }
                yield "AND " + employeeField + " = " + ctx.getEmployeeId();
            }
            default -> null;
        };
    }
}
