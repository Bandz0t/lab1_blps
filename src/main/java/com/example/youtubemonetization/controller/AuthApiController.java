package com.example.youtubemonetization.controller;

import com.example.youtubemonetization.dto.request.auth.LoginRequest;
import com.example.youtubemonetization.dto.response.auth.AuthResponse;
import com.example.youtubemonetization.security.AccessGuard;
import com.example.youtubemonetization.security.AuthenticatedUser;
import com.example.youtubemonetization.security.JwtAuthenticationFilter;
import com.example.youtubemonetization.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthApiController {

    private final AuthService authService;
    private final AccessGuard accessGuard;

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        AuthenticatedUser user = authService.authenticate(request.getUsername(), request.getPassword());
        String token = authService.issueToken(user);
        ResponseCookie cookie = ResponseCookie.from(JwtAuthenticationFilter.AUTH_COOKIE, token)
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(24 * 60 * 60)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return toResponse(user, token);
    }

    @GetMapping("/me")
    public AuthResponse me() {
        return toResponse(accessGuard.currentUser(), null);
    }

    private AuthResponse toResponse(AuthenticatedUser user, String token) {
        return AuthResponse.builder()
                .accessToken(token)
                .tokenType(token == null ? null : "Bearer")
                .userId(user.id())
                .username(user.username())
                .roles(user.roles())
                .privileges(user.privileges())
                .build();
    }
}
