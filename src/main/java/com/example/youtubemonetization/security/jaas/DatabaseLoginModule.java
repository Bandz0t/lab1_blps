package com.example.youtubemonetization.security.jaas;

import com.example.youtubemonetization.entity.Privilege;
import com.example.youtubemonetization.entity.Role;
import com.example.youtubemonetization.entity.User;
import com.example.youtubemonetization.security.AuthenticatedUser;
import com.example.youtubemonetization.security.PrivilegePrincipal;
import com.example.youtubemonetization.security.RolePrincipal;
import java.security.Principal;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

public class DatabaseLoginModule implements LoginModule {

    private Subject subject;
    private CallbackHandler callbackHandler;
    private Set<Principal> principals = new LinkedHashSet<>();
    private boolean authenticated;

    @Override
    public void initialize(
            Subject subject,
            CallbackHandler callbackHandler,
            Map<String, ?> sharedState,
            Map<String, ?> options
    ) {
        this.subject = subject;
        this.callbackHandler = callbackHandler;
    }

    @Override
    public boolean login() throws LoginException {
        NameCallback nameCallback = new NameCallback("username");
        PasswordCallback passwordCallback = new PasswordCallback("password", false);
        try {
            callbackHandler.handle(new javax.security.auth.callback.Callback[] {nameCallback, passwordCallback});
            User user = JaasUserStore.authenticate(nameCallback.getName(), passwordCallback.getPassword())
                    .orElseThrow(() -> new FailedLoginException("Invalid username or password"));

            Set<String> roles = new TreeSet<>();
            Set<String> privileges = new TreeSet<>();
            for (Role role : user.getRoles()) {
                roles.add(role.getName());
                for (Privilege privilege : role.getPrivileges()) {
                    privileges.add(privilege.getName());
                }
            }
            if (roles.isEmpty() && user.getRole() != null) {
                roles.add(user.getRole());
            }

            principals.add(new AuthenticatedUser(user.getId(), user.getUsername(), roles, privileges));
            roles.forEach(role -> principals.add(new RolePrincipal(role)));
            privileges.forEach(privilege -> principals.add(new PrivilegePrincipal(privilege)));
            authenticated = true;
            return true;
        } catch (FailedLoginException e) {
            throw e;
        } catch (Exception e) {
            LoginException loginException = new LoginException("JAAS authentication failed");
            loginException.initCause(e);
            throw loginException;
        } finally {
            passwordCallback.clearPassword();
        }
    }

    @Override
    public boolean commit() {
        if (!authenticated) {
            return false;
        }
        subject.getPrincipals().addAll(principals);
        return true;
    }

    @Override
    public boolean abort() {
        clear();
        return true;
    }

    @Override
    public boolean logout() {
        subject.getPrincipals().removeAll(principals);
        clear();
        return true;
    }

    private void clear() {
        authenticated = false;
        principals.clear();
    }
}
