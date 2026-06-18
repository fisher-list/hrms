# EP02-S06: 班次管理 + 排班 + 打卡导入 + 考勤汇总

**Epic**: 02 考勤与假期
**Sprint**: 2
**模块**: hrms-common (attendance package)

## 背景
Epic 02 的基础设施层。提供班次模板、排班指派、打卡记录录入（含 CSV 批量导入）和考勤日汇总生成。

## 涉及表

| 表名 | 说明 |
|---|---|
| `at_shift` | 班次模板：名称、上班时间、下班时间、弹性分钟、是否夜班 |
| `at_schedule` | 排班：员工 ID + 日期 + 班次 ID |
| `at_time_punch` | 打卡记录：员工 ID + 日期 + 上班打卡时间 + 下班打卡时间 + 来源(MANUAL/CSV) |
| `at_daily_summary` | 考勤日汇总：员工 ID + 日期 + 应出勤 + 实际出勤 + 迟到分钟 + 早退分钟 + 缺勤 + 加班小时 |

## 涉及接口

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| POST | `/api/attendance/shifts` | `at:shift:edit` | 创建班次 |
| GET | `/api/attendance/shifts` | `at:shift:list` | 查询班次列表 |
| PUT | `/api/attendance/shifts/{id}` | `at:shift:edit` | 修改班次 |
| DELETE | `/api/attendance/shifts/{id}` | `at:shift:edit` | 删除班次 |
| POST | `/api/attendance/schedules` | `at:schedule:edit` | 批量排班（员工ID列表 + 日期范围 + 班次ID） |
| GET | `/api/attendance/schedules` | `at:schedule:list` | 查询排班（按员工/部门/日期范围） |
| POST | `/api/attendance/punches/import` | `at:punch:import` | CSV 批量导入打卡 |
| POST | `/api/attendance/punches` | `at:punch:edit` | 手工录入打卡 |
| GET | `/api/attendance/punches` | `at:punch:list` | 查询打卡记录 |
| POST | `/api/attendance/summaries/generate` | `at:summary:generate` | 按日期范围生成考勤日汇总 |
| GET | `/api/attendance/summaries` | `at:summary:list` | 查询考勤日汇总 |

## 验收标准

### AC1：班次模板 CRUD
- 管理员可创建/修改/删除/查询班次模板
- 字段：名称、上班时间（HH:mm）、下班时间（HH:mm）、弹性分钟（默认0）、是否夜班（boolean）、备注
- 班次被排班引用时不可删除

### AC2：排班
- 管理员可为一个或多个员工在日期范围内指派班次
- 参数：employeeIds（列表）、shiftId、startDate、endDate
- 排班结果可按员工/部门/日期范围查询
- 同一员工同一天只能有一个排班（覆盖更新）

### AC3：打卡记录（手工 + CSV）
- 手工录入：员工ID、日期、上班打卡时间、下班打卡时间
- CSV 格式：`emp_no,date,clock_in,clock_out`（UTF-8，首行表头）
- CSV 校验失败时输出行号+原因，整批不写入
- 校验项：工号存在、日期格式合法、clock_in < clock_out

### AC4：考勤日汇总生成
- 指定日期范围触发，遍历当日有排班的员工
- 比对排班时间与打卡时间：
  - 实际出勤 = 有打卡记录
  - 迟到分钟 = max(0, clock_in - 班次上班时间)
  - 早退分钟 = max(0, 班次下班时间 - clock_out)
  - 缺勤 = 有排班但无打卡
  - 加班小时 = max(0, (clock_out - 班次下班时间) / 60)，不足30分钟不计
- 已有汇总时覆盖更新

### AC5：请假日期校验
- 请假起止日跨越员工离职日时，系统拒绝（依赖 S05 员工状态）

## 技术上下文

### 依赖的已有服务
- `HrEmployeeService` - 验证员工存在与状态
- `DepartmentService` - 数据权限
- `DataScope` / `DataScopeInterceptor` - 行级权限
- `@HasPermission` - 按钮级权限

### Liquibase changelog
- 文件：`changelog-05-attendance-shift.yaml`
- 包含：4 表 + 索引 + 权限种子数据 + 默认班次（朝九晚六）

### 注意事项
- 时间字段使用 `LocalTime`，日期使用 `LocalDate`
- 打卡来源区分 `MANUAL` / `CSV`
- 考勤汇总的加班小时暂存到 `at_daily_summary.overtime_hours`，S08 年度结算时读取
- CSV 解析使用 OpenCSV 或 hutool-csv
