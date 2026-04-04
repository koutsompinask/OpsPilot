package com.opspilot.assistant.service.retrieval;

import java.util.List;

/**
 * Contract for reranking back-ends that score (query, passage) pairs for relevance.
 *
 * <p>Implementations send the query and a list of candidate passages to an external model
 * and return {@link RerankResult} entries ordered by descending relevance score.
 * The active implementation is selected at startup via {@code assistant.reranker.provider}
 * and delegated to from {@link RerankerService}.</p>
 */
public interface RerankerProvider {

    /**
     * Returns the short provider identifier (e.g. {@code tei}).
     *
     * @return the provider name used in logs and configuration
     */
    String providerName();

    /**
     * Returns the name of the reranker model currently in use.
     *
     * @return the model identifier (e.g. {@code BAAI/bge-reranker-base})
     */
    String modelName();

    /**
     * Reranks a list of passages against the given query and returns scored results.
     *
     * @param query    the user's question or search query
     * @param passages the candidate passages to score; must be non-null and non-empty
     * @return results sorted by descending relevance score, one entry per input passage
     */
    List<RerankResult> rerank(String query, List<String> passages);
}
