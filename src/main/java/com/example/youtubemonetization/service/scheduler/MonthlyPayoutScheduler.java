package com.example.youtubemonetization.service.scheduler;

import com.example.youtubemonetization.repository.SchedulerLockRepository;
import com.example.youtubemonetization.service.ProcessService;
import java.time.Duration;
import java.time.YearMonth;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.scheduler.monthly-payout.enabled", havingValue = "true", matchIfMissing = true)
public class MonthlyPayoutScheduler {

    private final SchedulerLockRepository schedulerLockRepository;
    private final ProcessService processService;

    @Value("${app.node-id:local-node}")
    private String nodeId;

    @Value("${app.scheduler.monthly-payout.lock-name:monthly-payout-scheduler}")
    private String lockName;

    @Value("${app.scheduler.monthly-payout.lock-ttl-seconds:300}")
    private long lockTtlSeconds;

    @Scheduled(cron = "${app.scheduler.monthly-payout.cron:0 0 3 1 * *}")
    public void schedulePreviousMonthPayout() {
        boolean acquired = schedulerLockRepository.tryAcquire(lockName, nodeId, Duration.ofSeconds(lockTtlSeconds));
        if (!acquired) {
            log.debug("Node {} skipped monthly payout scheduling because lock is held", nodeId);
            return;
        }
        YearMonth period = YearMonth.now().minusMonths(1);
        processService.requestMonthlyRevenueProcess(Optional.of(period.getYear()), Optional.of(period.getMonthValue()));
        log.info("Node {} queued monthly payout process for {}", nodeId, period);
    }
}
