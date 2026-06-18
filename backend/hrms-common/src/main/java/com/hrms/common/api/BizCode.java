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

    /** 考勤异常处理错误 (40xx) */
    public static final int ANOMALY_NOT_FOUND = 40010;
    public static final int ANOMALY_INVALID_STATE = 40011;

    /** 调休管理错误 (41xx) */
    public static final int COMP_LEAVE_INSUFFICIENT = 41001;
    public static final int OVERTIME_NOT_APPROVED = 41002;

    /** 出差管理错误 (42xx) */
    public static final int TRIP_DATE_INVALID = 42001;

    /** Settlement domain errors (6xxx). */
    public static final int SETTLEMENT_ALREADY_COMPLETED = 6001;

    /** Payroll domain errors (7xxx). */
    public static final int PAYROLL_RUN_LOCKED = 7001;
    public static final int PAYROLL_DUPLICATE_RUN = 7002;
    public static final int PAYROLL_NO_COMPENSATION = 7003;
    /** 年终奖批次重复 */
    public static final int PAYROLL_YEAR_END_BONUS_DUPLICATE = 7004;
    /** 年终奖计算异常 */
    public static final int PAYROLL_YEAR_END_BONUS_ERROR = 7005;
    /** 银行代发批次异常 */
    public static final int PAYROLL_BANK_PAYMENT_ERROR = 7006;
    /** 工资模拟运行异常 */
    public static final int PAYROLL_SIMULATION_ERROR = 7007;
    /** 薪酬运行未锁定，无法生成银行代发 */
    public static final int PAYROLL_RUN_NOT_LOCKED = 7008;

    /** Recruitment domain errors (8xxx). */
    public static final int REQUISITION_CLOSED = 8001;
    public static final int CANDIDATE_DUPLICATE_ID_CARD = 8002;
    public static final int OFFER_ALREADY_APPROVED = 8003;
    public static final int CANDIDATE_NOT_ACCEPTED = 8004;

    /** 合同续签域错误 (10xxx). */
    public static final int CONTRACT_RENEWAL_NOT_FOUND = 10001;
    public static final int CONTRACT_RENEWAL_DUPLICATE = 10002;
    public static final int CONTRACT_NOT_EXPIRING = 10003;

    /** 试用期转正域错误 (11xxx). */
    public static final int PROBATION_CONVERSION_NOT_FOUND = 11001;
    public static final int PROBATION_CONVERSION_DUPLICATE = 11002;
    public static final int PROBATION_NOT_EXPIRING = 11003;

    /** 批量导入导出域错误 (12xxx). */
    public static final int BATCH_IMPORT_FORMAT_ERROR = 12001;
    public static final int BATCH_IMPORT_DATA_ERROR = 12002;

    /** Performance domain errors (9xxx). */
    public static final int APPRAISAL_NOT_FOUND = 9001;
    public static final int APPRAISAL_INVALID_STATE = 9002;
    public static final int APPRAISAL_ALREADY_EXISTS = 9003;
    /** 目标分解域错误 (91xx). */
    public static final int GOAL_NOT_FOUND = 9101;
    public static final int GOAL_WEIGHT_MISMATCH = 9102;

    /** 证明开具域错误 (13xxx). */
    public static final int CERTIFICATE_NOT_FOUND = 13001;
    public static final int CERTIFICATE_INVALID_TYPE = 13002;

    /** 法定报表域错误 (14xxx). */
    public static final int REPORT_NO_DATA = 14001;

    private BizCode() {
    }
}
