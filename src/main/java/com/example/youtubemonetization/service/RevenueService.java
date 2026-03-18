package com.example.youtubemonetization.service;

import com.example.youtubemonetization.entity.Revenue;
import java.time.YearMonth;
import java.util.List;

public interface RevenueService {

    List<Revenue> getByAuthor(Long authorId, Integer year, Integer month);

    List<Revenue> getByVideo(Long videoId);

    List<Revenue> calculateMonthlyRevenue(YearMonth period);
}
