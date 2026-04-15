package com.example.youtubemonetization.dto.event;

import com.example.youtubemonetization.enums.ModerationDecisionType;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ModerationDecisionEvent {

    private final Long videoId;
    private final Long moderationRequestId;
    private final Long moderatorId;
    private final String moderatorUsername;
    private final ModerationDecisionType decision;
    private final String reason;
    private final LocalDateTime decidedAt;
}
