package com.opspilot.assistant.service.retrieval;

import com.opspilot.assistant.repository.RetrievedChunk;
import com.opspilot.assistant.util.logging.RequestCorrelation;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Provider-aware reranking service that re-scores and selects the top-K chunks from
 * the fused retrieval candidates.
 *
 * <p>At construction the configured {@code reranker.provider} property determines which
 * {@link RerankerProvider} implementation is wired in (currently only {@code tei} is
 * supported). When the neural reranker is disabled or fails at runtime, the service
 * transparently falls back to the {@link HeuristicReranker}, which applies token-overlap
 * and domain-specific signal boosts without any external network call.</p>
 *
 * <p>The passage sent to the reranker for each chunk is constructed as
 * {@code sectionTitle + "\n" + chunkText}, truncated to {@code reranker.max-passage-characters}
 * to stay within the reranker model's token limit.</p>
 */
@Service
public class RerankerService {

    private static final Logger log = LoggerFactory.getLogger(RerankerService.class);

    private final RerankerProperties properties;
    private final RerankerProvider provider;
    private final HeuristicReranker heuristicReranker;

    public RerankerService(
            RerankerProperties properties,
            TeiRerankerProvider teiRerankerProvider,
            HeuristicReranker heuristicReranker
    ) {
        this.properties = properties;
        this.provider = switch (properties.getProvider().toLowerCase(Locale.ROOT)) {
            case "tei" -> teiRerankerProvider;
            default -> throw new IllegalArgumentException("Unsupported reranker provider: " + properties.getProvider());
        };
        this.heuristicReranker = heuristicReranker;
    }

    /**
     * Re-scores and trims the candidate list to the top-{@code topK} most relevant chunks.
     *
     * <p>If the neural reranker is disabled or throws an exception, the heuristic fallback
     * is applied automatically so the method always returns a ranked result.</p>
     *
     * @param question   the user's question used as the reference text for relevance scoring
     * @param candidates the fused retrieval candidates to re-score
     * @param topK       the maximum number of chunks to return
     * @return the top-K chunks sorted by reranker score descending, with ties broken by retrieval score
     */
    public List<RetrievedChunk> rerank(String question, List<RetrievedChunk> candidates, int topK) {
        if (candidates.isEmpty()) {
            return List.of();
        }

        if (!properties.isEnabled()) {
            return applyFallback(question, candidates, topK, "disabled");
        }

        // Cap the number of passages sent to the reranker at the configured candidateLimit
        // to control latency; the RRF-fused order already surfaces the best candidates first.
        List<RetrievedChunk> limitedCandidates = candidates.stream()
                .limit(Math.max(topK, properties.getCandidateLimit()))
                .toList();
        List<String> passages = limitedCandidates.stream()
                .map(this::toPassage)
                .toList();

        long startedAt = System.nanoTime();
        try {
            log.info(
                    "assistant_reranker_request_started provider={} model={} requestId={} candidateCount={}",
                    provider.providerName(),
                    provider.modelName(),
                    RequestCorrelation.currentRequestId(),
                    limitedCandidates.size()
            );

            List<RerankResult> scores = provider.rerank(question, passages);
            long elapsedMs = (System.nanoTime() - startedAt) / 1_000_000;

            List<RetrievedChunk> reranked = mergeScores(limitedCandidates, scores, topK);
            log.info(
                    "assistant_reranker_request_completed provider={} model={} requestId={} candidateCount={} selectedCount={} latencyMs={} topScore={}",
                    provider.providerName(),
                    provider.modelName(),
                    RequestCorrelation.currentRequestId(),
                    limitedCandidates.size(),
                    reranked.size(),
                    elapsedMs,
                    reranked.isEmpty() ? 0.0 : reranked.getFirst().rerankerScore()
            );
            return reranked;
        } catch (Exception ex) {
            long elapsedMs = (System.nanoTime() - startedAt) / 1_000_000;
            log.warn(
                    "assistant_reranker_request_failed provider={} model={} requestId={} candidateCount={} latencyMs={} reason={} fallback=heuristic",
                    provider.providerName(),
                    provider.modelName(),
                    RequestCorrelation.currentRequestId(),
                    limitedCandidates.size(),
                    elapsedMs,
                    ex.getMessage()
            );
            return applyFallback(question, limitedCandidates, topK, "provider_failure");
        }
    }

    /**
     * Calls the reranker with a fixed two-passage sample to verify connectivity and model behaviour.
     *
     * <p>Used by {@link RerankerStartupValidator} at application startup to fail fast if the
     * reranker endpoint is unreachable or returns unexpected results.</p>
     *
     * @return the raw reranker scores for the two sample passages
     */
    public List<RerankResult> validateSample() {
        return provider.rerank(
                "What time is check-in and check-out?",
                List.of(
                        "Front Desk Operations\nCheck-in time starts at 15:00 local time. Check-out time is 11:00 local time.",
                        "Breakfast is served from 07:00 to 10:30 in the dining room."
                )
        );
    }

    /**
     * Returns {@code true} if the neural reranker is enabled in configuration.
     * When disabled, every rerank call falls through to the heuristic reranker.
     */
    public boolean isEnabled() {
        return properties.isEnabled();
    }

    /** Returns the name of the active reranker provider (e.g. {@code tei}). */
    public String providerName() {
        return provider.providerName();
    }

    /** Returns the model identifier reported by the active reranker provider. */
    public String modelName() {
        return provider.modelName();
    }

    private List<RetrievedChunk> mergeScores(List<RetrievedChunk> candidates, List<RerankResult> scores, int topK) {
        List<RetrievedChunk> reranked = new ArrayList<>(candidates.size());
        for (RetrievedChunk candidate : candidates) {
            reranked.add(candidate.withRerankerScore(0.0));
        }

        for (RerankResult score : scores) {
            if (score.index() < 0 || score.index() >= reranked.size()) {
                continue;
            }
            RetrievedChunk candidate = reranked.get(score.index());
            reranked.set(score.index(), candidate.withRerankerScore(round(score.score())));
        }

        return reranked.stream()
                .sorted(Comparator.comparingDouble(RetrievedChunk::rerankerScore).reversed()
                        .thenComparing(Comparator.comparingDouble(RetrievedChunk::retrievalScore).reversed()))
                .limit(topK)
                .toList();
    }

    private List<RetrievedChunk> applyFallback(String question, List<RetrievedChunk> candidates, int topK, String reason) {
        List<RerankResult> scores = heuristicReranker.rerank(question, candidates);
        List<RetrievedChunk> reranked = mergeScores(candidates, scores, topK);
        log.info(
                "assistant_reranker_fallback_applied reason={} requestId={} candidateCount={} selectedCount={}",
                reason,
                RequestCorrelation.currentRequestId(),
                candidates.size(),
                reranked.size()
        );
        return reranked;
    }

    private String toPassage(RetrievedChunk chunk) {
        String sectionTitle = chunk.sectionTitle() == null ? "" : chunk.sectionTitle().trim();
        String chunkText = chunk.chunkText() == null ? "" : chunk.chunkText().trim();
        String combined = sectionTitle.isBlank() ? chunkText : sectionTitle + "\n" + chunkText;
        if (combined.length() <= properties.getMaxPassageCharacters()) {
            return combined;
        }
        return combined.substring(0, properties.getMaxPassageCharacters()).trim();
    }

    private double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }
}
