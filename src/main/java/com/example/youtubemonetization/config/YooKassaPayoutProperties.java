package com.example.youtubemonetization.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.payment-eis.yookassa")
public class YooKassaPayoutProperties {

    private String baseUrl = "https://api.yookassa.ru";
    private String agentId;
    private String secretKey;
    private String destinationType = "yoo_money";
    private String accountNumber;
    private String currency = "RUB";
    private String descriptionPrefix = "YouTube monetization payout";
    private int timeoutSeconds = 15;
}
