package com.hrms.common.payroll.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hrms.common.api.BizCode;
import com.hrms.common.exception.BizException;
import com.hrms.common.payroll.dto.PayrollPeriodCreateDto;
import com.hrms.common.payroll.entity.PyPayrollPeriod;
import com.hrms.common.payroll.mapper.PyPayrollPeriodMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for payroll period CRUD.
 */
@Service
@RequiredArgsConstructor
public class PayrollPeriodService {

    private final PyPayrollPeriodMapper periodMapper;

    /**
     * Create a new payroll period.
     * Validates format and uniqueness.
     */
    @Transactional
    public PyPayrollPeriod create(PayrollPeriodCreateDto dto) {
        // Check for duplicate period_month
        Long count = periodMapper.selectCount(
                new LambdaQueryWrapper<PyPayrollPeriod>()
                        .eq(PyPayrollPeriod::getPeriodMonth, dto.getPeriodMonth()));
        if (count > 0) {
            throw new BizException(BizCode.BAD_REQUEST,
                    "Payroll period " + dto.getPeriodMonth() + " already exists");
        }

        PyPayrollPeriod period = new PyPayrollPeriod();
        period.setPeriodMonth(dto.getPeriodMonth());
        period.setStatus("DRAFT");
        periodMapper.insert(period);
        return period;
    }

    /**
     * List all payroll periods.
     */
    public List<PyPayrollPeriod> list() {
        return periodMapper.selectList(
                new LambdaQueryWrapper<PyPayrollPeriod>()
                        .orderByDesc(PyPayrollPeriod::getPeriodMonth));
    }
}
