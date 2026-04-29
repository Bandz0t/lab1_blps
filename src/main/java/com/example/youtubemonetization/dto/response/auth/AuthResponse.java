package com.example.youtubemonetization.dto.response.auth;

import java.util.Set;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthResponse {

    private String accessToken;
    private String tokenType;
    private Long userId;
    private String username;
    private Set<String> roles;
    private Set<String> privileges;
}
