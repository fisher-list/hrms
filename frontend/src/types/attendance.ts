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
