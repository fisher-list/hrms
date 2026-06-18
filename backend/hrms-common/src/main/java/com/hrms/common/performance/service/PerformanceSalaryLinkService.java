package com.hrms.common.performance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hrms.common.api.BizCode;
import com.hrms.common.employee.entity.HrEmployee;
import com.hrms.common.employee.mapper.HrEmployeeMapper;
import com.hrms.common.exception.BizException;
import com.hrms.common.payroll.entity.PyCompensationMaster;
import com.hrms.common.payroll.mapper.PyCompensationMasterMapper;
import com.hrms.common.performance.dto.SalaryAdjustmentVo;
import com.hrms.common.performance.entity.PfAppraisal;
import com.hrms.common.performance.entity.PfSalaryAdjustment;
import com.hrms.common.performance.mapper.PfAppraisalMapper;
import com.hrms.common.performance.mapper.PfSalaryAdjustmentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 绩效结果联动薪酬调整服务。
 * <p>
 * 绩效等级与调薪比例映射：
 * S级(≥90分): 加薪20%
 * A级(≥80分): 加薪10%
 * B级(≥70分): 加薪5%
 * C级(≥60分): 不调薪
 * D级(<60分): 降薪5%
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PerformanceSalaryLinkService {

    private final PfAppraisalMapper appraisalMapper;
    private final PfSalaryAdjustmentMapper salaryAdjustmentMapper;
    private final PyCompensationMasterMapper compensationMapper;
    private final HrEmployeeMapper employeeMapper;

    /**
     * 为已完成的考核自动生成薪酬调整建议单。
     */
    @Transactional
    public PfSalaryAdjustment generateAdjustment(Long appraisalId) {
        PfAppraisal appraisal = appraisalMapper.selectById(appraisalId);
        if (appraisal == null) {
            throw new BizException(BizCode.APPRAISAL_NOT_FOUND, "考核不存在: " + appraisalId);
        }
        if (!"COMPLETED".equals(appraisal.getStatus())) {
            throw new BizException(BizCode.APPRAISAL_INVALID_STATE, "考核未完成，无法生成调薪建议");
        }

        // 检查是否已生成
        PfSalaryAdjustment existing = salaryAdjustmentMapper.selectOne(
                new LambdaQueryWrapper<PfSalaryAdjustment>()
                        .eq(PfSalaryAdjustment::getAppraisalId, appraisalId));
        if (existing != null) {
            throw new BizException(BizCode.BAD_REQUEST, "该考核已生成调薪建议");
        }

        BigDecimal finalScore = appraisal.getFinalScore();
        String grade = determineGrade(finalScore);
        BigDecimal adjustmentPct = getAdjustmentPct(grade);

        // 获取当前薪酬
        PyCompensationMaster compensation = compensationMapper.selectOne(
                new LambdaQueryWrapper<PyCompensationMaster>()
                        .eq(PyCompensationMaster::getEmployeeId, appraisal.getEmployeeId())
                        .orderByDesc(PyCompensationMaster::getEffectiveDate)
                        .last("LIMIT 1"));

        BigDecimal currentBase = compensation != null ? compensation.getBaseSalary() : BigDecimal.ZERO;
        BigDecimal suggestedSalary = currentBase.multiply(BigDecimal.ONE.add(adjustmentPct.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)))
                .setScale(2, RoundingMode.HALF_UP);

        PfSalaryAdjustment adj = new PfSalaryAdjustment();
        adj.setEmployeeId(appraisal.getEmployeeId());
        adj.setAppraisalId(appraisalId);
        adj.setCycleId(appraisal.getCycleId());
        adj.setGrade(grade);
        adj.setFinalScore(finalScore);
        adj.setCurrentBaseSalary(currentBase);
        adj.setAdjustmentPct(adjustmentPct);
        adj.setSuggestedSalary(suggestedSalary);
        adj.setEffectiveDate(LocalDate.now().plusMonths(1).withDayOfMonth(1));
        adj.setStatus("PENDING");
        adj.setRemark("绩效等级" + grade + "，建议调薪" + adjustmentPct + "%");
        salaryAdjustmentMapper.insert(adj);

        log.info("生成调薪建议: 员工={}, 等级={}, 调薪比例={}%", appraisal.getEmployeeId(), grade, adjustmentPct);
        return adj;
    }

    /**
     * 批量为周期内所有已完成考核生成调薪建议。
     */
    @Transactional
    public List<PfSalaryAdjustment> batchGenerateForCycle(Long cycleId) {
        List<PfAppraisal> appraisals = appraisalMapper.selectList(
                new LambdaQueryWrapper<PfAppraisal>()
                        .eq(PfAppraisal::getCycleId, cycleId)
                        .eq(PfAppraisal::getStatus, "COMPLETED"));

        List<PfSalaryAdjustment> result = new ArrayList<>();
        for (PfAppraisal appraisal : appraisals) {
            // 跳过已生成的
            PfSalaryAdjustment existing = salaryAdjustmentMapper.selectOne(
                    new LambdaQueryWrapper<PfSalaryAdjustment>()
                            .eq(PfSalaryAdjustment::getAppraisalId, appraisal.getId()));
            if (existing == null) {
                result.add(generateAdjustment(appraisal.getId()));
            }
        }
        return result;
    }

    /**
     * 审批通过调薪建议。
     */
    @Transactional
    public void approve(Long adjustmentId) {
        PfSalaryAdjustment adj = salaryAdjustmentMapper.selectById(adjustmentId);
        if (adj == null) {
            throw new BizException(BizCode.BAD_REQUEST, "调薪建议不存在: " + adjustmentId);
        }
        if (!"PENDING".equals(adj.getStatus())) {
            throw new BizException(BizCode.APPRAISAL_INVALID_STATE, "当前状态不允许审批: " + adj.getStatus());
        }
        adj.setStatus("APPROVED");
        salaryAdjustmentMapper.updateById(adj);
    }

    /**
     * 拒绝调薪建议。
     */
    @Transactional
    public void reject(Long adjustmentId, String reason) {
        PfSalaryAdjustment adj = salaryAdjustmentMapper.selectById(adjustmentId);
        if (adj == null) {
            throw new BizException(BizCode.BAD_REQUEST, "调薪建议不存在: " + adjustmentId);
        }
        if (!"PENDING".equals(adj.getStatus())) {
            throw new BizException(BizCode.APPRAISAL_INVALID_STATE, "当前状态不允许拒绝: " + adj.getStatus());
        }
        adj.setStatus("REJECTED");
        adj.setRemark(reason);
        salaryAdjustmentMapper.updateById(adj);
    }

    /**
     * 执行已审批的调薪（更新薪酬主数据）。
     */
    @Transactional
    public void execute(Long adjustmentId) {
        PfSalaryAdjustment adj = salaryAdjustmentMapper.selectById(adjustmentId);
        if (adj == null) {
            throw new BizException(BizCode.BAD_REQUEST, "调薪建议不存在: " + adjustmentId);
        }
        if (!"APPROVED".equals(adj.getStatus())) {
            throw new BizException(BizCode.APPRAISAL_INVALID_STATE, "仅已审批的建议可执行: " + adj.getStatus());
        }

        // 更新薪酬主数据
        PyCompensationMaster newComp = new PyCompensationMaster();
        newComp.setEmployeeId(adj.getEmployeeId());
        newComp.setBaseSalary(adj.getSuggestedSalary());
        newComp.setEffectiveDate(adj.getEffectiveDate());
        compensationMapper.insert(newComp);

        adj.setStatus("EXECUTED");
        salaryAdjustmentMapper.updateById(adj);
        log.info("执行调薪: 员工={}, 新工资={}", adj.getEmployeeId(), adj.getSuggestedSalary());
    }

    /**
     * 查询调薪建议列表。
     */
    public List<SalaryAdjustmentVo> list(Long employeeId, Long cycleId, String status) {
        LambdaQueryWrapper<PfSalaryAdjustment> wrapper = new LambdaQueryWrapper<>();
        if (employeeId != null) {
            wrapper.eq(PfSalaryAdjustment::getEmployeeId, employeeId);
        }
        if (cycleId != null) {
            wrapper.eq(PfSalaryAdjustment::getCycleId, cycleId);
        }
        if (status != null) {
            wrapper.eq(PfSalaryAdjustment::getStatus, status);
        }
        wrapper.orderByDesc(PfSalaryAdjustment::getCreatedAt);

        return salaryAdjustmentMapper.selectList(wrapper).stream().map(this::toVo).toList();
    }

    /**
     * 根据分数确定绩效等级。
     */
    private String determineGrade(BigDecimal score) {
        if (score.compareTo(new BigDecimal("90")) >= 0) return "S";
        if (score.compareTo(new BigDecimal("80")) >= 0) return "A";
        if (score.compareTo(new BigDecimal("70")) >= 0) return "B";
        if (score.compareTo(new BigDecimal("60")) >= 0) return "C";
        return "D";
    }

    /**
     * 根据绩效等级获取调薪比例。
     */
    private BigDecimal getAdjustmentPct(String grade) {
        return switch (grade) {
            case "S" -> new BigDecimal("20.00");
            case "A" -> new BigDecimal("10.00");
            case "B" -> new BigDecimal("5.00");
            case "C" -> BigDecimal.ZERO;
            case "D" -> new BigDecimal("-5.00");
            default -> BigDecimal.ZERO;
        };
    }

    private SalaryAdjustmentVo toVo(PfSalaryAdjustment adj) {
        SalaryAdjustmentVo vo = new SalaryAdjustmentVo();
        vo.setId(adj.getId());
        vo.setEmployeeId(adj.getEmployeeId());
        vo.setAppraisalId(adj.getAppraisalId());
        vo.setCycleId(adj.getCycleId());
        vo.setGrade(adj.getGrade());
        vo.setFinalScore(adj.getFinalScore());
        vo.setCurrentBaseSalary(adj.getCurrentBaseSalary());
        vo.setAdjustmentPct(adj.getAdjustmentPct());
        vo.setSuggestedSalary(adj.getSuggestedSalary());
        vo.setEffectiveDate(adj.getEffectiveDate());
        vo.setStatus(adj.getStatus());
        vo.setRemark(adj.getRemark());

        // 填充员工信息
        HrEmployee emp = employeeMapper.selectById(adj.getEmployeeId());
        if (emp != null) {
            vo.setEmployeeName(emp.getName());
            vo.setEmpNo(emp.getEmpNo());
        }
        return vo;
    }
}
