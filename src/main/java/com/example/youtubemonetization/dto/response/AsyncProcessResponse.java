package com.example.youtubemonetization.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AsyncProcessResponse {

    private String status;
    private String topic;
    private Integer periodYear;
    private Integer periodMonth;
}
