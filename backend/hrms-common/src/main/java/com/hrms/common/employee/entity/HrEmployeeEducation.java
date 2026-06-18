package com.hrms.common.employee.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hrms.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * Employee education history.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hr_employee_education")
public class HrEmployeeEducation extends BaseEntity {

    private Long employeeId;

    private String school;

    private String degree;

    private String major;

    private LocalDate startDate;

    private LocalDate endDate;

    private Boolean isHighest;
}
