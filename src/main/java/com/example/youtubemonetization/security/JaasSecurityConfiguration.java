package com.example.youtubemonetization.security;

import java.util.Map;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;

@org.springframework.context.annotation.Configuration
public class JaasSecurityConfiguration {

    public JaasSecurityConfiguration() {
        Configuration.setConfiguration(new InMemoryJaasConfiguration());
    }

    private static final class InMemoryJaasConfiguration extends Configuration {
        @Override
        public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
            if (!"YoutubeMonetizationJaas".equals(name)) {
                return new AppConfigurationEntry[0];
            }
            return new AppConfigurationEntry[] {
                    new AppConfigurationEntry(
                            JaasUserLoginModule.class.getName(),
                            AppConfigurationEntry.LoginModuleControlFlag.REQUIRED,
                            Map.of())
            };
        }
    }
}
