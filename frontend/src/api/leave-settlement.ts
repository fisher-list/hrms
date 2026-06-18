import { request } from './http';

/* ===== 假期结算相关类型 ===== */

/** 结算策略 */
export type CarryStrategy = 'CARRY_MAX5' | 'NONE' | 'EXPIRE_2M';

/** 结算批次状态 */
export type SettlementStatus = 'DRAFT' | 'EXECUTING' | 'COMPLETED' | 'REVERSED';

/** 结算批次 VO */
export interface LeaveSettlementVo {
  id: number;
  year: number;
  carryStrategy: CarryStrategy;
  status: SettlementStatus;
  employeeCount: number;
  carriedDays: number;
  expiredDays: number;
  createdAt: string;
}

/** 发起结算请求 */
export interface SettlementCreateReq {
  year: number;
  carryStrategy: CarryStrategy;
}

/** 结算明细 VO */
export interface SettlementDetailVo {
  id: number;
  employeeId: number;
  empNo: string;
  employeeName: string;
  department: string;
  leaveTypeName: string;
  carriedDays: number;
  expiredDays: number;
}

/** 假期余额 VO */
export interface LeaveBalanceVo {
  id: number;
  employeeId: number;
  empNo: string;
  employeeName: string;
  leaveTypeName: string;
  totalDays: number;
  usedDays: number;
  remainingDays: number;
  year: number;
}

/** 余额查询参数 */
export interface BalanceQuery {
  year?: number;
  leaveTypeName?: string;
  employeeName?: string;
}

/* ===== API ===== */

/** 结算批次列表 */
export function listSettlements(): Promise<LeaveSettlementVo[]> {
  return request<LeaveSettlementVo[]>({
    url: '/attendance/leave-settlements',
    method: 'GET',
  });
}

/** 发起结算 */
export function createSettlement(req: SettlementCreateReq): Promise<LeaveSettlementVo> {
  return request<LeaveSettlementVo>({
    url: '/attendance/leave-settlements',
    method: 'POST',
    data: req,
  });
}

/** 获取结算明细 */
export function getSettlementDetails(settlementId: number): Promise<SettlementDetailVo[]> {
  return request<SettlementDetailVo[]>({
    url: `/attendance/leave-settlements/${settlementId}/details`,
    method: 'GET',
  });
}

/** 冲销结算 */
export function reverseSettlement(settlementId: number): Promise<void> {
  return request<void>({
    url: `/attendance/leave-settlements/${settlementId}/reverse`,
    method: 'POST',
  });
}

/** 假期余额查询 */
export function queryLeaveBalances(params: BalanceQuery): Promise<LeaveBalanceVo[]> {
  return request<LeaveBalanceVo[]>({
    url: '/attendance/leave-balances',
    method: 'GET',
    params,
  });
}
