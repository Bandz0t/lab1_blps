package com.example.youtubemonetization.service.messaging;

import com.example.youtubemonetization.dto.event.ModerationDecisionEvent;
import com.example.youtubemonetization.dto.event.MonthlyPayoutRequestedEvent;
import com.example.youtubemonetization.dto.event.PayoutRegistrationCompletedEvent;
import com.example.youtubemonetization.dto.event.PayoutRegistrationRequestedEvent;
import com.example.youtubemonetization.dto.event.VideoProcessingCompletedEvent;
import com.example.youtubemonetization.dto.event.VideoProcessingRequestedEvent;
import com.example.youtubemonetization.entity.OutboxEvent;
import java.util.List;

public interface OutboxEventService {

    void enqueueModerationDecisionEvent(ModerationDecisionEvent event);

    void enqueueVideoProcessingRequestedEvent(VideoProcessingRequestedEvent event);

    void enqueueVideoProcessingCompletedEvent(VideoProcessingCompletedEvent event);

    void enqueueMonthlyPayoutRequestedEvent(MonthlyPayoutRequestedEvent event);

    void enqueuePayoutRegistrationRequestedEvent(PayoutRegistrationRequestedEvent event);

    void enqueuePayoutRegistrationCompletedEvent(PayoutRegistrationCompletedEvent event);

    List<OutboxEvent> getPendingEvents();

    void markSent(Long outboxEventId);

    void markFailed(Long outboxEventId, String errorMessage);
}
