package com.hrms.common.attendance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hrms.common.api.BizCode;
import com.hrms.common.attendance.dto.OvertimeToCompLeaveDto;
import com.hrms.common.attendance.entity.AtCompensatoryLeave;
import com.hrms.common.attendance.entity.AtCompensatoryLeaveLog;
import com.hrms.common.attendance.entity.AtOvertimeRequest;
import com.hrms.common.attendance.mapper.AtCompensatoryLeaveLogMapper;
import com.hrms.common.attendance.mapper.AtCompensatoryLeaveMapper;
import com.hrms.common.attendance.mapper.AtOvertimeRequestMapper;
import com.hrms.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

/**
 * 调休管理服务。
 * 实现加班转调休（支持倍率）、调休余额管理。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CompensatoryLeaveService {

    private final AtCompensatoryLeaveMapper compLeaveMapper;
    private final AtCompensatoryLeaveLogMapper compLeaveLogMapper;
    private final AtOvertimeRequestMapper overtimeRequestMapper;

    /**
     * 将已审批的加班申请转换为调休余额。
     *
     * @param dto        转换请求
     * @param employeeId 员工ID
     * @return 更新后的调休余额
     */
    @Transactional
    public AtCompensatoryLeave convertOvertime(OvertimeToCompLeaveDto dto, Long employeeId) {
        // 1. 校验加班申请
        AtOvertimeRequest overtime = overtimeRequestMapper.selectById(dto.getOvertimeRequestId());
        if (overtime == null) {
            throw new BizException(BizCode.BAD_REQUEST, "加班申请不存在: " + dto.getOvertimeRequestId());
        }
        if (!"APPROVED".equals(overtime.getStatus())) {
            throw new BizException(BizCode.BAD_REQUEST, "只有已审批的加班申请才能转换: " + overtime.getStatus());
        }
        if (!overtime.getEmployeeId().equals(employeeId)) {
            throw new BizException(BizCode.FORBIDDEN, "只能转换自己的加班申请");
        }

        // 2. 校验倍率
        BigDecimal rate = dto.getConvertRate();
        if (rate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException(BizCode.BAD_REQUEST, "转换倍率必须大于0");
        }

        // 3. 计算调休天数（加班小时数 / 8 * 倍率）
        BigDecimal compDays = overtime.getHours()
                .divide(BigDecimal.valueOf(8), 4, RoundingMode.HALF_UP)
                .multiply(rate)
                .setScale(1, RoundingMode.HALF_UP);

        // 4. 获取或创建调休余额
        int year = overtime.getOvertimeDate().getYear();
        AtCompensatoryLeave compLeave = getOrCreateBalance(employeeId, year);

        // 5. 更新余额
        compLeave.setTotalQuota(compLeave.getTotalQuota().add(compDays));
        compLeave.setRemaining(compLeave.getRemaining().add(compDays));
        compLeaveMapper.updateById(compLeave);

        // 6. 记录日志
        recordLog(compLeave.getId(), "CONVERT", compDays, rate,
                dto.getOvertimeRequestId(), null,
                dto.getRemark() != null ? dto.getRemark() : "加班转调休");

        log.info("加班转调休完成: employeeId={}, overtimeId={}, rate={}, days={}",
                employeeId, dto.getOvertimeRequestId(), rate, compDays);
        return compLeave;
    }

    /**
     * 获取员工指定年度的调休余额。
     */
    public AtCompensatoryLeave getBalance(Long employeeId, Integer year) {
        return compLeaveMapper.selectOne(
                new LambdaQueryWrapper<AtCompensatoryLeave>()
                        .eq(AtCompensatoryLeave::getEmployeeId, employeeId)
                        .eq(AtCompensatoryLeave::getYear, year));
    }

    /**
     * 获取员工所有年度的调休余额列表。
     */
    public List<AtCompensatoryLeave> listBalances(Long employeeId) {
        return compLeaveMapper.selectList(
                new LambdaQueryWrapper<AtCompensatoryLeave>()
                        .eq(AtCompensatoryLeave::getEmployeeId, employeeId)
                        .orderByDesc(AtCompensatoryLeave::getYear));
    }

    /**
     * 扣减调休余额（由调休请假审批通过后调用）。
     *
     * @param compLeaveId    调休余额ID
     * @param days           扣减天数
     * @param leaveRequestId 关联的请假申请ID
     */
    @Transactional
    public void deduct(Long compLeaveId, BigDecimal days, Long leaveRequestId) {
        AtCompensatoryLeave compLeave = compLeaveMapper.selectById(compLeaveId);
        if (compLeave == null) {
            throw new BizException(BizCode.BAD_REQUEST, "调休余额记录不存在: " + compLeaveId);
        }
        if (compLeave.getRemaining().compareTo(days) < 0) {
            throw new BizException(BizCode.LEAVE_BALANCE_INSUFFICIENT,
                    "调休余额不足，剩余: " + compLeave.getRemaining() + "，申请: " + days);
        }

        compLeave.setUsed(compLeave.getUsed().add(days));
        compLeave.setRemaining(compLeave.getRemaining().subtract(days));
        compLeaveMapper.updateById(compLeave);

        recordLog(compLeaveId, "DEDUCT", days.negate(), null, null, leaveRequestId, "调休申请扣减");
        log.info("调休余额扣减: compLeaveId={}, days={}", compLeaveId, days);
    }

    /**
     * 退回调休余额（由调休请假取消/驳回后调用）。
     */
    @Transactional
    public void refund(Long compLeaveId, BigDecimal days, Long leaveRequestId) {
        AtCompensatoryLeave compLeave = compLeaveMapper.selectById(compLeaveId);
        if (compLeave == null) {
            throw new BizException(BizCode.BAD_REQUEST, "调休余额记录不存在: " + compLeaveId);
        }

        compLeave.setUsed(compLeave.getUsed().subtract(days));
        compLeave.setRemaining(compLeave.getRemaining().add(days));
        compLeaveMapper.updateById(compLeave);

        recordLog(compLeaveId, "REFUND", days, null, null, leaveRequestId, "调休退回");
        log.info("调休余额退回: compLeaveId={}, days={}", compLeaveId, days);
    }

    /**
     * 获取调休余额变动日志。
     */
    public List<AtCompensatoryLeaveLog> listLogs(Long compLeaveId) {
        return compLeaveLogMapper.selectList(
                new LambdaQueryWrapper<AtCompensatoryLeaveLog>()
                        .eq(AtCompensatoryLeaveLog::getCompLeaveId, compLeaveId)
                        .orderByDesc(AtCompensatoryLeaveLog::getCreatedAt));
    }

    /**
     * 获取或创建员工指定年度的调休余额记录。
     */
    private AtCompensatoryLeave getOrCreateBalance(Long employeeId, Integer year) {
        AtCompensatoryLeave existing = compLeaveMapper.selectOne(
                new LambdaQueryWrapper<AtCompensatoryLeave>()
                        .eq(AtCompensatoryLeave::getEmployeeId, employeeId)
                        .eq(AtCompensatoryLeave::getYear, year));
        if (existing != null) {
            return existing;
        }

        AtCompensatoryLeave newBalance = new AtCompensatoryLeave();
        newBalance.setEmployeeId(employeeId);
        newBalance.setYear(year);
        newBalance.setTotalQuota(BigDecimal.ZERO);
        newBalance.setUsed(BigDecimal.ZERO);
        newBalance.setRemaining(BigDecimal.ZERO);
        compLeaveMapper.insert(newBalance);
        return newBalance;
    }

    /**
     * 记录调休余额变动日志
     */
    private void recordLog(Long compLeaveId, String changeType, BigDecimal changeValue,
                           BigDecimal convertRate, Long overtimeRequestId,
                           Long leaveRequestId, String remark) {
        AtCompensatoryLeaveLog logEntry = new AtCompensatoryLeaveLog();
        logEntry.setCompLeaveId(compLeaveId);
        logEntry.setChangeType(changeType);
        logEntry.setChangeValue(changeValue);
        logEntry.setConvertRate(convertRate);
        logEntry.setOvertimeRequestId(overtimeRequestId);
        logEntry.setLeaveRequestId(leaveRequestId);
        logEntry.setRemark(remark);
        compLeaveLogMapper.insert(logEntry);
    }
}
