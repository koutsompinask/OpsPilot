package com.opspilot.aiorchestrator.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class DocumentChunkSearchRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public DocumentChunkSearchRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<RetrievedChunk> searchTopChunks(UUID tenantId, List<Double> queryEmbedding, int limit) {
        String sql = """
                SELECT d.id AS document_id,
                       d.original_filename AS document_name,
                       dc.chunk_index,
                       dc.chunk_text,
                       (dc.embedding <=> CAST(:embedding AS vector)) AS distance
                FROM knowledge.document_chunks dc
                INNER JOIN knowledge.documents d ON d.id = dc.document_id
                WHERE dc.tenant_id = :tenantId
                  AND d.tenant_id = :tenantId
                  AND d.status = 'READY'
                ORDER BY dc.embedding <=> CAST(:embedding AS vector)
                LIMIT :limit
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("tenantId", tenantId)
                .addValue("embedding", toVectorLiteral(queryEmbedding))
                .addValue("limit", limit);

        return jdbcTemplate.query(sql, params, (rs, rowNum) -> new RetrievedChunk(
                rs.getObject("document_id", UUID.class),
                rs.getString("document_name"),
                rs.getInt("chunk_index"),
                rs.getString("chunk_text"),
                rs.getDouble("distance")
        ));
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
