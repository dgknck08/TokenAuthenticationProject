package com.example.ecommerce.payment.iyzico;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.payment.iyzico")
public class IyzicoProperties {
    private boolean enabled = false;
    private String apiBaseUrl = "https://sandbox-api.iyzipay.com";
    private String apiKey;
    private String secretKey;
    private String webhookSecret;
    private boolean verifyWebhookSignature = true;
    private String callbackUrl = "http://localhost:8080/api/payments/iyzico/callback";
    private String frontendBaseUrl = "http://localhost:3000";
    private String defaultIdentityNumber;
    private String defaultBuyerIp;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getApiBaseUrl() {
        return apiBaseUrl;
    }

    public void setApiBaseUrl(String apiBaseUrl) {
        this.apiBaseUrl = apiBaseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getWebhookSecret() {
        return webhookSecret;
    }

    public void setWebhookSecret(String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }

    public boolean isVerifyWebhookSignature() {
        return verifyWebhookSignature;
    }

    public void setVerifyWebhookSignature(boolean verifyWebhookSignature) {
        this.verifyWebhookSignature = verifyWebhookSignature;
    }

    public String getCallbackUrl() {
        return callbackUrl;
    }

    public void setCallbackUrl(String callbackUrl) {
        this.callbackUrl = callbackUrl;
    }

    public String getFrontendBaseUrl() {
        return frontendBaseUrl;
    }

    public void setFrontendBaseUrl(String frontendBaseUrl) {
        this.frontendBaseUrl = frontendBaseUrl;
    }

    public String getDefaultIdentityNumber() {
        return defaultIdentityNumber;
    }

    public void setDefaultIdentityNumber(String defaultIdentityNumber) {
        this.defaultIdentityNumber = defaultIdentityNumber;
    }

    public String getDefaultBuyerIp() {
        return defaultBuyerIp;
    }

    public void setDefaultBuyerIp(String defaultBuyerIp) {
        this.defaultBuyerIp = defaultBuyerIp;
    }
}
