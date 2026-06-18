package com.hrms.common.employee.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 即将到期试用期员工VO，用于前端展示试用期转正提醒。
 * 包含预警等级标记：URGENT(红色，7天内到期) / WARNING(黄色，30天内到期)。
 */
@Data
public class ProbationEmployeeVo {

    /** 员工ID */
    private Long employeeId;

    /** 员工工号 */
    private String empNo;

    /** 员工姓名 */
    private String name;

    /** 部门ID */
    private Long deptId;

    /** 部门名称 */
    private String deptName;

    /** 岗位ID */
    private Long positionId;

    /** 入职日期 */
    private LocalDate hireDate;

    /** 试用期结束日期 */
    private LocalDate probationEndDate;

    /** 距离试用期到期天数 */
    private Long daysUntilExpiry;

    /**
     * 预警等级:
     * URGENT - 红色标记，7天内到期或已过期
     * WARNING - 黄色标记，30天内到期
     */
    private String alertLevel;

    /** 是否已有待处理的转正申请 */
    private Boolean hasPendingConversion;
}
