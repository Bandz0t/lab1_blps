package com.example.youtubemonetization.service.impl;

import com.example.youtubemonetization.dto.event.VideoProcessingCompletedEvent;
import com.example.youtubemonetization.entity.Video;
import com.example.youtubemonetization.enums.CopyrightStatus;
import com.example.youtubemonetization.enums.ValidationStatus;
import com.example.youtubemonetization.service.CopyrightService;
import com.example.youtubemonetization.service.ValidationService;
import com.example.youtubemonetization.service.VideoDataService;
import com.example.youtubemonetization.service.messaging.OutboxEventService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class VideoProcessingWorkerService {

    private final VideoDataService videoDataService;
    private final ValidationService validationService;
    private final CopyrightService copyrightService;
    private final OutboxEventService outboxEventService;

    @Value("${app.node-id:local-node}")
    private String nodeId;

    public void processVideo(Long videoId) {
        Video initial = videoDataService.getById(videoId);
        if (initial.getValidationStatus() != ValidationStatus.PENDING
                && initial.getCopyrightStatus() != CopyrightStatus.PENDING) {
            return;
        }

        if (initial.getValidationStatus() == ValidationStatus.PENDING) {
            validationService.validateVideo(videoId);
        }

        Video validated = videoDataService.getById(videoId);
        if (validated.getValidationStatus() == ValidationStatus.PASSED
                && validated.getCopyrightStatus() == CopyrightStatus.PENDING) {
            copyrightService.processAutomaticCopyrightCheck(videoId);
        }

        Video completed = videoDataService.getById(videoId);
        outboxEventService.enqueueVideoProcessingCompletedEvent(VideoProcessingCompletedEvent.builder()
                .videoId(videoId)
                .uploadStatus(completed.getUploadStatus().name())
                .validationStatus(completed.getValidationStatus().name())
                .copyrightStatus(completed.getCopyrightStatus().name())
                .processedByNode(nodeId)
                .processedAt(LocalDateTime.now())
                .build());
    }
}
