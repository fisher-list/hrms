# EP05-S13: 绩效周期 + 模板 + 评分项

**Epic**: 05 绩效
**Sprint**: 4
**模块**: hrms-common (performance package)

## 涉及表

| 表名 | 说明 |
|---|---|
| `pf_cycle` | 绩效周期：名称(如2026H1)、开始日、结束日、状态(DRAFT/ACTIVE/CLOSED)、适用范围 |
| `pf_template` | 绩效模板：名称、描述、评分项列表(JSON) |
| `pf_scoring_item` | 评分项：模板ID、名称、权重(%)、分值范围(min/max) |
| `pf_appraisal` | 绩效单：周期ID、模板ID、员工ID、状态(DRAFT/GOAL_SET/GOAL_CONFIRMED/SELF_REVIEWED/MANAGER_REVIEWED/COMPLETED)、自评分、上级评分、最终得分 |

## 接口
- POST/GET `/api/performance/cycles` — 周期 CRUD
- POST/GET `/api/performance/templates` — 模板 CRUD
- GET `/api/performance/appraisals` — 查询绩效单列表

## 验收标准
1. 创建周期时自动生成适用员工范围的绩效单
2. 同一员工同一周期只有 1 份绩效单
3. 模板含 3+ 评分项，权重合计=100%

## 种子数据
- 1 个模板："标准绩效模板"（3 项：工作质量40%、团队协作30%、创新能力30%）
- 权限：pf:cycle:edit, pf:cycle:list, pf:template:edit, pf:template:list, pf:appraisal:list
