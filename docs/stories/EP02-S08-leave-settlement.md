# EP02-S08: 假期年度结算

**Epic**: 02 考勤与假期
**Sprint**: 2
**模块**: hrms-common (attendance package)

## 背景
Epic 02 的管理层。提供年度结算批次功能，自动按规则结转/清零/计提次年余额。

## 涉及表

| 表名 | 说明 |
|---|---|
| `at_settlement_batch` | 结算批次：年度、状态(PENDING/RUNNING/COMPLETED)、处理人数、结转总额 |
| `at_leave_balance_log` | 复用 S07 表，记录 SETTLE 类型变动 |

## 涉及接口

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| POST | `/api/attendance/settlements` | `at:settlement:run` | 触发年度结算 |
| GET | `/api/attendance/settlements` | `at:settlement:list` | 查询结算批次历史 |
| GET | `/api/attendance/settlements/{id}` | `at:settlement:list` | 查询结算批次详情（含变动明细） |

## 验收标准

### AC1：年度结算批次
- 管理员指定年度（如 2025），系统遍历所有在职员工的假期余额
- 生成结算批次记录，记录处理人数

### AC2：结转规则
- **年假（ANNUAL）**：结转上限 5 天，超出部分清零；结转余额有效期至次年 3 月底
- **事假（PERSONAL）、病假（SICK）**：当年清零，不结转
- **婚假/产假/陪产假/丧假**：当年清零，不结转
- **调休（COMPOFF）**：2 月内有效，过期清零（关联加班单日期 + 2个月）

### AC3：次年余额初始化
- 结算完成后，为每位在职员工创建次年余额记录
- 次年余额 = 年度额度 + 结转量（受上限约束）
- 记录余额变动日志（SETTLE 类型）

### AC4：幂等性
- 同一年度不可重复结算
- 结算批次状态为 COMPLETED 后，再次触发同一年度返回错误

### AC5：调休过期处理
- 调休余额关联的加班单日期 + 2个月 < 结算日期 → 自动清零
- 清零记录写入余额变动日志

## 技术上下文

### 依赖的已有服务
- `at_leave_balance` 表（S07 创建）
- `at_leave_balance_log` 表（S07 创建）
- `at_overtime_request` 表（S07 创建，用于调休来源判断）
- `HrEmployeeService` - 获取在职员工列表

### Liquibase changelog
- 文件：`changelog-07-attendance-settlement.yaml`
- 包含：`at_settlement_batch` 表 + 权限种子

### 注意事项
- 结算为批量操作，20 人规模下单事务即可
- 调休 FIFO 抵扣逻辑：按加班单日期升序，优先抵扣最早的调休余额
- 结算批次记录每人每假类的结转明细，便于审计
