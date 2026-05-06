package com.example.youtubemonetization.config;

import com.example.youtubemonetization.eis.payment.PaymentEisConnectionFactory;
import com.example.youtubemonetization.eis.payment.jca.PaymentEisManagedConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PaymentEisConnectorConfig {

    @Bean
    public PaymentEisConnectionFactory paymentEisConnectionFactory() {
        return new PaymentEisManagedConnectionFactory().paymentConnectionFactory();
    }
}
