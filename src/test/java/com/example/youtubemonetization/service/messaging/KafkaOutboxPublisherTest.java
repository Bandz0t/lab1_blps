package com.example.youtubemonetization.service.messaging;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.youtubemonetization.entity.OutboxEvent;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.apache.kafka.common.KafkaException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

@ExtendWith(MockitoExtension.class)
class KafkaOutboxPublisherTest {

    @Mock
    private OutboxEventService outboxEventService;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Test
    void shouldMarkEventSentAfterKafkaPublish() {
        OutboxEvent event = event();
        when(outboxEventService.getPendingEvents()).thenReturn(List.of(event));
        when(kafkaTemplate.send(event.getChannel(), "44", event.getPayload()))
                .thenReturn(CompletableFuture.completedFuture(null));

        new KafkaOutboxPublisher(outboxEventService, kafkaTemplate).publishPendingEvents();

        verify(kafkaTemplate).send("moderation.decision", "44", "{\"ok\":true}");
        verify(outboxEventService).markSent(1L);
    }

    @Test
    void shouldMarkEventFailedWhenKafkaPublishFails() {
        OutboxEvent event = event();
        CompletableFuture future = new CompletableFuture();
        future.completeExceptionally(new KafkaException("kafka down"));
        when(outboxEventService.getPendingEvents()).thenReturn(List.of(event));
        when(kafkaTemplate.send(event.getChannel(), "44", event.getPayload())).thenReturn(future);

        new KafkaOutboxPublisher(outboxEventService, kafkaTemplate).publishPendingEvents();

        verify(outboxEventService).markFailed(1L, "org.apache.kafka.common.KafkaException: kafka down");
    }

    private OutboxEvent event() {
        OutboxEvent event = new OutboxEvent();
        event.setId(1L);
        event.setAggregateId(44L);
        event.setChannel("moderation.decision");
        event.setPayload("{\"ok\":true}");
        return event;
    }
}
