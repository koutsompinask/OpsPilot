package com.opspilot.assistant.service.answering;

/**
 * The result of an answer generation call, carrying the generated text and metadata
 * used to populate the {@link com.opspilot.assistant.dto.ChatAskResponse}.
 *
 * @param answer           the generated answer text to return to the user
 * @param reasoningSummary a brief explanation of which evidence was used to construct the answer
 * @param provider         identifies the generator that produced this result (e.g. {@code "ollama"}, {@code "openai"}, {@code "extractive"})
 * @param answerMode       describes the generation strategy (e.g. {@code "llm-grounded"}, {@code "extractive-grounded"}, {@code "insufficient-evidence"})
 */
public record AnswerGenerationResult(
        String answer,
        String reasoningSummary,
        String provider,
        String answerMode
) {
}
