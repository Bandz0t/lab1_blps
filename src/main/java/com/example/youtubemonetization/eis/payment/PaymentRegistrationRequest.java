package com.example.youtubemonetization.eis.payment;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PaymentRegistrationRequest {

    private Long payoutId;
    private Long userId;
    private BigDecimal amount;
    private String currency;
    private String idempotencyKey;
}
