package com.employeehub.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/** AI settings, bound from employeehub.ai.* in application.yml (which reads environment variables). */
@ConfigurationProperties(prefix = "employeehub.ai")
public class AiProperties {

    /** Which implementation to use. Only "anthropic" exists today. */
    private String provider = "anthropic";
    /** Provider API key. Blank means the Ask feature is switched off. */
    private String apiKey;
    private String model = "claude-opus-5-5";
    /** Reasoning effort (low/medium/high/xhigh/max). Turning a question into filters is simple, so low is enough. */
    private String effort = "low";
    /** Per-attempt timeout for one provider call. */
    private Duration timeout = Duration.ofSeconds(20);
    /** Retries the SDK makes for timeouts, 429s, and 5xx errors. */
    private int maxRetries = 2;

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getEffort() {
        return effort;
    }

    public void setEffort(String effort) {
        this.effort = effort;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public void setTimeout(Duration timeout) {
        this.timeout = timeout;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }
}
