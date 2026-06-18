package com.hrms.common.employee.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hrms.common.api.BizCode;
import com.hrms.common.approval.service.ApprovalService;
import com.hrms.common.employee.dto.ProbationConversionCreateDto;
import com.hrms.common.employee.dto.ProbationEmployeeVo;
import com.hrms.common.employee.entity.HrEmployee;
import com.hrms.common.employee.entity.HrProbationConversion;
import com.hrms.common.employee.mapper.HrEmployeeMapper;
import com.hrms.common.employee.mapper.HrProbationConversionMapper;
import com.hrms.common.exception.BizException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 试用期转正服务。
 * 功能：扫描即将到期试用期员工、创建转正申请、提交审批、处理审批结果。
 * 审批通过后将员工状态从PROBATION转为ACTIVE。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProbationConversionService {

    private final HrProbationConversionMapper conversionMapper;
    private final HrEmployeeMapper employeeMapper;
    private final ApprovalService approvalService;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    /**
     * 扫描即将到期试用期员工列表。
     *
     * @param daysAhead 扫描天数，默认30天
     * @return 即将到期试用期员工列表（含预警等级标记）
     */
    public List<ProbationEmployeeVo> listExpiringProbations(int daysAhead) {
        if (daysAhead <= 0) {
            daysAhead = 30;
        }
        LocalDate today = LocalDate.now(clock);
        LocalDate deadline = today.plusDays(daysAhead);

        // 查询状态为PROBATION且试用期结束日期在扫描范围内的员工
        List<HrEmployee> employees = employeeMapper.selectList(
                new LambdaQueryWrapper<HrEmployee>()
                        .eq(HrEmployee::getStatus, "PROBATION")
                        .isNotNull(HrEmployee::getProbationEnd)
                        .le(HrEmployee::getProbationEnd, deadline)
                        .ge(HrEmployee::getProbationEnd, today.minusDays(7))); // 包含7天内已过期的

        List<ProbationEmployeeVo> result = new ArrayList<>();
        for (HrEmployee emp : employees) {
            ProbationEmployeeVo vo = new ProbationEmployeeVo();
            vo.setEmployeeId(emp.getId());
            vo.setEmpNo(emp.getEmpNo());
            vo.setName(emp.getName());
            vo.setDeptId(emp.getDeptId());
            vo.setPositionId(emp.getPositionId());
            vo.setHireDate(emp.getHireDate());
            vo.setProbationEndDate(emp.getProbationEnd());

            // 计算距离到期天数
            long daysUntil = ChronoUnit.DAYS.between(today, emp.getProbationEnd());
            vo.setDaysUntilExpiry(daysUntil);

            // 设置预警等级
            if (daysUntil <= 7) {
                vo.setAlertLevel("URGENT");
            } else {
                vo.setAlertLevel("WARNING");
            }

            // 检查是否已有待处理的转正申请
            long pendingCount = conversionMapper.selectCount(
                    new LambdaQueryWrapper<HrProbationConversion>()
                            .eq(HrProbationConversion::getEmployeeId, emp.getId())
                            .in(HrProbationConversion::getStatus, "PENDING", "SUBMITTED"));
            vo.setHasPendingConversion(pendingCount > 0);

            result.add(vo);
        }

        // 按到期天数升序排列
        result.sort((a, b) -> Long.compare(a.getDaysUntilExpiry(), b.getDaysUntilExpiry()));
        return result;
    }

    /**
     * 创建试用期转正申请单（状态为PENDING）。
     */
    @Transactional
    public HrProbationConversion create(ProbationConversionCreateDto dto) {
        // 校验员工存在且处于试用期
        HrEmployee emp = employeeMapper.selectById(dto.getEmployeeId());
        if (emp == null) {
            throw new BizException(BizCode.EMPLOYEE_NOT_FOUND, "员工不存在: " + dto.getEmployeeId());
        }
        if (!"PROBATION".equals(emp.getStatus())) {
            throw new BizException(BizCode.PROBATION_NOT_EXPIRING, "该员工不在试用期状态");
        }

        // 检查是否已有待处理的转正申请
        long existCount = conversionMapper.selectCount(
                new LambdaQueryWrapper<HrProbationConversion>()
                        .eq(HrProbationConversion::getEmployeeId, dto.getEmployeeId())
                        .in(HrProbationConversion::getStatus, "PENDING", "SUBMITTED"));
        if (existCount > 0) {
            throw new BizException(BizCode.PROBATION_CONVERSION_DUPLICATE, "该员工已有待处理的转正申请");
        }

        HrProbationConversion conversion = new HrProbationConversion();
        conversion.setEmployeeId(dto.getEmployeeId());
        conversion.setConversionNo("PC" + cn.hutool.core.util.IdUtil.getSnowflakeNextIdStr());
        conversion.setProbationStartDate(emp.getHireDate());
        conversion.setProbationEndDate(emp.getProbationEnd());
        conversion.setPlannedConversionDate(
                dto.getPlannedConversionDate() != null ? dto.getPlannedConversionDate() : emp.getProbationEnd());
        conversion.setEvaluationRemark(dto.getEvaluationRemark());
        conversion.setEvaluationScore(dto.getEvaluationScore());
        conversion.setStatus("PENDING");

        conversionMapper.insert(conversion);
        log.info("创建试用期转正申请: 员工={}, 申请单号={}", emp.getName(), conversion.getConversionNo());
        return conversion;
    }

    /**
     * 提交转正申请进入审批流程。
     */
    @Transactional
    public void submit(Long conversionId) {
        HrProbationConversion conversion = conversionMapper.selectById(conversionId);
        if (conversion == null) {
            throw new BizException(BizCode.PROBATION_CONVERSION_NOT_FOUND, "转正申请不存在: " + conversionId);
        }
        if (!"PENDING".equals(conversion.getStatus())) {
            throw new BizException(BizCode.BAD_REQUEST, "当前状态不允许提交: " + conversion.getStatus());
        }

        // 构建审批payload
        Map<String, Object> payload = new HashMap<>();
        payload.put("employeeId", conversion.getEmployeeId());
        payload.put("probationStartDate", conversion.getProbationStartDate().toString());
        payload.put("probationEndDate", conversion.getProbationEndDate().toString());
        payload.put("plannedConversionDate", conversion.getPlannedConversionDate().toString());
        payload.put("evaluationRemark", conversion.getEvaluationRemark());
        if (conversion.getEvaluationScore() != null) {
            payload.put("evaluationScore", conversion.getEvaluationScore());
        }

        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            payloadJson = "{}";
        }

        Long instanceId = approvalService.start(
                "HR_PROBATION_CONVERSION",
                conversion.getId().toString(),
                "试用期转正: " + conversion.getConversionNo(),
                payloadJson,
                null);

        conversion.setApprovalInstanceId(instanceId);
        conversion.setStatus("SUBMITTED");
        conversionMapper.updateById(conversion);
        log.info("试用期转正申请已提交审批: 申请单号={}, 审批实例={}", conversion.getConversionNo(), instanceId);
    }

    /**
     * 审批通过回调：将员工状态从PROBATION转为ACTIVE。
     */
    @Transactional
    public void onApproved(Long conversionId) {
        HrProbationConversion conversion = conversionMapper.selectById(conversionId);
        if (conversion == null) {
            log.warn("转正申请不存在: {}", conversionId);
            return;
        }

        // 状态转换: PROBATION -> ACTIVE
        HrEmployee emp = employeeMapper.selectById(conversion.getEmployeeId());
        if (emp != null) {
            EmployeeStateMachine.validate(emp.getStatus(), "ACTIVE");
            emp.setStatus("ACTIVE");
            employeeMapper.updateById(emp);
            log.info("员工转正成功: 工号={}, 姓名={}", emp.getEmpNo(), emp.getName());
        }

        conversion.setStatus("APPROVED");
        conversionMapper.updateById(conversion);
        log.info("试用期转正审批通过: 申请单号={}", conversion.getConversionNo());
    }

    /**
     * 审批驳回回调。
     */
    @Transactional
    public void onRejected(Long conversionId) {
        HrProbationConversion conversion = conversionMapper.selectById(conversionId);
        if (conversion == null) {
            log.warn("转正申请不存在: {}", conversionId);
            return;
        }
        conversion.setStatus("REJECTED");
        conversionMapper.updateById(conversion);
        log.info("试用期转正审批驳回: 申请单号={}", conversion.getConversionNo());
    }

    /**
     * 分页查询转正申请列表。
     */
    public IPage<HrProbationConversion> list(String status, Page<HrProbationConversion> page) {
        LambdaQueryWrapper<HrProbationConversion> qw = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            qw.eq(HrProbationConversion::getStatus, status);
        }
        qw.orderByDesc(HrProbationConversion::getCreatedAt);
        return conversionMapper.selectPage(page, qw);
    }

    /**
     * 获取转正申请详情。
     */
    public HrProbationConversion getById(Long id) {
        HrProbationConversion conversion = conversionMapper.selectById(id);
        if (conversion == null) {
            throw new BizException(BizCode.PROBATION_CONVERSION_NOT_FOUND, "转正申请不存在: " + id);
        }
        return conversion;
    }
}
