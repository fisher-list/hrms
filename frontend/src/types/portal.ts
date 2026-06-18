// ESS / MSS portal types
export interface LeaveBalanceVo {
  id: number;
  employeeId: number;
  leaveTypeId: number;
  year: number;
  totalDays: number;
  usedDays: number;
  remainingDays: number;
}

export interface LeaveRequestVo {
  id: number;
  employeeId: number;
  leaveTypeId: number;
  startDate: string;
  endDate: string;
  totalDays: number;
  reason: string;
  status: string;
  approvalInstanceId?: number;
}

export interface ApprovalTaskVo {
  id: number;
  instanceId: number;
  assigneeId: number;
  status: string;
  nodeName: string;
  createdAt: string;
}

export interface PayslipVo {
  id: number;
  runId: number;
  employeeId: number;
  grossPay: number;
  socialInsurance: number;
  housingFund: number;
  iitTax: number;
  netPay: number;
}

export interface PageVo<T> {
  records: T[];
  total: number;
  size: number;
  current: number;
}

// ==================== 审批 ====================
export interface ApprovalHistoryVo {
  id: number;
  instanceId: number;
  taskId?: number;
  nodeSeq: number;
  actorId: number;
  action: string;
  comment?: string;
  actedAt: string;
}
