package com.hrms.app;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * HRMS bootstrap entry point.  Component scanning extends to {@code com.hrms} so
 * everything in {@code hrms-common} (security, auth, user, config) is wired in.
 */
@SpringBootApplication(scanBasePackages = "com.hrms")
@MapperScan("com.hrms.common.**.mapper")
@EnableAsync
@EnableScheduling
public class HrmsApplication {

    public static void main(String[] args) {
        SpringApplication.run(HrmsApplication.class, args);
    }
}
