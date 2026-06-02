package com.example.youtubemonetization.service.camunda;

import com.example.youtubemonetization.dto.request.CopyrightCheckRequest;
import com.example.youtubemonetization.enums.MonetizationType;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.task.Task;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CamundaUserTaskBridge {

    private static final String COPYRIGHT_TASK_KEY = "reviewCopyrightTask";
    private static final String MONETIZATION_TASK_KEY = "chooseMonetizationTask";
    private final ThreadLocal<Boolean> completionSuppressed = ThreadLocal.withInitial(() -> false);

    private final TaskService taskService;

    public void runWithCompletionSuppressed(Runnable runnable) {
        boolean previous = Boolean.TRUE.equals(completionSuppressed.get());
        completionSuppressed.set(true);
        try {
            runnable.run();
        } finally {
            completionSuppressed.set(previous);
        }
    }

    public void completeCopyrightReview(Long videoId, CopyrightCheckRequest request, boolean dbAlreadyApplied) {
        if (Boolean.TRUE.equals(completionSuppressed.get())) {
            return;
        }
        Task task = findTask(videoId, COPYRIGHT_TASK_KEY);
        if (task == null) {
            return;
        }
        Map<String, Object> variables = new HashMap<>();
        variables.put("videoId", videoId);
        variables.put("dbAlreadyApplied", dbAlreadyApplied);
        variables.put("hasViolation", request.isHasViolation());
        variables.put("copyrightCleared", !request.isHasViolation());
        variables.put("copyrightIssue", request.isHasViolation());
        if (request.getClaimType() != null) {
            variables.put("claimType", request.getClaimType().name());
        }
        variables.put("description", request.getDescription());
        variables.put("detectedFragment", request.getDetectedFragment());
        taskService.complete(task.getId(), variables);
        log.info("Completed Camunda copyright task for videoId={}", videoId);
    }

    public void completeMonetization(Long videoId, MonetizationType monetizationType, boolean dbAlreadyApplied) {
        if (Boolean.TRUE.equals(completionSuppressed.get())) {
            return;
        }
        Task task = findTask(videoId, MONETIZATION_TASK_KEY);
        if (task == null) {
            return;
        }
        Map<String, Object> variables = new HashMap<>();
        variables.put("videoId", videoId);
        variables.put("dbAlreadyApplied", dbAlreadyApplied);
        variables.put("monetizationType", monetizationType.name());
        taskService.complete(task.getId(), variables);
        log.info("Completed Camunda monetization task for videoId={}", videoId);
    }

    private Task findTask(Long videoId, String taskDefinitionKey) {
        Task task = taskService.createTaskQuery()
                .processInstanceBusinessKey(CamundaBusinessKeys.video(videoId))
                .taskDefinitionKey(taskDefinitionKey)
                .active()
                .singleResult();
        if (task == null) {
            log.debug("No active Camunda task {} for videoId={}", taskDefinitionKey, videoId);
        }
        return task;
    }
}
