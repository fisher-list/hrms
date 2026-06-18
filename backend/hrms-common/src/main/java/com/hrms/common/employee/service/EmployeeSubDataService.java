package com.hrms.common.employee.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hrms.common.employee.dto.EmployeeCreateDto;
import com.hrms.common.employee.dto.EmployeeUpdateDto;
import com.hrms.common.employee.entity.*;
import com.hrms.common.employee.mapper.*;
import com.hrms.common.util.AesUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service for employee sub-table CRUD with diff-based update strategy.
 * Only inserts new records, updates changed records, and deletes removed records.
 */
@Service
@RequiredArgsConstructor
public class EmployeeSubDataService {

    private final HrEmployeeEducationMapper educationMapper;
    private final HrEmployeeWorkExpMapper workExpMapper;
    private final HrEmployeeFamilyMapper familyMapper;
    private final HrEmployeeContractMapper contractMapper;
    private final HrEmployeeBankAccountMapper bankAccountMapper;
    private final HrEmployeeAddressMapper addressMapper;

    // ---- list methods for getDetailById ----

    public List<HrEmployeeEducation> listEducations(Long employeeId) {
        return educationMapper.selectList(
                new LambdaQueryWrapper<HrEmployeeEducation>().eq(HrEmployeeEducation::getEmployeeId, employeeId));
    }

    public List<HrEmployeeWorkExp> listWorkExps(Long employeeId) {
        return workExpMapper.selectList(
                new LambdaQueryWrapper<HrEmployeeWorkExp>().eq(HrEmployeeWorkExp::getEmployeeId, employeeId));
    }

    public List<HrEmployeeFamily> listFamily(Long employeeId) {
        return familyMapper.selectList(
                new LambdaQueryWrapper<HrEmployeeFamily>().eq(HrEmployeeFamily::getEmployeeId, employeeId));
    }

    public List<HrEmployeeContract> listContracts(Long employeeId) {
        return contractMapper.selectList(
                new LambdaQueryWrapper<HrEmployeeContract>().eq(HrEmployeeContract::getEmployeeId, employeeId));
    }

    public List<HrEmployeeBankAccount> listBankAccounts(Long employeeId) {
        return bankAccountMapper.selectList(
                new LambdaQueryWrapper<HrEmployeeBankAccount>().eq(HrEmployeeBankAccount::getEmployeeId, employeeId));
    }

    public List<HrEmployeeAddress> listAddresses(Long employeeId) {
        return addressMapper.selectList(
                new LambdaQueryWrapper<HrEmployeeAddress>().eq(HrEmployeeAddress::getEmployeeId, employeeId));
    }

    // ---- insert sub-tables (for create) ----

    public void insertSubTables(Long employeeId, EmployeeCreateDto dto) {
        if (dto.getEducations() != null) {
            dto.getEducations().forEach(d -> {
                HrEmployeeEducation e = new HrEmployeeEducation();
                e.setEmployeeId(employeeId);
                e.setSchool(d.getSchool());
                e.setDegree(d.getDegree());
                e.setMajor(d.getMajor());
                e.setStartDate(d.getStartDate());
                e.setEndDate(d.getEndDate());
                e.setIsHighest(d.getIsHighest());
                educationMapper.insert(e);
            });
        }
        if (dto.getWorkExps() != null) {
            dto.getWorkExps().forEach(d -> {
                HrEmployeeWorkExp e = new HrEmployeeWorkExp();
                e.setEmployeeId(employeeId);
                e.setCompanyName(d.getCompanyName());
                e.setPosition(d.getPosition());
                e.setStartDate(d.getStartDate());
                e.setEndDate(d.getEndDate());
                e.setReasonForLeaving(d.getReasonForLeaving());
                workExpMapper.insert(e);
            });
        }
        if (dto.getFamily() != null) {
            dto.getFamily().forEach(d -> {
                HrEmployeeFamily e = new HrEmployeeFamily();
                e.setEmployeeId(employeeId);
                e.setName(d.getName());
                e.setRelationship(d.getRelationship());
                e.setPhoneEnc(AesUtil.encrypt(d.getPhone()));
                e.setIsEmergency(d.getIsEmergency());
                familyMapper.insert(e);
            });
        }
        if (dto.getContracts() != null) {
            dto.getContracts().forEach(d -> {
                HrEmployeeContract e = new HrEmployeeContract();
                e.setEmployeeId(employeeId);
                e.setContractNo(d.getContractNo());
                e.setContractType(d.getContractType());
                e.setStartDate(d.getStartDate());
                e.setEndDate(d.getEndDate());
                e.setSigningDate(d.getSigningDate());
                e.setStatus(d.getStatus());
                contractMapper.insert(e);
            });
        }
        if (dto.getBankAccounts() != null) {
            dto.getBankAccounts().forEach(d -> {
                HrEmployeeBankAccount e = new HrEmployeeBankAccount();
                e.setEmployeeId(employeeId);
                e.setBankName(d.getBankName());
                e.setAccountNoEnc(AesUtil.encrypt(d.getAccountNo()));
                e.setAccountName(d.getAccountName());
                e.setIsPrimary(d.getIsPrimary());
                bankAccountMapper.insert(e);
            });
        }
        if (dto.getAddresses() != null) {
            dto.getAddresses().forEach(d -> {
                HrEmployeeAddress e = new HrEmployeeAddress();
                e.setEmployeeId(employeeId);
                e.setType(d.getType());
                e.setProvince(d.getProvince());
                e.setCity(d.getCity());
                e.setDistrict(d.getDistrict());
                e.setDetail(d.getDetail());
                addressMapper.insert(e);
            });
        }
    }

    // ---- diff-based update sub-tables (P2-2 fix) ----

    public void updateSubTables(Long employeeId, EmployeeUpdateDto dto) {
        if (dto.getEducations() != null) {
            diffUpdateEducations(employeeId, dto.getEducations());
        }
        if (dto.getWorkExps() != null) {
            diffUpdateWorkExps(employeeId, dto.getWorkExps());
        }
        if (dto.getFamily() != null) {
            diffUpdateFamily(employeeId, dto.getFamily());
        }
        if (dto.getContracts() != null) {
            diffUpdateContracts(employeeId, dto.getContracts());
        }
        if (dto.getBankAccounts() != null) {
            diffUpdateBankAccounts(employeeId, dto.getBankAccounts());
        }
        if (dto.getAddresses() != null) {
            diffUpdateAddresses(employeeId, dto.getAddresses());
        }
    }

    // ---- private diff helpers ----

    private void diffUpdateEducations(Long employeeId, List<EmployeeCreateDto.EducationDto> dtos) {
        List<HrEmployeeEducation> existing = listEducations(employeeId);
        Map<Long, HrEmployeeEducation> existingMap = existing.stream()
                .collect(Collectors.toMap(HrEmployeeEducation::getId, Function.identity()));

        Set<Long> dtoIds = dtos.stream()
                .filter(d -> d.getId() != null)
                .map(EmployeeCreateDto.EducationDto::getId)
                .collect(Collectors.toSet());

        // Delete removed records
        for (HrEmployeeEducation old : existing) {
            if (!dtoIds.contains(old.getId())) {
                educationMapper.deleteById(old.getId());
            }
        }

        for (EmployeeCreateDto.EducationDto d : dtos) {
            if (d.getId() != null && existingMap.containsKey(d.getId())) {
                // Update existing
                HrEmployeeEducation e = existingMap.get(d.getId());
                e.setSchool(d.getSchool());
                e.setDegree(d.getDegree());
                e.setMajor(d.getMajor());
                e.setStartDate(d.getStartDate());
                e.setEndDate(d.getEndDate());
                e.setIsHighest(d.getIsHighest());
                educationMapper.updateById(e);
            } else {
                // Insert new
                HrEmployeeEducation e = new HrEmployeeEducation();
                e.setEmployeeId(employeeId);
                e.setSchool(d.getSchool());
                e.setDegree(d.getDegree());
                e.setMajor(d.getMajor());
                e.setStartDate(d.getStartDate());
                e.setEndDate(d.getEndDate());
                e.setIsHighest(d.getIsHighest());
                educationMapper.insert(e);
            }
        }
    }

    private void diffUpdateWorkExps(Long employeeId, List<EmployeeCreateDto.WorkExpDto> dtos) {
        List<HrEmployeeWorkExp> existing = listWorkExps(employeeId);
        Map<Long, HrEmployeeWorkExp> existingMap = existing.stream()
                .collect(Collectors.toMap(HrEmployeeWorkExp::getId, Function.identity()));

        Set<Long> dtoIds = dtos.stream()
                .filter(d -> d.getId() != null)
                .map(EmployeeCreateDto.WorkExpDto::getId)
                .collect(Collectors.toSet());

        for (HrEmployeeWorkExp old : existing) {
            if (!dtoIds.contains(old.getId())) {
                workExpMapper.deleteById(old.getId());
            }
        }

        for (EmployeeCreateDto.WorkExpDto d : dtos) {
            if (d.getId() != null && existingMap.containsKey(d.getId())) {
                HrEmployeeWorkExp e = existingMap.get(d.getId());
                e.setCompanyName(d.getCompanyName());
                e.setPosition(d.getPosition());
                e.setStartDate(d.getStartDate());
                e.setEndDate(d.getEndDate());
                e.setReasonForLeaving(d.getReasonForLeaving());
                workExpMapper.updateById(e);
            } else {
                HrEmployeeWorkExp e = new HrEmployeeWorkExp();
                e.setEmployeeId(employeeId);
                e.setCompanyName(d.getCompanyName());
                e.setPosition(d.getPosition());
                e.setStartDate(d.getStartDate());
                e.setEndDate(d.getEndDate());
                e.setReasonForLeaving(d.getReasonForLeaving());
                workExpMapper.insert(e);
            }
        }
    }

    private void diffUpdateFamily(Long employeeId, List<EmployeeCreateDto.FamilyDto> dtos) {
        List<HrEmployeeFamily> existing = listFamily(employeeId);
        Map<Long, HrEmployeeFamily> existingMap = existing.stream()
                .collect(Collectors.toMap(HrEmployeeFamily::getId, Function.identity()));

        Set<Long> dtoIds = dtos.stream()
                .filter(d -> d.getId() != null)
                .map(EmployeeCreateDto.FamilyDto::getId)
                .collect(Collectors.toSet());

        for (HrEmployeeFamily old : existing) {
            if (!dtoIds.contains(old.getId())) {
                familyMapper.deleteById(old.getId());
            }
        }

        for (EmployeeCreateDto.FamilyDto d : dtos) {
            if (d.getId() != null && existingMap.containsKey(d.getId())) {
                HrEmployeeFamily e = existingMap.get(d.getId());
                e.setName(d.getName());
                e.setRelationship(d.getRelationship());
                e.setPhoneEnc(AesUtil.encrypt(d.getPhone()));
                e.setIsEmergency(d.getIsEmergency());
                familyMapper.updateById(e);
            } else {
                HrEmployeeFamily e = new HrEmployeeFamily();
                e.setEmployeeId(employeeId);
                e.setName(d.getName());
                e.setRelationship(d.getRelationship());
                e.setPhoneEnc(AesUtil.encrypt(d.getPhone()));
                e.setIsEmergency(d.getIsEmergency());
                familyMapper.insert(e);
            }
        }
    }

    private void diffUpdateContracts(Long employeeId, List<EmployeeCreateDto.ContractDto> dtos) {
        List<HrEmployeeContract> existing = listContracts(employeeId);
        Map<Long, HrEmployeeContract> existingMap = existing.stream()
                .collect(Collectors.toMap(HrEmployeeContract::getId, Function.identity()));

        Set<Long> dtoIds = dtos.stream()
                .filter(d -> d.getId() != null)
                .map(EmployeeCreateDto.ContractDto::getId)
                .collect(Collectors.toSet());

        for (HrEmployeeContract old : existing) {
            if (!dtoIds.contains(old.getId())) {
                contractMapper.deleteById(old.getId());
            }
        }

        for (EmployeeCreateDto.ContractDto d : dtos) {
            if (d.getId() != null && existingMap.containsKey(d.getId())) {
                HrEmployeeContract e = existingMap.get(d.getId());
                e.setContractNo(d.getContractNo());
                e.setContractType(d.getContractType());
                e.setStartDate(d.getStartDate());
                e.setEndDate(d.getEndDate());
                e.setSigningDate(d.getSigningDate());
                e.setStatus(d.getStatus());
                contractMapper.updateById(e);
            } else {
                HrEmployeeContract e = new HrEmployeeContract();
                e.setEmployeeId(employeeId);
                e.setContractNo(d.getContractNo());
                e.setContractType(d.getContractType());
                e.setStartDate(d.getStartDate());
                e.setEndDate(d.getEndDate());
                e.setSigningDate(d.getSigningDate());
                e.setStatus(d.getStatus());
                contractMapper.insert(e);
            }
        }
    }

    private void diffUpdateBankAccounts(Long employeeId, List<EmployeeCreateDto.BankAccountDto> dtos) {
        List<HrEmployeeBankAccount> existing = listBankAccounts(employeeId);
        Map<Long, HrEmployeeBankAccount> existingMap = existing.stream()
                .collect(Collectors.toMap(HrEmployeeBankAccount::getId, Function.identity()));

        Set<Long> dtoIds = dtos.stream()
                .filter(d -> d.getId() != null)
                .map(EmployeeCreateDto.BankAccountDto::getId)
                .collect(Collectors.toSet());

        for (HrEmployeeBankAccount old : existing) {
            if (!dtoIds.contains(old.getId())) {
                bankAccountMapper.deleteById(old.getId());
            }
        }

        for (EmployeeCreateDto.BankAccountDto d : dtos) {
            if (d.getId() != null && existingMap.containsKey(d.getId())) {
                HrEmployeeBankAccount e = existingMap.get(d.getId());
                e.setBankName(d.getBankName());
                e.setAccountNoEnc(AesUtil.encrypt(d.getAccountNo()));
                e.setAccountName(d.getAccountName());
                e.setIsPrimary(d.getIsPrimary());
                bankAccountMapper.updateById(e);
            } else {
                HrEmployeeBankAccount e = new HrEmployeeBankAccount();
                e.setEmployeeId(employeeId);
                e.setBankName(d.getBankName());
                e.setAccountNoEnc(AesUtil.encrypt(d.getAccountNo()));
                e.setAccountName(d.getAccountName());
                e.setIsPrimary(d.getIsPrimary());
                bankAccountMapper.insert(e);
            }
        }
    }

    private void diffUpdateAddresses(Long employeeId, List<EmployeeCreateDto.AddressDto> dtos) {
        List<HrEmployeeAddress> existing = listAddresses(employeeId);
        Map<Long, HrEmployeeAddress> existingMap = existing.stream()
                .collect(Collectors.toMap(HrEmployeeAddress::getId, Function.identity()));

        Set<Long> dtoIds = dtos.stream()
                .filter(d -> d.getId() != null)
                .map(EmployeeCreateDto.AddressDto::getId)
                .collect(Collectors.toSet());

        for (HrEmployeeAddress old : existing) {
            if (!dtoIds.contains(old.getId())) {
                addressMapper.deleteById(old.getId());
            }
        }

        for (EmployeeCreateDto.AddressDto d : dtos) {
            if (d.getId() != null && existingMap.containsKey(d.getId())) {
                HrEmployeeAddress e = existingMap.get(d.getId());
                e.setType(d.getType());
                e.setProvince(d.getProvince());
                e.setCity(d.getCity());
                e.setDistrict(d.getDistrict());
                e.setDetail(d.getDetail());
                addressMapper.updateById(e);
            } else {
                HrEmployeeAddress e = new HrEmployeeAddress();
                e.setEmployeeId(employeeId);
                e.setType(d.getType());
                e.setProvince(d.getProvince());
                e.setCity(d.getCity());
                e.setDistrict(d.getDistrict());
                e.setDetail(d.getDetail());
                addressMapper.insert(e);
            }
        }
    }
}
