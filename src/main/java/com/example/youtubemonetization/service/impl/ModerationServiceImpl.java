package com.example.youtubemonetization.service.impl;

import com.example.youtubemonetization.dto.request.ModerationDecisionRequest;
import com.example.youtubemonetization.dto.response.ModerationDecisionResponse;
import com.example.youtubemonetization.entity.AuditLog;
import com.example.youtubemonetization.entity.ModerationRequest;
import com.example.youtubemonetization.entity.Notification;
import com.example.youtubemonetization.entity.User;
import com.example.youtubemonetization.entity.Video;
import com.example.youtubemonetization.enums.CopyrightStatus;
import com.example.youtubemonetization.enums.ModerationDecisionType;
import com.example.youtubemonetization.enums.ModerationRequestStatus;
import com.example.youtubemonetization.enums.MonetizationStatus;
import com.example.youtubemonetization.enums.UploadStatus;
import com.example.youtubemonetization.exception.ConflictException;
import com.example.youtubemonetization.exception.EntityNotFoundException;
import com.example.youtubemonetization.exception.RequestValidationException;
import com.example.youtubemonetization.repository.AuditLogRepository;
import com.example.youtubemonetization.repository.ModerationRequestRepository;
import com.example.youtubemonetization.repository.NotificationRepository;
import com.example.youtubemonetization.repository.UserRepository;
import com.example.youtubemonetization.repository.VideoRepository;
import com.example.youtubemonetization.service.ModerationService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ModerationServiceImpl implements ModerationService {

    private final VideoRepository videoRepository;
    private final ModerationRequestRepository moderationRequestRepository;
    private final NotificationRepository notificationRepository;
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public ModerationDecisionResponse processModerationDecision(Long videoId, ModerationDecisionRequest request) {
        log.info("Старт обработки решения модератора: videoId={}, decision={}", videoId, request.getDecision());
        validateRequest(request);

        Video video = videoRepository.findByIdForUpdate(videoId)
                .orElseThrow(() -> new EntityNotFoundException("Video with id=" + videoId + " not found"));
        log.info("Видео найдено: videoId={}, uploadStatus={}", videoId, video.getUploadStatus());

        if (video.getUploadStatus() == UploadStatus.APPROVED
                || video.getUploadStatus() == UploadStatus.PUBLISHED
                || video.getUploadStatus() == UploadStatus.REJECTED) {
            throw new ConflictException("Решение для видео id=" + videoId + " уже принято ранее");
        }
        if (video.getUploadStatus() != UploadStatus.READY_FOR_REVIEW
                && video.getUploadStatus() != UploadStatus.MANUAL_REVIEW) {
            throw new ConflictException("Видео id=" + videoId + " не находится в статусе модерации");
        }

        ModerationRequest moderationRequest = moderationRequestRepository
                .findFirstByVideoIdAndActiveTrueOrderByCreatedAtDesc(videoId)
                .orElseThrow(() -> new ConflictException("Для видео id=" + videoId + " не найдена активная заявка на модерацию"));
        log.info("Активная moderation request найдена: moderationRequestId={}", moderationRequest.getId());

        User moderator = getCurrentModerator();
        applyDecision(video, moderationRequest, request, moderator);

        Notification notification = buildNotification(video, request.getDecision());
        notificationRepository.save(notification);

        AuditLog auditLog = buildAuditLog(video, moderationRequest, request, moderator);
        auditLogRepository.save(auditLog);

        if ("ROLLBACK_TEST".equals(request.getReason())) {
            throw new RuntimeException("ROLLBACK_TEST");
        }

        log.info("Решение модератора успешно применено: videoId={}, decision={}", videoId, request.getDecision());
        return ModerationDecisionResponse.builder()
                .videoId(video.getId())
                .decision(request.getDecision().name())
                .videoStatus(video.getUploadStatus().name())
                .moderationStatus(moderationRequest.getStatus().name())
                .copyrightStatus(video.getCopyrightStatus().name())
                .monetizationStatus(video.getMonetizationStatus().name())
                .notificationCreated(notification.getId() != null)
                .auditCreated(auditLog.getId() != null)
                .moderatorId(moderator.getId())
                .moderatorUsername(moderator.getUsername())
                .message("Moderation decision applied successfully")
                .build();
    }

    private void validateRequest(ModerationDecisionRequest request) {
        if (request.getDecision() == null) {
            throw new RequestValidationException("Поле decision обязательно");
        }
        if (request.getDecision() == ModerationDecisionType.REJECT
                && (request.getReason() == null || request.getReason().isBlank())) {
            throw new RequestValidationException("Для решения REJECT необходимо указать reason");
        }
    }

    private User getCurrentModerator() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new ConflictException("Не удалось определить пользователя-модератора");
        }
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + username));
    }

    private void applyDecision(Video video, ModerationRequest moderationRequest, ModerationDecisionRequest request, User moderator) {
        switch (request.getDecision()) {
            case APPROVE -> {
                video.setUploadStatus(UploadStatus.APPROVED);
                video.setCopyrightStatus(CopyrightStatus.CLEARED);
                video.setMonetizationStatus(MonetizationStatus.ENABLED);
                moderationRequest.setStatus(ModerationRequestStatus.APPROVED);
            }
            case REJECT -> {
                video.setUploadStatus(UploadStatus.REJECTED);
                video.setCopyrightStatus(CopyrightStatus.VIOLATION_FOUND);
                video.setMonetizationStatus(MonetizationStatus.DENIED);
                moderationRequest.setStatus(ModerationRequestStatus.REJECTED);
            }
            case MANUAL_REVIEW -> {
                video.setUploadStatus(UploadStatus.MANUAL_REVIEW);
                video.setCopyrightStatus(CopyrightStatus.MANUAL_REVIEW);
                video.setMonetizationStatus(MonetizationStatus.PENDING);
                moderationRequest.setStatus(ModerationRequestStatus.MANUAL_REVIEW);
            }
        }
        moderationRequest.setReason(request.getReason());
        moderationRequest.setModerator(moderator);
        moderationRequest.setDecidedAt(LocalDateTime.now());
        moderationRequest.setActive(false);

        videoRepository.save(video);
        moderationRequestRepository.save(moderationRequest);
    }

    private Notification buildNotification(Video video, ModerationDecisionType decision) {
        Notification notification = new Notification();
        notification.setRecipient(video.getAuthor());
        notification.setVideo(video);
        notification.setRead(false);
        notification.setMessage(switch (decision) {
            case APPROVE -> "Ваше видео одобрено и монетизация разрешена";
            case REJECT -> "Ваше видео отклонено";
            case MANUAL_REVIEW -> "Ваше видео отправлено на ручную проверку";
        });
        return notification;
    }

    private AuditLog buildAuditLog(Video video, ModerationRequest moderationRequest, ModerationDecisionRequest request, User moderator) {
        AuditLog auditLog = new AuditLog();
        auditLog.setActorId(moderator.getId());
        auditLog.setActorUsername(moderator.getUsername());
        auditLog.setAction("VIDEO_MODERATION_DECISION");
        auditLog.setEntityType("VIDEO");
        auditLog.setEntityId(video.getId());
        auditLog.setDetails("decision=" + request.getDecision()
                + ", moderationRequestId=" + moderationRequest.getId()
                + ", reason=" + (request.getReason() == null ? "" : request.getReason()));
        return auditLog;
    }
}
