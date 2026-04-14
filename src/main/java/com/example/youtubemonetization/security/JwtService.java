package com.example.youtubemonetization.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final SecretKey signingKey;
    private final Duration ttl;

    public JwtService(
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.ttl-minutes:60}") long ttlMinutes
    ) {
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.ttl = Duration.ofMinutes(ttlMinutes);
    }

    public String generateToken(String username, String role, Collection<String> authorities) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(username)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .claim("role", role)
                .claim("authorities", authorities)
                .signWith(signingKey)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
                 .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    @SuppressWarnings("unchecked")
    public List<String> extractAuthorities(Claims claims) {
        return (List<String>) claims.getOrDefault("authorities", List.of());
    }
}
