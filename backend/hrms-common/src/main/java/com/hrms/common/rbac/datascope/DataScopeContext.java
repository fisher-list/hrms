package com.hrms.common.rbac.datascope;

/**
 * Holds data-scope filtering context for the current request, stored in ThreadLocal.
 *
 * <p>Populated by {@link com.hrms.common.rbac.aspect.DataScopeAspect} before the
 * target method executes; consumed by {@link DataScopeInterceptor} in the
 * MyBatis-Plus interceptor chain; cleared in a {@code finally} block.</p>
 */
public final class DataScopeContext {

    private static final ThreadLocal<DataScopeContext> HOLDER = new ThreadLocal<>();

    private com.hrms.common.rbac.annotation.DataScopeType scopeType;
    private String deptField;
    private String employeeField;
    private Long userId;
    private Long deptId;
    private Long employeeId;
    private boolean denyAll;
    private java.util.List<Long> subordinateDeptIds;

    // -- static helpers --------------------------------------------------

    public static void set(DataScopeContext ctx) {
        HOLDER.set(ctx);
    }

    public static DataScopeContext get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }

    // -- getters / setters ------------------------------------------------

    public com.hrms.common.rbac.annotation.DataScopeType getScopeType() {
        return scopeType;
    }

    public void setScopeType(com.hrms.common.rbac.annotation.DataScopeType scopeType) {
        this.scopeType = scopeType;
    }

    public String getDeptField() {
        return deptField;
    }

    public void setDeptField(String deptField) {
        this.deptField = deptField;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getDeptId() {
        return deptId;
    }

    public void setDeptId(Long deptId) {
        this.deptId = deptId;
    }

    public String getEmployeeField() {
        return employeeField;
    }

    public void setEmployeeField(String employeeField) {
        this.employeeField = employeeField;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public boolean isDenyAll() {
        return denyAll;
    }

    public void setDenyAll(boolean denyAll) {
        this.denyAll = denyAll;
    }

    public java.util.List<Long> getSubordinateDeptIds() {
        return subordinateDeptIds;
    }

    public void setSubordinateDeptIds(java.util.List<Long> subordinateDeptIds) {
        this.subordinateDeptIds = subordinateDeptIds;
    }
}
