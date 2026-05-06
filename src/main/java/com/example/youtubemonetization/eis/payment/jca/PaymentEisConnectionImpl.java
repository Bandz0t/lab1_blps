package com.example.youtubemonetization.eis.payment.jca;

import com.example.youtubemonetization.eis.payment.PaymentEisConnection;
import com.example.youtubemonetization.eis.payment.PaymentRegistrationRequest;
import com.example.youtubemonetization.eis.payment.PaymentRegistrationResult;
import jakarta.resource.ResourceException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class PaymentEisConnectionImpl implements PaymentEisConnection {

    private static final Map<String, String> REGISTERED_PAYMENTS = new ConcurrentHashMap<>();

    private boolean valid = true;

    @Override
    public PaymentRegistrationResult registerPayment(PaymentRegistrationRequest request) throws ResourceException {
        if (!valid) {
            throw new ResourceException("Payment EIS connection is closed");
        }
        if (request.getIdempotencyKey() == null || request.getIdempotencyKey().isBlank()) {
            throw new ResourceException("Payment EIS idempotency key is required");
        }
        String generatedId = externalId(request.getIdempotencyKey());
        String previous = REGISTERED_PAYMENTS.putIfAbsent(request.getIdempotencyKey(), generatedId);
        return PaymentRegistrationResult.builder()
                .externalPaymentId(previous == null ? generatedId : previous)
                .duplicate(previous != null)
                .build();
    }

    @Override
    public void close() {
        valid = false;
    }

    void invalidate() {
        valid = false;
    }

    private String externalId(String idempotencyKey) {
        return "EIS-PAY-" + Integer.toUnsignedString(idempotencyKey.hashCode()).toUpperCase();
    }
}
