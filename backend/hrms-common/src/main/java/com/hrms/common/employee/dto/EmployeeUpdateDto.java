package com.hrms.common.employee.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO for updating an existing employee. All fields are optional (PATCH-style).
 */
@Data
public class EmployeeUpdateDto {

    private String name;

    private String gender;

    private LocalDate birthDate;

    private String idCard;

    private String phone;

    private String email;

    private Long deptId;

    private Long positionId;

    private LocalDate hireDate;

    private LocalDate contractStart;

    private LocalDate contractEnd;

    private LocalDate probationEnd;

    private String emergencyContact;

    private String emergencyPhone;

    /** 参保城市（用于匹配地区社保政策） */
    private String insuranceCity;

    private List<EmployeeCreateDto.EducationDto> educations;

    private List<EmployeeCreateDto.WorkExpDto> workExps;

    private List<EmployeeCreateDto.FamilyDto> family;

    private List<EmployeeCreateDto.ContractDto> contracts;

    private List<EmployeeCreateDto.BankAccountDto> bankAccounts;

    private List<EmployeeCreateDto.AddressDto> addresses;
}
