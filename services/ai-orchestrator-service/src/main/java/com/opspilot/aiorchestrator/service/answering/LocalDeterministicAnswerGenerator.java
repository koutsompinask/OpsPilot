package com.opspilot.aiorchestrator.service.answering;

import com.opspilot.aiorchestrator.repository.RetrievedChunk;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class LocalDeterministicAnswerGenerator implements AnswerGenerator {

    @Override
    public AnswerGenerationResult generate(String question, List<RetrievedChunk> chunks) {
        if (chunks.isEmpty()) {
            return new AnswerGenerationResult(
                    "I could not find enough relevant document context to answer this confidently.",
                    "local"
            );
        }

        String citations = chunks.stream()
                .limit(3)
                .map(chunk -> "[chunk-" + chunk.chunkIndex() + "] " + (chunk.chunkText() == null ? "" : chunk.chunkText().trim()))
                .collect(Collectors.joining("\n\n"));

        String answer = "Based on the available documents, here is the best grounded answer:\n\n" + citations;
        return new AnswerGenerationResult(answer, "local");
    }
}
