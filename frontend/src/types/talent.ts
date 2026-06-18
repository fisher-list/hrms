// Recruit + performance frontend types

// ==================== 候选人 ====================
export interface CandidateVo {
  id: number;
  name: string;
  phone?: string;
  email?: string;
  idCardEnc?: string;
  expectedSalary?: number;
  currentStatus: string;
  jobRequisitionId?: number;
}

export interface CandidateCreateReq {
  name: string;
  phone?: string;
  email?: string;
  idCardEnc?: string;
  expectedSalary?: number;
  jobRequisitionId?: number;
}

// ==================== Offer ====================
export interface OfferVo {
  id: number;
  candidateId: number;
  jobRequisitionId?: number;
  salary: number;
  onboardDate: string;
  status: string;
  approvalInstanceId?: number;
}

export interface OfferCreateReq {
  candidateId: number;
  jobRequisitionId?: number;
  salary: number;
  onboardDate: string;
}

// ==================== 职位需求 ====================
export interface RequisitionVo {
  id: number;
  positionId?: number;
  title: string;
  description?: string;
  headcount: number;
  deadline?: string;
  status: string;
}

export interface RequisitionCreateReq {
  positionId?: number;
  title: string;
  description?: string;
  headcount: number;
  deadline?: string;
}

// ==================== 绩效 ====================
export interface PerformanceCycleVo {
  id: number;
  name: string;
  startDate: string;
  endDate: string;
  status: string;
  scopeType?: string;
  scopeDepts?: string;
}

export interface AppraisalVo {
  id: number;
  cycleId: number;
  templateId: number;
  employeeId: number;
  status: string;
  selfScore?: number;
  managerScore?: number;
  finalScore?: number;
}

export interface GoalItemReq {
  description: string;
  weight: number;
}

export interface ReviewItemReq {
  scoringItemId: number;
  score: number;
  comment?: string;
}
