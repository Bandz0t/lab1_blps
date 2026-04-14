package com.example.youtubemonetization.security;

import com.example.youtubemonetization.entity.User;
import com.example.youtubemonetization.repository.UserRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import org.springframework.security.crypto.password.PasswordEncoder;

public class JaasUserLoginModule implements LoginModule {

    private static UserRepository userRepository;
    private static PasswordEncoder passwordEncoder;

    private Subject subject;
    private CallbackHandler callbackHandler;
    private User authenticatedUser;
    private final List<JaasRolePrincipal> rolePrincipals = new ArrayList<>();
    private JaasPrincipal userPrincipal;

    public static void wire(UserRepository repository, PasswordEncoder encoder) {
        userRepository = repository;
        passwordEncoder = encoder;
    }

    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {
        this.subject = subject;
        this.callbackHandler = callbackHandler;
    }

    @Override
    public boolean login() throws LoginException {
        if (userRepository == null || passwordEncoder == null) {
            throw new LoginException("JAAS login module is not wired with Spring beans");
        }

        NameCallback nameCallback = new NameCallback("username");
        PasswordCallback passwordCallback = new PasswordCallback("password", false);

        try {
            callbackHandler.handle(new Callback[] {nameCallback, passwordCallback});
        } catch (UnsupportedCallbackException | java.io.IOException ex) {
            throw new LoginException("Unable to read credentials: " + ex.getMessage());
        }

        String username = nameCallback.getName();
        String password = new String(passwordCallback.getPassword() == null ? new char[0] : passwordCallback.getPassword());

        authenticatedUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new FailedLoginException("User not found"));

        if (!passwordEncoder.matches(password, authenticatedUser.getPasswordHash())) {
            throw new FailedLoginException("Invalid credentials");
        }

        return true;
    }

    @Override
    public boolean commit() {
        if (authenticatedUser == null) {
            return false;
        }
        AppRole role = AppRole.from(authenticatedUser.getRole());
        userPrincipal = new JaasPrincipal(authenticatedUser.getUsername());
        subject.getPrincipals().add(userPrincipal);
        rolePrincipals.add(new JaasRolePrincipal("ROLE_" + role.name()));
        role.toAuthorities().forEach(authority -> rolePrincipals.add(new JaasRolePrincipal(authority)));
        subject.getPrincipals().addAll(rolePrincipals);
        return true;
    }

    @Override
    public boolean abort() {
        logout();
        return true;
    }

    @Override
    public boolean logout() {
        if (userPrincipal != null) {
            subject.getPrincipals().remove(userPrincipal);
        }
        if (!rolePrincipals.isEmpty()) {
            subject.getPrincipals().removeAll(rolePrincipals);
            rolePrincipals.clear();
        }
        authenticatedUser = null;
        return true;
    }
}
