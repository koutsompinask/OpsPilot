package com.opspilot.assistant.service.answering;

import com.opspilot.assistant.repository.RetrievedChunk;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AnswerService {

    private static final Logger log = LoggerFactory.getLogger(AnswerService.class);

    private final AnswerProperties properties;
    private final OpenAiAnswerGenerator openAiAnswerGenerator;
    private final OllamaAnswerGenerator ollamaAnswerGenerator;
    private final LocalDeterministicAnswerGenerator localAnswerGenerator;

    public AnswerService(
            AnswerProperties properties,
            OpenAiAnswerGenerator openAiAnswerGenerator,
            OllamaAnswerGenerator ollamaAnswerGenerator,
            LocalDeterministicAnswerGenerator localAnswerGenerator
    ) {
        this.properties = properties;
        this.openAiAnswerGenerator = openAiAnswerGenerator;
        this.ollamaAnswerGenerator = ollamaAnswerGenerator;
        this.localAnswerGenerator = localAnswerGenerator;
    }

    public AnswerGenerationResult generate(String question, List<RetrievedChunk> chunks) {
        String provider = properties.getProvider() == null ? "extractive" : properties.getProvider().trim().toLowerCase();
        try {
            return switch (provider) {
                case "openai" -> generateWithOpenAi(question, chunks);
                case "ollama", "local-llm" -> generateWithOllama(question, chunks);
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

    private AnswerGenerationResult generateExtractive(String question, List<RetrievedChunk> chunks) {
        AnswerGenerationResult fallback = localAnswerGenerator.generate(question, chunks);
        log.info("ai_answer_generated provider={} mode={} chunkCount={}", fallback.provider(), fallback.answerMode(), chunks.size());
        return fallback;
    }
}
