package com.example.youtubemonetization.repository;

import com.example.youtubemonetization.entity.Payout;
import com.example.youtubemonetization.enums.PayoutStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PayoutRepository extends JpaRepository<Payout, Long> {

    List<Payout> findByUserId(Long userId);

    List<Payout> findByStatus(PayoutStatus status);

    List<Payout> findByStatusInAndAttemptsLessThan(List<PayoutStatus> statuses, Integer attempts);

    Optional<Payout> findByUserIdAndPeriodYearAndPeriodMonth(Long userId, Integer periodYear, Integer periodMonth);
}
