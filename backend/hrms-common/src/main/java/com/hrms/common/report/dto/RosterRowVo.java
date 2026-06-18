package com.hrms.common.report.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 人事花名册数据行VO
 * 包含员工档案的可选列，前端根据columns动态展示
 */
@Data
public class RosterRowVo {

    private Long id;
    private String empNo;
    private String name;
    private String gender;
    private LocalDate birthDate;
    private String email;
    private Long deptId;
    private String deptName;
    private Long positionId;
    private LocalDate hireDate;
    private String status;
    private LocalDate contractStart;
    private LocalDate contractEnd;
    private LocalDate probationEnd;
    private String emergencyContact;
}
