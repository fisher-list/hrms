package com.hrms.common.employee.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hrms.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Employee family member.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hr_employee_family")
public class HrEmployeeFamily extends BaseEntity {

    private Long employeeId;

    private String name;

    private String relationship;

    /** AES-encrypted phone number (base64). */
    private String phoneEnc;

    private Boolean isEmergency;
}
