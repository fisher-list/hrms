package com.hrms.common.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.hrms.common.rbac.datascope.DataScopeInterceptor;
import lombok.RequiredArgsConstructor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Enables MyBatis-Plus features for the common module's mappers and registers the
 * pagination interceptor.  {@code DbType} is intentionally not set: MyBatis-Plus auto
 * detects it from the active datasource (see architecture section 6.3).
 */
@Configuration
@RequiredArgsConstructor
@MapperScan({"com.hrms.common.user", "com.hrms.common.rbac.mapper", "com.hrms.common.approval.mapper", "com.hrms.common.employee.mapper", "com.hrms.common.org.mapper"})
public class MybatisPlusConfig {

    private final DataScopeInterceptor dataScopeInterceptor;

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // Data-scope interceptor must run before pagination
        interceptor.addInnerInterceptor(dataScopeInterceptor);
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor());
        return interceptor;
    }
}
