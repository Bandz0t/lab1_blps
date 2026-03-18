package com.example.youtubemonetization.service;

import com.example.youtubemonetization.dto.response.AuthorStatsResponse;
import java.util.Optional;

public interface StatsService {

    AuthorStatsResponse getAuthorStats(Long userId, Optional<Integer> year, Optional<Integer> month);
}
