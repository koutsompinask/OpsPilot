package com.opspilot.aiorchestrator.service.answering;

import com.opspilot.aiorchestrator.repository.RetrievedChunk;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AnswerService {

    private static final Logger log = LoggerFactory.getLogger(AnswerService.class);

    private final OpenAiAnswerGenerator openAiAnswerGenerator;
    private final LocalDeterministicAnswerGenerator localAnswerGenerator;

    public AnswerService(OpenAiAnswerGenerator openAiAnswerGenerator, LocalDeterministicAnswerGenerator localAnswerGenerator) {
        this.openAiAnswerGenerator = openAiAnswerGenerator;
        this.localAnswerGenerator = localAnswerGenerator;
    }

    public AnswerGenerationResult generate(String question, List<RetrievedChunk> chunks) {
        if (openAiAnswerGenerator.isConfigured()) {
            try {
                AnswerGenerationResult result = openAiAnswerGenerator.generate(question, chunks);
                log.info("ai_answer_generated provider={} chunkCount={}", result.provider(), chunks.size());
                return result;
            } catch (Exception ex) {
                log.warn("ai_answer_generation_fallback reason={} chunkCount={}", ex.getMessage(), chunks.size());
            }
        }

        AnswerGenerationResult fallback = localAnswerGenerator.generate(question, chunks);
        log.info("ai_answer_generated provider={} chunkCount={}", fallback.provider(), chunks.size());
        return fallback;
    }
}
