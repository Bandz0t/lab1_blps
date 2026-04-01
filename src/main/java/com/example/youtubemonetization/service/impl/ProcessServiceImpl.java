package com.example.youtubemonetization.service.impl;

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
import com.example.youtubemonetization.service.CopyrightService;
import com.example.youtubemonetization.service.ProcessService;
import com.example.youtubemonetization.service.RevenueService;
import com.example.youtubemonetization.service.ValidationService;
import com.example.youtubemonetization.service.VideoDataService;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ProcessServiceImpl implements ProcessService {

    private static final String BPMN_PROCESS_KEY = "youtube_monetization_process";

    private final VideoDataService videoDataService;
    private final ValidationService validationService;
    private final CopyrightService copyrightService;
    private final RevenueService revenueService;
    private final PayoutApplicationService payoutApplicationService;
    private final RevenueMapper revenueMapper;
    private final PayoutMapper payoutMapper;

    @Override
    public void startVideoUploadProcess(Long videoId) {
        Video video = videoDataService.getById(videoId);
        if (video.getProcessInstanceId() == null || video.getProcessInstanceId().isBlank()) {
            video.setProcessInstanceId(UUID.randomUUID().toString());
            videoDataService.save(video);
        }
        validationService.validateVideo(videoId);
        Video validatedVideo = videoDataService.getById(videoId);
        if (validatedVideo.getValidationStatus() == ValidationStatus.PASSED) {
            copyrightService.processAutomaticCopyrightCheck(videoId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ProcessStateResponse getProcessState(Long videoId) {
        Video video = videoDataService.getById(videoId);
        String currentStep = resolveCurrentStep(videoId);
        return ProcessStateResponse.builder()
                .videoId(videoId)
                .processInstanceId(video.getProcessInstanceId())
                .currentStep(currentStep)
                .bpmnProcessKey(BPMN_PROCESS_KEY)
                .active(video.getUploadStatus() != UploadStatus.REJECTED && video.getPublishedAt() == null)
                .build();
    }

    @Override
    public ProcessStateResponse continueProcess(Long videoId) {
        Video video = videoDataService.getById(videoId);
        if (video.getValidationStatus() == ValidationStatus.PENDING) {
            validationService.validateVideo(videoId);
            Video validatedVideo = videoDataService.getById(videoId);
            if (validatedVideo.getValidationStatus() == ValidationStatus.PASSED) {
                copyrightService.processAutomaticCopyrightCheck(videoId);
            }
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
    @Transactional(readOnly = true)
    public String resolveCurrentStep(Long videoId) {
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
}
