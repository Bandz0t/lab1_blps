package com.example.youtubemonetization.controller;

import com.example.youtubemonetization.dto.request.ModerationDecisionRequest;
import com.example.youtubemonetization.dto.response.ModerationDecisionResponse;
import com.example.youtubemonetization.service.ModerationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/moderation")
public class ModerationController {

    private final ModerationService moderationService;

    @PostMapping("/videos/{videoId}/decision")
    public ModerationDecisionResponse processDecision(
            @PathVariable Long videoId,
            @Valid @RequestBody ModerationDecisionRequest request
    ) {
        return moderationService.processModerationDecision(videoId, request);
    }
}
