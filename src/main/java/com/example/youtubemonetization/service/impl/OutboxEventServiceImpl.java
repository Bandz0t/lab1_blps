package com.example.youtubemonetization.service.impl;

import com.example.youtubemonetization.config.messaging.KafkaTopicProperties;
import com.example.youtubemonetization.dto.event.ModerationDecisionEvent;
import com.example.youtubemonetization.dto.event.MonthlyPayoutRequestedEvent;
import com.example.youtubemonetization.dto.event.PayoutRegistrationCompletedEvent;
import com.example.youtubemonetization.dto.event.PayoutRegistrationRequestedEvent;
import com.example.youtubemonetization.dto.event.VideoProcessingCompletedEvent;
import com.example.youtubemonetization.dto.event.VideoProcessingRequestedEvent;
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
    private final KafkaTopicProperties topicProperties;

    @Value("${app.outbox.lock.ttl-seconds:30}")
    private long lockTtlSeconds;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void enqueueModerationDecisionEvent(ModerationDecisionEvent event) {
        enqueueEvent(
                "MODERATION_DECISION",
                "VIDEO",
                event.getVideoId(),
                topicProperties.getModerationDecision(),
                event
        );
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void enqueueVideoProcessingRequestedEvent(VideoProcessingRequestedEvent event) {
        enqueueEvent(
                "VIDEO_PROCESSING_REQUESTED",
                "VIDEO",
                event.getVideoId(),
                topicProperties.getVideoProcessingRequested(),
                event
        );
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void enqueueVideoProcessingCompletedEvent(VideoProcessingCompletedEvent event) {
        enqueueEvent(
                "VIDEO_PROCESSING_COMPLETED",
                "VIDEO",
                event.getVideoId(),
                topicProperties.getVideoProcessingCompleted(),
                event
        );
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void enqueueMonthlyPayoutRequestedEvent(MonthlyPayoutRequestedEvent event) {
        enqueueEvent(
                "MONTHLY_PAYOUT_REQUESTED",
                "PAYOUT_PERIOD",
                aggregatePeriod(event.getYear(), event.getMonth()),
                topicProperties.getMonthlyPayoutRequested(),
                event
        );
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void enqueuePayoutRegistrationRequestedEvent(PayoutRegistrationRequestedEvent event) {
        enqueueEvent(
                "PAYOUT_REGISTRATION_REQUESTED",
                "PAYOUT",
                event.getPayoutId(),
                topicProperties.getPayoutRegistrationRequested(),
                event
        );
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void enqueuePayoutRegistrationCompletedEvent(PayoutRegistrationCompletedEvent event) {
        enqueueEvent(
                "PAYOUT_REGISTRATION_COMPLETED",
                "PAYOUT",
                event.getPayoutId(),
                topicProperties.getPayoutRegistrationCompleted(),
                event
        );
    }

    private void enqueueEvent(String eventType, String aggregateType, Long aggregateId, String topic, Object payload) {
        OutboxEvent outboxEvent = new OutboxEvent();
        outboxEvent.setEventType(eventType);
        outboxEvent.setAggregateType(aggregateType);
        outboxEvent.setAggregateId(aggregateId);
        outboxEvent.setChannel(topic);
        outboxEvent.setPayload(toJson(payload));
        outboxEvent.setStatus(OutboxEventStatus.NEW);
        outboxEventRepository.save(outboxEvent);
    }

    @Override
    @Transactional(transactionManager = "outboxTransactionManager")
    public List<OutboxEvent> getPendingEvents() {
        return outboxEventRepository.claimPending(100, Duration.ofSeconds(lockTtlSeconds));
    }

    @Override
    @Transactional(transactionManager = "outboxTransactionManager")
    public void markSent(Long outboxEventId) {
        outboxEventRepository.findById(outboxEventId)
                .orElseThrow(() -> new EntityNotFoundException("Outbox event not found: id=" + outboxEventId));
        outboxEventRepository.markSent(outboxEventId);
    }

    @Override
    @Transactional(transactionManager = "outboxTransactionManager")
    public void markFailed(Long outboxEventId, String errorMessage) {
        outboxEventRepository.findById(outboxEventId)
                .orElseThrow(() -> new EntityNotFoundException("Outbox event not found: id=" + outboxEventId));
        outboxEventRepository.markFailed(outboxEventId, errorMessage);
    }

    private Long aggregatePeriod(Integer year, Integer month) {
        return Long.valueOf(year + String.format("%02d", month));
    }

    private String toJson(Object event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize outbox event", exception);
        }
    }
}
