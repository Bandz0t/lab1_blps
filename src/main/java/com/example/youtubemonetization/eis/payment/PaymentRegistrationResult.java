package com.example.youtubemonetization.eis.payment;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PaymentRegistrationResult {

    private String externalPaymentId;
    private boolean duplicate;
}
