# Epic 02: 考勤与假期（Attendance & Leave）

**对标**：SAP PT / PeopleSoft Time and Labor + Absence Management

## 业务目标
管理员工的班次、排班、考勤记录与请假申请，提供假期余额查询与年度结算。

## Sprint 2 Stories

| Story | 名称 | 范围 | 状态 |
|---|---|---|---|
| EP02-S06 | 班次管理 + 排班 + 打卡导入 + 考勤汇总 | 基础设施层 | pending |
| EP02-S07 | 请假管理 + 假期余额 + 加班单 | 员工自助层 | pending |
| EP02-S08 | 假期年度结算 | 管理层 | pending |

## 硬边界
- 不接入硬件考勤机，仅手工/CSV
- 不做工时制差异（标准/不定时/综合 MVP 全部按标准制）
- 不做加班自动联动工资
- 不做缺勤自动扣薪
- 不做请假流程的会签或代理人
- 不做按工作日扣减（仅自然日）
