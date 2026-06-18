package com.hrms.common.employee.dto;

import com.hrms.common.util.AesUtil;
import com.hrms.common.util.MaskUtil;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Employee list view object with masked sensitive fields.
 */
@Data
public class EmployeeListVo {

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
    private LocalDateTime createdAt;

    /** Masked ID card for list view. */
    private String idCardMasked;

    /** Masked phone for list view. */
    private String phoneMasked;

    // idCardEnc and phoneEnc are NOT exposed
}
