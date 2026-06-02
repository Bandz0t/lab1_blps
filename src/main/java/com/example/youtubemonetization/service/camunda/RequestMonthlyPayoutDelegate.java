package com.example.youtubemonetization.service.camunda;

import com.example.youtubemonetization.dto.event.MonthlyPayoutRequestedEvent;
import com.example.youtubemonetization.service.messaging.OutboxEventService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RequestMonthlyPayoutDelegate implements JavaDelegate {

    private final OutboxEventService outboxEventService;

    @Value("${app.node-id:local-node}")
    private String nodeId;

    @Override
    public void execute(DelegateExecution execution) {
        int year = CamundaVariables.intValue(execution.getVariable("periodYear"), "periodYear");
        int month = CamundaVariables.intValue(execution.getVariable("periodMonth"), "periodMonth");
        outboxEventService.enqueueMonthlyPayoutRequestedEvent(MonthlyPayoutRequestedEvent.builder()
                .year(year)
                .month(month)
                .requestedByNode(nodeId)
                .requestedAt(LocalDateTime.now())
                .build());
    }
}
