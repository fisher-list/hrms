package com.hrms.common.employee.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO for creating a new employee with sub-table data.
 */
@Data
public class EmployeeCreateDto {

    @NotBlank
    private String name;

    @NotBlank
    private String gender;

    private LocalDate birthDate;

    private String idCard;

    private String phone;

    private String email;

    @NotNull
    private Long deptId;

    @NotNull
    private Long positionId;

    @NotNull
    private LocalDate hireDate;

    private LocalDate contractStart;

    private LocalDate contractEnd;

    private LocalDate probationEnd;

    private String emergencyContact;

    private String emergencyPhone;

    @Valid
    private List<EducationDto> educations;

    @Valid
    private List<WorkExpDto> workExps;

    @Valid
    private List<FamilyDto> family;

    @Valid
    private List<ContractDto> contracts;

    @Valid
    private List<BankAccountDto> bankAccounts;

    @Valid
    private List<AddressDto> addresses;

    @Data
    public static class EducationDto {
        private Long id; // null = new, non-null = update existing
        @NotBlank
        private String school;
        private String degree;
        private String major;
        private LocalDate startDate;
        private LocalDate endDate;
        private Boolean isHighest;
    }

    @Data
    public static class WorkExpDto {
        private Long id; // null = new, non-null = update existing
        @NotBlank
        private String companyName;
        private String position;
        private LocalDate startDate;
        private LocalDate endDate;
        private String reasonForLeaving;
    }

    @Data
    public static class FamilyDto {
        private Long id; // null = new, non-null = update existing
        @NotBlank
        private String name;
        private String relationship;
        private String phone;
        private Boolean isEmergency;
    }

    @Data
    public static class ContractDto {
        private Long id; // null = new, non-null = update existing
        private String contractNo;
        private String contractType;
        private LocalDate startDate;
        private LocalDate endDate;
        private LocalDate signingDate;
        private String status;
    }

    @Data
    public static class BankAccountDto {
        private Long id; // null = new, non-null = update existing
        private String bankName;
        private String accountNo;
        private String accountName;
        private Boolean isPrimary;
    }

    @Data
    public static class AddressDto {
        private Long id; // null = new, non-null = update existing
        @NotBlank
        private String type;
        private String province;
        private String city;
        private String district;
        private String detail;
    }
}
