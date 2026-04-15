package com.example.youtubemonetization.config.messaging;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.messaging.moderation")
public class ModerationMessagingProperties {

    private String channel;
}
