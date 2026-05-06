package com.example.youtubemonetization.mapper;

import com.example.youtubemonetization.dto.response.PayoutResponse;
import com.example.youtubemonetization.entity.Payout;
import org.springframework.stereotype.Component;

@Component
public class PayoutMapper {

    public PayoutResponse toResponse(Payout payout) {
        return PayoutResponse.builder()
                .userId(payout.getUser().getId())
                .periodYear(payout.getPeriodYear())
                .periodMonth(payout.getPeriodMonth())
                .totalAmount(payout.getTotalAmount())
                .status(payout.getStatus())
                .processedAt(payout.getProcessedAt())
                .externalPaymentId(payout.getExternalPaymentId())
                .attempts(payout.getAttempts())
                .lastError(payout.getLastError())
                .build();
    }
}
