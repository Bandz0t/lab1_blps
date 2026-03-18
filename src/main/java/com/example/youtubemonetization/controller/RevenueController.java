package com.example.youtubemonetization.controller;

import com.example.youtubemonetization.dto.response.AuthorStatsResponse;
import com.example.youtubemonetization.dto.response.RevenueResponse;
import com.example.youtubemonetization.mapper.RevenueMapper;
import com.example.youtubemonetization.service.RevenueService;
import com.example.youtubemonetization.service.StatsService;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class RevenueController {

    private final RevenueService revenueService;
    private final RevenueMapper revenueMapper;
    private final StatsService statsService;

    @GetMapping("/users/{userId}/revenues")
    public List<RevenueResponse> getUserRevenues(
            @PathVariable Long userId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month
    ) {
        return revenueService.getByAuthor(userId, year, month).stream().map(revenueMapper::toResponse).toList();
    }

    @GetMapping("/videos/{videoId}/revenues")
    public List<RevenueResponse> getVideoRevenues(@PathVariable Long videoId) {
        return revenueService.getByVideo(videoId).stream().map(revenueMapper::toResponse).toList();
    }

    @GetMapping("/users/{userId}/stats")
    public AuthorStatsResponse getAuthorStats(
            @PathVariable Long userId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month
    ) {
        return statsService.getAuthorStats(userId, Optional.ofNullable(year), Optional.ofNullable(month));
    }
}
