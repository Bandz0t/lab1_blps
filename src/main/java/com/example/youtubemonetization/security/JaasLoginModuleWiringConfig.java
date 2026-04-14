package com.example.youtubemonetization.security;

import com.example.youtubemonetization.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class JaasLoginModuleWiringConfig {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @PostConstruct
    void init() {
        JaasUserLoginModule.wire(userRepository, passwordEncoder);
    }
}
