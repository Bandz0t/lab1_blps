package com.example.youtubemonetization.config;

import com.atomikos.jdbc.AtomikosDataSourceBean;
import java.util.Properties;
import javax.sql.DataSource;
import org.postgresql.xa.PGXADataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
@ConditionalOnProperty(name = "spring.jta.enabled", havingValue = "true", matchIfMissing = true)
public class JtaAtomikosConfig {

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
}
