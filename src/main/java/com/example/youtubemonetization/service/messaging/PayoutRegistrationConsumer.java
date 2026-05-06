package com.example.youtubemonetization.service.messaging;

import com.example.youtubemonetization.dto.event.PayoutRegistrationRequestedEvent;
import com.example.youtubemonetization.service.impl.PaymentRegistrationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.kafka.consumers.enabled", havingValue = "true", matchIfMissing = true)
public class PayoutRegistrationConsumer {

    private final ObjectMapper objectMapper;
    private final PaymentRegistrationService paymentRegistrationService;

    @KafkaListener(
            topics = "${app.kafka.topics.payout-registration-requested}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void registerPayout(String payload) throws Exception {
        PayoutRegistrationRequestedEvent event = objectMapper.readValue(payload, PayoutRegistrationRequestedEvent.class);
        log.info("Registering payout in Payment EIS: payoutId={}", event.getPayoutId());
        paymentRegistrationService.registerPayout(event);
    }
}
