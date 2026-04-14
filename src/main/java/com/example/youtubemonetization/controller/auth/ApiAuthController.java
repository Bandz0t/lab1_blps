package com.example.youtubemonetization.controller.auth;

import com.example.youtubemonetization.dto.request.auth.JwtLoginRequest;
import com.example.youtubemonetization.dto.response.auth.JwtTokenResponse;
import com.example.youtubemonetization.service.auth.JwtAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class ApiAuthController {

    private final JwtAuthService jwtAuthService;

    @PostMapping("/token")
    public JwtTokenResponse token(@Valid @RequestBody JwtLoginRequest request) {
        return jwtAuthService.issueToken(request.getUsername(), request.getPassword());
    }
}
