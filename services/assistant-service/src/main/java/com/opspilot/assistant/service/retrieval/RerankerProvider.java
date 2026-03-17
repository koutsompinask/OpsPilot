package com.opspilot.assistant.service.retrieval;

import java.util.List;

public interface RerankerProvider {

    String providerName();

    String modelName();

    List<RerankResult> rerank(String query, List<String> passages);
}
