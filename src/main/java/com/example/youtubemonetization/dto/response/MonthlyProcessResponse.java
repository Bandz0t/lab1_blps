package com.example.youtubemonetization.dto.response;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MonthlyProcessResponse {

    private Integer periodYear;
    private Integer periodMonth;
    private int processedVideos;
    private int createdPayouts;
    private List<RevenueResponse> revenues;
    private List<PayoutResponse> payouts;
}
