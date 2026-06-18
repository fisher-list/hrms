# EP03-S09: 薪资档案 + 工资计算表结构

**Epic**: 03 薪酬基础
**Sprint**: 3
**模块**: hrms-common (payroll package)

## 背景
Epic 03 的基础设施层。创建薪资档案、工资周期、工资 run、工资明细、社保公积金费率、个税累进表等表结构和 CRUD。

## 涉及表

| 表名 | 说明 |
|---|---|
| `py_compensation_master` | 薪资档案：员工 ID、基本工资、岗位工资、绩效基数、补贴、生效日 |
| `py_payroll_period` | 工资周期：YYYY-MM 状态(DRAFT/OPEN/LOCKED) |
| `py_payroll_run` | 工资 run：周期ID、类型(NORMAL/REVERSAL)、状态(DRAFT/CALCULATED/LOCKED)、关联原run（红冲用） |
| `py_payroll_detail` | 工资明细：runID、员工ID、应发、社保扣款、公积金扣款、个税、实发 |
| `py_iit_bracket` | 个税累进税率表：上限、税率、速算扣除数（7档） |
| `py_social_insurance_rate` | 社保公积金费率：养老保险/医疗保险/失业保险/住房公积金的个人和企业比例 |
| `py_cumulative_tax_ledger` | 累计预扣预缴台账：员工ID、年度、累计应发、累计社保、累计公积金、累计已扣个税 |

## 涉及接口

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| POST | `/api/payroll/compensations` | `py:compensation:edit` | 创建/更新薪资档案 |
| GET | `/api/payroll/compensations` | `py:compensation:list` | 查询薪资档案（按员工） |
| GET | `/api/payroll/compensations/{employeeId}` | `py:compensation:list` | 查询员工薪资档案 |
| POST | `/api/payroll/periods` | `py:period:edit` | 创建工资周期 |
| GET | `/api/payroll/periods` | `py:period:list` | 查询工资周期列表 |

## 验收标准

### AC1：薪资档案
- 每位员工一条薪资档案，字段：基本工资、岗位工资、绩效基数、补贴（均 BigDecimal(18,4)）
- 生效日期（effectiveDate），支持多次调整（新增记录，取最新生效日的）
- 查询时返回最新有效的档案

### AC2：工资周期
- 按月创建（如 2026-06），状态：DRAFT → OPEN → LOCKED
- 同一月份不可重复创建
- OPEN 状态才允许创建 run

### AC3：社保公积金费率
- 预置北京 2024 一档示例费率（硬编码在代码或种子数据中）：
  - 养老：个人 8%，企业 16%
  - 医疗：个人 2% + 3元，企业 9.8%
  - 失业：个人 0.5%，企业 0.5%
  - 公积金：个人 12%，企业 12%
- 计算基数 = 应发工资（封顶基数按演示口径不设上限）

### AC4：个税累进税率表
- 预置 7 档累进税率（2024 年标准）：
  - ≤36000: 3%, 0
  - ≤144000: 10%, 2520
  - ≤300000: 20%, 16920
  - ≤420000: 25%, 31920
  - ≤660000: 30%, 52920
  - ≤960000: 35%, 85920
  - >960000: 45%, 181920

### AC5：累计预扣预缴台账
- 初始化：每位在职员工每年一条台账
- 字段：累计应发、累计社保、累计公积金、累计已扣个税
- 工资 run 后更新

## 技术上下文

### Liquibase changelog
- 文件：`changelog-08-payroll-master.yaml`
- 7 表 + 索引 + 种子数据（IIT 税率表、社保费率、权限）

### 注意事项
- 金额统一 BigDecimal(18,4)
- 日期统一 LocalDate/LocalDateTime
- 红冲在 S10 实现
