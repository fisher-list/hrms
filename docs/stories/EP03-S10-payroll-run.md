# EP03-S10: 工资 Run 计算引擎 + 红冲 + 工资条

**Epic**: 03 薪酬基础
**Sprint**: 3
**模块**: hrms-common (payroll package)

## 背景
Epic 03 的核心计算层。实现工资 run 计算引擎、锁定/红冲、工资条查询。

## 涉及接口

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| POST | `/api/payroll/runs` | `py:run:create` | 创建工资 run |
| POST | `/api/payroll/runs/{id}/calculate` | `py:run:calculate` | 执行计算 |
| POST | `/api/payroll/runs/{id}/lock` | `py:run:lock` | 锁定 run |
| POST | `/api/payroll/runs/{id}/reverse` | `py:run:reverse` | 红冲 |
| GET | `/api/payroll/runs` | `py:run:list` | 查询 run 列表 |
| GET | `/api/payroll/runs/{id}` | `py:run:list` | 查询 run 详情（含明细） |
| GET | `/api/payroll/payslips` | `py:payslip:list` | ESS 查询本人工资条（按周期） |
| GET | `/api/payroll/payslips/{runId}` | `py:payslip:list` | 查询指定 run 的工资条 |

## 验收标准

### AC1：创建工资 run
- 输入：periodId、类型（NORMAL/REVERSAL）
- NORMAL run：筛选全月在职员工（本月 1 日前入职且未离职或离职日在本月末之后）
- 同一员工同一周期不允许两个 active 正向 run
- REVERSAL run：关联原 run，生成等额负向明细

### AC2：执行计算
- 对每个参与员工：
  1. 读取最新薪资档案（compensation master）
  2. 应发 = 基本工资 + 岗位工资 + 绩效基数 + 补贴
  3. 社保 = 应发 × (8% + 2% + 0.5%) = 应发 × 10.5%
  4. 公积金 = 应发 × 12%
  5. 累计应纳税所得额 = 累计应发 + 本次应发 - 累计社保 - 本次社保 - 累计公积金 - 本次公积金 - 5000×已过月份
  6. 查 IIT 税率表得累计个税，本次个税 = 累计个税 - 已扣个税
  7. 实发 = 应发 - 社保 - 公积金 - 个税
  8. 实发 < 0 → 标记该员工为异常，不计入正常明细
- 更新累计台账

### AC3：锁定 run
- 状态 CALCULATED → LOCKED
- 锁定后明细只读，编辑接口返回 403

### AC4：红冲
- 输入：原 run ID
- 生成新 run（类型 REVERSAL），所有明细金额 = 原明细 × -1
- 红冲 run 自动锁定
- 红冲后才能对同一周期发起新的 NORMAL run

### AC5：工资条查询
- ESS：员工只能查看自己的工资条，按周期查询
- Admin：可查看所有员工工资条
- 返回：应发、社保、公积金、个税、实发

## 技术上下文

### 依赖的已有服务
- `py_compensation_master` - 薪资档案
- `py_iit_bracket` - 个税税率表
- `py_social_insurance_rate` - 社保费率
- `py_cumulative_tax_ledger` - 累计台账
- `HrEmployee` - 员工状态和入职日期

### 计算引擎设计
```
PayrollService.calculate(runId):
  1. 加载 run 和 period
  2. 查询符合条件的员工列表
  3. 遍历员工：
     - 加载薪资档案
     - 计算应发/社保/公积金/个税/实发
     - 创建明细
     - 更新累计台账
  4. 更新 run 状态为 CALCULATED
  5. 返回异常清单（实发<0 的员工）
```

### 注意事项
- 全部金额用 BigDecimal，setScale(4, HALF_UP)
- 红冲明细金额为负数
- 累计台账在每年 1 月重置（或初始化时处理）
