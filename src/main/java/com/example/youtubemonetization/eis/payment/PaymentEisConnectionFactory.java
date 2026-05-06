package com.example.youtubemonetization.eis.payment;

import jakarta.resource.ResourceException;

public interface PaymentEisConnectionFactory {

    PaymentEisConnection getConnection() throws ResourceException;
}
