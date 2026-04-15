package com.example.youtubemonetization.service.impl;

import com.example.youtubemonetization.config.messaging.ModerationMessagingProperties;
import com.example.youtubemonetization.dto.event.ModerationDecisionEvent;
import com.example.youtubemonetization.entity.OutboxEvent;
import com.example.youtubemonetization.enums.OutboxEventStatus;
import com.example.youtubemonetization.exception.EntityNotFoundException;
import com.example.youtubemonetization.repository.OutboxEventRepository;
import com.example.youtubemonetization.service.messaging.OutboxEventService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@ConditionalOnProperty(name = "app.redis.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxEventServiceImpl implements OutboxEventService {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final ModerationMessagingProperties messagingProperties;

    @Override
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
    @Transactional(readOnly = true)
    public List<OutboxEvent> getPendingEvents() {
        return outboxEventRepository.findTop100ByStatusOrderByCreatedAtAsc(OutboxEventStatus.NEW);
    }

    @Override
    public void markSent(Long outboxEventId) {
        OutboxEvent outboxEvent = outboxEventRepository.findById(outboxEventId)
                .orElseThrow(() -> new EntityNotFoundException("Outbox event not found: id=" + outboxEventId));
        outboxEvent.setStatus(OutboxEventStatus.SENT);
        outboxEvent.setError(null);
        outboxEvent.setSentAt(LocalDateTime.now());
        outboxEventRepository.save(outboxEvent);
    }

    @Override
    public void markFailed(Long outboxEventId, String errorMessage) {
        OutboxEvent outboxEvent = outboxEventRepository.findById(outboxEventId)
                .orElseThrow(() -> new EntityNotFoundException("Outbox event not found: id=" + outboxEventId));
        outboxEvent.setStatus(OutboxEventStatus.FAILED);
        outboxEvent.setError(errorMessage);
        outboxEvent.setAttempts(outboxEvent.getAttempts() + 1);
        outboxEventRepository.save(outboxEvent);
    }

    private String toJson(ModerationDecisionEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Не удалось сериализовать событие модерации для outbox", exception);
        }
    }
}
