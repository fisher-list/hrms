package com.hrms.common.employee.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hrms.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Employee address (HOME or REGISTERED).
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hr_employee_address")
public class HrEmployeeAddress extends BaseEntity {

    private Long employeeId;

    /** HOME or REGISTERED */
    private String type;

    private String province;

    private String city;

    private String district;

    private String detail;
}
