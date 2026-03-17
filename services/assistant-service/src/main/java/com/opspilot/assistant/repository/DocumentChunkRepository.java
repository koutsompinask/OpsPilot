package com.opspilot.assistant.repository;

import com.opspilot.assistant.service.chunking.TextChunk;
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
    public void replaceForDocument(UUID documentId, UUID tenantId, List<TextChunk> chunks, List<List<Double>> embeddings) {
        deleteForDocument(documentId, tenantId);
        String sql = """
                INSERT INTO assistant.document_chunks
                (id, document_id, tenant_id, chunk_index, chunk_text, section_title, chunk_type, embedding)
                VALUES (:id, :documentId, :tenantId, :chunkIndex, :chunkText, :sectionTitle, :chunkType, CAST(:embedding AS vector))
                """;

        if (chunks.isEmpty()) {
            return;
        }

        MapSqlParameterSource[] batch = new MapSqlParameterSource[chunks.size()];
        for (int i = 0; i < chunks.size(); i++) {
            batch[i] = new MapSqlParameterSource()
                    .addValue("id", UUID.randomUUID())
                    .addValue("documentId", documentId)
                    .addValue("tenantId", tenantId)
                    .addValue("chunkIndex", i)
                    .addValue("chunkText", chunks.get(i).text())
                    .addValue("sectionTitle", chunks.get(i).sectionTitle())
                    .addValue("chunkType", chunks.get(i).chunkType())
                    .addValue("embedding", PgVectorLiteral.from(embeddings.get(i)));
        }
        jdbcTemplate.batchUpdate(sql, batch);
    }

    @Transactional
    public void deleteForDocument(UUID documentId, UUID tenantId) {
        jdbcTemplate.update(
                "DELETE FROM assistant.document_chunks WHERE document_id = :documentId AND tenant_id = :tenantId",
                new MapSqlParameterSource()
                        .addValue("documentId", documentId)
                        .addValue("tenantId", tenantId)
        );
    }
}
