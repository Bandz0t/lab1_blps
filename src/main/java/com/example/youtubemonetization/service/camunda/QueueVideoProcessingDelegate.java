package com.example.youtubemonetization.service.camunda;

import com.example.youtubemonetization.dto.event.VideoProcessingRequestedEvent;
import com.example.youtubemonetization.service.messaging.OutboxEventService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class QueueVideoProcessingDelegate implements JavaDelegate {

    private final OutboxEventService outboxEventService;

    @Value("${app.node-id:local-node}")
    private String nodeId;

    @Override
    public void execute(DelegateExecution execution) {
        Long videoId = CamundaVariables.longValue(execution.getVariable("videoId"), "videoId");
        outboxEventService.enqueueVideoProcessingRequestedEvent(VideoProcessingRequestedEvent.builder()
                .videoId(videoId)
                .requestedByNode(nodeId)
                .requestedAt(LocalDateTime.now())
                .build());
    }
}
