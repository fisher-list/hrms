package com.hrms.common.integration;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Minimal Spring Boot application for integration tests in hrms-common.
 * <p>
 * Mirrors {@code com.hrms.app.HrmsApplication} but lives in the test source
 * of hrms-common so integration tests can bootstrap a full context without
 * depending on the hrms-app module.
 */
@SpringBootApplication(scanBasePackages = "com.hrms")
@MapperScan("com.hrms.common.**.mapper")
public class TestHrmsApplication {

    static {
        // AesUtil reads HRMS_AES_KEY from env or system property.
        // Surefire argLine already sets it; this is a safety net for IDE runs.
        if (System.getProperty("HRMS_AES_KEY") == null
                && System.getenv("HRMS_AES_KEY") == null) {
            System.setProperty("HRMS_AES_KEY", "12345678901234567890123456789012");
        }
    }

    public static void main(String[] args) {
        SpringApplication.run(TestHrmsApplication.class, args);
    }
}
