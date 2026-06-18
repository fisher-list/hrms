package com.hrms.common.rbac.annotation;

/**
 * Data-scope types that determine how row-level filtering is applied.
 */
public enum DataScopeType {

    /** No restriction — return all rows the tenant owns. */
    ALL,

    /** Only rows belonging to the user's own department. */
    OWN_DEPT,

    /** Rows belonging to the user's department and all its sub-departments. */
    SUBORDINATE_TREE,

    /** Only rows belonging to the user themselves (e.g. self-service). */
    SELF_ONLY
}
