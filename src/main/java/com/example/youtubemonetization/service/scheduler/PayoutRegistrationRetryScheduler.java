package com.example.youtubemonetization.service.scheduler;

import com.example.youtubemonetization.entity.Payout;
import com.example.youtubemonetization.repository.SchedulerLockRepository;
import com.example.youtubemonetization.service.PayoutDataService;
import com.example.youtubemonetization.service.impl.PayoutApplicationService;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.scheduler.payout-registration-retry.enabled", havingValue = "true", matchIfMissing = true)
public class PayoutRegistrationRetryScheduler {

    private static final int MAX_ATTEMPTS = 5;

    private final SchedulerLockRepository schedulerLockRepository;
    private final PayoutDataService payoutDataService;
    private final PayoutApplicationService payoutApplicationService;

    @Value("${app.node-id:local-node}")
    private String nodeId;

    @Scheduled(fixedDelayString = "${app.scheduler.payout-registration-retry.fixed-delay-ms:60000}")
    @Transactional
    public void requeueFailedPayoutRegistrations() {
        boolean acquired = schedulerLockRepository.tryAcquire(
                "payout-registration-retry",
                nodeId,
                Duration.ofSeconds(55)
        );
        if (!acquired) {
            return;
        }
        for (Payout payout : payoutDataService.getRetryableRegistrations(MAX_ATTEMPTS)) {
            payoutApplicationService.enqueueRegistrationRetry(payout);
            log.info("Queued Payment EIS retry for payoutId={}", payout.getId());
        }
    }
}
