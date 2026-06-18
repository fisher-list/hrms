package com.hrms.common.org.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hrms.common.api.BizCode;
import com.hrms.common.exception.BizException;
import com.hrms.common.org.dto.CompanyDto;
import com.hrms.common.org.entity.Company;
import com.hrms.common.org.mapper.CompanyMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service layer for company CRUD.
 *
 * <p>Company is a singleton in this system; delete is physically rejected.</p>
 */
@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyMapper companyMapper;

    public Company getSingle() {
        return companyMapper.selectOne(
                new LambdaQueryWrapper<Company>().last("LIMIT 1"));
    }

    @Transactional
    public Company create(CompanyDto dto) {
        Long existing = companyMapper.selectCount(
                new LambdaQueryWrapper<Company>().eq(Company::getCode, dto.getCode()));
        if (existing > 0) {
            throw new BizException(BizCode.BAD_REQUEST, "Company code already exists: " + dto.getCode());
        }
        Company company = new Company();
        applyDto(company, dto);
        companyMapper.insert(company);
        return company;
    }

    @Transactional
    public Company update(Long id, CompanyDto dto) {
        Company company = companyMapper.selectById(id);
        if (company == null) {
            throw new BizException(BizCode.BAD_REQUEST, "Company not found: " + id);
        }
        applyDto(company, dto);
        companyMapper.updateById(company);
        return company;
    }

    /**
     * Delete is physically rejected for company.
     */
    public void delete(Long id) {
        throw new BizException(BizCode.BAD_REQUEST, "Company cannot be deleted");
    }

    private void applyDto(Company company, CompanyDto dto) {
        company.setCode(dto.getCode());
        company.setName(dto.getName());
        company.setLegalRepresentative(dto.getLegalRepresentative());
        company.setAddress(dto.getAddress());
        company.setPhone(dto.getPhone());
        company.setEmail(dto.getEmail());
    }
}
