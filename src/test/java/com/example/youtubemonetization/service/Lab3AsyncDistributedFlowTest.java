package com.example.youtubemonetization.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.youtubemonetization.dto.event.PayoutRegistrationRequestedEvent;
import com.example.youtubemonetization.dto.request.VideoCreateRequest;
import com.example.youtubemonetization.entity.OutboxEvent;
import com.example.youtubemonetization.entity.Payout;
import com.example.youtubemonetization.entity.User;
import com.example.youtubemonetization.entity.Video;
import com.example.youtubemonetization.enums.MonetizationType;
import com.example.youtubemonetization.enums.OutboxEventStatus;
import com.example.youtubemonetization.enums.PayoutStatus;
import com.example.youtubemonetization.enums.UploadStatus;
import com.example.youtubemonetization.enums.ValidationStatus;
import com.example.youtubemonetization.repository.PayoutRepository;
import com.example.youtubemonetization.repository.SchedulerLockRepository;
import com.example.youtubemonetization.repository.UserRepository;
import com.example.youtubemonetization.service.impl.PaymentRegistrationService;
import com.example.youtubemonetization.service.messaging.OutboxEventService;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class Lab3AsyncDistributedFlowTest {

    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    @Autowired
    private VideoService videoService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private OutboxEventService outboxEventService;
    @Autowired
    private SchedulerLockRepository schedulerLockRepository;
    @Autowired
    private PayoutRepository payoutRepository;
    @Autowired
    private PaymentRegistrationService paymentRegistrationService;

    @Test
    void createVideoShouldQueueAsyncProcessingWithoutRunningValidationSynchronously() {
        User author = createUser("async_author");
        VideoCreateRequest request = new VideoCreateRequest();
        request.setAuthorId(author.getId());
        request.setTitle("Async video");
        request.setDescription("Queued processing");
        request.setFilePath("/uploads/async.mp4");
        request.setFormat("mp4");
        request.setSizeBytes(1024L);
        request.setDurationSeconds(30);

        Video video = videoService.createVideo(request);

        assertThat(video.getUploadStatus()).isEqualTo(UploadStatus.UPLOADED);
        assertThat(video.getValidationStatus()).isEqualTo(ValidationStatus.PENDING);
        OutboxEvent event = outboxEventService.getPendingEvents().stream()
                .filter(candidate -> video.getId().equals(candidate.getAggregateId()))
                .filter(candidate -> "video.processing.requested".equals(candidate.getChannel()))
                .findFirst()
                .orElseThrow();
        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.PROCESSING);
        assertThat(event.getPayload()).contains("\"videoId\":" + video.getId());
    }

    @Test
    void schedulerLockShouldAllowOnlyOneNodeToAcquireLock() {
        String lockName = "test-lock-" + SEQUENCE.incrementAndGet();

        boolean first = schedulerLockRepository.tryAcquire(lockName, "node-1", Duration.ofMinutes(5));
        boolean second = schedulerLockRepository.tryAcquire(lockName, "node-2", Duration.ofMinutes(5));

        assertThat(first).isTrue();
        assertThat(second).isFalse();
    }

    @Test
    void paymentJcaRegistrationShouldMarkPayoutProcessedAndBeIdempotent() {
        User author = createUser("payment_author");
        Payout payout = new Payout();
        payout.setUser(author);
        payout.setPeriodYear(2026);
        payout.setPeriodMonth(4);
        payout.setTotalAmount(new BigDecimal("42.50"));
        payout.setStatus(PayoutStatus.PENDING);
        payout.setCreatedAt(LocalDateTime.now());
        payout = payoutRepository.save(payout);

        PayoutRegistrationRequestedEvent event = PayoutRegistrationRequestedEvent.builder()
                .payoutId(payout.getId())
                .userId(author.getId())
                .periodYear(2026)
                .periodMonth(4)
                .amount(new BigDecimal("42.50"))
                .currency("USD")
                .idempotencyKey("payout-" + payout.getId())
                .requestedAt(LocalDateTime.now())
                .build();

        paymentRegistrationService.registerPayout(event);
        paymentRegistrationService.registerPayout(event);

        Payout actual = payoutRepository.findById(payout.getId()).orElseThrow();
        assertThat(actual.getStatus()).isEqualTo(PayoutStatus.PROCESSED);
        assertThat(actual.getExternalPaymentId()).startsWith("EIS-PAY-");
        assertThat(actual.getAttempts()).isZero();
        assertThat(actual.getLastError()).isNull();
    }

    private User createUser(String prefix) {
        int suffix = SEQUENCE.incrementAndGet();
        User user = new User();
        user.setUsername(prefix + "_" + suffix);
        user.setEmail(prefix + "_" + suffix + "@example.com");
        user.setFullName("Lab3 User");
        user.setChannelName("Lab3 Channel");
        user.setPasswordHash("{noop}pass");
        user.setRole("AUTHOR");
        return userRepository.save(user);
    }
}
