package ru.ifmo.se.lab1.dto;

import lombok.Data;
import ru.ifmo.se.lab1.domain.RequestStatus;
import java.time.LocalDateTime;

@Data
public class MonetizationRequestDto {
    private Long id;
    private Long videoId;
    private RequestStatus status;
    private Long moderatorId;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;
}
