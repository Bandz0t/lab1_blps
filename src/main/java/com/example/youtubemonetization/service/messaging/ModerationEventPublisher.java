package com.example.youtubemonetization.service.messaging;

import com.example.youtubemonetization.config.messaging.ModerationMessagingProperties;
import com.example.youtubemonetization.dto.event.ModerationDecisionEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.redis.enabled", havingValue = "true", matchIfMissing = true)
public class ModerationEventPublisher {

    private final StringRedisTemplate stringRedisTemplate;
    private final ModerationMessagingProperties properties;
    private final ObjectMapper objectMapper;

    public void publishAfterCommit(ModerationDecisionEvent event) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send(event);
                }
            });
            return;
        }
        send(event);
    }

    private void send(ModerationDecisionEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            stringRedisTemplate.convertAndSend(properties.getChannel(), payload);
            log.info(
                    "Событие модерации отправлено в Redis: channel={}, videoId={}, decision={}",
                    properties.getChannel(),
                    event.getVideoId(),
                    event.getDecision());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Не удалось сериализовать событие модерации для Redis", exception);
        }
    }
}
