package com.hrms.common.employee.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hrms.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Employee bank account.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hr_employee_bank_account")
public class HrEmployeeBankAccount extends BaseEntity {

    private Long employeeId;

    private String bankName;

    /** AES-encrypted bank account number (base64). */
    private String accountNoEnc;

    private String accountName;

    private Boolean isPrimary;
}
