package com.hrms.common.attendance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hrms.common.api.BizCode;
import com.hrms.common.attendance.dto.AnomalyHandleDto;
import com.hrms.common.attendance.entity.*;
import com.hrms.common.attendance.mapper.AtAttendanceAnomalyMapper;
import com.hrms.common.attendance.mapper.AtScheduleMapper;
import com.hrms.common.attendance.mapper.AtShiftMapper;
import com.hrms.common.attendance.mapper.AtTimePunchMapper;
import com.hrms.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * 考勤异常处理服务。
 * 根据排班+打卡数据自动识别迟到、早退、旷工、漏打卡等异常，
 * 并提供异常列表查询和处理功能。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AttendanceAnomalyService {

    private final AtScheduleMapper scheduleMapper;
    private final AtShiftMapper shiftMapper;
    private final AtTimePunchMapper punchMapper;
    private final AtAttendanceAnomalyMapper anomalyMapper;

    /**
     * 检测指定日期范围内的考勤异常。
     * 遍历每个排班记录，与打卡记录比对，自动识别异常类型。
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 本次检测到的异常列表
     */
    @Transactional
    public List<AtAttendanceAnomaly> detect(LocalDate startDate, LocalDate endDate) {
        // 查询日期范围内的所有排班
        List<AtSchedule> schedules = scheduleMapper.selectList(
                new LambdaQueryWrapper<AtSchedule>()
                        .ge(AtSchedule::getScheduleDate, startDate)
                        .le(AtSchedule::getScheduleDate, endDate)
                        .orderByAsc(AtSchedule::getEmployeeId)
                        .orderByAsc(AtSchedule::getScheduleDate));

        List<AtAttendanceAnomaly> detected = new ArrayList<>();

        for (AtSchedule schedule : schedules) {
            AtShift shift = shiftMapper.selectById(schedule.getShiftId());
            if (shift == null) {
                continue;
            }

            // 查询该员工当天的打卡记录
            AtTimePunch punch = punchMapper.selectOne(
                    new LambdaQueryWrapper<AtTimePunch>()
                            .eq(AtTimePunch::getEmployeeId, schedule.getEmployeeId())
                            .eq(AtTimePunch::getPunchDate, schedule.getScheduleDate()));

            List<AtAttendanceAnomaly> anomalies = detectAnomalies(schedule, shift, punch);
            for (AtAttendanceAnomaly anomaly : anomalies) {
                // 去重：同员工同日期同类型不重复生成
                AtAttendanceAnomaly existing = anomalyMapper.selectOne(
                        new LambdaQueryWrapper<AtAttendanceAnomaly>()
                                .eq(AtAttendanceAnomaly::getEmployeeId, anomaly.getEmployeeId())
                                .eq(AtAttendanceAnomaly::getAnomalyDate, anomaly.getAnomalyDate())
                                .eq(AtAttendanceAnomaly::getAnomalyType, anomaly.getAnomalyType()));
                if (existing != null) {
                    // 更新已有记录
                    existing.setDurationMinutes(anomaly.getDurationMinutes());
                    existing.setScheduleId(anomaly.getScheduleId());
                    existing.setPunchId(anomaly.getPunchId());
                    anomalyMapper.updateById(existing);
                } else {
                    anomalyMapper.insert(anomaly);
                    detected.add(anomaly);
                }
            }
        }

        log.info("考勤异常检测完成：日期范围 {} ~ {}，新增异常 {} 条", startDate, endDate, detected.size());
        return detected;
    }

    /**
     * 根据排班、班次和打卡记录，判断异常类型。
     */
    private List<AtAttendanceAnomaly> detectAnomalies(AtSchedule schedule, AtShift shift, AtTimePunch punch) {
        List<AtAttendanceAnomaly> anomalies = new ArrayList<>();

        LocalTime shiftStart = LocalTime.parse(shift.getStartTime());
        LocalTime shiftEnd = LocalTime.parse(shift.getEndTime());
        int flex = shift.getFlexMinutes() != null ? shift.getFlexMinutes() : 0;

        // 场景1：无打卡记录 → 旷工
        if (punch == null || (punch.getClockIn() == null && punch.getClockOut() == null)) {
            anomalies.add(createAnomaly(schedule, punch, "ABSENT", 0));
            return anomalies;
        }

        // 场景2：仅有上班卡或仅有下班卡 → 漏打卡
        if (punch.getClockIn() == null || punch.getClockOut() == null) {
            anomalies.add(createAnomaly(schedule, punch, "MISSING", 0));
            return anomalies;
        }

        LocalTime clockIn = LocalTime.parse(punch.getClockIn());
        LocalTime clockOut = LocalTime.parse(punch.getClockOut());

        // 场景3：迟到检测 — 上班打卡时间 > 排班开始时间 + 弹性时间
        long lateRaw = ChronoUnit.MINUTES.between(shiftStart, clockIn);
        if (lateRaw > flex) {
            int lateMinutes = (int) (lateRaw - flex);
            anomalies.add(createAnomaly(schedule, punch, "LATE", lateMinutes));
        }

        // 场景4：早退检测 — 下班打卡时间 < 排班结束时间 - 弹性时间
        long earlyRaw = ChronoUnit.MINUTES.between(clockOut, shiftEnd);
        if (earlyRaw > flex) {
            int earlyMinutes = (int) (earlyRaw - flex);
            anomalies.add(createAnomaly(schedule, punch, "EARLY", earlyMinutes));
        }

        return anomalies;
    }

    /**
     * 创建一条异常记录
     */
    private AtAttendanceAnomaly createAnomaly(AtSchedule schedule, AtTimePunch punch,
                                              String anomalyType, int durationMinutes) {
        AtAttendanceAnomaly anomaly = new AtAttendanceAnomaly();
        anomaly.setEmployeeId(schedule.getEmployeeId());
        anomaly.setAnomalyDate(schedule.getScheduleDate());
        anomaly.setAnomalyType(anomalyType);
        anomaly.setDurationMinutes(durationMinutes);
        anomaly.setStatus("PENDING");
        anomaly.setScheduleId(schedule.getId());
        if (punch != null) {
            anomaly.setPunchId(punch.getId());
        }
        return anomaly;
    }

    /**
     * 查询异常列表，支持按员工、日期、类型、状态筛选。
     */
    public IPage<AtAttendanceAnomaly> list(Long employeeId, LocalDate startDate, LocalDate endDate,
                                            String anomalyType, String status,
                                            Page<AtAttendanceAnomaly> page) {
        LambdaQueryWrapper<AtAttendanceAnomaly> qw = new LambdaQueryWrapper<>();
        if (employeeId != null) {
            qw.eq(AtAttendanceAnomaly::getEmployeeId, employeeId);
        }
        if (startDate != null) {
            qw.ge(AtAttendanceAnomaly::getAnomalyDate, startDate);
        }
        if (endDate != null) {
            qw.le(AtAttendanceAnomaly::getAnomalyDate, endDate);
        }
        if (anomalyType != null && !anomalyType.isEmpty()) {
            qw.eq(AtAttendanceAnomaly::getAnomalyType, anomalyType);
        }
        if (status != null && !status.isEmpty()) {
            qw.eq(AtAttendanceAnomaly::getStatus, status);
        }
        qw.orderByDesc(AtAttendanceAnomaly::getAnomalyDate);
        return anomalyMapper.selectPage(page, qw);
    }

    /**
     * 处理异常记录（标记为已处理或已忽略）。
     */
    @Transactional
    public void handle(AnomalyHandleDto dto) {
        AtAttendanceAnomaly anomaly = anomalyMapper.selectById(dto.getAnomalyId());
        if (anomaly == null) {
            throw new BizException(BizCode.BAD_REQUEST, "异常记录不存在: " + dto.getAnomalyId());
        }
        if (!"PENDING".equals(anomaly.getStatus())) {
            throw new BizException(BizCode.BAD_REQUEST, "当前状态不允许处理: " + anomaly.getStatus());
        }

        String action = dto.getAction();
        if (!"HANDLED".equals(action) && !"IGNORED".equals(action)) {
            throw new BizException(BizCode.BAD_REQUEST, "不支持的处理动作: " + action);
        }

        anomaly.setStatus(action);
        anomaly.setHandleRemark(dto.getRemark());
        anomalyMapper.updateById(anomaly);

        log.info("考勤异常已处理: anomalyId={}, action={}", dto.getAnomalyId(), action);
    }
}
