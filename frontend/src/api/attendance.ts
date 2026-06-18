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
