package com.example.youtubemonetization.security;

import java.security.Principal;
import java.util.Set;

public record AuthenticatedUser(Long id, String username, Set<String> roles, Set<String> privileges) implements Principal {

    @Override
    public String getName() {
        return username;
    }
}
