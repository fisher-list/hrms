/**
 * 报表相关类型定义
 */

/** HR仪表盘概览数据 */
export interface DashboardVo {
  // 人员指标
  activeCount: number;
  newHireThisMonth: number;
  terminatedThisMonth: number;
  terminatedThisYear: number;
  turnoverRate: number;
  // 招聘指标
  openRequisitionCount: number;
  candidateCount: number;
  offerCount: number;
  // 考勤指标
  attendanceAnomalyRate: number;
  leaveRequestCount: number;
  overtimeRequestCount: number;
  // 薪资指标
  latestPayrollGross: number;
  latestPayrollNet: number;
  latestPayrollEmployeeCount: number;
  latestPayrollMonth: string;
}

/** 人事花名册查询条件 */
export interface RosterQueryDto {
  keyword?: string;
  status?: string;
  deptId?: number;
  columns?: string[];
  orderBy?: string;
  orderDir?: string;
}

/** 人事花名册数据行 */
export interface RosterRowVo {
  id: number;
  empNo: string;
  name: string;
  gender?: string;
  birthDate?: string;
  email?: string;
  deptId?: number;
  deptName?: string;
  positionId?: number;
  hireDate?: string;
  status: string;
  contractStart?: string;
  contractEnd?: string;
  probationEnd?: string;
  emergencyContact?: string;
}

/** 考勤月报数据 */
export interface AttendanceMonthlyVo {
  dimensionId: number;
  dimensionName: string;
  empNo?: string;
  month: string;
  scheduledDays: number;
  attendedDays: number;
  attendanceRate: number;
  lateCount: number;
  earlyLeaveCount: number;
  absentDays: number;
  overtimeHours: number;
  leaveDays: number;
}

/** 薪资汇总报表数据 */
export interface PayrollSummaryVo {
  deptId: number;
  deptName: string;
  month: string;
  employeeCount: number;
  totalBaseSalary: number;
  totalPositionSalary: number;
  totalPerformance: number;
  totalAllowance: number;
  totalGrossPay: number;
  totalSocialInsurance: number;
  totalHousingFund: number;
  totalIit: number;
  totalNetPay: number;
  avgGrossPay: number;
}
