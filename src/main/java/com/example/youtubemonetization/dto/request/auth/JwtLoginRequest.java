package com.example.youtubemonetization.dto.request.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JwtLoginRequest {

    @NotBlank
    private String username;

    @NotBlank
    private String password;
}
