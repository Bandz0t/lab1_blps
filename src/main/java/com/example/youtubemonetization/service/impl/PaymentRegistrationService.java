package com.example.youtubemonetization.service.impl;

import com.example.youtubemonetization.dto.event.PayoutRegistrationCompletedEvent;
import com.example.youtubemonetization.dto.event.PayoutRegistrationRequestedEvent;
import com.example.youtubemonetization.eis.payment.PaymentEisConnection;
import com.example.youtubemonetization.eis.payment.PaymentEisConnectionFactory;
import com.example.youtubemonetization.eis.payment.PaymentRegistrationRequest;
import com.example.youtubemonetization.eis.payment.PaymentRegistrationResult;
import com.example.youtubemonetization.entity.Payout;
import com.example.youtubemonetization.enums.PayoutStatus;
import com.example.youtubemonetization.service.PayoutDataService;
import com.example.youtubemonetization.service.messaging.OutboxEventService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentRegistrationService {

    private final PayoutDataService payoutDataService;
    private final PaymentEisConnectionFactory paymentEisConnectionFactory;
    private final OutboxEventService outboxEventService;

    @Value("${app.node-id:local-node}")
    private String nodeId;

    public void registerPayout(PayoutRegistrationRequestedEvent event) {
        Payout payout = payoutDataService.getById(event.getPayoutId());
        if (payout.getStatus() == PayoutStatus.PROCESSED) {
            return;
        }

        try (PaymentEisConnection connection = paymentEisConnectionFactory.getConnection()) {
            PaymentRegistrationResult result = connection.registerPayment(PaymentRegistrationRequest.builder()
                    .payoutId(payout.getId())
                    .userId(payout.getUser().getId())
                    .amount(payout.getTotalAmount())
                    .currency(event.getCurrency())
                    .idempotencyKey(event.getIdempotencyKey())
                    .build());
            payout.setExternalPaymentId(result.getExternalPaymentId());
            payout.setStatus(PayoutStatus.PROCESSED);
            payout.setProcessedAt(LocalDateTime.now());
            payout.setLastError(null);
            payoutDataService.save(payout);
            outboxEventService.enqueuePayoutRegistrationCompletedEvent(PayoutRegistrationCompletedEvent.builder()
                    .payoutId(payout.getId())
                    .externalPaymentId(result.getExternalPaymentId())
                    .registeredByNode(nodeId)
                    .registeredAt(LocalDateTime.now())
                    .build());
        } catch (Exception exception) {
            payout.setStatus(PayoutStatus.FAILED);
            payout.setAttempts((payout.getAttempts() == null ? 0 : payout.getAttempts()) + 1);
            payout.setLastError(exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage());
            payoutDataService.save(payout);
        }
    }
}
