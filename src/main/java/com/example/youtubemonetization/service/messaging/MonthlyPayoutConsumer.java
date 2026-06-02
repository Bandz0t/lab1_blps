package com.example.youtubemonetization.service.messaging;

import com.example.youtubemonetization.dto.event.MonthlyPayoutRequestedEvent;
import com.example.youtubemonetization.exception.ConflictException;
import com.example.youtubemonetization.service.ProcessService;
import com.example.youtubemonetization.service.camunda.CamundaProcessCorrelationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.kafka.consumers.enabled", havingValue = "true", matchIfMissing = true)
public class MonthlyPayoutConsumer {

    private final ObjectMapper objectMapper;
    private final ProcessService processService;
    private final CamundaProcessCorrelationService camundaProcessCorrelationService;

    @KafkaListener(
            topics = "${app.kafka.topics.monthly-payout-requested}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void processMonthlyPayout(String payload) throws Exception {
        MonthlyPayoutRequestedEvent event = objectMapper.readValue(payload, MonthlyPayoutRequestedEvent.class);
        try {
            processService.runMonthlyRevenueProcess(Optional.of(event.getYear()), Optional.of(event.getMonth()));
            camundaProcessCorrelationService.correlateMonthlyPayoutCompleted(event.getYear(), event.getMonth());
            log.info("Monthly payout process completed: year={}, month={}", event.getYear(), event.getMonth());
        } catch (ConflictException exception) {
            camundaProcessCorrelationService.correlateMonthlyPayoutCompleted(event.getYear(), event.getMonth());
            log.info(
                    "Monthly payout process already completed or partially exists: year={}, month={}, message={}",
                    event.getYear(),
                    event.getMonth(),
                    exception.getMessage());
        }
    }
}
