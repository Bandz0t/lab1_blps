package com.example.youtubemonetization.service;

import com.example.youtubemonetization.security.AuthenticatedUser;

public interface AuthService {

    AuthenticatedUser authenticate(String username, String password);

    String issueToken(AuthenticatedUser user);
}
