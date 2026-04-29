package com.example.youtubemonetization.service.impl;

import com.example.youtubemonetization.security.AuthenticatedUser;
import com.example.youtubemonetization.security.JwtService;
import com.example.youtubemonetization.security.jaas.JaasConfiguration;
import com.example.youtubemonetization.security.jaas.UsernamePasswordCallbackHandler;
import com.example.youtubemonetization.service.AuthService;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

@Service
public class JaasAuthService implements AuthService {

    private final JwtService jwtService;

    public JaasAuthService(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public AuthenticatedUser authenticate(String username, String password) {
        try {
            LoginContext loginContext = new LoginContext(
                    "YoutubeMonetization",
                    null,
                    new UsernamePasswordCallbackHandler(username, password),
                    new JaasConfiguration()
            );
            loginContext.login();
            return loginContext.getSubject().getPrincipals(AuthenticatedUser.class).stream()
                    .findFirst()
                    .orElseThrow(() -> new BadCredentialsException("Authenticated principal is missing"));
        } catch (LoginException e) {
            throw new BadCredentialsException("Invalid username or password", e);
        }
    }

    @Override
    public String issueToken(AuthenticatedUser user) {
        return jwtService.createToken(user);
    }
}
