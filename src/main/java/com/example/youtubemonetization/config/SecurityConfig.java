package com.example.youtubemonetization.config;

import com.example.youtubemonetization.service.impl.DatabaseUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final DatabaseUserDetailsService userDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .userDetailsService(userDetailsService)
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/login", "/register", "/app.css").permitAll()
                        .requestMatchers("/moderation/**").hasAnyRole("MODERATOR", "ADMIN")
                        .requestMatchers("/api/moderation/**").hasAnyRole("MODERATOR", "ADMIN")
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/dashboard", true)
                        .permitAll())
                .logout(logout -> logout
                        .logoutSuccessUrl("/login?logout")
                        .permitAll())
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }
}
