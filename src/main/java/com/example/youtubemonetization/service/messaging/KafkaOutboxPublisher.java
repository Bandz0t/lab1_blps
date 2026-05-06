package com.example.youtubemonetization.service.messaging;

import com.example.youtubemonetization.entity.OutboxEvent;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class KafkaOutboxPublisher {

    private final OutboxEventService outboxEventService;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${app.kafka.producer.send-timeout-seconds:10}")
    private long sendTimeoutSeconds = 10;

    @Scheduled(fixedDelayString = "${app.messaging.outbox.poll-delay-ms:1000}")
    public void publishPendingEvents() {
        List<OutboxEvent> pendingEvents = outboxEventService.getPendingEvents();
        for (OutboxEvent outboxEvent : pendingEvents) {
            try {
                String key = outboxEvent.getAggregateId() == null ? String.valueOf(outboxEvent.getId()) : String.valueOf(outboxEvent.getAggregateId());
                kafkaTemplate.send(outboxEvent.getChannel(), key, outboxEvent.getPayload())
                        .get(sendTimeoutSeconds, TimeUnit.SECONDS);
                outboxEventService.markSent(outboxEvent.getId());
            } catch (Exception exception) {
                String message = exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
                log.warn(
                        "Failed to publish outbox event id={} to Kafka topic {}: {}",
                        outboxEvent.getId(),
                        outboxEvent.getChannel(),
                        message);
                outboxEventService.markFailed(outboxEvent.getId(), message);
            }
        }
    }

    Duration sendTimeout() {
        return Duration.ofSeconds(sendTimeoutSeconds);
    }
}
