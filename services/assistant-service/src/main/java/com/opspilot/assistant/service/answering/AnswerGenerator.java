package com.opspilot.assistant.service.answering;

import com.opspilot.assistant.repository.RetrievedChunk;
import java.util.List;

/**
 * Provider interface for LLM-backed or extractive answer generation.
 *
 * Implementations receive the user's question and the top-ranked retrieved chunks,
 * and return a structured result containing the answer text, a reasoning summary,
 * the provider name, and the answer mode (e.g. {@code "llm-grounded"} or
 * {@code "extractive-grounded"}).
 *
 * Available implementations: {@link OllamaAnswerGenerator}, {@link OpenAiAnswerGenerator},
 * {@link LocalDeterministicAnswerGenerator}.
 */
public interface AnswerGenerator {

    /**
     * Generates a grounded answer to the question using the supplied evidence chunks.
     *
     * @param question the user's question (already normalised)
     * @param chunks   the top-ranked chunks to use as evidence context
     * @return a structured answer result
     */
    AnswerGenerationResult generate(String question, List<RetrievedChunk> chunks);
}
