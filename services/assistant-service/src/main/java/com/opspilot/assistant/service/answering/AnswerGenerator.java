package com.opspilot.assistant.service.answering;

import com.opspilot.assistant.repository.RetrievedChunk;
import java.util.List;

public interface AnswerGenerator {

    AnswerGenerationResult generate(String question, List<RetrievedChunk> chunks);
}
