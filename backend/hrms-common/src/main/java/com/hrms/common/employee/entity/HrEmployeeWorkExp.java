package com.hrms.common.employee.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hrms.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * Employee work experience.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hr_employee_work_exp")
public class HrEmployeeWorkExp extends BaseEntity {

    private Long employeeId;

    private String companyName;

    private String position;

    private LocalDate startDate;

    private LocalDate endDate;

    private String reasonForLeaving;
}
