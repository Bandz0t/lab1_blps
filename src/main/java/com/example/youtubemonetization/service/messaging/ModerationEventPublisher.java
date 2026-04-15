package com.example.youtubemonetization.service.messaging;

import com.example.youtubemonetization.entity.OutboxEvent;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.redis.enabled", havingValue = "true", matchIfMissing = true)
public class ModerationEventPublisher {

    private final OutboxEventService outboxEventService;
    private final StringRedisTemplate stringRedisTemplate;

    @Scheduled(fixedDelayString = "${app.messaging.outbox.poll-delay-ms:1000}")
    public void publishPendingEvents() {
        List<OutboxEvent> pendingEvents = outboxEventService.getPendingEvents();
        for (OutboxEvent outboxEvent : pendingEvents) {
            try {
                stringRedisTemplate.convertAndSend(outboxEvent.getChannel(), outboxEvent.getPayload());
                outboxEventService.markSent(outboxEvent.getId());
            } catch (RuntimeException exception) {
                log.warn(
                        "Не удалось отправить outbox событие id={} в Redis канал {}: {}",
                        outboxEvent.getId(),
                        outboxEvent.getChannel(),
                        exception.getMessage());
                outboxEventService.markFailed(outboxEvent.getId(), exception.getMessage());
            }
        }
    }
}
