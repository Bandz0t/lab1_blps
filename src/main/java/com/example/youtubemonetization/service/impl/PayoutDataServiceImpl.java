package com.example.youtubemonetization.service.impl;

import com.example.youtubemonetization.entity.Payout;
import com.example.youtubemonetization.enums.PayoutStatus;
import com.example.youtubemonetization.exception.EntityNotFoundException;
import com.example.youtubemonetization.repository.PayoutRepository;
import com.example.youtubemonetization.service.PayoutDataService;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class PayoutDataServiceImpl implements PayoutDataService {

    private final PayoutRepository payoutRepository;

    @Override
    public Payout save(Payout payout) {
        return payoutRepository.save(payout);
    }

    @Override
    @Transactional(readOnly = true)
    public Payout getById(Long payoutId) {
        return payoutRepository.findById(payoutId)
                .orElseThrow(() -> new EntityNotFoundException("Payout not found: id=" + payoutId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Payout> getByUserId(Long userId) {
        return payoutRepository.findByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Payout> getByStatus(PayoutStatus status) {
        return payoutRepository.findByStatus(status);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Payout> getRetryableRegistrations(int maxAttempts) {
        return payoutRepository.findByStatusInAndAttemptsLessThan(List.of(PayoutStatus.PENDING, PayoutStatus.FAILED), maxAttempts);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Payout> findByUserIdAndPeriod(Long userId, Integer periodYear, Integer periodMonth) {
        return payoutRepository.findByUserIdAndPeriodYearAndPeriodMonth(userId, periodYear, periodMonth);
    }
}
