package com.opspilot.knowledgebase.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class DocumentChunkRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public DocumentChunkRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public void replaceForDocument(UUID documentId, UUID tenantId, List<String> chunks, List<List<Double>> embeddings) {
        deleteForDocument(documentId, tenantId);
        String sql = """
                INSERT INTO knowledge.document_chunks
                (id, document_id, tenant_id, chunk_index, chunk_text, embedding)
                VALUES (:id, :documentId, :tenantId, :chunkIndex, :chunkText, CAST(:embedding AS vector))
                """;

        for (int i = 0; i < chunks.size(); i++) {
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("id", UUID.randomUUID())
                    .addValue("documentId", documentId)
                    .addValue("tenantId", tenantId)
                    .addValue("chunkIndex", i)
                    .addValue("chunkText", chunks.get(i))
                    .addValue("embedding", toVectorLiteral(embeddings.get(i)));
            jdbcTemplate.update(sql, params);
        }
    }

    @Transactional
    public void deleteForDocument(UUID documentId, UUID tenantId) {
        jdbcTemplate.update(
                "DELETE FROM knowledge.document_chunks WHERE document_id = :documentId AND tenant_id = :tenantId",
                new MapSqlParameterSource()
                        .addValue("documentId", documentId)
                        .addValue("tenantId", tenantId)
        );
    }

    private String toVectorLiteral(List<Double> embedding) {
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < embedding.size(); i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(embedding.get(i));
        }
        builder.append(']');
        return builder.toString();
    }
}
