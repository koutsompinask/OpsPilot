package com.opspilot.assistant.service.answering;

public record AnswerGenerationResult(
        String answer,
        String reasoningSummary,
        String provider,
        String answerMode
) {
}
