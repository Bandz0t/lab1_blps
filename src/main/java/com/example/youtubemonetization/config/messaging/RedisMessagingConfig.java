package com.example.youtubemonetization.config.messaging;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ModerationMessagingProperties.class)
public class RedisMessagingConfig {
}
