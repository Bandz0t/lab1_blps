package com.example.youtubemonetization.service;

import com.example.youtubemonetization.entity.Payout;
import com.example.youtubemonetization.enums.PayoutStatus;
import java.util.List;
import java.util.Optional;

public interface PayoutDataService {

    Payout save(Payout payout);

    Payout getById(Long payoutId);

    List<Payout> getByUserId(Long userId);

    List<Payout> getByStatus(PayoutStatus status);

    List<Payout> getRetryableRegistrations(int maxAttempts);

    Optional<Payout> findByUserIdAndPeriod(Long userId, Integer periodYear, Integer periodMonth);
}
