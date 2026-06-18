package com.hrms.common.api;

/**
 * Business code constants for {@link R}.
 *
 * <p>Range convention:
 * <ul>
 *   <li>0       — success</li>
 *   <li>400-499 — client side errors</li>
 *   <li>500     — internal server error</li>
 *   <li>1xxx    — auth domain (this story)</li>
 * </ul>
 */
public final class BizCode {

    public static final int OK = 0;

    public static final int BAD_REQUEST = 400;
    public static final int UNAUTHORIZED = 401;
    public static final int FORBIDDEN = 403;
    public static final int INTERNAL_ERROR = 500;
    public static final int TOO_MANY_REQUESTS = 429;

    /** Username or password incorrect. */
    public static final int AUTH_BAD_CREDENTIALS = 1001;
    /** Account locked due to too many failed attempts. */
    public static final int AUTH_ACCOUNT_LOCKED = 1002;
    /** Account disabled by administrator. */
    public static final int AUTH_ACCOUNT_DISABLED = 1003;
    /** Refresh token invalid / expired / revoked. */
    public static final int AUTH_REFRESH_INVALID = 1004;
    /** New password does not meet strength requirements. */
    public static final int AUTH_PASSWORD_WEAK = 1005;
    /** Old password is incorrect. */
    public static final int AUTH_OLD_PASSWORD_WRONG = 1006;
    /** New password same as old password. */
    public static final int AUTH_PASSWORD_SAME = 1007;

    /** Approval domain errors (2xxx). */
    public static final int APPROVAL_DEF_NOT_FOUND = 2001;
    public static final int APPROVAL_TASK_NOT_FOUND = 2002;
    public static final int APPROVAL_INVALID_STATE = 2003;

    /** Employee domain errors (3xxx). */
    public static final int EMPLOYEE_NOT_FOUND = 3001;
    public static final int EMPLOYEE_DUPLICATE_ID_CARD = 3002;
    public static final int EMPLOYEE_INVALID_STATE_TRANSITION = 3003;

    /** Attendance domain errors (4xxx). */
    public static final int SHIFT_IN_USE = 4001;
    public static final int ATTENDANCE_PUNCH_INVALID = 4002;
    public static final int SCHEDULE_CONFLICT = 4003;

    /** Leave domain errors (5xxx). */
    public static final int LEAVE_BALANCE_INSUFFICIENT = 5001;
    public static final int LEAVE_DATE_CONFLICT = 5002;
    public static final int LEAVE_DATE_INVALID = 5003;

    /** Settlement domain errors (6xxx). */
    public static final int SETTLEMENT_ALREADY_COMPLETED = 6001;

    /** Payroll domain errors (7xxx). */
    public static final int PAYROLL_RUN_LOCKED = 7001;
    public static final int PAYROLL_DUPLICATE_RUN = 7002;
    public static final int PAYROLL_NO_COMPENSATION = 7003;

    /** Recruitment domain errors (8xxx). */
    public static final int REQUISITION_CLOSED = 8001;
    public static final int CANDIDATE_DUPLICATE_ID_CARD = 8002;
    public static final int OFFER_ALREADY_APPROVED = 8003;
    public static final int CANDIDATE_NOT_ACCEPTED = 8004;

    /** Performance domain errors (9xxx). */
    public static final int APPRAISAL_NOT_FOUND = 9001;
    public static final int APPRAISAL_INVALID_STATE = 9002;
    public static final int APPRAISAL_ALREADY_EXISTS = 9003;

    private BizCode() {
    }
}
