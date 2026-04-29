package com.example.youtubemonetization.security;

import com.example.youtubemonetization.exception.BusinessException;
import java.util.Optional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component("accessGuard")
public class AccessGuard {

    public AuthenticatedUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new AccessDeniedException("Authentication is required");
        }
        return user;
    }

    public Optional<AuthenticatedUser> maybeCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
            return Optional.of(user);
        }
        return Optional.empty();
    }

    public boolean hasPrivilege(String privilege) {
        return maybeCurrentUser()
                .map(user -> user.privileges().contains(privilege))
                .orElse(false);
    }

    public void requirePrivilege(String privilege) {
        if (!hasPrivilege(privilege)) {
            throw new AccessDeniedException("Missing privilege: " + privilege);
        }
    }

    public void requireOwnOrAll(Long ownerId, String ownPrivilege, String allPrivilege) {
        AuthenticatedUser user = currentUser();
        if (user.privileges().contains(allPrivilege)) {
            return;
        }
        if (user.id().equals(ownerId) && user.privileges().contains(ownPrivilege)) {
            return;
        }
        throw new AccessDeniedException("Operation is not allowed for current user");
    }

    public void requireSameUserOrAll(Long userId, String ownPrivilege, String allPrivilege) {
        requireOwnOrAll(userId, ownPrivilege, allPrivilege);
    }

    public Long currentUserIdOrThrow() {
        return currentUser().id();
    }

    public void ensureSelfOrAdmin(Long userId) {
        AuthenticatedUser user = currentUser();
        if (!user.id().equals(userId) && !user.privileges().contains(SecurityPrivileges.USER_READ_ALL)) {
            throw new BusinessException("Cannot operate on another user's account");
        }
    }
}
