package com.example.youtubemonetization.service;

import com.example.youtubemonetization.dto.response.AsyncProcessResponse;
import com.example.youtubemonetization.dto.response.MonthlyProcessResponse;
import com.example.youtubemonetization.dto.response.ProcessStateResponse;
import java.util.Optional;

public interface ProcessService {

    void startVideoUploadProcess(Long videoId);

    ProcessStateResponse getProcessState(Long videoId);

    ProcessStateResponse continueProcess(Long videoId);

    MonthlyProcessResponse runMonthlyRevenueProcess(Optional<Integer> year, Optional<Integer> month);

    AsyncProcessResponse requestMonthlyRevenueProcess(Optional<Integer> year, Optional<Integer> month);

    String resolveCurrentStep(Long videoId);
}
