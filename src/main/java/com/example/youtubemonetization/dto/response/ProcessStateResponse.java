package com.example.youtubemonetization.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProcessStateResponse {

    private Long videoId;
    private String processInstanceId;
    private String currentStep;
    private String bpmnProcessKey;
    private boolean active;
}
