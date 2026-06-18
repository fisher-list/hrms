package com.hrms.common.employee.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 即将到期合同VO，用于前端预警列表展示。
 * 包含预警等级标记：URGENT(红色，7天内到期) / WARNING(黄色，30天内到期)。
 */
@Data
public class ExpiringContractVo {

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

    /** 合同ID */
    private Long contractId;

    /** 合同编号 */
    private String contractNo;

    /** 合同类型 */
    private String contractType;

    /** 合同开始日期 */
    private LocalDate contractStartDate;

    /** 合同结束日期 */
    private LocalDate contractEndDate;

    /** 距离到期天数（负数表示已过期） */
    private Long daysUntilExpiry;

    /**
     * 预警等级:
     * URGENT - 红色标记，7天内到期或已过期
     * WARNING - 黄色标记，30天内到期
     */
    private String alertLevel;
}
