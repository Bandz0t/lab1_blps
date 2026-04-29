package com.example.youtubemonetization.security;

import java.security.Principal;

public record RolePrincipal(String name) implements Principal {

    @Override
    public String getName() {
        return name;
    }
}
