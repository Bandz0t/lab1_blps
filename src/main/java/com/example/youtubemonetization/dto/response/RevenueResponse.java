package com.example.youtubemonetization.dto.response;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RevenueResponse {

    private Long videoId;
    private String videoTitle;
    private Integer periodYear;
    private Integer periodMonth;
    private Long viewsCount;
    private Long monetizedViews;
    private BigDecimal cpm;
    private BigDecimal amount;
    private String currency;
}
