package com.example.youtubemonetization.config;

import jakarta.annotation.PostConstruct;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "app.outbox.flyway.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxFlywayConfig {

    private final DataSource outboxDataSource;
    private final String locations;

    public OutboxFlywayConfig(
            @Qualifier("outboxDataSource") DataSource outboxDataSource,
            @Value("${app.outbox.flyway.locations:classpath:db/outbox}") String locations
    ) {
        this.outboxDataSource = outboxDataSource;
        this.locations = locations;
    }

    @PostConstruct
    public void migrateOutboxSchema() {
        Flyway.configure()
                .dataSource(outboxDataSource)
                .locations(locations)
                .baselineOnMigrate(true)
                .load()
                .migrate();
    }
}
