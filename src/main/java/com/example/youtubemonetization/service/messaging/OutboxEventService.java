package com.example.youtubemonetization.service.messaging;

import com.example.youtubemonetization.dto.event.ModerationDecisionEvent;
import com.example.youtubemonetization.entity.OutboxEvent;
import java.util.List;

public interface OutboxEventService {

    void enqueueModerationDecisionEvent(ModerationDecisionEvent event);

    List<OutboxEvent> getPendingEvents();

    void markSent(Long outboxEventId);

    void markFailed(Long outboxEventId, String errorMessage);
}
