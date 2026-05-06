package com.example.youtubemonetization.config.messaging;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.kafka.topics")
public class KafkaTopicProperties {

    private String videoProcessingRequested = "video.processing.requested";
    private String videoProcessingCompleted = "video.processing.completed";
    private String moderationDecision = "moderation.decision";
    private String monthlyPayoutRequested = "monthly.payout.requested";
    private String payoutRegistrationRequested = "payout.registration.requested";
    private String payoutRegistrationCompleted = "payout.registration.completed";
}
