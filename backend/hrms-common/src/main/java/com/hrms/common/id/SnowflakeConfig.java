package com.hrms.common.id;

import cn.hutool.core.lang.Snowflake;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provides the application-wide Snowflake ID generator.
 *
 * <p>Worker / datacenter ids default to 1/1 for the single-instance MVP and can be
 * overridden via {@code hrms.snowflake.worker-id} / {@code hrms.snowflake.datacenter-id}
 * to support multi-instance deployments later.</p>
 */
@Configuration
public class SnowflakeConfig {

    @Bean
    public Snowflake snowflake(
            @Value("${hrms.snowflake.worker-id:1}") long workerId,
            @Value("${hrms.snowflake.datacenter-id:1}") long datacenterId) {
        return new Snowflake(workerId, datacenterId);
    }
}
