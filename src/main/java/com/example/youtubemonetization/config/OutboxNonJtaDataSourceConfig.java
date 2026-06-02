package com.example.youtubemonetization.config;

import jakarta.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@ConditionalOnProperty(name = "spring.jta.enabled", havingValue = "false")
public class OutboxNonJtaDataSourceConfig {

    @Bean
    @Primary
    public DataSource dataSource(
            @Value("${spring.datasource.url}") String jdbcUrl,
            @Value("${spring.datasource.username}") String username,
            @Value("${spring.datasource.password:}") String password,
            @Value("${spring.datasource.driver-class-name}") String driverClassName
    ) {
        return DataSourceBuilder.create()
                .url(jdbcUrl)
                .username(username)
                .password(password)
                .driverClassName(driverClassName)
                .build();
    }

    @Bean(name = "outboxDataSource")
    public DataSource outboxDataSource(
            @Value("${app.outbox.datasource.url}") String jdbcUrl,
            @Value("${app.outbox.datasource.username}") String username,
            @Value("${app.outbox.datasource.password:}") String password,
            @Value("${app.outbox.datasource.driver-class-name}") String driverClassName
    ) {
        return DataSourceBuilder.create()
                .url(jdbcUrl)
                .username(username)
                .password(password)
                .driverClassName(driverClassName)
                .build();
    }

    @Bean(name = "outboxJdbcTemplate")
    public JdbcTemplate outboxJdbcTemplate(@Qualifier("outboxDataSource") DataSource outboxDataSource) {
        return new JdbcTemplate(outboxDataSource);
    }

    @Bean(name = "outboxTransactionManager")
    public PlatformTransactionManager outboxTransactionManager(@Qualifier("outboxDataSource") DataSource outboxDataSource) {
        return new org.springframework.jdbc.datasource.DataSourceTransactionManager(outboxDataSource);
    }

    @Bean(name = "jdbcTemplate")
    @Primary
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    @Primary
    public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}
