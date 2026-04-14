package com.example.youtubemonetization.service;

import com.example.youtubemonetization.dto.request.ModerationDecisionRequest;
import com.example.youtubemonetization.dto.response.ModerationDecisionResponse;

public interface ModerationService {

    ModerationDecisionResponse processModerationDecision(Long videoId, ModerationDecisionRequest request);
}
