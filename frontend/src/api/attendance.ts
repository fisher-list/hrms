import { request } from './http';
import type { PageVo } from '@/types/portal';
import type {
  ShiftVo,
  ShiftCreateReq,
  ScheduleVo,
  ScheduleCreateReq,
  TimePunchVo,
  LeaveRequestListVo,
  LeaveRequestCreateReq,
  OvertimeRequestVo,
  OvertimeRequestCreateReq,
  AttendanceAnomalyVo,
  AnomalyHandleReq,
  CompensatoryLeaveVo,
  CompensatoryLeaveLogVo,
  OvertimeToCompLeaveReq,
  BusinessTripVo,
  BusinessTripCreateReq,
} from '@/types/attendance';

// ==================== 班次管理 ====================
export function listShifts(current = 1, size = 50): Promise<PageVo<ShiftVo>> {
  return request<PageVo<ShiftVo>>({
    url: '/attendance/shifts',
    method: 'GET',
    params: { current, size },
  });
}

export function createShift(data: ShiftCreateReq): Promise<ShiftVo> {
  return request<ShiftVo>({ url: '/attendance/shifts', method: 'POST', data });
}

export function updateShift(id: number, data: ShiftCreateReq): Promise<ShiftVo> {
  return request<ShiftVo>({ url: `/attendance/shifts/${id}`, method: 'PUT', data });
}

export function deleteShift(id: number): Promise<void> {
  return request<void>({ url: `/attendance/shifts/${id}`, method: 'DELETE' });
}

// ==================== 排班管理 ====================
export function listSchedules(params: {
  employeeId?: number;
  deptId?: number;
  startDate?: string;
  endDate?: string;
}): Promise<ScheduleVo[]> {
  return request<ScheduleVo[]>({
    url: '/attendance/schedules',
    method: 'GET',
    params,
  });
}

export function createSchedule(data: ScheduleCreateReq): Promise<ScheduleVo[]> {
  return request<ScheduleVo[]>({ url: '/attendance/schedules', method: 'POST', data });
}

// ==================== 打卡记录 ====================
export function listTimePunches(params: {
  employeeId?: number;
  startDate?: string;
  endDate?: string;
}): Promise<TimePunchVo[]> {
  return request<TimePunchVo[]>({
    url: '/attendance/punches',
    method: 'GET',
    params,
  });
}

// ==================== 请假申请 ====================
export function listLeaveRequests(params: {
  employeeId?: number;
  status?: string;
  current?: number;
  size?: number;
}): Promise<PageVo<LeaveRequestListVo>> {
  return request<PageVo<LeaveRequestListVo>>({
    url: '/attendance/leave-requests',
    method: 'GET',
    params,
  });
}

export function createLeaveRequest(req: LeaveRequestCreateReq): Promise<LeaveRequestListVo> {
  return request<LeaveRequestListVo>({
    url: '/attendance/leave-requests',
    method: 'POST',
    data: req,
  });
}

export function submitLeaveRequest(id: number): Promise<void> {
  return request<void>({
    url: `/attendance/leave-requests/${id}/submit`,
    method: 'POST',
  });
}

export function getLeaveRequest(id: number): Promise<LeaveRequestListVo> {
  return request<LeaveRequestListVo>({
    url: `/attendance/leave-requests/${id}`,
    method: 'GET',
  });
}

// ==================== 加班申请 ====================
export function listOvertimeRequests(params: {
  employeeId?: number;
  status?: string;
  current?: number;
  size?: number;
}): Promise<PageVo<OvertimeRequestVo>> {
  return request<PageVo<OvertimeRequestVo>>({
    url: '/attendance/overtime-requests',
    method: 'GET',
    params,
  });
}

export function createOvertimeRequest(req: OvertimeRequestCreateReq): Promise<OvertimeRequestVo> {
  return request<OvertimeRequestVo>({
    url: '/attendance/overtime-requests',
    method: 'POST',
    data: req,
  });
}

export function submitOvertimeRequest(id: number): Promise<void> {
  return request<void>({
    url: `/attendance/overtime-requests/${id}/submit`,
    method: 'POST',
  });
}

// ==================== 考勤异常管理 ====================
/** 检测考勤异常 */
export function detectAnomalies(startDate: string, endDate: string): Promise<AttendanceAnomalyVo[]> {
  return request<AttendanceAnomalyVo[]>({
    url: '/attendance/anomalies/detect',
    method: 'POST',
    params: { startDate, endDate },
  });
}

/** 查询异常列表 */
export function listAnomalies(params: {
  employeeId?: number;
  startDate?: string;
  endDate?: string;
  anomalyType?: string;
  status?: string;
  current?: number;
  size?: number;
}): Promise<PageVo<AttendanceAnomalyVo>> {
  return request<PageVo<AttendanceAnomalyVo>>({
    url: '/attendance/anomalies',
    method: 'GET',
    params,
  });
}

/** 处理异常记录 */
export function handleAnomaly(req: AnomalyHandleReq): Promise<void> {
  return request<void>({
    url: '/attendance/anomalies/handle',
    method: 'POST',
    data: req,
  });
}

// ==================== 调休管理 ====================
/** 加班转调休 */
export function convertOvertimeToCompLeave(req: OvertimeToCompLeaveReq): Promise<CompensatoryLeaveVo> {
  return request<CompensatoryLeaveVo>({
    url: '/attendance/comp-leave/convert',
    method: 'POST',
    data: req,
  });
}

/** 查询调休余额 */
export function getCompLeaveBalance(year?: number): Promise<CompensatoryLeaveVo> {
  return request<CompensatoryLeaveVo>({
    url: '/attendance/comp-leave/balance',
    method: 'GET',
    params: year ? { year } : {},
  });
}

/** 查询调休余额历史 */
export function listCompLeaveBalances(): Promise<CompensatoryLeaveVo[]> {
  return request<CompensatoryLeaveVo[]>({
    url: '/attendance/comp-leave/balances',
    method: 'GET',
  });
}

/** 查询调休变动日志 */
export function listCompLeaveLogs(compLeaveId: number): Promise<CompensatoryLeaveLogVo[]> {
  return request<CompensatoryLeaveLogVo[]>({
    url: `/attendance/comp-leave/logs/${compLeaveId}`,
    method: 'GET',
  });
}

// ==================== 出差管理 ====================
/** 创建出差申请 */
export function createBusinessTrip(req: BusinessTripCreateReq): Promise<BusinessTripVo> {
  return request<BusinessTripVo>({
    url: '/attendance/business-trips',
    method: 'POST',
    data: req,
  });
}

/** 提交出差审批 */
export function submitBusinessTrip(id: number): Promise<void> {
  return request<void>({
    url: `/attendance/business-trips/${id}/submit`,
    method: 'POST',
  });
}

/** 查询出差申请列表 */
export function listBusinessTrips(params: {
  employeeId?: number;
  status?: string;
  current?: number;
  size?: number;
}): Promise<PageVo<BusinessTripVo>> {
  return request<PageVo<BusinessTripVo>>({
    url: '/attendance/business-trips',
    method: 'GET',
    params,
  });
}

/** 获取出差申请详情 */
export function getBusinessTrip(id: number): Promise<BusinessTripVo> {
  return request<BusinessTripVo>({
    url: `/attendance/business-trips/${id}`,
    method: 'GET',
  });
}

// ==================== 缺勤配额管理 ====================
/** 查询所有配额规则 */
export function listLeaveQuotaRules(): Promise<any[]> {
  return request<any[]>({ url: '/attendance/leave-quota/rules', method: 'GET' });
}

/** 创建配额规则 */
export function createLeaveQuotaRule(data: any): Promise<any> {
  return request<any>({ url: '/attendance/leave-quota/rules', method: 'POST', data });
}

/** 更新配额规则 */
export function updateLeaveQuotaRule(id: number, data: any): Promise<any> {
  return request<any>({ url: `/attendance/leave-quota/rules/${id}`, method: 'PUT', data });
}

/** 禁用配额规则 */
export function disableLeaveQuotaRule(id: number): Promise<void> {
  return request<void>({ url: `/attendance/leave-quota/rules/${id}`, method: 'DELETE' });
}

/** 为单个员工生成年假配额 */
export function generateLeaveQuota(data: { employeeId: number; year: number }): Promise<any> {
  return request<any>({ url: '/attendance/leave-quota/generate', method: 'POST', data });
}

/** 批量为所有在职员工生成年假配额 */
export function batchGenerateLeaveQuota(year: number): Promise<any> {
  return request<any>({ url: '/attendance/leave-quota/generate-batch', method: 'POST', params: { year } });
}
