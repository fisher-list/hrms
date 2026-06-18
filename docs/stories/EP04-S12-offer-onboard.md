# EP04-S12: Offer 管理 + 自动建档 + 确认入职

**Epic**: 04 招聘与入职
**Sprint**: 4
**模块**: hrms-common (recruit package)

## 涉及表

| 表名 | 说明 |
|---|---|
| `rc_offer` | Offer：候选人ID、职位ID、薪资、入职日、状态(DRAFT/SENT/ACCEPTED/DECLINED/EXPIRED)、审批实例ID |
| `rc_candidate_status_log` | 候选人状态历史：候选人ID、旧状态、新状态、变更时间、备注 |

## 接口
- POST `/api/recruit/offers` — 创建 Offer
- POST `/api/recruit/offers/{id}/submit` — 提交审批
- POST `/api/recruit/offers/{id}/accept` — 标记接受
- POST `/api/recruit/offers/{id}/decline` — 标记拒绝
- POST `/api/recruit/candidates/{id}/confirm-onboard` — 确认入职（转 ACTIVE）

## 验收标准
1. Offer 审批通过后不可改金额
2. 接受 Offer 后自动创建员工档案草稿（PENDING_HIRE）+ SysUser
3. 确认入职后状态转 PROBATION，触发账号创建
4. 拒绝或超期（7天）自动作废

## 种子数据
- 审批定义：RC_OFFER_APPROVAL (approver_type='ROLE:ADMIN')
- 权限：rc:offer:edit, rc:offer:list, rc:offer:accept
