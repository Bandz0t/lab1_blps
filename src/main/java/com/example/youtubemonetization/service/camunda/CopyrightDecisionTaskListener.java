package com.example.youtubemonetization.service.camunda;

import com.example.youtubemonetization.dto.request.CopyrightCheckRequest;
import com.example.youtubemonetization.enums.ClaimType;
import com.example.youtubemonetization.service.CopyrightService;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.delegate.TaskListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CopyrightDecisionTaskListener implements TaskListener {

    private final CopyrightService copyrightService;
    private final CamundaUserTaskBridge camundaUserTaskBridge;

    @Override
    public void notify(DelegateTask delegateTask) {
        if (Boolean.TRUE.equals(delegateTask.getVariable("dbAlreadyApplied"))) {
            return;
        }
        Long videoId = CamundaVariables.longValue(delegateTask.getVariable("videoId"), "videoId");
        Boolean hasViolation = (Boolean) delegateTask.getVariable("hasViolation");
        CopyrightCheckRequest request = new CopyrightCheckRequest();
        request.setHasViolation(Boolean.TRUE.equals(hasViolation));
        String claimType = (String) delegateTask.getVariable("claimType");
        if (claimType != null && !claimType.isBlank()) {
            request.setClaimType(ClaimType.valueOf(claimType));
        }
        request.setDescription((String) delegateTask.getVariable("description"));
        request.setDetectedFragment((String) delegateTask.getVariable("detectedFragment"));
        camundaUserTaskBridge.runWithCompletionSuppressed(() -> copyrightService.processCopyrightCheck(videoId, request));
        delegateTask.setVariable("copyrightCleared", !request.isHasViolation());
        delegateTask.setVariable("copyrightIssue", request.isHasViolation());
    }
}
