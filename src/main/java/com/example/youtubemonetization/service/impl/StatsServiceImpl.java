package com.example.youtubemonetization.service.impl;

import com.example.youtubemonetization.dto.response.AuthorStatsResponse;
import com.example.youtubemonetization.dto.response.RevenueResponse;
import com.example.youtubemonetization.mapper.RevenueMapper;
import com.example.youtubemonetization.service.RevenueService;
import com.example.youtubemonetization.service.StatsService;
import com.example.youtubemonetization.service.UserDataService;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatsServiceImpl implements StatsService {

    private final RevenueService revenueService;
    private final RevenueMapper revenueMapper;
    private final UserDataService userDataService;

    @Override
    public AuthorStatsResponse getAuthorStats(Long userId, Optional<Integer> year, Optional<Integer> month) {
        userDataService.getById(userId);
        List<RevenueResponse> revenues = revenueService.getByAuthor(userId, year.orElse(null), month.orElse(null)).stream()
                .map(revenueMapper::toResponse)
                .toList();
        BigDecimal totalRevenue = revenues.stream()
                .map(RevenueResponse::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long totalViews = revenues.stream().mapToLong(RevenueResponse::getViewsCount).sum();
        long monetizedViews = revenues.stream().mapToLong(RevenueResponse::getMonetizedViews).sum();
        YearMonth current = YearMonth.now();
        return AuthorStatsResponse.builder()
                .userId(userId)
                .year(year.orElse(current.getYear()))
                .month(month.orElse(current.getMonthValue()))
                .totalRevenue(totalRevenue)
                .totalViews(totalViews)
                .totalMonetizedViews(monetizedViews)
                .revenues(revenues)
                .build();
    }
}
