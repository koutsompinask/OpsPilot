package com.opspilot.aiorchestrator.service.embedding;

import java.util.List;

public interface EmbeddingProvider {

    List<List<Double>> embed(List<String> inputs);
}
