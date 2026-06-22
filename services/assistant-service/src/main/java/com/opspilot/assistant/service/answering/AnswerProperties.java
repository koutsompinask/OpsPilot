package com.opspilot.assistant.service.answering;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the answer-generation provider, bound from {@code ai.answer.*}.
 *
 * {@code provider} selects the active {@link AnswerGenerator} implementation:
 * {@code "ollama"} (default), {@code "openai"}, or {@code "local"} (deterministic stub).
 * Provider-specific settings are nested under {@code ai.answer.ollama.*} and
 * {@code ai.answer.openai.*}.
 */
@ConfigurationProperties(prefix = "ai.answer")
public class AnswerProperties {

    private String provider = "ollama";
    private long requestTimeoutMs = 20000;
    private final OpenAi openai = new OpenAi();
    private final Ollama ollama = new Ollama();
    private final Gemini gemini = new Gemini();

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public long getRequestTimeoutMs() {
        return requestTimeoutMs;
    }

    public void setRequestTimeoutMs(long requestTimeoutMs) {
        this.requestTimeoutMs = requestTimeoutMs;
    }

    public OpenAi getOpenai() {
        return openai;
    }

    public Ollama getOllama() {
        return ollama;
    }

    public Gemini getGemini() {
        return gemini;
    }

    public static class OpenAi {
        private String apiKey = "";
        private String model = "gpt-4o-mini";
        private String url = "https://api.openai.com/v1/chat/completions";

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

    public static class Ollama {
        private String model = "qwen2.5:7b-instruct";
        private String url = "http://localhost:11434/api/generate";

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
        private String model = "gemini-2.5-flash";
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
