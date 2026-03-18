package com.example.youtubemonetization.dto.response;

import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthorStatsResponse {

    private Long userId;
    private Integer year;
    private Integer month;
    private BigDecimal totalRevenue;
    private Long totalViews;
    private Long totalMonetizedViews;
    private List<RevenueResponse> revenues;
}
