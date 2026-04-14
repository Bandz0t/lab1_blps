package com.example.youtubemonetization.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;

import com.example.youtubemonetization.dto.request.ModerationDecisionRequest;
import com.example.youtubemonetization.dto.response.ModerationDecisionResponse;
import com.example.youtubemonetization.entity.ModerationRequest;
import com.example.youtubemonetization.entity.User;
import com.example.youtubemonetization.entity.Video;
import com.example.youtubemonetization.enums.CopyrightStatus;
import com.example.youtubemonetization.enums.ModerationDecisionType;
import com.example.youtubemonetization.enums.ModerationRequestStatus;
import com.example.youtubemonetization.enums.MonetizationStatus;
import com.example.youtubemonetization.enums.MonetizationType;
import com.example.youtubemonetization.enums.UploadStatus;
import com.example.youtubemonetization.enums.ValidationStatus;
import com.example.youtubemonetization.exception.ConflictException;
import com.example.youtubemonetization.exception.EntityNotFoundException;
import com.example.youtubemonetization.exception.RequestValidationException;
import com.example.youtubemonetization.repository.ModerationRequestRepository;
import com.example.youtubemonetization.repository.UserRepository;
import com.example.youtubemonetization.repository.VideoRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class ModerationServiceImplTest {

    @Autowired
    private ModerationService moderationService;
    @Autowired
    private VideoRepository videoRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ModerationRequestRepository moderationRequestRepository;

    @Autowired
    private com.example.youtubemonetization.repository.NotificationRepository notificationRepository;

    @SpyBean
    private com.example.youtubemonetization.repository.AuditLogRepository auditLogRepository;

    private User author;
    private User moderator;

    @BeforeEach
    void setUp() {
        author = new User();
        author.setUsername("author_test");
        author.setEmail("author_test@example.com");
        author.setFullName("Author Test");
        author.setChannelName("Author Channel");
        author.setPasswordHash("{noop}pass");
        author.setRole("AUTHOR");
        author = userRepository.save(author);

        moderator = new User();
        moderator.setUsername("moderator_test");
        moderator.setEmail("moderator_test@example.com");
        moderator.setFullName("Moderator Test");
        moderator.setChannelName("Moderator Channel");
        moderator.setPasswordHash("{noop}pass");
        moderator.setRole("MODERATOR");
        moderator = userRepository.save(moderator);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(moderator.getUsername(), "n/a")
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        reset(auditLogRepository);
    }

    @Test
    void approveDecisionShouldSucceed() {
        Video video = createVideoInReview();
        createModerationRequest(video);

        ModerationDecisionRequest request = new ModerationDecisionRequest();
        request.setDecision(ModerationDecisionType.APPROVE);
        request.setReason("Все проверки пройдены");

        ModerationDecisionResponse response = moderationService.processModerationDecision(video.getId(), request);

        Video updated = videoRepository.findById(video.getId()).orElseThrow();
        assertThat(response.getDecision()).isEqualTo("APPROVE");
        assertThat(updated.getUploadStatus()).isEqualTo(UploadStatus.APPROVED);
        assertThat(updated.getMonetizationStatus()).isEqualTo(MonetizationStatus.ENABLED);
    }

    @Test
    void rejectDecisionShouldSucceed() {
        Video video = createVideoInReview();
        createModerationRequest(video);

        ModerationDecisionRequest request = new ModerationDecisionRequest();
        request.setDecision(ModerationDecisionType.REJECT);
        request.setReason("Нарушение правил");

        moderationService.processModerationDecision(video.getId(), request);

        Video updated = videoRepository.findById(video.getId()).orElseThrow();
        assertThat(updated.getUploadStatus()).isEqualTo(UploadStatus.REJECTED);
        assertThat(updated.getMonetizationStatus()).isEqualTo(MonetizationStatus.DENIED);
    }

    @Test
    void manualReviewDecisionShouldSucceed() {
        Video video = createVideoInReview();
        createModerationRequest(video);

        ModerationDecisionRequest request = new ModerationDecisionRequest();
        request.setDecision(ModerationDecisionType.MANUAL_REVIEW);

        moderationService.processModerationDecision(video.getId(), request);

        Video updated = videoRepository.findById(video.getId()).orElseThrow();
        assertThat(updated.getUploadStatus()).isEqualTo(UploadStatus.MANUAL_REVIEW);
        assertThat(updated.getCopyrightStatus()).isEqualTo(CopyrightStatus.MANUAL_REVIEW);
    }

    @Test
    void shouldThrowWhenVideoDoesNotExist() {
        ModerationDecisionRequest request = new ModerationDecisionRequest();
        request.setDecision(ModerationDecisionType.APPROVE);

        assertThrows(EntityNotFoundException.class, () -> moderationService.processModerationDecision(999999L, request));
    }

    @Test
    void shouldThrowWhenModerationRequestDoesNotExist() {
        Video video = createVideoInReview();
        ModerationDecisionRequest request = new ModerationDecisionRequest();
        request.setDecision(ModerationDecisionType.APPROVE);

        assertThrows(ConflictException.class, () -> moderationService.processModerationDecision(video.getId(), request));
    }

    @Test
    void shouldThrowWhenDecisionAlreadyApplied() {
        Video video = createVideoInReview();
        video.setUploadStatus(UploadStatus.APPROVED);
        videoRepository.save(video);
        createModerationRequest(video);

        ModerationDecisionRequest request = new ModerationDecisionRequest();
        request.setDecision(ModerationDecisionType.REJECT);
        request.setReason("late");

        assertThrows(ConflictException.class, () -> moderationService.processModerationDecision(video.getId(), request));
    }

    @Test
    void shouldValidateRejectReason() {
        Video video = createVideoInReview();
        createModerationRequest(video);

        ModerationDecisionRequest request = new ModerationDecisionRequest();
        request.setDecision(ModerationDecisionType.REJECT);

        assertThrows(RequestValidationException.class, () -> moderationService.processModerationDecision(video.getId(), request));
    }

    @Test
    void shouldRollbackWhenAuditSaveFails() {
        Video video = createVideoInReview();
        createModerationRequest(video);

        long notificationsBefore = notificationRepository.count();
        long auditsBefore = auditLogRepository.count();

        ModerationDecisionRequest request = new ModerationDecisionRequest();
        request.setDecision(ModerationDecisionType.APPROVE);
        request.setReason("force rollback");

        doThrow(new RuntimeException("audit fail")).when(auditLogRepository).save(any());
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> moderationService.processModerationDecision(video.getId(), request));
        assertThat(ex.getMessage()).contains("audit fail");

        Video actual = videoRepository.findById(video.getId()).orElseThrow();
        ModerationRequest moderationRequest = moderationRequestRepository
                .findFirstByVideoIdAndActiveTrueOrderByCreatedAtDesc(video.getId())
                .orElseThrow();

        assertThat(actual.getUploadStatus()).isEqualTo(UploadStatus.READY_FOR_REVIEW);
        assertThat(moderationRequest.getStatus()).isEqualTo(ModerationRequestStatus.PENDING);
        assertThat(notificationRepository.count()).isEqualTo(notificationsBefore);
        assertThat(auditLogRepository.count()).isEqualTo(auditsBefore);
    }

    private Video createVideoInReview() {
        Video video = new Video();
        video.setAuthor(author);
        video.setTitle("t");
        video.setDescription("d");
        video.setFilePath("f.mp4");
        video.setFormat("mp4");
        video.setSizeBytes(100L);
        video.setDurationSeconds(60);
        video.setUploadStatus(UploadStatus.READY_FOR_REVIEW);
        video.setValidationStatus(ValidationStatus.PASSED);
        video.setCopyrightStatus(CopyrightStatus.CLEARED);
        video.setMonetizationStatus(MonetizationStatus.PENDING);
        video.setMonetizationType(MonetizationType.NONE);
        video.setCreatedAt(LocalDateTime.now());
        video.setUpdatedAt(LocalDateTime.now());
        return videoRepository.save(video);
    }

    private ModerationRequest createModerationRequest(Video video) {
        ModerationRequest request = new ModerationRequest();
        request.setVideo(video);
        request.setStatus(ModerationRequestStatus.PENDING);
        request.setActive(true);
        return moderationRequestRepository.save(request);
    }
}
