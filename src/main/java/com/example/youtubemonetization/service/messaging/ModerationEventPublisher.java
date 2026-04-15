package com.example.youtubemonetization.service.messaging;

import com.example.youtubemonetization.config.messaging.ModerationMessagingProperties;
import com.example.youtubemonetization.dto.event.ModerationDecisionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.rabbitmq.enabled", havingValue = "true", matchIfMissing = true)
public class ModerationEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final ModerationMessagingProperties properties;

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
        rabbitTemplate.convertAndSend(properties.getExchange(), properties.getRoutingKey(), event);
        log.info(
                "Событие модерации отправлено в RabbitMQ: exchange={}, routingKey={}, videoId={}, decision={}",
                properties.getExchange(),
                properties.getRoutingKey(),
                event.getVideoId(),
                event.getDecision());
    }
}
