package com.hrms.common.employee.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hrms.common.api.BizCode;
import com.hrms.common.approval.service.ApprovalService;
import com.hrms.common.employee.dto.ContractRenewalCreateDto;
import com.hrms.common.employee.dto.ExpiringContractVo;
import com.hrms.common.employee.entity.HrContractRenewal;
import com.hrms.common.employee.entity.HrEmployee;
import com.hrms.common.employee.entity.HrEmployeeContract;
import com.hrms.common.employee.mapper.HrContractRenewalMapper;
import com.hrms.common.employee.mapper.HrEmployeeContractMapper;
import com.hrms.common.employee.mapper.HrEmployeeMapper;
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
 * 合同续签服务。
 * 功能：扫描即将到期合同、创建续签申请、提交审批、处理审批结果。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContractRenewalService {

    private final HrContractRenewalMapper renewalMapper;
    private final HrEmployeeMapper employeeMapper;
    private final HrEmployeeContractMapper contractMapper;
    private final ApprovalService approvalService;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    /**
     * 扫描指定天数内即将到期的合同列表。
     *
     * @param daysAhead 扫描天数，默认30天
     * @return 即将到期的合同列表（含预警等级标记）
     */
    public List<ExpiringContractVo> listExpiringContracts(int daysAhead) {
        if (daysAhead <= 0) {
            daysAhead = 30; // 默认30天
        }
        LocalDate today = LocalDate.now(clock);
        LocalDate deadline = today.plusDays(daysAhead);

        // 查询状态为ACTIVE且结束日期在扫描范围内的合同
        List<HrEmployeeContract> contracts = contractMapper.selectList(
                new LambdaQueryWrapper<HrEmployeeContract>()
                        .eq(HrEmployeeContract::getStatus, "ACTIVE")
                        .isNotNull(HrEmployeeContract::getEndDate)
                        .le(HrEmployeeContract::getEndDate, deadline)
                        .ge(HrEmployeeContract::getEndDate, today.minusDays(7))); // 包含7天内已过期的

        List<ExpiringContractVo> result = new ArrayList<>();
        for (HrEmployeeContract contract : contracts) {
            HrEmployee emp = employeeMapper.selectById(contract.getEmployeeId());
            if (emp == null || "TERMINATED".equals(emp.getStatus())) {
                continue;
            }

            ExpiringContractVo vo = new ExpiringContractVo();
            vo.setEmployeeId(emp.getId());
            vo.setEmpNo(emp.getEmpNo());
            vo.setName(emp.getName());
            vo.setDeptId(emp.getDeptId());
            vo.setContractId(contract.getId());
            vo.setContractNo(contract.getContractNo());
            vo.setContractType(contract.getContractType());
            vo.setContractStartDate(contract.getStartDate());
            vo.setContractEndDate(contract.getEndDate());

            // 计算距离到期天数
            long daysUntil = ChronoUnit.DAYS.between(today, contract.getEndDate());
            vo.setDaysUntilExpiry(daysUntil);

            // 设置预警等级：7天内为URGENT(红色)，30天内为WARNING(黄色)
            if (daysUntil <= 7) {
                vo.setAlertLevel("URGENT");
            } else {
                vo.setAlertLevel("WARNING");
            }

            result.add(vo);
        }

        // 按到期天数升序排列（最紧急的在前面）
        result.sort((a, b) -> Long.compare(a.getDaysUntilExpiry(), b.getDaysUntilExpiry()));
        return result;
    }

    /**
     * 创建合同续签申请单（状态为PENDING）。
     */
    @Transactional
    public HrContractRenewal create(ContractRenewalCreateDto dto) {
        // 校验员工存在
        HrEmployee emp = employeeMapper.selectById(dto.getEmployeeId());
        if (emp == null) {
            throw new BizException(BizCode.EMPLOYEE_NOT_FOUND, "员工不存在: " + dto.getEmployeeId());
        }

        // 校验原合同存在且为ACTIVE状态
        HrEmployeeContract contract = contractMapper.selectById(dto.getOriginalContractId());
        if (contract == null || !"ACTIVE".equals(contract.getStatus())) {
            throw new BizException(BizCode.CONTRACT_NOT_EXPIRING, "原合同不存在或非有效状态");
        }

        // 检查是否已有待处理的续签申请
        long existCount = renewalMapper.selectCount(
                new LambdaQueryWrapper<HrContractRenewal>()
                        .eq(HrContractRenewal::getEmployeeId, dto.getEmployeeId())
                        .eq(HrContractRenewal::getOriginalContractId, dto.getOriginalContractId())
                        .in(HrContractRenewal::getStatus, "PENDING", "SUBMITTED"));
        if (existCount > 0) {
            throw new BizException(BizCode.CONTRACT_RENEWAL_DUPLICATE, "该员工已有待处理的续签申请");
        }

        HrContractRenewal renewal = new HrContractRenewal();
        renewal.setEmployeeId(dto.getEmployeeId());
        renewal.setOriginalContractId(dto.getOriginalContractId());
        renewal.setRenewalNo("CR" + cn.hutool.core.util.IdUtil.getSnowflakeNextIdStr());
        renewal.setNewContractType(dto.getNewContractType());
        renewal.setNewStartDate(dto.getNewStartDate());
        renewal.setNewEndDate(dto.getNewEndDate());
        renewal.setRemark(dto.getRemark());
        renewal.setStatus("PENDING");

        renewalMapper.insert(renewal);
        log.info("创建合同续签申请: 员工={}, 申请单号={}", emp.getName(), renewal.getRenewalNo());
        return renewal;
    }

    /**
     * 提交续签申请进入审批流程。
     */
    @Transactional
    public void submit(Long renewalId) {
        HrContractRenewal renewal = renewalMapper.selectById(renewalId);
        if (renewal == null) {
            throw new BizException(BizCode.CONTRACT_RENEWAL_NOT_FOUND, "续签申请不存在: " + renewalId);
        }
        if (!"PENDING".equals(renewal.getStatus())) {
            throw new BizException(BizCode.BAD_REQUEST, "当前状态不允许提交: " + renewal.getStatus());
        }

        // 构建审批payload
        Map<String, Object> payload = new HashMap<>();
        payload.put("employeeId", renewal.getEmployeeId());
        payload.put("newContractType", renewal.getNewContractType());
        payload.put("newStartDate", renewal.getNewStartDate().toString());
        if (renewal.getNewEndDate() != null) {
            payload.put("newEndDate", renewal.getNewEndDate().toString());
        }
        payload.put("remark", renewal.getRemark());

        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            payloadJson = "{}";
        }

        Long instanceId = approvalService.start(
                "HR_CONTRACT_RENEWAL",
                renewal.getId().toString(),
                "合同续签: " + renewal.getRenewalNo(),
                payloadJson,
                null);

        renewal.setApprovalInstanceId(instanceId);
        renewal.setStatus("SUBMITTED");
        renewalMapper.updateById(renewal);
        log.info("合同续签申请已提交审批: 申请单号={}, 审批实例={}", renewal.getRenewalNo(), instanceId);
    }

    /**
     * 审批通过回调：更新原合同状态为EXPIRED，创建新合同，更新员工合同信息。
     */
    @Transactional
    public void onApproved(Long renewalId) {
        HrContractRenewal renewal = renewalMapper.selectById(renewalId);
        if (renewal == null) {
            log.warn("续签申请不存在: {}", renewalId);
            return;
        }

        // 将原合同标记为EXPIRED
        HrEmployeeContract oldContract = contractMapper.selectById(renewal.getOriginalContractId());
        if (oldContract != null) {
            oldContract.setStatus("EXPIRED");
            contractMapper.updateById(oldContract);
        }

        // 创建新合同
        HrEmployeeContract newContract = new HrEmployeeContract();
        newContract.setEmployeeId(renewal.getEmployeeId());
        newContract.setContractNo("C" + cn.hutool.core.util.IdUtil.getSnowflakeNextIdStr());
        newContract.setContractType(renewal.getNewContractType());
        newContract.setStartDate(renewal.getNewStartDate());
        newContract.setEndDate(renewal.getNewEndDate());
        newContract.setSigningDate(LocalDate.now(clock));
        newContract.setStatus("ACTIVE");
        contractMapper.insert(newContract);

        // 更新员工主表的合同信息
        HrEmployee emp = employeeMapper.selectById(renewal.getEmployeeId());
        if (emp != null) {
            emp.setContractStart(renewal.getNewStartDate());
            emp.setContractEnd(renewal.getNewEndDate());
            employeeMapper.updateById(emp);
        }

        renewal.setStatus("APPROVED");
        renewalMapper.updateById(renewal);
        log.info("合同续签审批通过: 申请单号={}, 新合同ID={}", renewal.getRenewalNo(), newContract.getId());
    }

    /**
     * 审批驳回回调。
     */
    @Transactional
    public void onRejected(Long renewalId) {
        HrContractRenewal renewal = renewalMapper.selectById(renewalId);
        if (renewal == null) {
            log.warn("续签申请不存在: {}", renewalId);
            return;
        }
        renewal.setStatus("REJECTED");
        renewalMapper.updateById(renewal);
        log.info("合同续签审批驳回: 申请单号={}", renewal.getRenewalNo());
    }

    /**
     * 分页查询续签申请列表。
     */
    public IPage<HrContractRenewal> list(String status, Page<HrContractRenewal> page) {
        LambdaQueryWrapper<HrContractRenewal> qw = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            qw.eq(HrContractRenewal::getStatus, status);
        }
        qw.orderByDesc(HrContractRenewal::getCreatedAt);
        return renewalMapper.selectPage(page, qw);
    }

    /**
     * 获取续签申请详情。
     */
    public HrContractRenewal getById(Long id) {
        HrContractRenewal renewal = renewalMapper.selectById(id);
        if (renewal == null) {
            throw new BizException(BizCode.CONTRACT_RENEWAL_NOT_FOUND, "续签申请不存在: " + id);
        }
        return renewal;
    }
}
