package com.hrms.common.employee.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hrms.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * Employee master entity.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hr_employee")
public class HrEmployee extends BaseEntity {

    /** Employee number, auto-generated: E + zero-padded snowflake id. */
    private String empNo;

    private String name;

    /** M=male, F=female */
    private String gender;

    private LocalDate birthDate;

    /** AES-encrypted ID card number (base64). */
    private String idCardEnc;

    /** HMAC-SHA256 hash used only for duplicate checks. */
    private String idCardHash;

    /** AES-encrypted phone number (base64). */
    private String phoneEnc;

    private String email;

    private Long deptId;

    private Long positionId;

    private LocalDate hireDate;

    /** PENDING_HIRE / PROBATION / ACTIVE / ON_LEAVE / TERMINATED */
    private String status;

    private LocalDate contractStart;

    private LocalDate contractEnd;

    private LocalDate probationEnd;

    private String emergencyContact;

    /** AES-encrypted emergency phone (base64). */
    private String emergencyPhoneEnc;
}
