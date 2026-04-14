package com.example.youtubemonetization.service.auth;

import com.example.youtubemonetization.dto.response.auth.JwtTokenResponse;
import com.example.youtubemonetization.entity.User;
import com.example.youtubemonetization.repository.UserRepository;
import com.example.youtubemonetization.security.AppRole;
import com.example.youtubemonetization.security.JaasAuthenticationCallbackHandler;
import com.example.youtubemonetization.security.JwtService;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JwtAuthService {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Value("${security.jwt.ttl-minutes:60}")
    private long ttlMinutes;

    public JwtTokenResponse issueToken(String username, String password) {
        try {
            LoginContext loginContext = new LoginContext(
                    "YoutubeMonetizationJaas",
                    new JaasAuthenticationCallbackHandler(username, password)
            );
            loginContext.login();
        } catch (LoginException ex) {
            throw new BadCredentialsException("Неверный логин или пароль");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("Пользователь не найден"));

        AppRole role = AppRole.from(user.getRole());
        String token = jwtService.generateToken(user.getUsername(), role.name(), role.toAuthorities());
        return JwtTokenResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresInSeconds(ttlMinutes * 60)
                .role(role.name())
                .build();
    }
}
