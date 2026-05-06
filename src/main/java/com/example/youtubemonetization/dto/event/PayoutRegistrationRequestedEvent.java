package com.example.youtubemonetization.dto.event;

import java.math.BigDecimal;
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
public class PayoutRegistrationRequestedEvent {

    private Long payoutId;
    private Long userId;
    private Integer periodYear;
    private Integer periodMonth;
    private BigDecimal amount;
    private String currency;
    private String idempotencyKey;
    private LocalDateTime requestedAt;
}
