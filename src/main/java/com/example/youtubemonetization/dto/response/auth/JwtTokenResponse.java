package com.example.youtubemonetization.dto.response.auth;

import lombok.Builder;

@Builder
public record JwtTokenResponse(
        String token,
        String tokenType,
        long expiresInSeconds,
        String role
) {
}
