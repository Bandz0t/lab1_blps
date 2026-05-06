package com.example.youtubemonetization.service.messaging;

import com.example.youtubemonetization.dto.event.VideoProcessingRequestedEvent;
import com.example.youtubemonetization.service.impl.VideoProcessingWorkerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.kafka.consumers.enabled", havingValue = "true", matchIfMissing = true)
public class VideoProcessingConsumer {

    private final ObjectMapper objectMapper;
    private final VideoProcessingWorkerService videoProcessingWorkerService;

    @KafkaListener(
            topics = "${app.kafka.topics.video-processing-requested}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void processVideo(String payload) throws Exception {
        VideoProcessingRequestedEvent event = objectMapper.readValue(payload, VideoProcessingRequestedEvent.class);
        log.info("Processing video async event: videoId={}, requestedBy={}", event.getVideoId(), event.getRequestedByNode());
        videoProcessingWorkerService.processVideo(event.getVideoId());
    }
}
