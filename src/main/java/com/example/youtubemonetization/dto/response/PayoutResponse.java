package com.example.youtubemonetization.dto.response;

import com.example.youtubemonetization.enums.PayoutStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PayoutResponse {

    private Long userId;
    private Integer periodYear;
    private Integer periodMonth;
    private BigDecimal totalAmount;
    private PayoutStatus status;
    private LocalDateTime processedAt;
}
