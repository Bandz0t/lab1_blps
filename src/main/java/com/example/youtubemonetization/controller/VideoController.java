package com.example.youtubemonetization.controller;

import com.example.youtubemonetization.dto.request.CopyrightCheckRequest;
import com.example.youtubemonetization.dto.request.EditVideoRequest;
import com.example.youtubemonetization.dto.request.VideoCreateRequest;
import com.example.youtubemonetization.dto.response.VideoResponse;
import com.example.youtubemonetization.dto.response.VideoStatusResponse;
import com.example.youtubemonetization.mapper.VideoMapper;
import com.example.youtubemonetization.service.CopyrightService;
import com.example.youtubemonetization.service.ProcessService;
import com.example.youtubemonetization.service.VideoService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/videos")
@RequiredArgsConstructor
public class VideoController {

    private final VideoService videoService;
    private final CopyrightService copyrightService;
    private final VideoMapper videoMapper;
    private final ProcessService processService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VideoResponse createVideo(@Valid @RequestBody VideoCreateRequest request) {
        return videoMapper.toResponse(videoService.createVideo(request));
    }

    @GetMapping("/{id}")
    public VideoResponse getVideo(@PathVariable Long id) {
        return videoMapper.toResponse(videoService.getVideo(id));
    }

    @GetMapping
    public List<VideoResponse> getVideos(@RequestParam(required = false) Long authorId) {
        return videoService.getVideos(authorId).stream().map(videoMapper::toResponse).toList();
    }

    @PostMapping("/{id}/edit")
    public VideoResponse editVideo(@PathVariable Long id, @Valid @RequestBody EditVideoRequest request) {
        return videoMapper.toResponse(videoService.editVideo(id, request));
    }

    @GetMapping("/{id}/status")
    public VideoStatusResponse getVideoStatus(@PathVariable Long id) {
        return videoMapper.toStatusResponse(videoService.getVideo(id), processService.resolveCurrentStep(id));
    }

    @PostMapping("/{id}/copyright-check")
    public VideoStatusResponse checkCopyright(@PathVariable Long id, @RequestBody CopyrightCheckRequest request) {
        return videoMapper.toStatusResponse(copyrightService.processCopyrightCheck(id, request), processService.resolveCurrentStep(id));
    }
}
