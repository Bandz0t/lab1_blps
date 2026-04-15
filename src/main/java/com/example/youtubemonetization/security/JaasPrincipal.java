package com.example.youtubemonetization.security;

import java.io.Serial;
import java.io.Serializable;
import java.security.Principal;

public record JaasPrincipal(String name) implements Principal, Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Override
    public String getName() {
        return name;
    }
}
