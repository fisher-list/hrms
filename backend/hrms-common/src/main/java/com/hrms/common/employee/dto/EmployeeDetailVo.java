package com.hrms.common.employee.dto;

import com.hrms.common.employee.entity.*;
import lombok.Data;

import java.util.List;

/**
 * Type-safe VO for employee detail, replacing R<Map<String, Object>>.
 */
@Data
public class EmployeeDetailVo {

    private HrEmployee employee;
    private List<HrEmployeeEducation> educations;
    private List<HrEmployeeWorkExp> workExps;
    private List<HrEmployeeFamily> family;
    private List<HrEmployeeContract> contracts;
    private List<HrEmployeeBankAccount> bankAccounts;
    private List<HrEmployeeAddress> addresses;
}
