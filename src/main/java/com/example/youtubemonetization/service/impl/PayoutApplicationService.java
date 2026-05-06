package com.example.youtubemonetization.service.impl;

import com.example.youtubemonetization.dto.event.PayoutRegistrationRequestedEvent;
import com.example.youtubemonetization.entity.Payout;
import com.example.youtubemonetization.entity.User;
import com.example.youtubemonetization.enums.PayoutStatus;
import com.example.youtubemonetization.exception.ConflictException;
import com.example.youtubemonetization.service.PayoutDataService;
import com.example.youtubemonetization.service.RevenueDataService;
import com.example.youtubemonetization.service.UserDataService;
import com.example.youtubemonetization.service.messaging.OutboxEventService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class PayoutApplicationService {

    private final PayoutDataService payoutDataService;
    private final RevenueDataService revenueDataService;
    private final UserDataService userDataService;
    private final OutboxEventService outboxEventService;

    public List<Payout> createMonthlyPayouts(YearMonth period) {
        List<Payout> payouts = new ArrayList<>();
        for (User user : userDataService.findAll()) {
            payoutDataService.findByUserIdAndPeriod(user.getId(), period.getYear(), period.getMonthValue())
                    .ifPresent(existing -> {
                        throw new ConflictException(
                                "Выплата пользователю userId=" + user.getId() + " за " + period + " уже создана"
                        );
                    });
            BigDecimal total = revenueDataService.sumAuthorRevenueForPeriod(user.getId(), period.getYear(), period.getMonthValue());
            if (total == null || total.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            Payout payout = new Payout();
            payout.setUser(user);
            payout.setPeriodYear(period.getYear());
            payout.setPeriodMonth(period.getMonthValue());
            payout.setTotalAmount(total);
            payout.setStatus(PayoutStatus.PENDING);
            payout.setProcessedAt(null);
            payout.setAttempts(0);
            Payout saved = payoutDataService.save(payout);
            outboxEventService.enqueuePayoutRegistrationRequestedEvent(toRegistrationEvent(saved));
            payouts.add(saved);
        }
        return payouts;
    }

    public void enqueueRegistrationRetry(Payout payout) {
        outboxEventService.enqueuePayoutRegistrationRequestedEvent(toRegistrationEvent(payout));
    }

    private PayoutRegistrationRequestedEvent toRegistrationEvent(Payout payout) {
        return PayoutRegistrationRequestedEvent.builder()
                .payoutId(payout.getId())
                .userId(payout.getUser().getId())
                .periodYear(payout.getPeriodYear())
                .periodMonth(payout.getPeriodMonth())
                .amount(payout.getTotalAmount())
                .currency("USD")
                .idempotencyKey("payout-" + payout.getId())
                .requestedAt(LocalDateTime.now())
                .build();
    }
}
