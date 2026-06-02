package com.example.youtubemonetization.service.camunda;

import com.example.youtubemonetization.enums.MonetizationType;
import com.example.youtubemonetization.service.MonetizationService;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.delegate.TaskListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MonetizationTaskListener implements TaskListener {

    private final MonetizationService monetizationService;
    private final CamundaUserTaskBridge camundaUserTaskBridge;

    @Override
    public void notify(DelegateTask delegateTask) {
        if (Boolean.TRUE.equals(delegateTask.getVariable("dbAlreadyApplied"))) {
            return;
        }
        Long videoId = CamundaVariables.longValue(delegateTask.getVariable("videoId"), "videoId");
        String monetizationType = (String) delegateTask.getVariable("monetizationType");
        camundaUserTaskBridge.runWithCompletionSuppressed(
                () -> monetizationService.chooseMonetization(videoId, MonetizationType.valueOf(monetizationType))
        );
    }
}
