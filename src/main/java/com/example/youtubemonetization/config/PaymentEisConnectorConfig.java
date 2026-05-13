package com.example.youtubemonetization.config;

import com.example.youtubemonetization.eis.payment.PaymentEisConnectionFactory;
import com.example.youtubemonetization.eis.payment.jca.PaymentEisManagedConnectionFactory;
import com.example.youtubemonetization.eis.payment.yookassa.YooKassaPaymentEisConnection;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(YooKassaPayoutProperties.class)
public class PaymentEisConnectorConfig {

    private final YooKassaPayoutProperties yooKassaPayoutProperties;
    private final ObjectMapper objectMapper;

    @Value("${app.payment-eis.provider:mock}")
    private String paymentEisProvider;

    @Bean
    public PaymentEisConnectionFactory paymentEisConnectionFactory() {
        if ("yookassa".equalsIgnoreCase(paymentEisProvider)) {
            return new PaymentEisManagedConnectionFactory(
                    () -> new YooKassaPaymentEisConnection(yooKassaPayoutProperties, objectMapper)
            ).paymentConnectionFactory();
        }
        return new PaymentEisManagedConnectionFactory().paymentConnectionFactory();
    }
}
