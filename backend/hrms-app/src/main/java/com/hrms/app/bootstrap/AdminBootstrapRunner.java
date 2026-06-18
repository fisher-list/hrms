package com.hrms.app.bootstrap;

import com.hrms.common.employee.entity.HrEmployee;
import com.hrms.common.employee.mapper.HrEmployeeMapper;
import com.hrms.common.user.SysUser;
import com.hrms.common.user.SysUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Replaces the placeholder password hash for the seeded {@code admin} user with a real
 * BCrypt(Admin@2026) value on first start, then is a no-op on subsequent starts.
 *
 * <p>Also seeds an employee profile for the admin user and binds
 * {@code sys_user.employee_id} to it, so that ESS endpoints (which require a
 * non-null employeeId) become accessible.</p>
 *
 * <p>This keeps the Liquibase changelog dbms-neutral and free of real password hashes
 * checked into VCS, while still satisfying Story T11 (seed an admin account).</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminBootstrapRunner implements ApplicationRunner {

    public static final String DEFAULT_USERNAME = "admin";
    public static final String DEFAULT_PASSWORD = "Admin@2026";
    public static final String PLACEHOLDER = "__BOOTSTRAP_PLACEHOLDER__";
    /** Re-use the same id convention as the seeded sys_user (id=1). */
    public static final Long ADMIN_EMPLOYEE_ID = 1L;

    private final SysUserService userService;
    private final PasswordEncoder passwordEncoder;
    private final HrEmployeeMapper employeeMapper;

    @Override
    @Transactional
    public void run(org.springframework.boot.ApplicationArguments args) {
        userService.findByUsername(DEFAULT_USERNAME).ifPresent(user -> {
            // 1) Initialise BCrypt password on first start.
            if (PLACEHOLDER.equals(user.getPasswordHash())) {
                String hash = passwordEncoder.encode(DEFAULT_PASSWORD);
                user.setPasswordHash(hash);
                user.setPasswordChangedAt(LocalDateTime.now());
                userService.update(user);
                log.info("admin user password initialised (BCrypt) — please change it on first login");
            }

            // 2) Seed admin's employee profile (idempotent).
            HrEmployee existing = employeeMapper.selectById(ADMIN_EMPLOYEE_ID);
            if (existing == null) {
                HrEmployee emp = new HrEmployee();
                emp.setId(ADMIN_EMPLOYEE_ID);
                emp.setEmpNo("E000000");
                emp.setName("系统管理员");
                emp.setGender("M");
                emp.setHireDate(LocalDate.now());
                emp.setStatus("ACTIVE");
                try {
                    employeeMapper.insert(emp);
                    log.info("admin employee profile seeded (id={})", ADMIN_EMPLOYEE_ID);
                } catch (Exception e) {
                    // demo data may already have a record with a different id but same emp_no; safe to ignore
                    log.warn("admin employee profile seed skipped: {}", e.getMessage());
                }
            }

            // 3) Bind sys_user.employee_id so JWT carries the employeeId claim.
            if (user.getEmployeeId() == null) {
                user.setEmployeeId(ADMIN_EMPLOYEE_ID);
                userService.update(user);
                log.info("admin sys_user.employee_id bound to {}", ADMIN_EMPLOYEE_ID);
            }
        });
    }
}
