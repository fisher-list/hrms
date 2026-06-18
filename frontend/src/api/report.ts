import { request } from './http';
import type { PageVo } from '@/types/portal';
import type {
  DashboardVo,
  RosterQueryDto,
  RosterRowVo,
  AttendanceMonthlyVo,
  PayrollSummaryVo,
} from '@/types/report';

// ==================== HR仪表盘 ====================

/** 获取HR仪表盘概览数据 */
export function getDashboard(): Promise<DashboardVo> {
  return request<DashboardVo>({
    url: '/reports/dashboard',
    method: 'GET',
  });
}

// ==================== 人事花名册报表 ====================

/** 人事花名册查询 */
export function getRoster(
  query: RosterQueryDto = {},
  current = 1,
  size = 20,
): Promise<PageVo<RosterRowVo>> {
  return request<PageVo<RosterRowVo>>({
    url: '/reports/roster',
    method: 'POST',
    data: query,
    params: { current, size },
  });
}

// ==================== 考勤月报 ====================

/** 考勤月报 - 按部门维度 */
export function getAttendanceMonthlyByDept(month: string): Promise<AttendanceMonthlyVo[]> {
  return request<AttendanceMonthlyVo[]>({
    url: '/reports/attendance-monthly/dept',
    method: 'GET',
    params: { month },
  });
}

/** 考勤月报 - 按个人维度 */
export function getAttendanceMonthlyByEmployee(
  month: string,
  deptId?: number,
): Promise<AttendanceMonthlyVo[]> {
  return request<AttendanceMonthlyVo[]>({
    url: '/reports/attendance-monthly/employee',
    method: 'GET',
    params: { month, deptId },
  });
}

// ==================== 薪资汇总报表 ====================

/** 薪资汇总报表 - 按部门维度 */
export function getPayrollSummaryByDept(month: string): Promise<PayrollSummaryVo[]> {
  return request<PayrollSummaryVo[]>({
    url: '/reports/payroll-summary/dept',
    method: 'GET',
    params: { month },
  });
}

/** 薪资汇总报表 - 年度薪资趋势 */
export function getPayrollYearlyTrend(year: number): Promise<PayrollSummaryVo[]> {
  return request<PayrollSummaryVo[]>({
    url: '/reports/payroll-summary/trend',
    method: 'GET',
    params: { year },
  });
}
