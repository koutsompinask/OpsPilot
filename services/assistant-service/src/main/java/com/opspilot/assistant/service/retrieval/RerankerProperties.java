package com.opspilot.assistant.service.retrieval;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "assistant.reranker")
public class RerankerProperties {

    private boolean enabled = true;

    @NotBlank
    private String provider = "tei";

    @Min(1)
    private int requestTimeoutMs = 10000;

    private boolean validateOnStartup = true;

    @Min(1)
    private int candidateLimit = 12;

    @Min(32)
    private int maxPassageCharacters = 1200;

    @Valid
    private final Tei tei = new Tei();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public int getRequestTimeoutMs() {
        return requestTimeoutMs;
    }

    public void setRequestTimeoutMs(int requestTimeoutMs) {
        this.requestTimeoutMs = requestTimeoutMs;
    }

    public boolean isValidateOnStartup() {
        return validateOnStartup;
    }

    public void setValidateOnStartup(boolean validateOnStartup) {
        this.validateOnStartup = validateOnStartup;
    }

    public int getCandidateLimit() {
        return candidateLimit;
    }

    public void setCandidateLimit(int candidateLimit) {
        this.candidateLimit = candidateLimit;
    }

    public int getMaxPassageCharacters() {
        return maxPassageCharacters;
    }

    public void setMaxPassageCharacters(int maxPassageCharacters) {
        this.maxPassageCharacters = maxPassageCharacters;
    }

    public Tei getTei() {
        return tei;
    }

    public static class Tei {

        @NotBlank
        private String model = "BAAI/bge-reranker-base";

        @NotBlank
        private String url = "http://localhost:8092/rerank";

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }
}
