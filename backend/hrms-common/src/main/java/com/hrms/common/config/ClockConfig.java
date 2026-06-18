package com.hrms.common.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Provides a default system {@link Clock} bean.  Tests can override it with a fixed
 * Clock to deterministically advance time across lockout / token-expiry assertions.
 */
@Configuration
public class ClockConfig {

    @Bean
    @ConditionalOnMissingBean
    public Clock systemClock() {
        return Clock.system(ZoneId.systemDefault());
    }
}
