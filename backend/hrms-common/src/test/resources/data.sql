-- ============================================================
-- Integration test seed data
-- ============================================================

-- Admin user (id=1, password=Admin@2026 BCrypt)
INSERT INTO sys_user (id, username, password_hash, nickname, status, failed_attempts, deleted, version, tenant_id, employee_id)
VALUES (1, 'admin', '$2b$10$85G64qURSz4UDwB8U7FZ/.mfHA.7yrgxUic/gUvEcD/XHf/d4ey9y', '系统管理员', 'ACTIVE', 0, false, 0, 1, 1);

-- Test user for rate-limit (id=2, password=Admin@2026 BCrypt)
INSERT INTO sys_user (id, username, password_hash, nickname, status, failed_attempts, deleted, version, tenant_id)
VALUES (2, 'ratelimituser', '$2b$10$85G64qURSz4UDwB8U7FZ/.mfHA.7yrgxUic/gUvEcD/XHf/d4ey9y', '限流测试', 'ACTIVE', 0, false, 0, 1);

-- Roles
INSERT INTO sys_role (id, code, name, description, builtin, enabled, data_scope, deleted, version, tenant_id)
VALUES (100, 'ADMIN', '系统管理员', 'Super administrator', true, true, 'ALL', false, 0, 1);
INSERT INTO sys_role (id, code, name, description, builtin, enabled, data_scope, deleted, version, tenant_id)
VALUES (101, 'HR_MANAGER', 'HR经理', 'HR manager', true, true, 'ALL', false, 0, 1);

-- Permissions
INSERT INTO sys_permission (id, code, name, type, sort_no, deleted, version, tenant_id)
VALUES (1000, 'hr:employee:list', 'View employees', 'API', 1, false, 0, 1);
INSERT INTO sys_permission (id, code, name, type, sort_no, deleted, version, tenant_id)
VALUES (1001, 'hr:employee:edit', 'Edit employees', 'API', 2, false, 0, 1);
INSERT INTO sys_permission (id, code, name, type, sort_no, deleted, version, tenant_id)
VALUES (1010, 'org:view', 'View org structure', 'API', 10, false, 0, 1);
INSERT INTO sys_permission (id, code, name, type, sort_no, deleted, version, tenant_id)
VALUES (1011, 'org:create', 'Create departments', 'API', 11, false, 0, 1);

-- User-role: admin -> ADMIN
INSERT INTO sys_user_role (id, user_id, role_id, deleted, version, tenant_id)
VALUES (10000, 1, 100, false, 0, 1);

-- Role-permission: ADMIN gets all (use wildcard '*')
INSERT INTO sys_permission (id, code, name, type, sort_no, deleted, version, tenant_id)
VALUES (9999, '*', 'Super admin wildcard', 'API', 999, false, 0, 1);
INSERT INTO sys_role_permission (id, role_id, permission_id, deleted, version, tenant_id)
VALUES (20000, 100, 9999, false, 0, 1);

-- Also grant explicit permissions for HR_MANAGER role
INSERT INTO sys_role_permission (id, role_id, permission_id, deleted, version, tenant_id)
VALUES (20001, 101, 1000, false, 0, 1);
INSERT INTO sys_role_permission (id, role_id, permission_id, deleted, version, tenant_id)
VALUES (20002, 101, 1001, false, 0, 1);
INSERT INTO sys_role_permission (id, role_id, permission_id, deleted, version, tenant_id)
VALUES (20003, 101, 1010, false, 0, 1);
INSERT INTO sys_role_permission (id, role_id, permission_id, deleted, version, tenant_id)
VALUES (20004, 101, 1011, false, 0, 1);

-- Company
INSERT INTO company (id, code, name, deleted, version, tenant_id)
VALUES (100, 'ZK_GROUP', '智科集团', false, 0, 1);

-- Departments
INSERT INTO department (id, company_id, name, code, path, level, sort_no, deleted, version, tenant_id)
VALUES (201, 100, '人力资源部', 'HR', '/201', 1, 1, false, 0, 1);
INSERT INTO department (id, company_id, name, code, path, level, sort_no, deleted, version, tenant_id)
VALUES (202, 100, '研发部', 'RD', '/202', 1, 2, false, 0, 1);
INSERT INTO department (id, company_id, name, code, path, level, sort_no, deleted, version, tenant_id)
VALUES (203, 100, '销售部', 'SALES', '/203', 1, 3, false, 0, 1);

-- Jobs
INSERT INTO job (id, code, name, sequence, grade, deleted, version, tenant_id)
VALUES (301, 'M1', '管理序列L1', 'M', 1, false, 0, 1);
INSERT INTO job (id, code, name, sequence, grade, deleted, version, tenant_id)
VALUES (302, 'P1', '专业序列P1', 'P', 1, false, 0, 1);
INSERT INTO job (id, code, name, sequence, grade, deleted, version, tenant_id)
VALUES (303, 'T1', '技术序列T1', 'T', 1, false, 0, 1);

-- Positions
INSERT INTO position (id, dept_id, job_id, name, code, headcount, occupied, deleted, version, tenant_id)
VALUES (401, 201, 302, 'HR专员', 'POS-HR-01', 1, 0, false, 0, 1);
INSERT INTO position (id, dept_id, job_id, name, code, headcount, occupied, deleted, version, tenant_id)
VALUES (402, 201, 301, '招聘经理', 'POS-HR-02', 1, 0, false, 0, 1);
INSERT INTO position (id, dept_id, job_id, name, code, headcount, occupied, deleted, version, tenant_id)
VALUES (403, 202, 303, 'Java开发', 'POS-RD-01', 1, 0, false, 0, 1);

-- Admin employee profile (linked to sys_user.employee_id=1)
INSERT INTO hr_employee (id, emp_no, name, gender, hire_date, status, dept_id, position_id, deleted, version, tenant_id)
VALUES (1, 'E000001', '系统管理员', 'M', CURRENT_DATE, 'ACTIVE', 201, 401, false, 0, 1);

-- Seed a test employee for list/detail tests
INSERT INTO hr_employee (id, emp_no, name, gender, hire_date, status, dept_id, position_id, email, deleted, version, tenant_id)
VALUES (1001, 'E001001', '张三', 'M', DATE '2024-01-15', 'ACTIVE', 202, 403, 'zhangsan@example.com', false, 0, 1);
INSERT INTO hr_employee (id, emp_no, name, gender, hire_date, status, dept_id, position_id, email, deleted, version, tenant_id)
VALUES (1002, 'E001002', '李四', 'F', DATE '2024-03-01', 'PROBATION', 201, 401, 'lisi@example.com', false, 0, 1);
