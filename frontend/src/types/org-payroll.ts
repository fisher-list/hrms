// Org + payroll types
export interface DepartmentTreeVo {
  id: number;
  companyId: number;
  parentId?: number;
  name: string;
  code: string;
  path?: string;
  level?: number;
  headId?: number;
  sortOrder?: number;
  children?: DepartmentTreeVo[];
}

export interface DepartmentReq {
  companyId: number;
  parentId?: number;
  name: string;
  code: string;
  headId?: number;
  sortOrder?: number;
}

export interface CompanyVo {
  id: number;
  name: string;
  code?: string;
}

// ==================== 岗位 ====================
export interface PositionVo {
  id: number;
  deptId: number;
  jobId: number;
  name: string;
  code: string;
  headcount?: number;
  occupied?: number;
}

export interface PositionReq {
  deptId: number;
  jobId: number;
  name: string;
  code?: string;
  headcount?: number;
}

// ==================== 薪酬周期/批次 ====================
export interface PayrollPeriodVo {
  id: number;
  periodMonth: string;
  status: string;
}

export interface PayrollRunVo {
  id: number;
  periodId: number;
  runType: string;
  status: string;
  reverseOfRunId?: number;
  employeeCount?: number;
  totalGross?: number;
  totalNet?: number;
}

export interface PayrollDetailVo {
  id: number;
  runId: number;
  employeeId: number;
  employeeName?: string;
  empNo?: string;
  grossPay: number;
  socialInsurance: number;
  housingFund: number;
  iit: number;
  netPay: number;
  isException?: boolean;
}

// ==================== 薪资档案 ====================
export interface CompensationVo {
  id: number;
  employeeId: number;
  baseSalary: number;
  positionSalary: number;
  performanceBase: number;
  allowance: number;
  effectiveDate: string;
}

export interface CompensationCreateReq {
  employeeId: number;
  baseSalary?: number;
  positionSalary?: number;
  performanceBase?: number;
  allowance?: number;
  effectiveDate: string;
}

// ==================== 多地区社保 ====================
export interface RegionSocialRateVo {
  id: number;
  city: string;
  pensionPersonal: number;
  pensionCompany: number;
  medicalPersonal: number;
  medicalFixedFee: number;
  medicalCompany: number;
  unemploymentPersonal: number;
  unemploymentCompany: number;
  injuryCompany?: number;
  maternityCompany?: number;
  housingFundPersonal: number;
  housingFundCompany: number;
  socialBaseFloor?: number;
  socialBaseCeil?: number;
  fundBaseFloor?: number;
  fundBaseCeil?: number;
  enabled: boolean;
}

// ==================== 缺勤配额规则 ====================
export interface LeaveQuotaRuleVo {
  id: number;
  name: string;
  code: string;
  leaveTypeId: number;
  seniorityMin: number;
  seniorityMax?: number;
  gradeMin?: number;
  gradeMax?: number;
  quotaDays: number;
  enabled: boolean;
  sortNo: number;
}

export interface LeaveQuotaGenerateReq {
  employeeId?: number;
  year: number;
}
