package com.hrms.common.performance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hrms.common.api.BizCode;
import com.hrms.common.employee.entity.HrEmployee;
import com.hrms.common.employee.mapper.HrEmployeeMapper;
import com.hrms.common.exception.BizException;
import com.hrms.common.performance.dto.CycleCreateDto;
import com.hrms.common.performance.entity.PfAppraisal;
import com.hrms.common.performance.entity.PfCycle;
import com.hrms.common.performance.mapper.PfAppraisalMapper;
import com.hrms.common.performance.mapper.PfCycleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Performance cycle management: create, activate, close, list.
 * On create, auto-generates appraisals for all ACTIVE employees.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CycleService {

    private static final Long DEFAULT_TEMPLATE_ID = 901L;

    private final PfCycleMapper cycleMapper;
    private final PfAppraisalMapper appraisalMapper;
    private final HrEmployeeMapper employeeMapper;

    /**
     * Create a new cycle in DRAFT status and auto-generate appraisals.
     */
    @Transactional
    public PfCycle create(CycleCreateDto dto) {
        PfCycle cycle = new PfCycle();
        cycle.setName(dto.getName());
        cycle.setStartDate(dto.getStartDate());
        cycle.setEndDate(dto.getEndDate());
        cycle.setStatus("DRAFT");
        cycle.setScopeType(dto.getScopeType() != null ? dto.getScopeType() : "ALL");
        cycle.setScopeDepts(dto.getScopeDepts());
        cycleMapper.insert(cycle);

        // Auto-generate appraisals for all ACTIVE employees
        List<HrEmployee> employees = employeeMapper.selectList(
                new LambdaQueryWrapper<HrEmployee>()
                        .eq(HrEmployee::getStatus, "ACTIVE"));

        for (HrEmployee emp : employees) {
            PfAppraisal appraisal = new PfAppraisal();
            appraisal.setCycleId(cycle.getId());
            appraisal.setTemplateId(DEFAULT_TEMPLATE_ID);
            appraisal.setEmployeeId(emp.getId());
            appraisal.setStatus("DRAFT");
            appraisalMapper.insert(appraisal);
        }

        log.info("Created performance cycle {} with {} appraisals", cycle.getId(), employees.size());
        return cycle;
    }

    /**
     * Activate a cycle (DRAFT -> ACTIVE).
     */
    @Transactional
    public PfCycle activate(Long id) {
        PfCycle cycle = cycleMapper.selectById(id);
        if (cycle == null) {
            throw new BizException(BizCode.APPRAISAL_NOT_FOUND, "Performance cycle not found");
        }
        if (!"DRAFT".equals(cycle.getStatus())) {
            throw new BizException(BizCode.APPRAISAL_INVALID_STATE,
                    "Cycle must be DRAFT to activate, current: " + cycle.getStatus());
        }
        cycle.setStatus("ACTIVE");
        cycleMapper.updateById(cycle);
        return cycle;
    }

    /**
     * Close a cycle (ACTIVE -> CLOSED).
     */
    @Transactional
    public PfCycle close(Long id) {
        PfCycle cycle = cycleMapper.selectById(id);
        if (cycle == null) {
            throw new BizException(BizCode.APPRAISAL_NOT_FOUND, "Performance cycle not found");
        }
        if (!"ACTIVE".equals(cycle.getStatus())) {
            throw new BizException(BizCode.APPRAISAL_INVALID_STATE,
                    "Cycle must be ACTIVE to close, current: " + cycle.getStatus());
        }
        cycle.setStatus("CLOSED");
        cycleMapper.updateById(cycle);
        return cycle;
    }

    /**
     * List all cycles.
     */
    public List<PfCycle> list() {
        return cycleMapper.selectList(
                new LambdaQueryWrapper<PfCycle>()
                        .orderByDesc(PfCycle::getCreatedAt));
    }
}
