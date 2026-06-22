package com.opspilot.assistant.service.answering;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class LlmJsonTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void extractsSimpleStringField() {
        String json = "{\"answer\":\"Check-in is at 15:00.\",\"reasoningSummary\":\"From section.\"}";
        assertThat(LlmJson.extractField(objectMapper, json, "answer")).isEqualTo("Check-in is at 15:00.");
        assertThat(LlmJson.extractField(objectMapper, json, "reasoningSummary")).isEqualTo("From section.");
    }

    @Test
    void stripsJsonFenceBeforeParsing() {
        String fenced = "```json\n{\"answer\":\"Yes.\",\"reasoningSummary\":\"Ok.\"}\n```";
        assertThat(LlmJson.extractField(objectMapper, fenced, "answer")).isEqualTo("Yes.");
    }

    @Test
    void stripsPlainFence() {
        String fenced = "```\n{\"answer\":\"No.\",\"reasoningSummary\":\"Ok.\"}\n```";
        assertThat(LlmJson.extractField(objectMapper, fenced, "answer")).isEqualTo("No.");
    }

    @Test
    void returnsNullForMissingField() {
        String json = "{\"answer\":\"Yes.\"}";
        assertThat(LlmJson.extractField(objectMapper, json, "reasoningSummary")).isNull();
    }

    @Test
    void returnsNullForMalformedInput() {
        assertThat(LlmJson.extractField(objectMapper, "not json at all", "answer")).isNull();
    }

    @Test
    void returnsNullForBlankContent() {
        assertThat(LlmJson.extractField(objectMapper, "   ", "answer")).isNull();
        assertThat(LlmJson.extractField(objectMapper, null, "answer")).isNull();
    }

    @Test
    void handlesQuoteInsideValue() {
        String json = "{\"answer\":\"It's 15:00, according to \\\"policy\\\".\",\"reasoningSummary\":\"From doc.\"}";
        String answer = LlmJson.extractField(objectMapper, json, "answer");
        assertThat(answer).contains("15:00");
    }
}
