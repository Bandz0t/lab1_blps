package ru.ifmo.se.lab1.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.ifmo.se.lab1.dto.CreateVideoRequest;
import ru.ifmo.se.lab1.dto.MonetizationRequestDto;
import ru.ifmo.se.lab1.dto.VideoDto;
import ru.ifmo.se.lab1.service.MonetizationService;
import ru.ifmo.se.lab1.service.VideoService;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class VideoController {
    private final VideoService videoService;
    private final MonetizationService monetizationService;

    @PostMapping("/channels/{channelId}/videos")
    @ResponseStatus(HttpStatus.CREATED)
    public VideoDto uploadVideo(@PathVariable Long channelId, @Valid @RequestBody CreateVideoRequest request) {
        return videoService.uploadVideo(channelId, request);
    }

    @PutMapping("/videos/{id}")
    public VideoDto updateVideo(@PathVariable Long id, @Valid @RequestBody CreateVideoRequest request) {
        return videoService.updateVideo(id, request);
    }

    @PostMapping("/videos/{id}/publish")
    public VideoDto publishVideo(@PathVariable Long id) {
        return videoService.publishVideo(id);
    }

    @GetMapping("/videos/{id}")
    public VideoDto getVideo(@PathVariable Long id) {
        return videoService.getVideo(id);
    }

    @PostMapping("/videos/{id}/views")
    public void addView(@PathVariable Long id) {
        videoService.addView(id);
    }

    @PostMapping("/videos/{id}/monetization-requests")
    @ResponseStatus(HttpStatus.CREATED)
    public MonetizationRequestDto applyForMonetization(@PathVariable Long id) {
        return monetizationService.applyForMonetization(id);
    }
}
