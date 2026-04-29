package com.example.youtubemonetization.entity;

import com.example.youtubemonetization.enums.OutboxEventStatus;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OutboxEvent {

    private Long id;

    private String eventType;

    private String aggregateType;

    private Long aggregateId;

    private String channel;

    private String payload;

    private OutboxEventStatus status;

    private Integer attempts;

    private String error;

    private LocalDateTime createdAt;

    private LocalDateTime lockedUntil;

    private LocalDateTime sentAt;
}
