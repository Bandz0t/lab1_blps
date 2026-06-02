package com.example.youtubemonetization.config;

import com.atomikos.jdbc.AtomikosDataSourceBean;
import com.atomikos.icatch.jta.UserTransactionImp;
import com.atomikos.icatch.jta.UserTransactionManager;
import java.util.Properties;
import jakarta.transaction.UserTransaction;
import javax.sql.DataSource;
import org.postgresql.xa.PGXADataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.jta.JtaTransactionManager;

@Configuration
@EnableTransactionManagement
@ConditionalOnProperty(name = "spring.jta.enabled", havingValue = "true", matchIfMissing = true)
public class JtaAtomikosConfig {

    @Bean(initMethod = "init", destroyMethod = "close")
    public UserTransactionManager atomikosTransactionManager() {
        UserTransactionManager userTransactionManager = new UserTransactionManager();
        userTransactionManager.setForceShutdown(false);
        return userTransactionManager;
    }

    @Bean
    public UserTransaction atomikosUserTransaction() throws Throwable {
        UserTransactionImp userTransaction = new UserTransactionImp();
        userTransaction.setTransactionTimeout(300);
        return userTransaction;
    }

    @Bean
    @Primary
    public PlatformTransactionManager transactionManager(
            UserTransaction atomikosUserTransaction,
            UserTransactionManager atomikosTransactionManager
    ) {
        return new JtaTransactionManager(atomikosUserTransaction, atomikosTransactionManager);
    }

    @Bean(initMethod = "init", destroyMethod = "close")
    @Primary
    public DataSource dataSource(
            @Value("${spring.datasource.url}") String jdbcUrl,
            @Value("${spring.datasource.username}") String username,
            @Value("${spring.datasource.password}") String password
    ) {
        PGXADataSource xaDataSource = new PGXADataSource();
        xaDataSource.setUrl(jdbcUrl);
        xaDataSource.setUser(username);
        xaDataSource.setPassword(password);

        AtomikosDataSourceBean atomikos = new AtomikosDataSourceBean();
        atomikos.setUniqueResourceName("youtubeMonetizationXaDs");
        atomikos.setXaDataSource(xaDataSource);
        atomikos.setMinPoolSize(2);
        atomikos.setMaxPoolSize(10);
        Properties xaProps = new Properties();
        xaProps.setProperty("user", username);
        xaProps.setProperty("password", password);
        xaProps.setProperty("url", jdbcUrl);
        atomikos.setXaProperties(xaProps);
        return atomikos;
    }

    @Bean(name = "outboxDataSource", initMethod = "init", destroyMethod = "close")
    public DataSource outboxDataSource(
            @Value("${app.outbox.datasource.url}") String jdbcUrl,
            @Value("${app.outbox.datasource.username}") String username,
            @Value("${app.outbox.datasource.password}") String password,
            @Value("${app.outbox.datasource.unique-resource-name:youtubeMonetizationOutboxXaDs}") String uniqueResourceName,
            @Value("${app.outbox.datasource.min-pool-size:2}") int minPoolSize,
            @Value("${app.outbox.datasource.max-pool-size:10}") int maxPoolSize
    ) {
        PGXADataSource xaDataSource = new PGXADataSource();
        xaDataSource.setUrl(jdbcUrl);
        xaDataSource.setUser(username);
        xaDataSource.setPassword(password);

        AtomikosDataSourceBean atomikos = new AtomikosDataSourceBean();
        atomikos.setUniqueResourceName(uniqueResourceName);
        atomikos.setXaDataSource(xaDataSource);
        atomikos.setMinPoolSize(minPoolSize);
        atomikos.setMaxPoolSize(maxPoolSize);
        Properties xaProps = new Properties();
        xaProps.setProperty("user", username);
        xaProps.setProperty("password", password);
        xaProps.setProperty("url", jdbcUrl);
        atomikos.setXaProperties(xaProps);
        return atomikos;
    }

    @Bean(name = "outboxJdbcTemplate")
    public JdbcTemplate outboxJdbcTemplate(@Qualifier("outboxDataSource") DataSource outboxDataSource) {
        return new JdbcTemplate(outboxDataSource);
    }

    @Bean(name = "outboxTransactionManager")
    public PlatformTransactionManager outboxTransactionManager(@Qualifier("outboxDataSource") DataSource outboxDataSource) {
        return new DataSourceTransactionManager(outboxDataSource);
    }

    @Bean(name = "jdbcTemplate")
    @Primary
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
