package com.example.youtubemonetization.security;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

public enum AppRole {
    AUTHOR(EnumSet.of(
            Privilege.VIDEO_CREATE,
            Privilege.VIDEO_READ,
            Privilege.VIDEO_EDIT,
            Privilege.MONETIZATION_MANAGE,
            Privilege.PROCESS_VIEW,
            Privilege.PROCESS_CONTINUE,
            Privilege.REVENUE_READ,
            Privilege.PAYOUT_READ
    )),
    MODERATOR(EnumSet.of(
            Privilege.VIDEO_READ,
            Privilege.COPYRIGHT_CHECK,
            Privilege.MODERATION_REVIEW,
            Privilege.PROCESS_VIEW,
            Privilege.REVENUE_READ,
            Privilege.PAYOUT_READ
    )),
    ADMIN(EnumSet.allOf(Privilege.class));

    private final Set<Privilege> privileges;

    AppRole(Set<Privilege> privileges) {
        this.privileges = privileges;
    }

    public Set<String> toAuthorities() {
        Set<String> authorities = privileges.stream()
                .map(Enum::name)
                .collect(Collectors.toSet());
        authorities.add("ROLE_" + name());
        return authorities;
    }

    public static AppRole from(String rawRole) {
        return Arrays.stream(values())
                .filter(role -> role.name().equalsIgnoreCase(rawRole))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported role: " + rawRole));
    }
}
