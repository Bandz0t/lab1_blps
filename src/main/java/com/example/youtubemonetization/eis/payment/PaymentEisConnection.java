package com.example.youtubemonetization.eis.payment;

import jakarta.resource.ResourceException;

public interface PaymentEisConnection extends AutoCloseable {

    PaymentRegistrationResult registerPayment(PaymentRegistrationRequest request) throws ResourceException;

    @Override
    void close() throws ResourceException;
}
