package com.hrms.common.employee.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 证明开具详情VO。
 */
@Data
public class CertificateVo {

    private Long id;
    private Long employeeId;
    private String employeeName;
    private String empNo;
    private String type;
    private String purpose;
    private Integer copies;
    private String status;
    private Long approvalInstanceId;
    private LocalDateTime issuedAt;
    private LocalDate incomeStartDate;
    private LocalDate incomeEndDate;
    private String rejectReason;
}
