# EP05-S14: 目标 + 自评 + 上级评分 + 最终得分

**Epic**: 05 绩效
**Sprint**: 4
**模块**: hrms-common (performance package)

## 涉及表

| 表名 | 说明 |
|---|---|
| `pf_goal` | 目标条目：绩效单ID、描述、权重(%)、状态(DRAFT/CONFIRMED) |
| `pf_self_review` | 自评：绩效单ID、评分项ID、得分(1-5)、评论 |
| `pf_manager_review` | 上级评分：绩效单ID、评分项ID、得分(1-5)、评论 |

## 接口
- POST `/api/performance/appraisals/{id}/goals` — 录入目标
- POST `/api/performance/appraisals/{id}/goals/confirm` — 上级确认目标
- POST `/api/performance/appraisals/{id}/self-review` — 提交自评
- POST `/api/performance/appraisals/{id}/manager-review` — 提交上级评分
- POST `/api/performance/appraisals/{id}/finalize` — 计算最终得分

## 验收标准
1. 最终得分 = 自评加权均值 × 0.3 + 上级加权均值 × 0.7
2. 状态严格流转：DRAFT→GOAL_SET→GOAL_CONFIRMED→SELF_REVIEWED→MANAGER_REVIEWED→COMPLETED
3. COMPLETED 后对员工可见最终得分

## 种子数据
- 权限：pf:goal:edit, pf:review:self, pf:review:manager, pf:appraisal:finalize
