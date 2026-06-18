// ==================== 班次 ====================
export interface ShiftVo {
  id: number;
  name: string;
  startTime: string;   // HH:mm
  endTime: string;     // HH:mm
  crossDay: boolean;
  status: string;      // ACTIVE / INACTIVE
}

export interface ShiftCreateReq {
  name: string;
  startTime: string;
  endTime: string;
  crossDay: boolean;
  status: string;
}

// ==================== 排班 ====================
export interface ScheduleVo {
  id: number;
  employeeId: number;
  employeeName?: string;
  shiftId: number;
  shiftName?: string;
  workDate: string;    // YYYY-MM-DD
}

export interface ScheduleCreateReq {
  employeeIds: number[];
  shiftId: number;
  dates: string[];     // YYYY-MM-DD[]
}

// ==================== 打卡 ====================
export interface TimePunchVo {
  id: number;
  employeeId: number;
  employeeName?: string;
  punchTime: string;   // ISO datetime
  punchType: string;   // CLOCK_IN / CLOCK_OUT
  source?: string;
}

// ==================== 请假 ====================
export interface LeaveRequestListVo {
  id: number;
  employeeId: number;
  leaveTypeId: number;
  startDate: string;
  endDate: string;
  days: number;
  reason: string;
  status: string;
  approvalInstanceId?: number;
}

export interface LeaveRequestCreateReq {
  leaveTypeId: number;
  startDate: string;
  endDate: string;
  days: number;
  reason?: string;
}

// ==================== 加班 ====================
export interface OvertimeRequestVo {
  id: number;
  employeeId: number;
  overtimeDate: string;
  hours: number;
  reason?: string;
  status: string;
  approvalInstanceId?: number;
}

export interface OvertimeRequestCreateReq {
  overtimeDate: string;
  hours: number;
  reason?: string;
}

// ==================== 考勤异常 ====================
export interface AttendanceAnomalyVo {
  id: number;
  employeeId: number;
  anomalyDate: string;
  anomalyType: string;    // LATE / EARLY / ABSENT / MISSING
  durationMinutes: number;
  status: string;          // PENDING / HANDLED / IGNORED
  handleRemark?: string;
  scheduleId?: number;
  punchId?: number;
}

export interface AnomalyHandleReq {
  anomalyId: number;
  action: string;          // HANDLED / IGNORED
  remark?: string;
}

// ==================== 调休管理 ====================
export interface CompensatoryLeaveVo {
  id: number;
  employeeId: number;
  year: number;
  totalQuota: number;
  used: number;
  remaining: number;
}

export interface CompensatoryLeaveLogVo {
  id: number;
  compLeaveId: number;
  changeType: string;      // CONVERT / DEDUCT / REFUND
  changeValue: number;
  convertRate?: number;
  overtimeRequestId?: number;
  leaveRequestId?: number;
  remark?: string;
  createdAt?: string;
}

export interface OvertimeToCompLeaveReq {
  overtimeRequestId: number;
  convertRate: number;
  remark?: string;
}

// ==================== 出差管理 ====================
export interface BusinessTripVo {
  id: number;
  employeeId: number;
  destination: string;
  reason: string;
  startDate: string;
  endDate: string;
  days: number;
  status: string;          // PENDING / SUBMITTED / APPROVED / REJECTED / CANCELLED
  approvalInstanceId?: number;
}

export interface BusinessTripCreateReq {
  destination: string;
  reason: string;
  startDate: string;
  endDate: string;
  days: number;
}
