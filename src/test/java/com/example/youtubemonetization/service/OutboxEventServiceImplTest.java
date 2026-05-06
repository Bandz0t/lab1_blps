package com.example.youtubemonetization.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.youtubemonetization.dto.event.ModerationDecisionEvent;
import com.example.youtubemonetization.entity.OutboxEvent;
import com.example.youtubemonetization.enums.ModerationDecisionType;
import com.example.youtubemonetization.enums.OutboxEventStatus;
import com.example.youtubemonetization.service.messaging.OutboxEventService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
class OutboxEventServiceImplTest {

    @Autowired
    private OutboxEventService outboxEventService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    void shouldPersistClaimAndRetryModerationOutboxEvent() {
        Long videoId = System.nanoTime();

        transactionTemplate.executeWithoutResult(status -> outboxEventService.enqueueModerationDecisionEvent(
                ModerationDecisionEvent.builder()
                        .videoId(videoId)
                        .moderationRequestId(101L)
                        .moderatorId(202L)
                        .moderatorUsername("moderator")
                        .decision(ModerationDecisionType.APPROVE)
                        .reason("ok")
                        .decidedAt(LocalDateTime.now())
                        .build()
        ));

        OutboxEvent claimed = findByAggregateId(outboxEventService.getPendingEvents(), videoId);
        assertThat(claimed.getStatus()).isEqualTo(OutboxEventStatus.PROCESSING);
        assertThat(claimed.getChannel()).isEqualTo("moderation.decision");
        assertThat(claimed.getPayload()).contains("\"videoId\":" + videoId);

        outboxEventService.markFailed(claimed.getId(), "kafka down");

        OutboxEvent retried = findByAggregateId(outboxEventService.getPendingEvents(), videoId);
        assertThat(retried.getStatus()).isEqualTo(OutboxEventStatus.PROCESSING);
        assertThat(retried.getAttempts()).isEqualTo(1);
    }

    private OutboxEvent findByAggregateId(List<OutboxEvent> events, Long aggregateId) {
        return events.stream()
                .filter(event -> aggregateId.equals(event.getAggregateId()))
                .findFirst()
                .orElseThrow();
    }
}
