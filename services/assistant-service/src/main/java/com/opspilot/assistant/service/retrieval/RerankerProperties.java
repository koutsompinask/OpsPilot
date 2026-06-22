package com.opspilot.assistant.service.retrieval;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for the reranking subsystem, bound from the {@code assistant.reranker} prefix.
 *
 * <p>{@code candidateLimit} controls how many chunks are sent to the reranker per query (the top
 * candidates from hybrid retrieval). {@code maxPassageCharacters} truncates each passage before
 * sending it to guard against model token limits and to keep latency predictable.</p>
 */
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

    @Valid
    private final Gemini gemini = new Gemini();

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

    public Gemini getGemini() {
        return gemini;
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

    public static class Gemini {

        private String apiKey = "";

        @NotBlank
        private String model = "gemini-2.5-flash";

        @NotBlank
        private String url = "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions";

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

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }
}
