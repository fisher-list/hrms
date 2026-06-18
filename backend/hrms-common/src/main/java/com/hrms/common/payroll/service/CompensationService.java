package com.hrms.common.payroll.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hrms.common.payroll.dto.CompensationCreateDto;
import com.hrms.common.payroll.entity.PyCompensationMaster;
import com.hrms.common.payroll.mapper.PyCompensationMasterMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for compensation master CRUD.
 */
@Service
@RequiredArgsConstructor
public class CompensationService {

    private final PyCompensationMasterMapper compensationMapper;

    /**
     * Create a new compensation record.
     */
    @Transactional
    public PyCompensationMaster create(CompensationCreateDto dto) {
        PyCompensationMaster entity = new PyCompensationMaster();
        entity.setEmployeeId(dto.getEmployeeId());
        entity.setBaseSalary(dto.getBaseSalary());
        entity.setPositionSalary(dto.getPositionSalary());
        entity.setPerformanceBase(dto.getPerformanceBase());
        entity.setAllowance(dto.getAllowance());
        entity.setEffectiveDate(dto.getEffectiveDate());
        compensationMapper.insert(entity);
        return entity;
    }

    /**
     * Get latest effective compensation for an employee (most recent effective_date).
     */
    public PyCompensationMaster getByEmployee(Long employeeId) {
        return compensationMapper.selectOne(
                new LambdaQueryWrapper<PyCompensationMaster>()
                        .eq(PyCompensationMaster::getEmployeeId, employeeId)
                        .orderByDesc(PyCompensationMaster::getEffectiveDate)
                        .last("LIMIT 1"));
    }

    /**
     * List all compensations with pagination.
     */
    public IPage<PyCompensationMaster> list(Page<PyCompensationMaster> page) {
        return compensationMapper.selectPage(page,
                new LambdaQueryWrapper<PyCompensationMaster>()
                        .orderByDesc(PyCompensationMaster::getEffectiveDate));
    }
}
