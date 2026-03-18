package com.example.youtubemonetization.controller;

import com.example.youtubemonetization.dto.request.MonthlyProcessRequest;
import com.example.youtubemonetization.dto.response.MonthlyProcessResponse;
import com.example.youtubemonetization.dto.response.PayoutResponse;
import com.example.youtubemonetization.mapper.PayoutMapper;
import com.example.youtubemonetization.service.PayoutDataService;
import com.example.youtubemonetization.service.ProcessService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class PayoutController {

    private final PayoutDataService payoutDataService;
    private final PayoutMapper payoutMapper;
    private final ProcessService processService;

    @GetMapping("/users/{userId}/payouts")
    public List<PayoutResponse> getPayouts(@PathVariable Long userId) {
        return payoutDataService.getByUserId(userId).stream().map(payoutMapper::toResponse).toList();
    }

    @PostMapping("/payouts/process-monthly")
    public MonthlyProcessResponse processMonthly(@Valid @RequestBody(required = false) MonthlyProcessRequest request) {
        return processService.runMonthlyRevenueProcess(
                request == null ? Optional.empty() : Optional.ofNullable(request.getYear()),
                request == null ? Optional.empty() : Optional.ofNullable(request.getMonth())
        );
    }
}
