-- ============================================================
-- Integration test schema – mirrors Liquibase changelogs
-- Uses H2 in PostgreSQL mode
-- ============================================================

-- sys_user
CREATE TABLE IF NOT EXISTS sys_user (
    id              BIGINT       NOT NULL PRIMARY KEY,
    created_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP,
    created_by      BIGINT,
    updated_by      BIGINT,
    deleted         BOOLEAN      NOT NULL DEFAULT FALSE,
    version         INT          NOT NULL DEFAULT 0,
    tenant_id       BIGINT       NOT NULL DEFAULT 1,
    username        VARCHAR(64)  NOT NULL,
    password_hash   VARCHAR(128) NOT NULL,
    nickname        VARCHAR(64),
    email           VARCHAR(128),
    phone           VARCHAR(32),
    employee_id     BIGINT,
    status          VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
    last_login_at   TIMESTAMP,
    last_login_ip   VARCHAR(64),
    failed_attempts INT          NOT NULL DEFAULT 0,
    locked_until    TIMESTAMP,
    password_changed_at TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_user_username ON sys_user(username);

-- sys_user_token
CREATE TABLE IF NOT EXISTS sys_user_token (
    id               BIGINT        NOT NULL PRIMARY KEY,
    created_at       TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP,
    created_by       BIGINT,
    updated_by       BIGINT,
    deleted          BOOLEAN       NOT NULL DEFAULT FALSE,
    version          INT           NOT NULL DEFAULT 0,
    tenant_id        BIGINT        NOT NULL DEFAULT 1,
    user_id          BIGINT        NOT NULL,
    refresh_jti      VARCHAR(64)   NOT NULL,
    refresh_token_hash VARCHAR(512),
    device           VARCHAR(256),
    ip               VARCHAR(64),
    issued_at        TIMESTAMP     NOT NULL,
    expires_at       TIMESTAMP     NOT NULL,
    revoked          BOOLEAN       NOT NULL DEFAULT FALSE,
    revoked_at       TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_token_refresh_jti ON sys_user_token(refresh_jti);

-- sys_role
CREATE TABLE IF NOT EXISTS sys_role (
    id          BIGINT      NOT NULL PRIMARY KEY,
    created_at  TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP,
    created_by  BIGINT,
    updated_by  BIGINT,
    deleted     BOOLEAN     NOT NULL DEFAULT FALSE,
    version     INT         NOT NULL DEFAULT 0,
    tenant_id   BIGINT      NOT NULL DEFAULT 1,
    code        VARCHAR(64) NOT NULL,
    name        VARCHAR(64) NOT NULL,
    description VARCHAR(256),
    builtin     BOOLEAN     NOT NULL DEFAULT FALSE,
    enabled     BOOLEAN     NOT NULL DEFAULT TRUE,
    data_scope  VARCHAR(32) NOT NULL DEFAULT 'SELF_ONLY'
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_role_code ON sys_role(code);

-- sys_permission
CREATE TABLE IF NOT EXISTS sys_permission (
    id        BIGINT       NOT NULL PRIMARY KEY,
    created_at  TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP,
    created_by  BIGINT,
    updated_by  BIGINT,
    deleted   BOOLEAN      NOT NULL DEFAULT FALSE,
    version   INT          NOT NULL DEFAULT 0,
    tenant_id BIGINT       NOT NULL DEFAULT 1,
    code      VARCHAR(128) NOT NULL,
    name      VARCHAR(128) NOT NULL,
    type      VARCHAR(16)  NOT NULL,
    parent_id BIGINT,
    sort_no   INT          NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_permission_code ON sys_permission(code);

-- sys_user_role
CREATE TABLE IF NOT EXISTS sys_user_role (
    id        BIGINT  NOT NULL PRIMARY KEY,
    created_at  TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP,
    created_by  BIGINT,
    updated_by  BIGINT,
    deleted   BOOLEAN NOT NULL DEFAULT FALSE,
    version   INT     NOT NULL DEFAULT 0,
    tenant_id BIGINT  NOT NULL DEFAULT 1,
    user_id   BIGINT  NOT NULL,
    role_id   BIGINT  NOT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_user_role ON sys_user_role(user_id, role_id);

-- sys_role_permission
CREATE TABLE IF NOT EXISTS sys_role_permission (
    id            BIGINT  NOT NULL PRIMARY KEY,
    created_at  TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP,
    created_by  BIGINT,
    updated_by  BIGINT,
    deleted       BOOLEAN NOT NULL DEFAULT FALSE,
    version       INT     NOT NULL DEFAULT 0,
    tenant_id     BIGINT  NOT NULL DEFAULT 1,
    role_id       BIGINT  NOT NULL,
    permission_id BIGINT  NOT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_role_permission ON sys_role_permission(role_id, permission_id);

-- company
CREATE TABLE IF NOT EXISTS company (
    id                   BIGINT       NOT NULL PRIMARY KEY,
    created_at           TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP,
    created_by           BIGINT,
    updated_by           BIGINT,
    deleted              BOOLEAN      NOT NULL DEFAULT FALSE,
    version              INT          NOT NULL DEFAULT 0,
    tenant_id            BIGINT       NOT NULL DEFAULT 1,
    code                 VARCHAR(64)  NOT NULL,
    name                 VARCHAR(128) NOT NULL,
    legal_representative VARCHAR(64),
    address              VARCHAR(256),
    phone                VARCHAR(32),
    email                VARCHAR(128)
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_company_code ON company(code);

-- department
CREATE TABLE IF NOT EXISTS department (
    id         BIGINT       NOT NULL PRIMARY KEY,
    created_at TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted    BOOLEAN      NOT NULL DEFAULT FALSE,
    version    INT          NOT NULL DEFAULT 0,
    tenant_id  BIGINT       NOT NULL DEFAULT 1,
    company_id BIGINT       NOT NULL,
    parent_id  BIGINT,
    name       VARCHAR(128) NOT NULL,
    code       VARCHAR(64)  NOT NULL,
    path       VARCHAR(512),
    level      INT,
    head_id    BIGINT,
    sort_no    INT          DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_dept_company ON department(company_id);
CREATE INDEX IF NOT EXISTS idx_dept_parent  ON department(parent_id);

-- job
CREATE TABLE IF NOT EXISTS job (
    id         BIGINT        NOT NULL PRIMARY KEY,
    created_at TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted    BOOLEAN       NOT NULL DEFAULT FALSE,
    version    INT           NOT NULL DEFAULT 0,
    tenant_id  BIGINT        NOT NULL DEFAULT 1,
    code       VARCHAR(64)   NOT NULL,
    name       VARCHAR(128)  NOT NULL,
    sequence   VARCHAR(16)   NOT NULL,
    grade      INT           NOT NULL,
    min_salary DECIMAL(18,4),
    max_salary DECIMAL(18,4)
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_job_code ON job(code);

-- position
CREATE TABLE IF NOT EXISTS position (
    id         BIGINT       NOT NULL PRIMARY KEY,
    created_at TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted    BOOLEAN      NOT NULL DEFAULT FALSE,
    version    INT          NOT NULL DEFAULT 0,
    tenant_id  BIGINT       NOT NULL DEFAULT 1,
    dept_id    BIGINT       NOT NULL,
    job_id     BIGINT       NOT NULL,
    name       VARCHAR(128) NOT NULL,
    code       VARCHAR(64),
    headcount  INT          NOT NULL DEFAULT 1,
    occupied   INT          NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_pos_dept ON position(dept_id);
CREATE INDEX IF NOT EXISTS idx_pos_job  ON position(job_id);

-- hr_employee
CREATE TABLE IF NOT EXISTS hr_employee (
    id                  BIGINT       NOT NULL PRIMARY KEY,
    created_at          TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP,
    created_by          BIGINT,
    updated_by          BIGINT,
    deleted             BOOLEAN      NOT NULL DEFAULT FALSE,
    version             INT          NOT NULL DEFAULT 0,
    tenant_id           BIGINT       NOT NULL DEFAULT 1,
    emp_no              VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    name                VARCHAR(50)  NOT NULL,
    gender              CHAR(1),
    birth_date          DATE,
    id_card_enc         TEXT,
    id_card_hash        VARCHAR(64),
    phone_enc           TEXT,
    email               VARCHAR(100),
    dept_id             BIGINT,
    position_id         BIGINT,
    hire_date           DATE,
    status              VARCHAR(20)  NOT NULL DEFAULT 'PENDING_HIRE',
    contract_start      DATE,
    contract_end        DATE,
    probation_end       DATE,
    emergency_contact   VARCHAR(50),
    emergency_phone_enc TEXT
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_employee_emp_no ON hr_employee(emp_no);
CREATE INDEX IF NOT EXISTS idx_employee_dept    ON hr_employee(dept_id);
CREATE INDEX IF NOT EXISTS idx_employee_position ON hr_employee(position_id);
CREATE INDEX IF NOT EXISTS idx_employee_status  ON hr_employee(status);

-- hr_employee_education
CREATE TABLE IF NOT EXISTS hr_employee_education (
    id         BIGINT      NOT NULL PRIMARY KEY,
    created_at TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted    BOOLEAN     NOT NULL DEFAULT FALSE,
    version    INT         NOT NULL DEFAULT 0,
    tenant_id  BIGINT      NOT NULL DEFAULT 1,
    employee_id BIGINT     NOT NULL,
    school     VARCHAR(100),
    degree     VARCHAR(20),
    major      VARCHAR(100),
    start_date DATE,
    end_date   DATE,
    is_highest BOOLEAN     DEFAULT FALSE
);

-- hr_employee_work_exp
CREATE TABLE IF NOT EXISTS hr_employee_work_exp (
    id                BIGINT       NOT NULL PRIMARY KEY,
    created_at        TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP,
    created_by        BIGINT,
    updated_by        BIGINT,
    deleted           BOOLEAN      NOT NULL DEFAULT FALSE,
    version           INT          NOT NULL DEFAULT 0,
    tenant_id         BIGINT       NOT NULL DEFAULT 1,
    employee_id       BIGINT       NOT NULL,
    company_name      VARCHAR(100),
    position          VARCHAR(100),
    start_date        DATE,
    end_date          DATE,
    reason_for_leaving VARCHAR(200)
);

-- hr_employee_family
CREATE TABLE IF NOT EXISTS hr_employee_family (
    id            BIGINT      NOT NULL PRIMARY KEY,
    created_at    TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP,
    created_by    BIGINT,
    updated_by    BIGINT,
    deleted       BOOLEAN     NOT NULL DEFAULT FALSE,
    version       INT         NOT NULL DEFAULT 0,
    tenant_id     BIGINT      NOT NULL DEFAULT 1,
    employee_id   BIGINT      NOT NULL,
    name          VARCHAR(50),
    relationship  VARCHAR(20),
    phone_enc     TEXT,
    is_emergency  BOOLEAN     DEFAULT FALSE
);

-- hr_employee_contract
CREATE TABLE IF NOT EXISTS hr_employee_contract (
    id           BIGINT      NOT NULL PRIMARY KEY,
    created_at   TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP,
    created_by   BIGINT,
    updated_by   BIGINT,
    deleted      BOOLEAN     NOT NULL DEFAULT FALSE,
    version      INT         NOT NULL DEFAULT 0,
    tenant_id    BIGINT      NOT NULL DEFAULT 1,
    employee_id  BIGINT      NOT NULL,
    contract_no  VARCHAR(50),
    contract_type VARCHAR(20),
    start_date   DATE,
    end_date     DATE,
    signing_date DATE,
    status       VARCHAR(20) DEFAULT 'ACTIVE'
);

-- hr_employee_bank_account
CREATE TABLE IF NOT EXISTS hr_employee_bank_account (
    id             BIGINT      NOT NULL PRIMARY KEY,
    created_at     TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP,
    created_by     BIGINT,
    updated_by     BIGINT,
    deleted        BOOLEAN     NOT NULL DEFAULT FALSE,
    version        INT         NOT NULL DEFAULT 0,
    tenant_id      BIGINT      NOT NULL DEFAULT 1,
    employee_id    BIGINT      NOT NULL,
    bank_name      VARCHAR(100),
    account_no_enc TEXT,
    account_name   VARCHAR(50),
    is_primary     BOOLEAN     DEFAULT FALSE
);

-- hr_employee_address
CREATE TABLE IF NOT EXISTS hr_employee_address (
    id          BIGINT      NOT NULL PRIMARY KEY,
    created_at  TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP,
    created_by  BIGINT,
    updated_by  BIGINT,
    deleted     BOOLEAN     NOT NULL DEFAULT FALSE,
    version     INT         NOT NULL DEFAULT 0,
    tenant_id   BIGINT      NOT NULL DEFAULT 1,
    employee_id BIGINT      NOT NULL,
    type        VARCHAR(20) NOT NULL,
    province    VARCHAR(50),
    city        VARCHAR(50),
    district    VARCHAR(50),
    detail      VARCHAR(255)
);

-- py_payroll_period (薪酬期间)
CREATE TABLE IF NOT EXISTS py_payroll_period (
    id              BIGINT       NOT NULL PRIMARY KEY,
    created_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP,
    created_by      BIGINT,
    updated_by      BIGINT,
    deleted         BOOLEAN      NOT NULL DEFAULT FALSE,
    version         INT          NOT NULL DEFAULT 0,
    tenant_id       BIGINT       NOT NULL DEFAULT 1,
    period_month    VARCHAR(7)   NOT NULL,
    status          VARCHAR(10)  DEFAULT 'DRAFT'
);

-- py_payroll_run (薪酬运行)
CREATE TABLE IF NOT EXISTS py_payroll_run (
    id                BIGINT       NOT NULL PRIMARY KEY,
    created_at        TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP,
    created_by        BIGINT,
    updated_by        BIGINT,
    deleted           BOOLEAN      NOT NULL DEFAULT FALSE,
    version           INT          NOT NULL DEFAULT 0,
    tenant_id         BIGINT       NOT NULL DEFAULT 1,
    period_id         BIGINT       NOT NULL,
    run_type          VARCHAR(10)  DEFAULT 'NORMAL',
    status            VARCHAR(15)  DEFAULT 'DRAFT',
    reverse_of_run_id BIGINT,
    employee_count    INT          DEFAULT 0,
    total_gross       DECIMAL(18,4) DEFAULT 0,
    total_net         DECIMAL(18,4) DEFAULT 0
);

-- py_payroll_detail (薪酬明细)
CREATE TABLE IF NOT EXISTS py_payroll_detail (
    id                BIGINT       NOT NULL PRIMARY KEY,
    created_at        TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP,
    created_by        BIGINT,
    updated_by        BIGINT,
    deleted           BOOLEAN      NOT NULL DEFAULT FALSE,
    version           INT          NOT NULL DEFAULT 0,
    tenant_id         BIGINT       NOT NULL DEFAULT 1,
    run_id            BIGINT       NOT NULL,
    employee_id       BIGINT       NOT NULL,
    employee_name     VARCHAR(50),
    emp_no            VARCHAR(20),
    gross_pay         DECIMAL(18,4),
    social_insurance  DECIMAL(18,4),
    housing_fund      DECIMAL(18,4),
    iit               DECIMAL(18,4),
    net_pay           DECIMAL(18,4),
    is_exception      BOOLEAN      DEFAULT FALSE
);

-- py_compensation_master (薪酬档案)
CREATE TABLE IF NOT EXISTS py_compensation_master (
    id                BIGINT       NOT NULL PRIMARY KEY,
    created_at        TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP,
    created_by        BIGINT,
    updated_by        BIGINT,
    deleted           BOOLEAN      NOT NULL DEFAULT FALSE,
    version           INT          NOT NULL DEFAULT 0,
    tenant_id         BIGINT       NOT NULL DEFAULT 1,
    employee_id       BIGINT       NOT NULL,
    base_salary       DECIMAL(18,4),
    position_salary   DECIMAL(18,4),
    performance_base  DECIMAL(18,4),
    allowance         DECIMAL(18,4),
    effective_date    DATE         NOT NULL
);

-- py_iit_bracket (个税税率表)
CREATE TABLE IF NOT EXISTS py_iit_bracket (
    id               BIGINT        NOT NULL PRIMARY KEY,
    lower_limit      DECIMAL(18,2),
    upper_limit      DECIMAL(18,2),
    rate             DECIMAL(5,4),
    quick_deduction  DECIMAL(18,2)
);

-- py_social_insurance_rate (社保公积金比例)
CREATE TABLE IF NOT EXISTS py_social_insurance_rate (
    id                       BIGINT        NOT NULL PRIMARY KEY,
    pension_personal         DECIMAL(5,4)  DEFAULT 0.08,
    pension_company          DECIMAL(5,4)  DEFAULT 0.16,
    medical_personal         DECIMAL(5,4)  DEFAULT 0.02,
    medical_fixed_fee        DECIMAL(18,2) DEFAULT 3.00,
    medical_company          DECIMAL(5,4)  DEFAULT 0.098,
    unemployment_personal    DECIMAL(5,4)  DEFAULT 0.005,
    unemployment_company     DECIMAL(5,4)  DEFAULT 0.005,
    housing_fund_personal    DECIMAL(5,4)  DEFAULT 0.12,
    housing_fund_company     DECIMAL(5,4)  DEFAULT 0.12
);

-- py_cumulative_tax_ledger (累计税台账)
CREATE TABLE IF NOT EXISTS py_cumulative_tax_ledger (
    id                    BIGINT       NOT NULL PRIMARY KEY,
    created_at            TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP,
    created_by            BIGINT,
    updated_by            BIGINT,
    deleted               BOOLEAN      NOT NULL DEFAULT FALSE,
    version               INT          NOT NULL DEFAULT 0,
    tenant_id             BIGINT       NOT NULL DEFAULT 1,
    employee_id           BIGINT       NOT NULL,
    tax_year              INT          NOT NULL,
    cumulative_gross      DECIMAL(18,4) DEFAULT 0,
    cumulative_social     DECIMAL(18,4) DEFAULT 0,
    cumulative_housing_fund DECIMAL(18,4) DEFAULT 0,
    cumulative_iit        DECIMAL(18,4) DEFAULT 0
);

-- py_year_end_bonus (年终奖)
CREATE TABLE IF NOT EXISTS py_year_end_bonus (
    id                      BIGINT       NOT NULL PRIMARY KEY,
    created_at              TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP,
    created_by              BIGINT,
    updated_by              BIGINT,
    deleted                 BOOLEAN      NOT NULL DEFAULT FALSE,
    version                 INT          NOT NULL DEFAULT 0,
    tenant_id               BIGINT       NOT NULL DEFAULT 1,
    employee_id             BIGINT       NOT NULL,
    employee_name           VARCHAR(50),
    emp_no                  VARCHAR(20),
    bonus_year              INT          NOT NULL,
    base_salary             DECIMAL(18,4),
    bonus_months            DECIMAL(5,2) DEFAULT 1,
    attendance_coefficient  DECIMAL(5,4) DEFAULT 1,
    bonus_amount            DECIMAL(18,4),
    tax_method              VARCHAR(20),
    tax_amount              DECIMAL(18,4),
    net_amount              DECIMAL(18,4),
    status                  VARCHAR(15)  DEFAULT 'DRAFT'
);

-- py_bank_payment_batch (银行代发批次)
CREATE TABLE IF NOT EXISTS py_bank_payment_batch (
    id                   BIGINT       NOT NULL PRIMARY KEY,
    created_at           TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP,
    created_by           BIGINT,
    updated_by           BIGINT,
    deleted              BOOLEAN      NOT NULL DEFAULT FALSE,
    version              INT          NOT NULL DEFAULT 0,
    tenant_id            BIGINT       NOT NULL DEFAULT 1,
    run_id               BIGINT       NOT NULL,
    batch_no             VARCHAR(30)  NOT NULL,
    bank_type            VARCHAR(20)  NOT NULL,
    payer_account_name   VARCHAR(100),
    payer_account_no     VARCHAR(50),
    payer_bank_name      VARCHAR(100),
    employee_count       INT          DEFAULT 0,
    total_amount         DECIMAL(18,4) DEFAULT 0,
    status               VARCHAR(15)  DEFAULT 'GENERATED',
    file_path            VARCHAR(500),
    remark               VARCHAR(500)
);

-- py_bank_payment_detail (银行代发明细)
CREATE TABLE IF NOT EXISTS py_bank_payment_detail (
    id              BIGINT       NOT NULL PRIMARY KEY,
    created_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP,
    created_by      BIGINT,
    updated_by      BIGINT,
    deleted         BOOLEAN      NOT NULL DEFAULT FALSE,
    version         INT          NOT NULL DEFAULT 0,
    tenant_id       BIGINT       NOT NULL DEFAULT 1,
    batch_id        BIGINT       NOT NULL,
    employee_id     BIGINT       NOT NULL,
    employee_name   VARCHAR(50),
    emp_no          VARCHAR(20),
    bank_name       VARCHAR(100),
    account_no      VARCHAR(50),
    account_name    VARCHAR(50),
    amount          DECIMAL(18,4),
    purpose         VARCHAR(100)
);

-- py_payroll_simulation (工资模拟运行)
CREATE TABLE IF NOT EXISTS py_payroll_simulation (
    id                               BIGINT       NOT NULL PRIMARY KEY,
    created_at                       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at                       TIMESTAMP,
    created_by                       BIGINT,
    updated_by                       BIGINT,
    deleted                          BOOLEAN      NOT NULL DEFAULT FALSE,
    version                          INT          NOT NULL DEFAULT 0,
    tenant_id                        BIGINT       NOT NULL DEFAULT 1,
    period_id                        BIGINT       NOT NULL,
    name                             VARCHAR(100) NOT NULL,
    status                           VARCHAR(15)  DEFAULT 'DRAFT',
    social_insurance_rate_override   VARCHAR(200),
    housing_fund_rate_override       VARCHAR(200),
    employee_count                   INT          DEFAULT 0,
    total_gross                      DECIMAL(18,4) DEFAULT 0,
    total_net                        DECIMAL(18,4) DEFAULT 0,
    compared                         BOOLEAN      DEFAULT FALSE
);

-- py_simulation_detail (工资模拟明细)
CREATE TABLE IF NOT EXISTS py_simulation_detail (
    id                BIGINT       NOT NULL PRIMARY KEY,
    created_at        TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP,
    created_by        BIGINT,
    updated_by        BIGINT,
    deleted           BOOLEAN      NOT NULL DEFAULT FALSE,
    version           INT          NOT NULL DEFAULT 0,
    tenant_id         BIGINT       NOT NULL DEFAULT 1,
    simulation_id     BIGINT       NOT NULL,
    employee_id       BIGINT       NOT NULL,
    employee_name     VARCHAR(50),
    emp_no            VARCHAR(20),
    gross_pay         DECIMAL(18,4),
    social_insurance  DECIMAL(18,4),
    housing_fund      DECIMAL(18,4),
    iit               DECIMAL(18,4),
    net_pay           DECIMAL(18,4),
    diff_net_pay      DECIMAL(18,4)
);

-- sys_audit_log
CREATE TABLE IF NOT EXISTS sys_audit_log (
    id            BIGINT       NOT NULL PRIMARY KEY,
    user_id       BIGINT,
    username      VARCHAR(64),
    action        VARCHAR(128),
    resource_type VARCHAR(128),
    resource_id   VARCHAR(64),
    ip_address    VARCHAR(64),
    user_agent    VARCHAR(512),
    request_method VARCHAR(16),
    request_uri   VARCHAR(512),
    status_code   INT,
    created_at    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- 第二批增强：多地区社保 + 缺勤配额自动 + 职位一键发布
-- ============================================================

-- hr_employee 新增字段（多地区社保）
ALTER TABLE hr_employee ADD COLUMN IF NOT EXISTS insurance_city VARCHAR(50);

-- py_region_social_rate（多地区社保公积金比例）
CREATE TABLE IF NOT EXISTS py_region_social_rate (
    id                       BIGINT        NOT NULL PRIMARY KEY,
    city                     VARCHAR(50)   NOT NULL,
    pension_personal         DECIMAL(5,4)  DEFAULT 0.08,
    pension_company          DECIMAL(5,4)  DEFAULT 0.16,
    medical_personal         DECIMAL(5,4)  DEFAULT 0.02,
    medical_fixed_fee        DECIMAL(18,2) DEFAULT 3.00,
    medical_company          DECIMAL(5,4)  DEFAULT 0.098,
    unemployment_personal    DECIMAL(5,4)  DEFAULT 0.005,
    unemployment_company     DECIMAL(5,4)  DEFAULT 0.005,
    injury_company           DECIMAL(5,4)  DEFAULT 0.004,
    maternity_company        DECIMAL(5,4)  DEFAULT 0.008,
    housing_fund_personal    DECIMAL(5,4)  DEFAULT 0.12,
    housing_fund_company     DECIMAL(5,4)  DEFAULT 0.12,
    social_base_floor        DECIMAL(18,2),
    social_base_ceil         DECIMAL(18,2),
    fund_base_floor          DECIMAL(18,2),
    fund_base_ceil           DECIMAL(18,2),
    enabled                  BOOLEAN       NOT NULL DEFAULT TRUE
);
CREATE INDEX IF NOT EXISTS idx_region_rate_city ON py_region_social_rate(city);

-- at_leave_quota_rule（缺勤配额规则）
CREATE TABLE IF NOT EXISTS at_leave_quota_rule (
    id              BIGINT       NOT NULL PRIMARY KEY,
    created_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP,
    created_by      BIGINT,
    updated_by      BIGINT,
    deleted         BOOLEAN      NOT NULL DEFAULT FALSE,
    version         INT          NOT NULL DEFAULT 0,
    tenant_id       BIGINT       NOT NULL DEFAULT 1,
    name            VARCHAR(100) NOT NULL,
    code            VARCHAR(50)  NOT NULL,
    leave_type_id   BIGINT       NOT NULL,
    seniority_min   INT          NOT NULL DEFAULT 0,
    seniority_max   INT,
    grade_min       INT,
    grade_max       INT,
    quota_days      DECIMAL(5,1) NOT NULL,
    enabled         BOOLEAN      NOT NULL DEFAULT TRUE,
    sort_no         INT          NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_quota_rule_code ON at_leave_quota_rule(code);

-- rc_requisition_publish（职位发布记录）
CREATE TABLE IF NOT EXISTS rc_requisition_publish (
    id              BIGINT       NOT NULL PRIMARY KEY,
    created_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP,
    created_by      BIGINT,
    updated_by      BIGINT,
    deleted         BOOLEAN      NOT NULL DEFAULT FALSE,
    version         INT          NOT NULL DEFAULT 0,
    tenant_id       BIGINT       NOT NULL DEFAULT 1,
    requisition_id  BIGINT       NOT NULL,
    channel         VARCHAR(30)  NOT NULL,
    publish_url     VARCHAR(500),
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    published_at    TIMESTAMP,
    closed_at       TIMESTAMP,
    fail_reason     VARCHAR(500),
    view_count      INT          DEFAULT 0,
    apply_count     INT          DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_publish_requisition ON rc_requisition_publish(requisition_id);
CREATE INDEX IF NOT EXISTS idx_publish_channel ON rc_requisition_publish(channel);
