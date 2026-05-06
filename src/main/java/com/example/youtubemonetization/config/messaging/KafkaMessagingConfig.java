package com.example.youtubemonetization.config.messaging;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties({KafkaTopicProperties.class, ModerationMessagingProperties.class})
public class KafkaMessagingConfig {

    private final KafkaTopicProperties topics;

    @Bean
    @ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true", matchIfMissing = true)
    public List<NewTopic> kafkaTopics() {
        return List.of(
                topic(topics.getVideoProcessingRequested()),
                topic(topics.getVideoProcessingCompleted()),
                topic(topics.getModerationDecision()),
                topic(topics.getMonthlyPayoutRequested()),
                topic(topics.getPayoutRegistrationRequested()),
                topic(topics.getPayoutRegistrationCompleted())
        );
    }

    private NewTopic topic(String name) {
        return TopicBuilder.name(name)
                .partitions(2)
                .replicas(1)
                .build();
    }
}
