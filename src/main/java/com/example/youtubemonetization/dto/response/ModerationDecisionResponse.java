package com.example.youtubemonetization.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ModerationDecisionResponse {

    private Long videoId;
    private String decision;
    private String videoStatus;
    private String moderationStatus;
    private String copyrightStatus;
    private String monetizationStatus;
    private boolean notificationCreated;
    private boolean auditCreated;
    private Long moderatorId;
    private String moderatorUsername;
    private String message;
}
