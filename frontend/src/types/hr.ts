// Employee + leave types for HR/attendance frontend
export interface EmployeeListVo {
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
  createdAt?: string;
  idCardMasked?: string;
  phoneMasked?: string;
}

export interface LeaveTypeVo {
  id: number;
  code: string;
  name: string;
  unit?: string;
}

export interface LeaveRequestCreateReq {
  leaveTypeId: number;
  startDate: string;
  endDate: string;
  days: number;
  reason?: string;
}
