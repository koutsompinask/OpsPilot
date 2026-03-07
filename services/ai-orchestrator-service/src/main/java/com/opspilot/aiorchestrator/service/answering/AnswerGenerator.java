package com.opspilot.aiorchestrator.service.answering;

import com.opspilot.aiorchestrator.repository.RetrievedChunk;
import java.util.List;

public interface AnswerGenerator {

    AnswerGenerationResult generate(String question, List<RetrievedChunk> chunks);
}
