package com.opspilot.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Externalized configuration for the outbound webhook target.
 *
 * <p>Properties are bound from the {@code notification.webhook.*} namespace. The optional
 * {@code authHeaderName} / {@code authHeaderValue} pair allows a shared-secret header
 * (e.g. {@code Authorization: Bearer <token>}) to be injected into every outbound request
 * without hard-coding credentials in application code.</p>
 *
 * <p>Timeout defaults are intentionally asymmetric: the connect timeout (2 s) is kept short
 * to fail fast when the webhook host is unreachable, while the read timeout (5 s) is slightly
 * more generous to accommodate slow webhook handlers that may buffer the body before responding.</p>
 */
@ConfigurationProperties(prefix = "notification.webhook")
public class WebhookProperties {

    // When false, deliver() logs a skip message and returns without making any HTTP call
    private boolean enabled = true;
    private String url = "http://localhost:8090/events";
    // Both fields must be non-blank for the auth header to be added to requests
    private String authHeaderName;
    private String authHeaderValue;
    // 2 s: fail fast if the webhook host TCP handshake does not complete promptly
    private int connectTimeoutMs = 2000;
    // 5 s: allow the remote handler a little time to process and acknowledge the body
    private int readTimeoutMs = 5000;
    // Number of additional attempts after the first failure before giving up; 0 means no retries
    private int maxRetries = 3;
    // Initial delay before the first retry (ms); doubles on each subsequent attempt (exponential backoff)
    private long retryInitialDelayMs = 1000;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getAuthHeaderName() {
        return authHeaderName;
    }

    public void setAuthHeaderName(String authHeaderName) {
        this.authHeaderName = authHeaderName;
    }

    public String getAuthHeaderValue() {
        return authHeaderValue;
    }

    public void setAuthHeaderValue(String authHeaderValue) {
        this.authHeaderValue = authHeaderValue;
    }

    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(int connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public int getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public void setReadTimeoutMs(int readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public long getRetryInitialDelayMs() {
        return retryInitialDelayMs;
    }

    public void setRetryInitialDelayMs(long retryInitialDelayMs) {
        this.retryInitialDelayMs = retryInitialDelayMs;
    }
}
