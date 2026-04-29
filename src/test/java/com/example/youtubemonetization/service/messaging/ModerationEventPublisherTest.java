package com.example.youtubemonetization.service.messaging;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.youtubemonetization.entity.OutboxEvent;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;

@ExtendWith(MockitoExtension.class)
class ModerationEventPublisherTest {

    @Mock
    private OutboxEventService outboxEventService;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Test
    void shouldMarkEventSentAfterRedisPublish() {
        OutboxEvent event = event();
        when(outboxEventService.getPendingEvents()).thenReturn(List.of(event));

        new ModerationEventPublisher(outboxEventService, stringRedisTemplate).publishPendingEvents();

        verify(stringRedisTemplate).convertAndSend("moderation.decision", "{\"ok\":true}");
        verify(outboxEventService).markSent(1L);
    }

    @Test
    void shouldMarkEventFailedWhenRedisPublishFails() {
        OutboxEvent event = event();
        when(outboxEventService.getPendingEvents()).thenReturn(List.of(event));
        when(stringRedisTemplate.convertAndSend(event.getChannel(), event.getPayload()))
                .thenThrow(new RedisConnectionFailureException("redis down"));

        new ModerationEventPublisher(outboxEventService, stringRedisTemplate).publishPendingEvents();

        verify(outboxEventService).markFailed(1L, "redis down");
    }

    private OutboxEvent event() {
        OutboxEvent event = new OutboxEvent();
        event.setId(1L);
        event.setChannel("moderation.decision");
        event.setPayload("{\"ok\":true}");
        return event;
    }
}
