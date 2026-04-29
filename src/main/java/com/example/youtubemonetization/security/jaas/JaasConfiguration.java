package com.example.youtubemonetization.security.jaas;

import java.util.Map;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;

public class JaasConfiguration extends Configuration {

    private static final String LOGIN_CONTEXT_NAME = "YoutubeMonetization";

    @Override
    public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
        if (!LOGIN_CONTEXT_NAME.equals(name)) {
            return null;
        }
        return new AppConfigurationEntry[] {
                new AppConfigurationEntry(
                        DatabaseLoginModule.class.getName(),
                        AppConfigurationEntry.LoginModuleControlFlag.REQUIRED,
                        Map.of()
                )
        };
    }
}
