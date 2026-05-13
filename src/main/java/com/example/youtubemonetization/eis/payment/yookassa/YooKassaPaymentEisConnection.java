package com.example.youtubemonetization.eis.payment.yookassa;

import com.example.youtubemonetization.config.YooKassaPayoutProperties;
import com.example.youtubemonetization.eis.payment.PaymentEisConnection;
import com.example.youtubemonetization.eis.payment.PaymentRegistrationRequest;
import com.example.youtubemonetization.eis.payment.PaymentRegistrationResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.resource.ResourceException;
import java.io.IOException;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

public class YooKassaPaymentEisConnection implements PaymentEisConnection {

    private final YooKassaPayoutProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private boolean valid = true;

    public YooKassaPaymentEisConnection(YooKassaPayoutProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                .build();
    }

    @Override
    public PaymentRegistrationResult registerPayment(PaymentRegistrationRequest request) throws ResourceException {
        ensureValid();
        ensureConfigured(request);

        try {
            String payload = objectMapper.writeValueAsString(payload(request));
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(normalizedBaseUrl() + "/v3/payouts"))
                    .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                    .header("Authorization", basicAuth())
                    .header("Idempotence-Key", request.getIdempotencyKey())
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ResourceException("YooKassa payout API returned HTTP "
                        + response.statusCode() + ": " + trim(response.body()));
            }

            JsonNode body = objectMapper.readTree(response.body());
            String externalPaymentId = body.path("id").asText(null);
            if (externalPaymentId == null || externalPaymentId.isBlank()) {
                throw new ResourceException("YooKassa payout API response does not contain payout id");
            }
            return PaymentRegistrationResult.builder()
                    .externalPaymentId(externalPaymentId)
                    .duplicate(false)
                    .build();
        } catch (IOException exception) {
            throw new ResourceException("Failed to call YooKassa payout API", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ResourceException("YooKassa payout API call was interrupted", exception);
        }
    }

    @Override
    public void close() {
        valid = false;
    }

    private Map<String, Object> payload(PaymentRegistrationRequest request) {
        Map<String, Object> amount = new LinkedHashMap<>();
        amount.put("value", request.getAmount().setScale(2, RoundingMode.HALF_UP).toPlainString());
        amount.put("currency", properties.getCurrency());

        Map<String, Object> destination = new LinkedHashMap<>();
        destination.put("type", properties.getDestinationType());
        destination.put("account_number", properties.getAccountNumber());

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("payout_id", String.valueOf(request.getPayoutId()));
        metadata.put("user_id", String.valueOf(request.getUserId()));
        metadata.put("source", "youtube-monetization-lab3");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("amount", amount);
        payload.put("payout_destination_data", destination);
        payload.put("description", properties.getDescriptionPrefix() + " #" + request.getPayoutId());
        payload.put("metadata", metadata);
        return payload;
    }

    private void ensureValid() throws ResourceException {
        if (!valid) {
            throw new ResourceException("YooKassa Payment EIS connection is closed");
        }
    }

    private void ensureConfigured(PaymentRegistrationRequest request) throws ResourceException {
        if (request.getIdempotencyKey() == null || request.getIdempotencyKey().isBlank()) {
            throw new ResourceException("YooKassa idempotency key is required");
        }
        if (request.getAmount() == null) {
            throw new ResourceException("YooKassa payout amount is required");
        }
        if (isBlank(properties.getAgentId())) {
            throw new ResourceException("YooKassa agentId is not configured");
        }
        if (isBlank(properties.getSecretKey())) {
            throw new ResourceException("YooKassa secret key is not configured");
        }
        if (isBlank(properties.getAccountNumber())) {
            throw new ResourceException("YooKassa payout destination account number is not configured");
        }
        if (!"yoo_money".equals(properties.getDestinationType())) {
            throw new ResourceException("Only YooKassa yoo_money payout destination is configured in this adapter");
        }
    }

    private String basicAuth() {
        String credentials = properties.getAgentId() + ":" + properties.getSecretKey();
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

    private String normalizedBaseUrl() {
        String baseUrl = properties.getBaseUrl();
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String trim(String body) {
        if (body == null) {
            return "";
        }
        return body.length() <= 500 ? body : body.substring(0, 500);
    }
}
