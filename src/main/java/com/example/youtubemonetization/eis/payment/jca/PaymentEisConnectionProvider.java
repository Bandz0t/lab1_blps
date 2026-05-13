package com.example.youtubemonetization.eis.payment.jca;

import com.example.youtubemonetization.eis.payment.PaymentEisConnection;
import jakarta.resource.ResourceException;

@FunctionalInterface
public interface PaymentEisConnectionProvider {

    PaymentEisConnection createConnection() throws ResourceException;
}
