package com.hrms.common.employee.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 员工花名册导出VO。
 * 对应Excel导出的列定义。
 */
@Data
public class EmployeeExportVo {

    /** 工号 */
    private String empNo;

    /** 姓名 */
    private String name;

    /** 性别 */
    private String gender;

    /** 出生日期 */
    private LocalDate birthDate;

    /** 身份证号（脱敏） */
    private String idCardMasked;

    /** 手机号（脱敏） */
    private String phoneMasked;

    /** 邮箱 */
    private String email;

    /** 部门名称 */
    private String deptName;

    /** 岗位名称 */
    private String positionName;

    /** 入职日期 */
    private LocalDate hireDate;

    /** 状态 */
    private String status;

    /** 合同开始日期 */
    private LocalDate contractStart;

    /** 合同结束日期 */
    private LocalDate contractEnd;

    /** 试用期结束日期 */
    private LocalDate probationEnd;

    /** 紧急联系人 */
    private String emergencyContact;
}
