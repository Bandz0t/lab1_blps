package com.example.youtubemonetization.security.jaas;

import java.io.IOException;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

public class UsernamePasswordCallbackHandler implements CallbackHandler {

    private final String username;
    private final char[] password;

    public UsernamePasswordCallbackHandler(String username, String password) {
        this.username = username;
        this.password = password == null ? new char[0] : password.toCharArray();
    }

    @Override
    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        for (Callback callback : callbacks) {
            if (callback instanceof NameCallback nameCallback) {
                nameCallback.setName(username);
            } else if (callback instanceof PasswordCallback passwordCallback) {
                passwordCallback.setPassword(password);
            } else {
                throw new UnsupportedCallbackException(callback);
            }
        }
    }
}
