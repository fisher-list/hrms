package com.hrms.common.report.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hrms.common.attendance.entity.AtAttendanceAnomaly;
import com.hrms.common.attendance.entity.AtDailySummary;
import com.hrms.common.attendance.entity.AtLeaveRequest;
import com.hrms.common.attendance.entity.AtOvertimeRequest;
import com.hrms.common.attendance.mapper.*;
import com.hrms.common.employee.entity.HrEmployee;
import com.hrms.common.employee.mapper.HrEmployeeMapper;
import com.hrms.common.org.entity.Department;
import com.hrms.common.org.mapper.DepartmentMapper;
import com.hrms.common.payroll.entity.*;
import com.hrms.common.payroll.mapper.*;
import com.hrms.common.recruit.entity.RcCandidate;
import com.hrms.common.recruit.entity.RcJobRequisition;
import com.hrms.common.recruit.mapper.RcCandidateMapper;
import com.hrms.common.recruit.mapper.RcJobRequisitionMapper;
import com.hrms.common.report.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 报表聚合服务
 * 负责HR仪表盘、人事花名册、考勤月报、薪资汇总等报表的数据聚合
 */
@Service
@RequiredArgsConstructor
public class ReportService {

    private final HrEmployeeMapper employeeMapper;
    private final DepartmentMapper departmentMapper;
    private final RcJobRequisitionMapper requisitionMapper;
    private final RcCandidateMapper candidateMapper;
    private final AtDailySummaryMapper summaryMapper;
    private final AtAttendanceAnomalyMapper anomalyMapper;
    private final AtLeaveRequestMapper leaveRequestMapper;
    private final AtOvertimeRequestMapper overtimeRequestMapper;
    private final PyPayrollRunMapper payrollRunMapper;
    private final PyPayrollDetailMapper payrollDetailMapper;
    private final PyPayrollPeriodMapper payrollPeriodMapper;
    private final PyCompensationMasterMapper compensationMapper;

    // ==================== HR仪表盘 ====================

    /**
     * 获取HR仪表盘概览数据
     * 聚合在职人数、离职率、招聘进度、考勤异常率、薪资总额等关键指标
     */
    public DashboardVo getDashboard() {
        DashboardVo vo = new DashboardVo();
        LocalDate today = LocalDate.now();
        YearMonth currentMonth = YearMonth.now();

        // ---------- 人员指标 ----------
        // 在职人数：状态为ACTIVE/PROBATION/ON_LEAVE的
        List<String> activeStatuses = List.of("ACTIVE", "PROBATION", "ON_LEAVE");
        Long activeCount = employeeMapper.selectCount(
                new LambdaQueryWrapper<HrEmployee>()
                        .in(HrEmployee::getStatus, activeStatuses));
        vo.setActiveCount(activeCount);

        // 本月新入职
        Long newHireThisMonth = employeeMapper.selectCount(
                new LambdaQueryWrapper<HrEmployee>()
                        .ge(HrEmployee::getHireDate, currentMonth.atDay(1))
                        .le(HrEmployee::getHireDate, currentMonth.atEndOfMonth()));
        vo.setNewHireThisMonth(newHireThisMonth);

        // 本月离职
        Long terminatedThisMonth = employeeMapper.selectCount(
                new LambdaQueryWrapper<HrEmployee>()
                        .eq(HrEmployee::getStatus, "TERMINATED")
                        .apply("YEAR(contract_end) = {0} AND MONTH(contract_end) = {1}",
                                today.getYear(), today.getMonthValue()));
        vo.setTerminatedThisMonth(terminatedThisMonth);

        // 年度累计离职
        Long terminatedThisYear = employeeMapper.selectCount(
                new LambdaQueryWrapper<HrEmployee>()
                        .eq(HrEmployee::getStatus, "TERMINATED")
                        .apply("YEAR(contract_end) = {0}", today.getYear()));
        vo.setTerminatedThisYear(terminatedThisYear);

        // 年度离职率 = 离职人数 / (当前在职+本年度离职)
        long denominator = activeCount + terminatedThisYear;
        BigDecimal turnoverRate = denominator > 0
                ? BigDecimal.valueOf(terminatedThisYear)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        vo.setTurnoverRate(turnoverRate);

        // ---------- 招聘指标 ----------
        Long openReqCount = requisitionMapper.selectCount(
                new LambdaQueryWrapper<RcJobRequisition>()
                        .eq(RcJobRequisition::getStatus, "OPEN"));
        vo.setOpenRequisitionCount(openReqCount);

        Long candidateCount = candidateMapper.selectCount(
                new LambdaQueryWrapper<RcCandidate>()
                        .in(RcCandidate::getCurrentStatus, "NEW", "INTERVIEW", "OFFER"));
        vo.setCandidateCount(candidateCount);

        Long offerCount = candidateMapper.selectCount(
                new LambdaQueryWrapper<RcCandidate>()
                        .eq(RcCandidate::getCurrentStatus, "OFFER"));
        vo.setOfferCount(offerCount);

        // ---------- 考勤指标 ----------
        LocalDate monthStart = currentMonth.atDay(1);
        LocalDate monthEnd = currentMonth.atEndOfMonth();

        // 总排班人次
        Long scheduledCount = summaryMapper.selectCount(
                new LambdaQueryWrapper<AtDailySummary>()
                        .eq(AtDailySummary::getScheduled, true)
                        .ge(AtDailySummary::getSummaryDate, monthStart)
                        .le(AtDailySummary::getSummaryDate, monthEnd));

        // 异常人次（迟到+早退+旷工）
        Long anomalyCount = anomalyMapper.selectCount(
                new LambdaQueryWrapper<AtAttendanceAnomaly>()
                        .ge(AtAttendanceAnomaly::getAnomalyDate, monthStart)
                        .le(AtAttendanceAnomaly::getAnomalyDate, monthEnd));
        BigDecimal anomalyRate = scheduledCount > 0
                ? BigDecimal.valueOf(anomalyCount)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(scheduledCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        vo.setAttendanceAnomalyRate(anomalyRate);

        Long leaveCount = leaveRequestMapper.selectCount(
                new LambdaQueryWrapper<AtLeaveRequest>()
                        .eq(AtLeaveRequest::getStatus, "APPROVED")
                        .ge(AtLeaveRequest::getStartDate, monthStart)
                        .le(AtLeaveRequest::getEndDate, monthEnd));
        vo.setLeaveRequestCount(leaveCount);

        Long overtimeCount = overtimeRequestMapper.selectCount(
                new LambdaQueryWrapper<AtOvertimeRequest>()
                        .eq(AtOvertimeRequest::getStatus, "APPROVED")
                        .ge(AtOvertimeRequest::getOvertimeDate, monthStart)
                        .le(AtOvertimeRequest::getOvertimeDate, monthEnd));
        vo.setOvertimeRequestCount(overtimeCount);

        // ---------- 薪资指标 ----------
        // 查找最近一个已锁定的薪资批次
        List<PyPayrollRun> lockedRuns = payrollRunMapper.selectList(
                new LambdaQueryWrapper<PyPayrollRun>()
                        .eq(PyPayrollRun::getStatus, "LOCKED")
                        .eq(PyPayrollRun::getRunType, "NORMAL")
                        .orderByDesc(PyPayrollRun::getCreatedAt)
                        .last("LIMIT 1"));

        if (!lockedRuns.isEmpty()) {
            PyPayrollRun latestRun = lockedRuns.get(0);
            vo.setLatestPayrollGross(latestRun.getTotalGross());
            vo.setLatestPayrollNet(latestRun.getTotalNet());
            vo.setLatestPayrollEmployeeCount(latestRun.getEmployeeCount());

            // 获取对应的薪资月份
            PyPayrollPeriod period = payrollPeriodMapper.selectById(latestRun.getPeriodId());
            if (period != null) {
                vo.setLatestPayrollMonth(period.getPeriodMonth());
            }
        }

        return vo;
    }

    // ==================== 人事花名册报表 ====================

    /**
     * 人事花名册查询
     * 支持自定义列、筛选条件、排序
     */
    public IPage<RosterRowVo> getRoster(RosterQueryDto query, Page<HrEmployee> page) {
        // 构造查询条件
        LambdaQueryWrapper<HrEmployee> qw = new LambdaQueryWrapper<>();
        if (query.getKeyword() != null && !query.getKeyword().isEmpty()) {
            qw.and(w -> w.like(HrEmployee::getName, query.getKeyword())
                    .or()
                    .like(HrEmployee::getEmpNo, query.getKeyword()));
        }
        if (query.getStatus() != null && !query.getStatus().isEmpty()) {
            qw.eq(HrEmployee::getStatus, query.getStatus());
        }
        if (query.getDeptId() != null) {
            qw.eq(HrEmployee::getDeptId, query.getDeptId());
        }

        // 排序
        if (query.getOrderBy() != null && !query.getOrderBy().isEmpty()) {
            boolean asc = !"DESC".equalsIgnoreCase(query.getOrderDir());
            // 支持常用字段排序
            switch (query.getOrderBy()) {
                case "empNo" -> qw.orderBy(true, asc, HrEmployee::getEmpNo);
                case "name" -> qw.orderBy(true, asc, HrEmployee::getName);
                case "hireDate" -> qw.orderBy(true, asc, HrEmployee::getHireDate);
                case "status" -> qw.orderBy(true, asc, HrEmployee::getStatus);
                default -> qw.orderByDesc(HrEmployee::getCreatedAt);
            }
        } else {
            qw.orderByDesc(HrEmployee::getCreatedAt);
        }

        // 分页查询
        IPage<HrEmployee> empPage = employeeMapper.selectPage(page, qw);

        // 批量加载部门名称
        Set<Long> deptIds = empPage.getRecords().stream()
                .map(HrEmployee::getDeptId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> deptNameMap = new HashMap<>();
        if (!deptIds.isEmpty()) {
            List<Department> depts = departmentMapper.selectBatchIds(deptIds);
            deptNameMap = depts.stream()
                    .collect(Collectors.toMap(Department::getId, Department::getName, (a, b) -> a));
        }

        final Map<Long, String> finalDeptNameMap = deptNameMap;

        // 转换为VO
        IPage<RosterRowVo> result = empPage.convert(emp -> {
            RosterRowVo row = new RosterRowVo();
            row.setId(emp.getId());
            row.setEmpNo(emp.getEmpNo());
            row.setName(emp.getName());
            row.setGender(emp.getGender());
            row.setBirthDate(emp.getBirthDate());
            row.setEmail(emp.getEmail());
            row.setDeptId(emp.getDeptId());
            row.setDeptName(finalDeptNameMap.get(emp.getDeptId()));
            row.setPositionId(emp.getPositionId());
            row.setHireDate(emp.getHireDate());
            row.setStatus(emp.getStatus());
            row.setContractStart(emp.getContractStart());
            row.setContractEnd(emp.getContractEnd());
            row.setProbationEnd(emp.getProbationEnd());
            row.setEmergencyContact(emp.getEmergencyContact());
            return row;
        });

        return result;
    }

    // ==================== 考勤月报 ====================

    /**
     * 获取考勤月报 - 按部门维度
     * 统计月度出勤率、迟到、早退、加班、请假等指标
     */
    public List<AttendanceMonthlyVo> getAttendanceMonthlyByDept(String month) {
        YearMonth ym = YearMonth.parse(month);
        LocalDate startDate = ym.atDay(1);
        LocalDate endDate = ym.atEndOfMonth();

        // 查询所有部门
        List<Department> departments = departmentMapper.selectList(null);
        Map<Long, String> deptNameMap = departments.stream()
                .collect(Collectors.toMap(Department::getId, Department::getName, (a, b) -> a));

        // 查询该月所有考勤汇总
        List<AtDailySummary> summaries = summaryMapper.selectList(
                new LambdaQueryWrapper<AtDailySummary>()
                        .ge(AtDailySummary::getSummaryDate, startDate)
                        .le(AtDailySummary::getSummaryDate, endDate));

        // 查询该月所有异常记录
        List<AtAttendanceAnomaly> anomalies = anomalyMapper.selectList(
                new LambdaQueryWrapper<AtAttendanceAnomaly>()
                        .ge(AtAttendanceAnomaly::getAnomalyDate, startDate)
                        .le(AtAttendanceAnomaly::getAnomalyDate, endDate));

        // 查询该月请假记录
        List<AtLeaveRequest> leaveRequests = leaveRequestMapper.selectList(
                new LambdaQueryWrapper<AtLeaveRequest>()
                        .eq(AtLeaveRequest::getStatus, "APPROVED")
                        .le(AtLeaveRequest::getStartDate, endDate)
                        .ge(AtLeaveRequest::getEndDate, startDate));

        // 查询所有员工以获取部门归属
        List<String> activeStatuses = List.of("ACTIVE", "PROBATION", "ON_LEAVE");
        List<HrEmployee> employees = employeeMapper.selectList(
                new LambdaQueryWrapper<HrEmployee>()
                        .in(HrEmployee::getStatus, activeStatuses));
        Map<Long, Long> empDeptMap = employees.stream()
                .filter(e -> e.getDeptId() != null)
                .collect(Collectors.toMap(HrEmployee::getId, HrEmployee::getDeptId, (a, b) -> a));

        // 按部门分组统计
        Map<Long, List<AtDailySummary>> summaryByDept = summaries.stream()
                .filter(s -> empDeptMap.containsKey(s.getEmployeeId()))
                .collect(Collectors.groupingBy(s -> empDeptMap.get(s.getEmployeeId())));

        Map<Long, List<AtAttendanceAnomaly>> anomalyByDept = anomalies.stream()
                .filter(a -> empDeptMap.containsKey(a.getEmployeeId()))
                .collect(Collectors.groupingBy(a -> empDeptMap.get(a.getEmployeeId())));

        Map<Long, List<AtLeaveRequest>> leaveByDept = leaveRequests.stream()
                .filter(l -> empDeptMap.containsKey(l.getEmployeeId()))
                .collect(Collectors.groupingBy(l -> empDeptMap.get(l.getEmployeeId())));

        List<AttendanceMonthlyVo> result = new ArrayList<>();
        for (Map.Entry<Long, List<AtDailySummary>> entry : summaryByDept.entrySet()) {
            Long deptId = entry.getKey();
            List<AtDailySummary> deptSummaries = entry.getValue();

            AttendanceMonthlyVo vo = buildMonthlyVo(
                    deptId, deptNameMap.get(deptId), null, month,
                    deptSummaries,
                    anomalyByDept.getOrDefault(deptId, Collections.emptyList()),
                    leaveByDept.getOrDefault(deptId, Collections.emptyList()));
            result.add(vo);
        }

        // 按部门名称排序
        result.sort(Comparator.comparing(AttendanceMonthlyVo::getDimensionName,
                Comparator.nullsLast(Comparator.naturalOrder())));

        return result;
    }

    /**
     * 获取考勤月报 - 按个人维度
     */
    public List<AttendanceMonthlyVo> getAttendanceMonthlyByEmployee(String month, Long deptId) {
        YearMonth ym = YearMonth.parse(month);
        LocalDate startDate = ym.atDay(1);
        LocalDate endDate = ym.atEndOfMonth();

        // 查询员工
        List<String> activeStatuses = List.of("ACTIVE", "PROBATION", "ON_LEAVE");
        LambdaQueryWrapper<HrEmployee> empQw = new LambdaQueryWrapper<HrEmployee>()
                .in(HrEmployee::getStatus, activeStatuses);
        if (deptId != null) {
            empQw.eq(HrEmployee::getDeptId, deptId);
        }
        List<HrEmployee> employees = employeeMapper.selectList(empQw);

        // 查询该月考勤汇总
        List<AtDailySummary> summaries = summaryMapper.selectList(
                new LambdaQueryWrapper<AtDailySummary>()
                        .ge(AtDailySummary::getSummaryDate, startDate)
                        .le(AtDailySummary::getSummaryDate, endDate));

        // 查询该月异常记录
        List<AtAttendanceAnomaly> anomalies = anomalyMapper.selectList(
                new LambdaQueryWrapper<AtAttendanceAnomaly>()
                        .ge(AtAttendanceAnomaly::getAnomalyDate, startDate)
                        .le(AtAttendanceAnomaly::getAnomalyDate, endDate));

        // 查询该月请假记录
        List<AtLeaveRequest> leaveRequests = leaveRequestMapper.selectList(
                new LambdaQueryWrapper<AtLeaveRequest>()
                        .eq(AtLeaveRequest::getStatus, "APPROVED")
                        .le(AtLeaveRequest::getStartDate, endDate)
                        .ge(AtLeaveRequest::getEndDate, startDate));

        Map<Long, List<AtDailySummary>> summaryByEmp = summaries.stream()
                .collect(Collectors.groupingBy(AtDailySummary::getEmployeeId));
        Map<Long, List<AtAttendanceAnomaly>> anomalyByEmp = anomalies.stream()
                .collect(Collectors.groupingBy(AtAttendanceAnomaly::getEmployeeId));
        Map<Long, List<AtLeaveRequest>> leaveByEmp = leaveRequests.stream()
                .filter(l -> l.getEmployeeId() != null)
                .collect(Collectors.groupingBy(AtLeaveRequest::getEmployeeId));

        List<AttendanceMonthlyVo> result = new ArrayList<>();
        for (HrEmployee emp : employees) {
            List<AtDailySummary> empSummaries = summaryByEmp.getOrDefault(emp.getId(), Collections.emptyList());
            if (empSummaries.isEmpty()) continue;

            AttendanceMonthlyVo vo = buildMonthlyVo(
                    emp.getId(), emp.getName(), emp.getEmpNo(), month,
                    empSummaries,
                    anomalyByEmp.getOrDefault(emp.getId(), Collections.emptyList()),
                    leaveByEmp.getOrDefault(emp.getId(), Collections.emptyList()));
            result.add(vo);
        }

        result.sort(Comparator.comparing(AttendanceMonthlyVo::getDimensionName,
                Comparator.nullsLast(Comparator.naturalOrder())));
        return result;
    }

    /**
     * 构建考勤月报VO（部门或个人维度通用）
     */
    private AttendanceMonthlyVo buildMonthlyVo(
            Long dimensionId, String dimensionName, String empNo, String month,
            List<AtDailySummary> summaries,
            List<AtAttendanceAnomaly> anomalies,
            List<AtLeaveRequest> leaveRequests) {

        AttendanceMonthlyVo vo = new AttendanceMonthlyVo();
        vo.setDimensionId(dimensionId);
        vo.setDimensionName(dimensionName);
        vo.setEmpNo(empNo);
        vo.setMonth(month);

        // 统计排班和出勤天数
        int scheduledDays = (int) summaries.stream().filter(s -> Boolean.TRUE.equals(s.getScheduled())).count();
        int attendedDays = (int) summaries.stream().filter(s -> Boolean.TRUE.equals(s.getAttended())).count();
        vo.setScheduledDays(scheduledDays);
        vo.setAttendedDays(attendedDays);

        // 出勤率
        BigDecimal rate = scheduledDays > 0
                ? BigDecimal.valueOf(attendedDays)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(scheduledDays), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        vo.setAttendanceRate(rate);

        // 迟到/早退/旷工
        vo.setLateCount((int) summaries.stream()
                .filter(s -> s.getLateMinutes() != null && s.getLateMinutes() > 0).count());
        vo.setEarlyLeaveCount((int) summaries.stream()
                .filter(s -> s.getEarlyLeaveMinutes() != null && s.getEarlyLeaveMinutes() > 0).count());
        vo.setAbsentDays((int) summaries.stream()
                .filter(s -> Boolean.TRUE.equals(s.getAbsent())).count());

        // 加班时长合计
        BigDecimal overtimeTotal = summaries.stream()
                .map(s -> s.getOvertimeHours() != null ? s.getOvertimeHours() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        vo.setOvertimeHours(overtimeTotal);

        // 请假天数合计
        BigDecimal leaveTotal = leaveRequests.stream()
                .map(l -> l.getDays() != null ? l.getDays() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        vo.setLeaveDays(leaveTotal);

        return vo;
    }

    // ==================== 薪资汇总报表 ====================

    /**
     * 获取薪资汇总报表 - 按部门维度
     * 部门薪资对比、薪资结构分析
     */
    public List<PayrollSummaryVo> getPayrollSummaryByDept(String month) {
        // 查找对应月份的薪资批次
        PyPayrollPeriod period = payrollPeriodMapper.selectOne(
                new LambdaQueryWrapper<PyPayrollPeriod>()
                        .eq(PyPayrollPeriod::getPeriodMonth, month)
                        .eq(PyPayrollPeriod::getStatus, "LOCKED"));
        if (period == null) {
            return Collections.emptyList();
        }

        // 查找该周期的LOCKED NORMAL run
        PyPayrollRun run = payrollRunMapper.selectOne(
                new LambdaQueryWrapper<PyPayrollRun>()
                        .eq(PyPayrollRun::getPeriodId, period.getId())
                        .eq(PyPayrollRun::getStatus, "LOCKED")
                        .eq(PyPayrollRun::getRunType, "NORMAL"));
        if (run == null) {
            return Collections.emptyList();
        }

        // 查询该批次的所有明细
        List<PyPayrollDetail> details = payrollDetailMapper.selectList(
                new LambdaQueryWrapper<PyPayrollDetail>()
                        .eq(PyPayrollDetail::getRunId, run.getId()));

        // 查询员工获取部门归属
        Set<Long> empIds = details.stream()
                .map(PyPayrollDetail::getEmployeeId)
                .collect(Collectors.toSet());
        Map<Long, HrEmployee> empMap;
        if (!empIds.isEmpty()) {
            List<HrEmployee> emps = employeeMapper.selectBatchIds(empIds);
            empMap = emps.stream()
                    .collect(Collectors.toMap(HrEmployee::getId, e -> e, (a, b) -> a));
        } else {
            empMap = Collections.emptyMap();
        }

        // 查询部门名称
        Set<Long> deptIds = empMap.values().stream()
                .map(HrEmployee::getDeptId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> deptNameMap;
        if (!deptIds.isEmpty()) {
            List<Department> depts = departmentMapper.selectBatchIds(deptIds);
            deptNameMap = depts.stream()
                    .collect(Collectors.toMap(Department::getId, Department::getName, (a, b) -> a));
        } else {
            deptNameMap = Collections.emptyMap();
        }

        // 查询薪资档案（用于结构分析）
        List<PyCompensationMaster> compensations = compensationMapper.selectList(
                new LambdaQueryWrapper<PyCompensationMaster>()
                        .in(PyCompensationMaster::getEmployeeId, empIds)
                        .orderByDesc(PyCompensationMaster::getEffectiveDate));
        Map<Long, PyCompensationMaster> compMap = new HashMap<>();
        for (PyCompensationMaster comp : compensations) {
            compMap.putIfAbsent(comp.getEmployeeId(), comp);
        }

        // 按部门分组
        Map<Long, List<PyPayrollDetail>> detailsByDept = details.stream()
                .filter(d -> empMap.containsKey(d.getEmployeeId()))
                .collect(Collectors.groupingBy(d -> {
                    HrEmployee emp = empMap.get(d.getEmployeeId());
                    return emp.getDeptId() != null ? emp.getDeptId() : 0L;
                }));

        List<PayrollSummaryVo> result = new ArrayList<>();
        for (Map.Entry<Long, List<PyPayrollDetail>> entry : detailsByDept.entrySet()) {
            Long deptId = entry.getKey();
            List<PyPayrollDetail> deptDetails = entry.getValue();

            PayrollSummaryVo vo = new PayrollSummaryVo();
            vo.setDeptId(deptId);
            vo.setDeptName(deptId == 0 ? "未分配部门" : deptNameMap.getOrDefault(deptId, "未知部门"));
            vo.setMonth(month);
            vo.setEmployeeCount(deptDetails.size());

            // 各项薪资合计
            BigDecimal totalGross = BigDecimal.ZERO;
            BigDecimal totalSocial = BigDecimal.ZERO;
            BigDecimal totalHousing = BigDecimal.ZERO;
            BigDecimal totalIit = BigDecimal.ZERO;
            BigDecimal totalNet = BigDecimal.ZERO;
            BigDecimal totalBase = BigDecimal.ZERO;
            BigDecimal totalPosition = BigDecimal.ZERO;
            BigDecimal totalPerf = BigDecimal.ZERO;
            BigDecimal totalAllowance = BigDecimal.ZERO;

            for (PyPayrollDetail d : deptDetails) {
                totalGross = totalGross.add(safe(d.getGrossPay()));
                totalSocial = totalSocial.add(safe(d.getSocialInsurance()));
                totalHousing = totalHousing.add(safe(d.getHousingFund()));
                totalIit = totalIit.add(safe(d.getIit()));
                totalNet = totalNet.add(safe(d.getNetPay()));

                // 薪资结构
                PyCompensationMaster comp = compMap.get(d.getEmployeeId());
                if (comp != null) {
                    totalBase = totalBase.add(safe(comp.getBaseSalary()));
                    totalPosition = totalPosition.add(safe(comp.getPositionSalary()));
                    totalPerf = totalPerf.add(safe(comp.getPerformanceBase()));
                    totalAllowance = totalAllowance.add(safe(comp.getAllowance()));
                }
            }

            vo.setTotalGrossPay(totalGross.setScale(2, RoundingMode.HALF_UP));
            vo.setTotalSocialInsurance(totalSocial.setScale(2, RoundingMode.HALF_UP));
            vo.setTotalHousingFund(totalHousing.setScale(2, RoundingMode.HALF_UP));
            vo.setTotalIit(totalIit.setScale(2, RoundingMode.HALF_UP));
            vo.setTotalNetPay(totalNet.setScale(2, RoundingMode.HALF_UP));
            vo.setTotalBaseSalary(totalBase.setScale(2, RoundingMode.HALF_UP));
            vo.setTotalPositionSalary(totalPosition.setScale(2, RoundingMode.HALF_UP));
            vo.setTotalPerformance(totalPerf.setScale(2, RoundingMode.HALF_UP));
            vo.setTotalAllowance(totalAllowance.setScale(2, RoundingMode.HALF_UP));

            // 人均薪资
            BigDecimal avgGross = deptDetails.size() > 0
                    ? totalGross.divide(BigDecimal.valueOf(deptDetails.size()), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            vo.setAvgGrossPay(avgGross);

            result.add(vo);
        }

        result.sort(Comparator.comparing(PayrollSummaryVo::getDeptName,
                Comparator.nullsLast(Comparator.naturalOrder())));
        return result;
    }

    /**
     * 获取薪资汇总报表 - 年度薪资趋势
     * 返回每个月各部门的薪资汇总
     */
    public List<PayrollSummaryVo> getPayrollYearlyTrend(Integer year) {
        List<PayrollSummaryVo> result = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            String month = String.format("%d-%02d", year, m);
            List<PayrollSummaryVo> monthlyData = getPayrollSummaryByDept(month);
            result.addAll(monthlyData);
        }
        return result;
    }

    private BigDecimal safe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
