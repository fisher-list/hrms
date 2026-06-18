# EP02-S07: 请假管理 + 假期余额 + 加班单

**Epic**: 02 考勤与假期
**Sprint**: 2
**模块**: hrms-common (attendance package)

## 背景
Epic 02 的员工自助层。提供假类管理、假期余额、请假单审批流、加班单审批流。

## 涉及表

| 表名 | 说明 |
|---|---|
| `at_leave_type` | 假类：编码、名称、是否带薪、年度额度、余额单位(天/小时)、结转规则 |
| `at_leave_balance` | 假期余额：员工 ID + 假类 + 年度 + 额度 + 已用 + 剩余 |
| `at_leave_request` | 请假单：员工 ID + 假类 + 起始日 + 结束日 + 天数 + 原因 + 状态 + 审批实例ID |
| `at_overtime_request` | 加班单：员工 ID + 日期 + 小时数 + 原因 + 状态 + 审批实例ID |
| `at_leave_balance_log` | 余额变动日志：余额ID + 变动类型(ADJUST/DEDUCT/REFUND/SETTLE) + 变动值 + 关联单据ID |

## 涉及接口

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| POST | `/api/attendance/leave-types` | `at:leave-type:edit` | 创建假类 |
| GET | `/api/attendance/leave-types` | `at:leave-type:list` | 查询假类列表 |
| GET | `/api/attendance/leave-balances` | `at:leave-balance:list` | 查询假期余额（自己的/下属的） |
| POST | `/api/attendance/leave-requests` | `at:leave:submit` | 提交请假单 |
| POST | `/api/attendance/leave-requests/{id}/submit` | `at:leave:submit` | 提交审批 |
| GET | `/api/attendance/leave-requests` | `at:leave:list` | 查询请假单列表 |
| GET | `/api/attendance/leave-requests/{id}` | `at:leave:list` | 查询请假单详情 |
| POST | `/api/attendance/overtime-requests` | `at:overtime:submit` | 提交加班单 |
| POST | `/api/attendance/overtime-requests/{id}/submit` | `at:overtime:submit` | 提交审批 |
| GET | `/api/attendance/overtime-requests` | `at:overtime:list` | 查询加班单列表 |

## 验收标准

### AC1：假类管理
- 管理员可创建/查询假类
- 字段：编码（ANNUAL/SICK/PERSONAL/MARRIAGE/MATERNITY/PATERNITY/BEREAVEMENT/COMPOFF）、名称、是否带薪、年度额度（天）、余额单位（DAY/HOUR）、结转规则（NONE/CARRY_MAX5/EXPIRE_2M）
- 系统预置 8 种假类种子数据

### AC2：假期余额
- 每个员工每年每种假类一条余额记录
- 余额 = 额度 - 已用
- 查询时支持查看自己的（ESS）和下属的（MSS/DataScope）
- 余额不足时请假单禁止提交，明确提示"余额不足"

### AC3：请假单提交
- 员工填写：假类、起始日、结束日、天数（支持 0.5 天）、原因
- 校验：天数 > 余额 → 拒绝；起止日跨越离职日 → 拒绝；与已批准请假单时间冲突 → 拒绝
- 提交后走审批流（`approvalService.start("AT_LEAVE_REQUEST", ...)`）

### AC4：请假单审批联动
- 审批通过：扣减假期余额 + 记录余额变动日志
- 审批驳回：不扣减
- 审批事件监听（`ApprovalCompletedEvent`）

### AC5：加班单
- 员工填写：日期、加班小时数、原因
- 提交后走审批流（`approvalService.start("AT_OVERTIME_REQUEST", ...)`）
- 审批通过：仅记录，不联动工资
- 加班单可被 S08 调休假申请引用

### AC6：请假天数计算
- 按自然日计算（非工作日），半天支持 0.5
- 跨月请假单按整段一次审批，不拆月

## 技术上下文

### 依赖的已有服务
- `ApprovalService` - 审批流
- `ApprovalCompletedEvent` - 审批完成事件
- `HrEmployeeService` - 验证员工状态
- `DataScope` - 数据权限

### Liquibase changelog
- 文件：`changelog-06-attendance-leave.yaml`
- 包含：5 表 + 索引 + 8 种假类种子 + 20 名员工初始余额 + 审批定义 + 权限

### 注意事项
- 余额扣减必须在审批通过后（事件监听），非请假提交时
- 余额变动日志用于年度结算审计
- 调休假（COMPOFF）关联加班单的逻辑在 S08 实现
