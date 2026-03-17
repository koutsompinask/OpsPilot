package com.opspilot.assistant.service.embedding;

import java.util.List;

public interface EmbeddingProvider {

    EmbeddingProfile profile();

    List<List<Double>> embed(List<String> inputs);
}
