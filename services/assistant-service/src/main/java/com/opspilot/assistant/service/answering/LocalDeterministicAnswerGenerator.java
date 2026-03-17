package com.opspilot.assistant.service.answering;

import com.opspilot.assistant.repository.RetrievedChunk;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class LocalDeterministicAnswerGenerator implements AnswerGenerator {

    private static final Pattern TOKEN_SPLIT = Pattern.compile("[^a-z0-9:]+");
    private static final Pattern SENTENCE_SPLIT = Pattern.compile("(?<=[.!?])\\s+|\\n+");
    private static final Set<String> STOPWORDS = Set.of(
            "a", "an", "and", "are", "as", "at", "be", "by", "can", "do", "for", "from", "how", "i", "if",
            "in", "is", "it", "of", "on", "or", "our", "please", "the", "their", "there", "to", "was", "we",
            "what", "when", "where", "which", "who", "with", "your"
    );

    @Override
    public AnswerGenerationResult generate(String question, List<RetrievedChunk> chunks) {
        if (chunks.isEmpty()) {
            return new AnswerGenerationResult(
                    "I could not find enough relevant document context to answer this confidently.",
                    "No relevant evidence was retrieved from the indexed documents.",
                    "extractive",
                    "insufficient-evidence"
            );
        }

        List<ScoredSentence> evidence = collectEvidence(question, chunks);
        if (evidence.isEmpty()) {
            return new AnswerGenerationResult(
                    "I found related documents, but not enough explicit evidence to answer that directly.",
                    "The retrieved chunks were too weakly aligned with the question to produce a grounded answer.",
                    "extractive",
                    "insufficient-evidence"
            );
        }

        List<ScoredSentence> topEvidence = evidence.stream()
                .sorted(Comparator.comparingDouble(ScoredSentence::score).reversed())
                .limit(3)
                .toList();

        String answer = buildAnswer(topEvidence);
        String reasoningSummary = buildReasoningSummary(topEvidence);
        return new AnswerGenerationResult(answer, reasoningSummary, "extractive", "extractive-grounded");
    }

    private List<ScoredSentence> collectEvidence(String question, List<RetrievedChunk> chunks) {
        Set<String> questionTokens = tokenize(question);
        List<ScoredSentence> evidence = new ArrayList<>();

        for (RetrievedChunk chunk : chunks) {
            String[] sentences = SENTENCE_SPLIT.split(chunk.chunkText() == null ? "" : chunk.chunkText().trim());
            for (String rawSentence : sentences) {
                String sentence = rawSentence.trim();
                if (sentence.isBlank()) {
                    continue;
                }

                Set<String> sentenceTokens = tokenize(sentence);
                int overlap = overlap(questionTokens, sentenceTokens);
                if (!questionTokens.isEmpty() && overlap == 0 && chunk.rerankerScore() < 0.35) {
                    continue;
                }

                double score = chunk.rerankerScore() + overlap * 0.35;
                evidence.add(new ScoredSentence(sentence, chunk, score));
            }
        }
        return evidence;
    }

    private String buildAnswer(List<ScoredSentence> evidence) {
        LinkedHashSet<String> uniqueSentences = new LinkedHashSet<>();
        for (ScoredSentence item : evidence) {
            uniqueSentences.add(trimSentence(item.sentence()));
        }
        return String.join(" ", uniqueSentences);
    }

    private String buildReasoningSummary(List<ScoredSentence> evidence) {
        LinkedHashSet<String> reasons = new LinkedHashSet<>();
        for (ScoredSentence item : evidence) {
            String section = item.chunk().sectionTitle() == null || item.chunk().sectionTitle().isBlank()
                    ? item.chunk().documentName()
                    : item.chunk().sectionTitle();
            reasons.add("Matched " + section + " in " + item.chunk().documentName());
        }
        return String.join("; ", reasons);
    }

    private Set<String> tokenize(String text) {
        LinkedHashSet<String> tokens = new LinkedHashSet<>();
        for (String token : TOKEN_SPLIT.split(text == null ? "" : text.toLowerCase(Locale.ROOT))) {
            if (token.isBlank() || STOPWORDS.contains(token)) {
                continue;
            }
            tokens.add(token);
        }
        return tokens;
    }

    private int overlap(Set<String> left, Set<String> right) {
        int overlap = 0;
        for (String token : left) {
            if (right.contains(token)) {
                overlap++;
            }
        }
        return overlap;
    }

    private String trimSentence(String sentence) {
        if (sentence.length() <= 220) {
            return sentence;
        }
        return sentence.substring(0, 217).trim() + "...";
    }

    private record ScoredSentence(String sentence, RetrievedChunk chunk, double score) {
    }
}
