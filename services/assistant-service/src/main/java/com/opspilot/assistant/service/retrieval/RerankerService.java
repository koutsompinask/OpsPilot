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

    public List<RetrievedChunk> rerank(String question, List<RetrievedChunk> candidates, int topK) {
        if (candidates.isEmpty()) {
            return List.of();
        }

        if (!properties.isEnabled()) {
            return applyFallback(question, candidates, topK, "disabled");
        }

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

    public List<RerankResult> validateSample() {
        return provider.rerank(
                "What time is check-in and check-out?",
                List.of(
                        "Front Desk Operations\nCheck-in time starts at 15:00 local time. Check-out time is 11:00 local time.",
                        "Breakfast is served from 07:00 to 10:30 in the dining room."
                )
        );
    }

    public boolean isEnabled() {
        return properties.isEnabled();
    }

    public String providerName() {
        return provider.providerName();
    }

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
