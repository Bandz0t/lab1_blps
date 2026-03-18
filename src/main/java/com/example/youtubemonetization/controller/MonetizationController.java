package com.example.youtubemonetization.controller;

import com.example.youtubemonetization.dto.request.MonetizationRequest;
import com.example.youtubemonetization.dto.response.MonetizationResponse;
import com.example.youtubemonetization.mapper.VideoMapper;
import com.example.youtubemonetization.service.MonetizationService;
import com.example.youtubemonetization.service.VideoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/videos/{id}/monetization")
@RequiredArgsConstructor
public class MonetizationController {

    private final MonetizationService monetizationService;
    private final VideoService videoService;
    private final VideoMapper videoMapper;

    @PostMapping
    public MonetizationResponse chooseMonetization(@PathVariable Long id, @Valid @RequestBody MonetizationRequest request) {
        return videoMapper.toMonetizationResponse(monetizationService.chooseMonetization(id, request.getMonetizationType()));
    }

    @GetMapping
    public MonetizationResponse getMonetization(@PathVariable Long id) {
        return videoMapper.toMonetizationResponse(videoService.getVideo(id));
    }
}
