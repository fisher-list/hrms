# EP04-S11: 职位需求 + 候选人 + 面试评价

**Epic**: 04 招聘与入职
**Sprint**: 4
**模块**: hrms-common (recruit package)

## 涉及表

| 表名 | 说明 |
|---|---|
| `rc_job_requisition` | 职位需求：岗位ID、标题、JD、招聘人数、截止日、状态(OPEN/CLOSED) |
| `rc_candidate` | 候选人：姓名、手机、邮箱、身份证、期望薪资、当前状态、关联职位ID |
| `rc_interview` | 面试安排：候选人ID、轮次、面试官ID、时间、地点、结果(PENDING/PASS/FAIL) |
| `rc_evaluation` | 面试评价：面试ID、评分(1-5)、通过/不通过、备注、评价人、评价时间 |

## 接口
- POST/GET `/api/recruit/requisitions` — 职位需求 CRUD
- POST/GET `/api/recruit/candidates` — 候选人 CRUD
- POST/GET `/api/recruit/interviews` — 面试安排
- POST `/api/recruit/evaluations` — 录入评价

## 验收标准
1. 职位达到招聘人数后自动 CLOSED
2. 身份证与历史员工匹配时提示
3. 面试评价不允许覆盖编辑（新增记录）

## 种子数据
- 权限：rc:requisition:edit, rc:requisition:list, rc:candidate:edit, rc:candidate:list, rc:interview:edit, rc:interview:list, rc:evaluation:edit
- 映射到 ADMIN 角色
