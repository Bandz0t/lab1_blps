package com.example.youtubemonetization.config;

import io.minio.MinioClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class StorageConfig {

    @Bean
    @ConditionalOnProperty(prefix = "storage", name = "minio-enabled", havingValue = "true")
    public MinioClient minioClient(StorageProperties properties) {
        return MinioClient.builder()
                .endpoint(properties.getMinio().getEndpoint())
                .credentials(properties.getMinio().getAccessKey(), properties.getMinio().getSecretKey())
                .build();
    }
}
