package com.opspilot.assistant.service.retrieval;

import com.opspilot.assistant.repository.DocumentChunkSearchRepository;
import com.opspilot.assistant.repository.RetrievedChunk;
import com.opspilot.assistant.service.embedding.EmbeddingService;
import com.opspilot.assistant.util.logging.RequestCorrelation;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ChunkRetrievalService {

    private static final Logger log = LoggerFactory.getLogger(ChunkRetrievalService.class);

    private final DocumentChunkSearchRepository searchRepository;
    private final EmbeddingService embeddingService;
    private final RerankerService rerankerService;

    public ChunkRetrievalService(
            DocumentChunkSearchRepository searchRepository,
            EmbeddingService embeddingService,
            RerankerService rerankerService
    ) {
        this.searchRepository = searchRepository;
        this.embeddingService = embeddingService;
        this.rerankerService = rerankerService;
    }

    public List<RetrievedChunk> retrieve(UUID tenantId, String question, int topK) {
        String profile = embeddingService.profile().id();
        List<Double> queryEmbedding = embeddingService.provider().embed(List.of(question)).getFirst();
        int recallLimit = Math.max(8, topK * 3);

        List<RetrievedChunk> vectorCandidates = searchRepository.searchTopVectorChunks(tenantId, profile, queryEmbedding, recallLimit);
        List<RetrievedChunk> lexicalCandidates = searchRepository.searchTopLexicalChunks(tenantId, profile, question, recallLimit);
        List<RetrievedChunk> merged = fuse(vectorCandidates, lexicalCandidates);
        List<RetrievedChunk> reranked = rerankerService.rerank(question, merged, topK);

        log.info(
                "assistant_chunk_retrieval_completed tenantId={} requestId={} vectorCandidates={} lexicalCandidates={} fusedCandidates={} selected={}",
                tenantId,
                RequestCorrelation.currentRequestId(),
                vectorCandidates.size(),
                lexicalCandidates.size(),
                merged.size(),
                reranked.size()
        );
        return reranked;
    }

    private List<RetrievedChunk> fuse(List<RetrievedChunk> vectorCandidates, List<RetrievedChunk> lexicalCandidates) {
        Map<String, CandidateAccumulator> merged = new LinkedHashMap<>();
        mergeCandidates(merged, vectorCandidates, true);
        mergeCandidates(merged, lexicalCandidates, false);
        List<RetrievedChunk> results = new ArrayList<>(merged.size());
        for (CandidateAccumulator candidate : merged.values()) {
            double retrievalScore = candidate.fusedScore();
            RetrievedChunk prototype = candidate.prototype();
            results.add(new RetrievedChunk(
                    prototype.documentId(),
                    prototype.documentName(),
                    prototype.chunkIndex(),
                    prototype.sectionTitle(),
                    prototype.chunkType(),
                    prototype.chunkText(),
                    candidate.vectorDistance(),
                    candidate.lexicalScore(),
                    round(retrievalScore),
                    0.0
            ));
        }
        return results.stream()
                .sorted(java.util.Comparator.comparingDouble(RetrievedChunk::retrievalScore).reversed())
                .toList();
    }

    private void mergeCandidates(Map<String, CandidateAccumulator> merged, List<RetrievedChunk> candidates, boolean vectorPass) {
        for (int index = 0; index < candidates.size(); index++) {
            RetrievedChunk candidate = candidates.get(index);
            String key = candidate.documentId() + ":" + candidate.chunkIndex();
            CandidateAccumulator accumulator = merged.computeIfAbsent(key, ignored -> new CandidateAccumulator(candidate));
            accumulator.addRank(index + 1, vectorPass, candidate);
        }
    }

    private double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }

    private static final class CandidateAccumulator {
        private final RetrievedChunk prototype;
        private double fusedScore;
        private Double vectorDistance;
        private Double lexicalScore;

        private CandidateAccumulator(RetrievedChunk prototype) {
            this.prototype = prototype;
        }

        private void addRank(int rank, boolean vectorPass, RetrievedChunk candidate) {
            fusedScore += 1.0 / (60.0 + rank);
            if (vectorPass) {
                vectorDistance = candidate.vectorDistance();
            } else {
                lexicalScore = candidate.lexicalScore();
            }
        }

        private RetrievedChunk prototype() {
            return prototype;
        }

        private double fusedScore() {
            return fusedScore;
        }

        private Double vectorDistance() {
            return vectorDistance;
        }

        private Double lexicalScore() {
            return lexicalScore;
        }
    }
}
