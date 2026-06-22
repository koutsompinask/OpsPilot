package com.opspilot.assistant.service.answering;

import com.opspilot.assistant.repository.RetrievedChunk;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Provider-agnostic facade for answer generation in the RAG pipeline.
 *
 * <p>Selects an {@link AnswerGenerator} implementation at runtime based on the
 * {@code ai.answer.provider} configuration property ({@code openai}, {@code ollama} /
 * {@code local-llm}, or {@code extractive}). If the selected provider fails at runtime,
 * the service falls back to the local extractive generator so that a response is always
 * returned to the caller.</p>
 */
@Service
public class AnswerService {

    private static final Logger log = LoggerFactory.getLogger(AnswerService.class);

    private final AnswerProperties properties;
    private final OpenAiAnswerGenerator openAiAnswerGenerator;
    private final OllamaAnswerGenerator ollamaAnswerGenerator;
    private final GeminiAnswerGenerator geminiAnswerGenerator;
    private final LocalDeterministicAnswerGenerator localAnswerGenerator;

    public AnswerService(
            AnswerProperties properties,
            OpenAiAnswerGenerator openAiAnswerGenerator,
            OllamaAnswerGenerator ollamaAnswerGenerator,
            GeminiAnswerGenerator geminiAnswerGenerator,
            LocalDeterministicAnswerGenerator localAnswerGenerator
    ) {
        this.properties = properties;
        this.openAiAnswerGenerator = openAiAnswerGenerator;
        this.ollamaAnswerGenerator = ollamaAnswerGenerator;
        this.geminiAnswerGenerator = geminiAnswerGenerator;
        this.localAnswerGenerator = localAnswerGenerator;
    }

    /**
     * Generates an answer for the given question using the configured provider, falling back
     * to the local extractive generator if the provider throws any exception.
     *
     * @param question the user's question (pre-normalized by the caller)
     * @param chunks   the ranked evidence chunks retrieved from the knowledge base
     * @return the generated answer along with provider metadata and the answer mode
     */
    public AnswerGenerationResult generate(String question, List<RetrievedChunk> chunks) {
        String provider = properties.getProvider() == null ? "extractive" : properties.getProvider().trim().toLowerCase();
        try {
            return switch (provider) {
                case "openai" -> generateWithOpenAi(question, chunks);
                case "ollama", "local-llm" -> generateWithOllama(question, chunks);
                case "gemini" -> generateWithGemini(question, chunks);
                // "extractive" or any unrecognised value falls through to the local generator
                default -> generateExtractive(question, chunks);
            };
        } catch (Exception ex) {
            log.warn("ai_answer_generation_fallback provider={} reason={} chunkCount={}", provider, ex.getMessage(), chunks.size());
            return generateExtractive(question, chunks);
        }
    }

    private AnswerGenerationResult generateWithOpenAi(String question, List<RetrievedChunk> chunks) {
        if (!openAiAnswerGenerator.isConfigured()) {
            throw new IllegalStateException("OpenAI answer provider is not configured");
        }
        AnswerGenerationResult result = openAiAnswerGenerator.generate(question, chunks);
        log.info("ai_answer_generated provider={} mode={} chunkCount={}", result.provider(), result.answerMode(), chunks.size());
        return result;
    }

    private AnswerGenerationResult generateWithOllama(String question, List<RetrievedChunk> chunks) {
        AnswerGenerationResult result = ollamaAnswerGenerator.generate(question, chunks);
        log.info("ai_answer_generated provider={} mode={} chunkCount={}", result.provider(), result.answerMode(), chunks.size());
        return result;
    }

    private AnswerGenerationResult generateWithGemini(String question, List<RetrievedChunk> chunks) {
        if (!geminiAnswerGenerator.isConfigured()) {
            throw new IllegalStateException("Gemini answer provider is not configured");
        }
        AnswerGenerationResult result = geminiAnswerGenerator.generate(question, chunks);
        log.info("ai_answer_generated provider={} mode={} chunkCount={}", result.provider(), result.answerMode(), chunks.size());
        return result;
    }

    private AnswerGenerationResult generateExtractive(String question, List<RetrievedChunk> chunks) {
        AnswerGenerationResult fallback = localAnswerGenerator.generate(question, chunks);
        log.info("ai_answer_generated provider={} mode={} chunkCount={}", fallback.provider(), fallback.answerMode(), chunks.size());
        return fallback;
    }
}
