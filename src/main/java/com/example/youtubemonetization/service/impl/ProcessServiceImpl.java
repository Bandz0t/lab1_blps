package com.example.youtubemonetization.service.impl;

import com.example.youtubemonetization.config.messaging.KafkaTopicProperties;
import com.example.youtubemonetization.dto.event.VideoProcessingRequestedEvent;
import com.example.youtubemonetization.dto.response.AsyncProcessResponse;
import com.example.youtubemonetization.dto.response.MonthlyProcessResponse;
import com.example.youtubemonetization.dto.response.PayoutResponse;
import com.example.youtubemonetization.dto.response.ProcessStateResponse;
import com.example.youtubemonetization.dto.response.RevenueResponse;
import com.example.youtubemonetization.entity.Payout;
import com.example.youtubemonetization.entity.Revenue;
import com.example.youtubemonetization.entity.Video;
import com.example.youtubemonetization.enums.CopyrightStatus;
import com.example.youtubemonetization.enums.UploadStatus;
import com.example.youtubemonetization.enums.ValidationStatus;
import com.example.youtubemonetization.exception.IllegalProcessStateException;
import com.example.youtubemonetization.mapper.PayoutMapper;
import com.example.youtubemonetization.mapper.RevenueMapper;
import com.example.youtubemonetization.service.ProcessService;
import com.example.youtubemonetization.service.RevenueService;
import com.example.youtubemonetization.service.VideoDataService;
import com.example.youtubemonetization.service.camunda.CamundaBusinessKeys;
import com.example.youtubemonetization.service.messaging.OutboxEventService;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ProcessServiceImpl implements ProcessService {

    private final VideoDataService videoDataService;
    private final RevenueService revenueService;
    private final PayoutApplicationService payoutApplicationService;
    private final RevenueMapper revenueMapper;
    private final PayoutMapper payoutMapper;
    private final OutboxEventService outboxEventService;
    private final KafkaTopicProperties topicProperties;
    private final RuntimeService runtimeService;
    private final TaskService taskService;

    @Value("${app.node-id:local-node}")
    private String nodeId;

    @Override
    public void startVideoUploadProcess(Long videoId) {
        Video video = videoDataService.getById(videoId);
        ProcessInstance existing = findActiveVideoProcess(videoId);
        if (existing != null) {
            video.setProcessInstanceId(existing.getId());
            videoDataService.save(video);
            return;
        }

        Map<String, Object> variables = new HashMap<>();
        variables.put("videoId", videoId);
        variables.put("authorId", video.getAuthor().getId());
        variables.put("validationPassed", false);
        variables.put("copyrightCleared", false);

        ProcessInstance instance = runtimeService.startProcessInstanceByKey(
                CamundaBusinessKeys.VIDEO_PROCESS_KEY,
                CamundaBusinessKeys.video(videoId),
                variables
        );
        video.setProcessInstanceId(instance.getId());
        videoDataService.save(video);
    }

    @Override
    @Transactional(readOnly = true)
    public ProcessStateResponse getProcessState(Long videoId) {
        Video video = videoDataService.getById(videoId);
        String currentStep = resolveCurrentStep(videoId);
        ProcessInstance activeInstance = findActiveVideoProcess(videoId);
        return ProcessStateResponse.builder()
                .videoId(videoId)
                .processInstanceId(activeInstance == null ? video.getProcessInstanceId() : activeInstance.getId())
                .currentStep(currentStep)
                .bpmnProcessKey(CamundaBusinessKeys.VIDEO_PROCESS_KEY)
                .active(activeInstance != null || (video.getUploadStatus() != UploadStatus.REJECTED && video.getPublishedAt() == null))
                .build();
    }

    @Override
    public ProcessStateResponse continueProcess(Long videoId) {
        Video video = videoDataService.getById(videoId);
        ProcessInstance activeInstance = findActiveVideoProcess(videoId);
        if (activeInstance != null && video.getValidationStatus() == ValidationStatus.PENDING) {
            outboxEventService.enqueueVideoProcessingRequestedEvent(VideoProcessingRequestedEvent.builder()
                    .videoId(videoId)
                    .requestedByNode(nodeId)
                    .requestedAt(LocalDateTime.now())
                    .build());
            return getProcessState(videoId);
        }
        throw new IllegalProcessStateException("Процесс видео id=" + videoId + " не требует технического продолжения");
    }

    @Override
    public MonthlyProcessResponse runMonthlyRevenueProcess(Optional<Integer> year, Optional<Integer> month) {
        YearMonth period = YearMonth.of(year.orElse(YearMonth.now().getYear()), month.orElse(YearMonth.now().getMonthValue()));
        List<Revenue> revenues = revenueService.calculateMonthlyRevenue(period);
        List<Payout> payouts = payoutApplicationService.createMonthlyPayouts(period);
        List<RevenueResponse> revenueResponses = revenues.stream().map(revenueMapper::toResponse).toList();
        List<PayoutResponse> payoutResponses = payouts.stream().map(payoutMapper::toResponse).toList();
        return MonthlyProcessResponse.builder()
                .periodYear(period.getYear())
                .periodMonth(period.getMonthValue())
                .processedVideos(revenues.size())
                .createdPayouts(payouts.size())
                .revenues(revenueResponses)
                .payouts(payoutResponses)
                .build();
    }

    @Override
    public AsyncProcessResponse requestMonthlyRevenueProcess(Optional<Integer> year, Optional<Integer> month) {
        YearMonth period = YearMonth.of(year.orElse(YearMonth.now().getYear()), month.orElse(YearMonth.now().getMonthValue()));
        String businessKey = CamundaBusinessKeys.monthlyPayout(period);
        ProcessInstance activeInstance = runtimeService.createProcessInstanceQuery()
                .processDefinitionKey(CamundaBusinessKeys.MONTHLY_PAYOUT_PROCESS_KEY)
                .processInstanceBusinessKey(businessKey)
                .active()
                .singleResult();
        if (activeInstance == null) {
            Map<String, Object> variables = Map.of(
                    "periodYear", period.getYear(),
                    "periodMonth", period.getMonthValue()
            );
            runtimeService.startProcessInstanceByKey(
                    CamundaBusinessKeys.MONTHLY_PAYOUT_PROCESS_KEY,
                    businessKey,
                    variables
            );
        }
        return AsyncProcessResponse.builder()
                .status("QUEUED")
                .topic(topicProperties.getMonthlyPayoutRequested())
                .periodYear(period.getYear())
                .periodMonth(period.getMonthValue())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public String resolveCurrentStep(Long videoId) {
        Task activeTask = taskService.createTaskQuery()
                .processInstanceBusinessKey(CamundaBusinessKeys.video(videoId))
                .active()
                .singleResult();
        if (activeTask != null) {
            if ("reviewCopyrightTask".equals(activeTask.getTaskDefinitionKey())) {
                return "WAITING_FOR_COPYRIGHT_CHECK";
            }
            if ("chooseMonetizationTask".equals(activeTask.getTaskDefinitionKey())) {
                return "WAITING_FOR_MONETIZATION_SELECTION";
            }
            return activeTask.getTaskDefinitionKey();
        }
        ProcessInstance activeInstance = findActiveVideoProcess(videoId);
        if (activeInstance != null) {
            return "TECHNICAL_VALIDATION";
        }

        Video video = videoDataService.getById(videoId);
        if (video.getUploadStatus() == UploadStatus.REJECTED || video.getValidationStatus() == ValidationStatus.FAILED) {
            return "REJECTED";
        }
        if (video.getPublishedAt() != null) {
            return "PUBLISHED";
        }
        if (video.getValidationStatus() == ValidationStatus.PENDING) {
            return "TECHNICAL_VALIDATION";
        }
        if (video.getValidationStatus() == ValidationStatus.PASSED && video.getCopyrightStatus() == CopyrightStatus.PENDING) {
            return "WAITING_FOR_COPYRIGHT_CHECK";
        }
        if (video.getCopyrightStatus() == CopyrightStatus.NEEDS_EDITING) {
            return "EDIT_REQUIRED";
        }
        if (video.getCopyrightStatus() == CopyrightStatus.CLEARED) {
            return "WAITING_FOR_MONETIZATION_SELECTION";
        }
        return "IN_PROGRESS";
    }

    private ProcessInstance findActiveVideoProcess(Long videoId) {
        return runtimeService.createProcessInstanceQuery()
                .processDefinitionKey(CamundaBusinessKeys.VIDEO_PROCESS_KEY)
                .processInstanceBusinessKey(CamundaBusinessKeys.video(videoId))
                .active()
                .singleResult();
    }
}
