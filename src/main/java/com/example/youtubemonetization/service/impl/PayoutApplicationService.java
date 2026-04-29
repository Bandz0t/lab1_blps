package com.example.youtubemonetization.service.impl;

import com.example.youtubemonetization.entity.Payout;
import com.example.youtubemonetization.entity.User;
import com.example.youtubemonetization.enums.PayoutStatus;
import com.example.youtubemonetization.exception.ConflictException;
import com.example.youtubemonetization.security.AccessGuard;
import com.example.youtubemonetization.security.SecurityPrivileges;
import com.example.youtubemonetization.service.PayoutDataService;
import com.example.youtubemonetization.service.RevenueDataService;
import com.example.youtubemonetization.service.UserDataService;
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
    private final AccessGuard accessGuard;

    public List<Payout> createMonthlyPayouts(YearMonth period) {
        accessGuard.requirePrivilege(SecurityPrivileges.MONTHLY_PROCESS_RUN);
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
            payout.setStatus(PayoutStatus.PROCESSED);
            payout.setProcessedAt(LocalDateTime.now());
            payouts.add(payoutDataService.save(payout));
        }
        return payouts;
    }
}
