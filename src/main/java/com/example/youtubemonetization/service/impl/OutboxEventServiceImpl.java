package com.example.youtubemonetization.service.impl;

import com.example.youtubemonetization.config.messaging.ModerationMessagingProperties;
import com.example.youtubemonetization.dto.event.ModerationDecisionEvent;
import com.example.youtubemonetization.entity.OutboxEvent;
import com.example.youtubemonetization.enums.OutboxEventStatus;
import com.example.youtubemonetization.exception.EntityNotFoundException;
import com.example.youtubemonetization.repository.OutboxEventJdbcRepository;
import com.example.youtubemonetization.service.messaging.OutboxEventService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class OutboxEventServiceImpl implements OutboxEventService {

    private final OutboxEventJdbcRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final ModerationMessagingProperties messagingProperties;

    @Value("${app.outbox.lock.ttl-seconds:30}")
    private long lockTtlSeconds;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void enqueueModerationDecisionEvent(ModerationDecisionEvent event) {
        OutboxEvent outboxEvent = new OutboxEvent();
        outboxEvent.setEventType("MODERATION_DECISION");
        outboxEvent.setAggregateType("VIDEO");
        outboxEvent.setAggregateId(event.getVideoId());
        outboxEvent.setChannel(messagingProperties.getChannel());
        outboxEvent.setPayload(toJson(event));
        outboxEvent.setStatus(OutboxEventStatus.NEW);
        outboxEventRepository.save(outboxEvent);
    }

    @Override
    public List<OutboxEvent> getPendingEvents() {
        return outboxEventRepository.claimPending(100, Duration.ofSeconds(lockTtlSeconds));
    }

    @Override
    public void markSent(Long outboxEventId) {
        outboxEventRepository.findById(outboxEventId)
                .orElseThrow(() -> new EntityNotFoundException("Outbox event not found: id=" + outboxEventId));
        outboxEventRepository.markSent(outboxEventId);
    }

    @Override
    public void markFailed(Long outboxEventId, String errorMessage) {
        outboxEventRepository.findById(outboxEventId)
                .orElseThrow(() -> new EntityNotFoundException("Outbox event not found: id=" + outboxEventId));
        outboxEventRepository.markFailed(outboxEventId, errorMessage);
    }

    private String toJson(ModerationDecisionEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize moderation outbox event", exception);
        }
    }
}
