package com.opspilot.assistant.service.retrieval;

import com.opspilot.assistant.repository.RetrievedChunk;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * A rule-based fallback reranker used when the TEI reranker is disabled or unavailable.
 *
 * Scores each candidate chunk using a weighted combination of:
 * <ul>
 *   <li>Token overlap between the question and chunk text (after stopword removal)</li>
 *   <li>Exact phrase matching for compound terms (e.g. "check-in and check-out")</li>
 *   <li>Domain-specific signals: time expressions, check-in/check-out mentions</li>
 *   <li>Section title relevance</li>
 *   <li>The original vector distance and lexical score from retrieval</li>
 * </ul>
 * Raw scores are normalised via a sigmoid function to produce values in (0, 1).
 */
@Component
public class HeuristicReranker {

    private static final Pattern TOKEN_SPLIT = Pattern.compile("[^a-z0-9:]+");
    private static final Set<String> STOPWORDS = Set.of(
            "a", "an", "and", "are", "as", "at", "be", "by", "can", "do", "for", "from", "how", "i", "if",
            "in", "is", "it", "of", "on", "or", "our", "please", "the", "their", "there", "to", "was", "what",
            "when", "where", "which", "who", "with", "your"
    );

    /**
     * Reranks the given candidate chunks relative to the question using heuristic scoring.
     *
     * @param question   the user's original question
     * @param candidates the candidate chunks to score, in their original retrieval order
     * @return a list of {@link RerankResult} objects containing each chunk's original index and normalised score
     */
    public List<RerankResult> rerank(String question, List<RetrievedChunk> candidates) {
        Set<String> questionTokens = tokenize(question);
        return java.util.stream.IntStream.range(0, candidates.size())
                .mapToObj(index -> new RerankResult(index, normalize(score(question, questionTokens, candidates.get(index)))))
                .toList();
    }

    private double score(String question, Set<String> questionTokens, RetrievedChunk candidate) {
        Set<String> chunkTokens = tokenize(candidate.chunkText() + " " + candidate.sectionTitle());
        int overlap = 0;
        for (String token : questionTokens) {
            if (chunkTokens.contains(token)) {
                overlap++;
            }
        }

        String lowerQuestion = question.toLowerCase(Locale.ROOT);
        String lowerChunk = candidate.chunkText().toLowerCase(Locale.ROOT);
        boolean asksForTime = lowerQuestion.contains("time") || lowerQuestion.contains("hours");
        boolean hasTime = lowerChunk.matches("(?s).*(\\b\\d{1,2}:\\d{2}\\b).*");
        boolean asksCheckIn = lowerQuestion.contains("check-in") || lowerQuestion.contains("check in");
        boolean asksCheckOut = lowerQuestion.contains("check-out") || lowerQuestion.contains("check out");
        boolean mentionsCheckIn = lowerChunk.contains("check-in") || lowerChunk.contains("check in");
        boolean mentionsCheckOut = lowerChunk.contains("check-out") || lowerChunk.contains("check out");

        double score = candidate.retrievalScore();
        score += overlap * 0.45;
        score += containsExactPhrase(lowerQuestion, lowerChunk) ? 0.8 : 0.0;
        score += asksForTime && hasTime ? 0.45 : 0.0;
        score += asksCheckIn && mentionsCheckIn ? 0.55 : 0.0;
        score += asksCheckOut && mentionsCheckOut ? 0.55 : 0.0;
        score += candidate.sectionTitle() != null && lowerQuestion.contains(candidate.sectionTitle().toLowerCase(Locale.ROOT)) ? 0.35 : 0.0;
        score += "heading".equals(candidate.chunkType()) ? -0.15 : 0.0;
        score += candidate.lexicalScore() == null ? 0.0 : Math.min(candidate.lexicalScore(), 1.5);
        score += candidate.vectorDistance() == null ? 0.0 : Math.max(0.0, 1.0 - candidate.vectorDistance());
        return score;
    }

    private boolean containsExactPhrase(String question, String chunk) {
        if (question.contains("check-in and check-out")) {
            return chunk.contains("check-in") && chunk.contains("check-out");
        }
        return false;
    }

    private Set<String> tokenize(String text) {
        LinkedHashSet<String> tokens = new LinkedHashSet<>();
        for (String token : TOKEN_SPLIT.split(text == null ? "" : text.toLowerCase(Locale.ROOT))) {
            if (token.isBlank() || STOPWORDS.contains(token)) {
                continue;
            }
            if ("check".equals(token)) {
                tokens.add("check");
                continue;
            }
            if ("in".equals(token) || "out".equals(token)) {
                continue;
            }
            tokens.add(token);
        }
        String normalized = text == null ? "" : text.toLowerCase(Locale.ROOT);
        if (normalized.contains("check-in") || normalized.contains("check in")) {
            tokens.add("checkin");
        }
        if (normalized.contains("check-out") || normalized.contains("check out")) {
            tokens.add("checkout");
        }
        return tokens;
    }

    private double normalize(double rawScore) {
        if (rawScore <= 0.0) {
            return 0.0;
        }
        return 1.0 / (1.0 + Math.exp(-rawScore));
    }
}
