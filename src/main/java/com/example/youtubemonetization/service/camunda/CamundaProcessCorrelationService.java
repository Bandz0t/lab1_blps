package com.example.youtubemonetization.service.camunda;

import com.example.youtubemonetization.entity.Video;
import com.example.youtubemonetization.enums.CopyrightStatus;
import com.example.youtubemonetization.enums.ValidationStatus;
import com.example.youtubemonetization.service.VideoDataService;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.MismatchingMessageCorrelationException;
import org.camunda.bpm.engine.RuntimeService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CamundaProcessCorrelationService {

    private static final String VIDEO_PROCESSING_COMPLETED_MESSAGE = "VideoProcessingCompleted";
    private static final String MONTHLY_PAYOUT_COMPLETED_MESSAGE = "MonthlyPayoutCompleted";

    private final RuntimeService runtimeService;
    private final VideoDataService videoDataService;

    public void correlateVideoProcessingCompleted(Long videoId) {
        Video video = videoDataService.getById(videoId);
        Map<String, Object> variables = new HashMap<>();
        boolean validationPassed = video.getValidationStatus() == ValidationStatus.PASSED;
        boolean copyrightCleared = video.getCopyrightStatus() == CopyrightStatus.CLEARED;
        variables.put("videoId", videoId);
        variables.put("validationPassed", validationPassed);
        variables.put("copyrightCleared", copyrightCleared);
        variables.put("copyrightIssue", validationPassed && !copyrightCleared);
        variables.put("validationStatus", video.getValidationStatus().name());
        variables.put("copyrightStatus", video.getCopyrightStatus().name());
        correlate(VIDEO_PROCESSING_COMPLETED_MESSAGE, CamundaBusinessKeys.video(videoId), variables);
    }

    public void correlateMonthlyPayoutCompleted(int year, int month) {
        YearMonth period = YearMonth.of(year, month);
        Map<String, Object> variables = Map.of(
                "periodYear", year,
                "periodMonth", month,
                "monthlyPayoutCompleted", true
        );
        correlate(MONTHLY_PAYOUT_COMPLETED_MESSAGE, CamundaBusinessKeys.monthlyPayout(period), variables);
    }

    private void correlate(String messageName, String businessKey, Map<String, Object> variables) {
        try {
            runtimeService.createMessageCorrelation(messageName)
                    .processInstanceBusinessKey(businessKey)
                    .setVariables(variables)
                    .correlate();
            log.info("Correlated Camunda message {} to businessKey={}", messageName, businessKey);
        } catch (MismatchingMessageCorrelationException exception) {
            log.debug("No waiting Camunda execution for message {} and businessKey={}", messageName, businessKey);
        }
    }
}
