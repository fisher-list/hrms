package com.hrms.common.report.dto;

import lombok.Data;

import java.util.List;

/**
 * 人事花名册查询条件DTO
 * 支持自定义列、筛选条件、排序
 */
@Data
public class RosterQueryDto {

    /** 关键字搜索 (姓名/工号) */
    private String keyword;

    /** 员工状态筛选: ACTIVE/PROBATION/ON_LEAVE/TERMINATED等 */
    private String status;

    /** 部门ID筛选 */
    private Long deptId;

    /** 在职状态筛选: ACTIVE / TERMINATED 等 */
    private String empStatus;

    /**
     * 需要返回的列名列表，为空时返回全部默认列
     * 可选值: empNo, name, gender, birthDate, email, deptName, hireDate,
     *         status, contractStart, contractEnd, probationEnd
     */
    private List<String> columns;

    /** 排序字段 */
    private String orderBy;

    /** 排序方向: ASC / DESC */
    private String orderDir;
}
