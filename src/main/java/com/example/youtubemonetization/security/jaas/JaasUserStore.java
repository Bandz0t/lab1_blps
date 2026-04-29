package com.example.youtubemonetization.security.jaas;

import com.example.youtubemonetization.entity.User;
import com.example.youtubemonetization.repository.UserRepository;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class JaasUserStore {

    private static UserRepository userRepository;
    private static PasswordEncoder passwordEncoder;

    public JaasUserStore(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        JaasUserStore.userRepository = userRepository;
        JaasUserStore.passwordEncoder = passwordEncoder;
    }

    static Optional<User> authenticate(String username, char[] password) {
        if (userRepository == null || passwordEncoder == null) {
            throw new IllegalStateException("JAAS user store is not initialized");
        }
        return userRepository.findByUsername(username)
                .filter(user -> passwordEncoder.matches(new String(password), user.getPasswordHash()));
    }
}
