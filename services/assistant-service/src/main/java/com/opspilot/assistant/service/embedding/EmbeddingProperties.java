package com.opspilot.assistant.service.embedding;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for the embedding subsystem, bound from the {@code assistant.embedding} prefix.
 *
 * <p>Contains nested blocks for each supported provider ({@code stub}, {@code openai}, {@code tei},
 * {@code ollama}). Only the block matching the active {@link #getProvider()} value is actually used
 * at runtime; the others are parsed but ignored.</p>
 */
@Validated
@ConfigurationProperties(prefix = "assistant.embedding")
public class EmbeddingProperties {

    @NotBlank
    private String provider = "stub";

    @Min(1)
    private int requestTimeoutMs = 10000;

    private boolean validateOnStartup = true;

    @Valid
    private final Stub stub = new Stub();

    @Valid
    private final OpenAi openai = new OpenAi();

    @Valid
    private final Tei tei = new Tei();

    @Valid
    private final Ollama ollama = new Ollama();

    @Valid
    private final Gemini gemini = new Gemini();

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

    public Stub getStub() {
        return stub;
    }

    public OpenAi getOpenai() {
        return openai;
    }

    public Tei getTei() {
        return tei;
    }

    public Ollama getOllama() {
        return ollama;
    }

    public Gemini getGemini() {
        return gemini;
    }

    public static class Stub {

        @Min(1)
        private int dimensions = 1536;

        public int getDimensions() {
            return dimensions;
        }

        public void setDimensions(int dimensions) {
            this.dimensions = dimensions;
        }
    }

    public static class OpenAi {

        private String apiKey = "";

        @NotBlank
        private String model = "text-embedding-3-small";

        @NotBlank
        private String url = "https://api.openai.com/v1/embeddings";

        @Min(1)
        private int dimensions = 1536;

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

        public int getDimensions() {
            return dimensions;
        }

        public void setDimensions(int dimensions) {
            this.dimensions = dimensions;
        }
    }

    public static class Tei {

        @NotBlank
        private String model = "BAAI/bge-small-en-v1.5";

        @NotBlank
        private String url = "http://localhost:8091/v1/embeddings";

        @Min(1)
        private int dimensions = 384;

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

        public int getDimensions() {
            return dimensions;
        }

        public void setDimensions(int dimensions) {
            this.dimensions = dimensions;
        }
    }

    public static class Ollama {

        @NotBlank
        private String model = "nomic-embed-text";

        @NotBlank
        private String url = "http://localhost:11434/api/embed";

        @Min(1)
        private int dimensions = 768;

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

        public int getDimensions() {
            return dimensions;
        }

        public void setDimensions(int dimensions) {
            this.dimensions = dimensions;
        }
    }

    public static class Gemini {

        private String apiKey = "";

        @NotBlank
        private String model = "gemini-embedding-001";

        @NotBlank
        private String url = "https://generativelanguage.googleapis.com/v1beta/openai/embeddings";

        @Min(1)
        private int dimensions = 1536;

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

        public int getDimensions() {
            return dimensions;
        }

        public void setDimensions(int dimensions) {
            this.dimensions = dimensions;
        }
    }
}
