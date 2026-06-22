package com.opspilot.assistant.service.answering;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility for parsing JSON fields from LLM responses that may contain markdown fences,
 * extra whitespace, or non-string node types. Returns {@code null} on any parse failure
 * so callers can fall back gracefully rather than crashing.
 */
final class LlmJson {

    private static final Logger log = LoggerFactory.getLogger(LlmJson.class);

    private LlmJson() {
    }

    /**
     * Extracts a string value for {@code fieldName} from {@code content}.
     * Strips leading/trailing {@code ```json} or {@code ```} fences before parsing with Jackson.
     *
     * @param objectMapper shared Jackson mapper
     * @param content      raw LLM response text
     * @param fieldName    JSON key to extract
     * @return the field value as a string, or {@code null} on missing key or any parse error
     */
    static String extractField(ObjectMapper objectMapper, String content, String fieldName) {
        if (content == null || content.isBlank()) {
            return null;
        }
        String cleaned = stripFences(content);
        try {
            JsonNode root = objectMapper.readTree(cleaned);
            JsonNode field = root.get(fieldName);
            if (field == null || field.isNull()) {
                return null;
            }
            return field.isTextual() ? field.asText() : field.toString();
        } catch (Exception ex) {
            log.debug("llm_json_parse_failed field={} reason={}", fieldName, ex.getMessage());
            return null;
        }
    }

    private static String stripFences(String content) {
        String s = content.strip();
        if (s.startsWith("```")) {
            int newline = s.indexOf('\n');
            s = newline > 0 ? s.substring(newline + 1) : s.substring(3);
        }
        if (s.endsWith("```")) {
            s = s.substring(0, s.lastIndexOf("```")).stripTrailing();
        }
        return s;
    }
}
