package com.example.youtubemonetization.config;

import com.example.youtubemonetization.security.JwtAuthenticationFilter;
import com.example.youtubemonetization.service.impl.DatabaseUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfig {

    private final DatabaseUserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    @Order(1)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/**")
                .userDetailsService(userDetailsService)
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/token").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/videos").hasAuthority("VIDEO_CREATE")
                        .requestMatchers(HttpMethod.POST, "/api/videos/*/edit").hasAuthority("VIDEO_EDIT")
                        .requestMatchers(HttpMethod.POST, "/api/videos/*/copyright-check").hasAuthority("COPYRIGHT_CHECK")
                        .requestMatchers("/api/videos/*/monetization").hasAuthority("MONETIZATION_MANAGE")
                        .requestMatchers(HttpMethod.GET, "/api/videos/**").hasAuthority("VIDEO_READ")
                        .requestMatchers("/api/processes/*/continue").hasAuthority("PROCESS_CONTINUE")
                        .requestMatchers("/api/processes/**").hasAuthority("PROCESS_VIEW")
                        .requestMatchers("/api/users/*/revenues", "/api/videos/*/revenues", "/api/users/*/stats").hasAuthority("REVENUE_READ")
                        .requestMatchers(HttpMethod.POST, "/api/payouts/process-monthly").hasAuthority("PAYOUT_PROCESS")
                        .requestMatchers(HttpMethod.GET, "/api/users/*/payouts").hasAuthority("PAYOUT_READ")
                        .requestMatchers("/api/moderation/**").hasAuthority("MODERATION_REVIEW")
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain webSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .userDetailsService(userDetailsService)
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/login", "/register", "/app.css").permitAll()
                        .requestMatchers("/moderation/**").hasAnyRole("MODERATOR", "ADMIN")
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/dashboard", true)
                        .permitAll())
                .logout(logout -> logout
                        .logoutSuccessUrl("/login?logout")
                        .permitAll());
        return http.build();
    }
}
