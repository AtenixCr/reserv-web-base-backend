package com.tourism.business.audit.config;

import javax.sql.DataSource;
import com.tourism.business.audit.service.AuditDataSource;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuditConfiguration {
    @Bean
    static BeanPostProcessor auditDataSourcePostProcessor() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String name) {
                if (name.equals("dataSource") && bean instanceof DataSource dataSource) return new AuditDataSource(dataSource);
                return bean;
            }
        };
    }
}
