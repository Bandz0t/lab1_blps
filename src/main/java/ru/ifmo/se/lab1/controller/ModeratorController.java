package ru.ifmo.se.lab1.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.ifmo.se.lab1.dto.ModerateRequest;
import ru.ifmo.se.lab1.dto.MonetizationRequestDto;
import ru.ifmo.se.lab1.service.MonetizationService;

@RestController
@RequestMapping("/api/moderation/monetization-requests")
@RequiredArgsConstructor
public class ModeratorController {
    private final MonetizationService monetizationService;

    @PostMapping("/{requestId}/approve")
    public MonetizationRequestDto approveRequest(@PathVariable Long requestId, @Valid @RequestBody ModerateRequest request) {
        return monetizationService.moderateRequest(requestId, request.getModeratorId(), true);
    }

    @PostMapping("/{requestId}/reject")
    public MonetizationRequestDto rejectRequest(@PathVariable Long requestId, @Valid @RequestBody ModerateRequest request) {
        return monetizationService.moderateRequest(requestId, request.getModeratorId(), false);
    }
}
