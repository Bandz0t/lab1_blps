package com.example.youtubemonetization.dto.event;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyPayoutRequestedEvent {

    private Integer year;
    private Integer month;
    private String requestedByNode;
    private LocalDateTime requestedAt;
}
