package com.example.youtubemonetization.mapper;

import com.example.youtubemonetization.dto.response.RevenueResponse;
import com.example.youtubemonetization.entity.Revenue;
import org.springframework.stereotype.Component;

@Component
public class RevenueMapper {

    public RevenueResponse toResponse(Revenue revenue) {
        return RevenueResponse.builder()
                .videoId(revenue.getVideo().getId())
                .videoTitle(revenue.getVideo().getTitle())
                .periodYear(revenue.getPeriodYear())
                .periodMonth(revenue.getPeriodMonth())
                .viewsCount(revenue.getViewsCount())
                .monetizedViews(revenue.getMonetizedViews())
                .cpm(revenue.getCpm())
                .amount(revenue.getAmount())
                .currency(revenue.getCurrency())
                .build();
    }
}
