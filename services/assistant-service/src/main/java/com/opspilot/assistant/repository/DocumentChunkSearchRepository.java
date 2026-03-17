package com.opspilot.assistant.repository;

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

    public List<RetrievedChunk> searchTopVectorChunks(UUID tenantId, String embeddingProfile, List<Double> queryEmbedding, int limit) {
        String sql = """
                SELECT d.id AS document_id,
                       d.original_filename AS document_name,
                       dc.chunk_index,
                       dc.section_title,
                       dc.chunk_type,
                       dc.chunk_text,
                       (dc.embedding <=> CAST(:embedding AS vector)) AS vector_distance
                FROM assistant.document_chunks dc
                INNER JOIN assistant.documents d ON d.id = dc.document_id
                WHERE dc.tenant_id = :tenantId
                  AND d.tenant_id = :tenantId
                  AND d.status = 'READY'
                  AND d.embedding_profile = :embeddingProfile
                ORDER BY dc.embedding <=> CAST(:embedding AS vector)
                LIMIT :limit
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("tenantId", tenantId)
                .addValue("embeddingProfile", embeddingProfile)
                .addValue("embedding", PgVectorLiteral.from(queryEmbedding))
                .addValue("limit", limit);

        return jdbcTemplate.query(sql, params, (rs, rowNum) -> new RetrievedChunk(
                rs.getObject("document_id", UUID.class),
                rs.getString("document_name"),
                rs.getInt("chunk_index"),
                rs.getString("section_title"),
                rs.getString("chunk_type"),
                rs.getString("chunk_text"),
                rs.getDouble("vector_distance"),
                null,
                0.0,
                0.0
        ));
    }

    public List<RetrievedChunk> searchTopLexicalChunks(UUID tenantId, String embeddingProfile, String question, int limit) {
        String sql = """
                SELECT d.id AS document_id,
                       d.original_filename AS document_name,
                       dc.chunk_index,
                       dc.section_title,
                       dc.chunk_type,
                       dc.chunk_text,
                       ts_rank_cd(
                            setweight(to_tsvector('english', coalesce(dc.section_title, '')), 'A') ||
                            setweight(to_tsvector('english', coalesce(d.original_filename, '')), 'B') ||
                            setweight(to_tsvector('english', coalesce(dc.chunk_text, '')), 'C'),
                            websearch_to_tsquery('english', :question)
                       ) AS lexical_score
                FROM assistant.document_chunks dc
                INNER JOIN assistant.documents d ON d.id = dc.document_id
                WHERE dc.tenant_id = :tenantId
                  AND d.tenant_id = :tenantId
                  AND d.status = 'READY'
                  AND d.embedding_profile = :embeddingProfile
                  AND (
                        setweight(to_tsvector('english', coalesce(dc.section_title, '')), 'A') ||
                        setweight(to_tsvector('english', coalesce(d.original_filename, '')), 'B') ||
                        setweight(to_tsvector('english', coalesce(dc.chunk_text, '')), 'C')
                      ) @@ websearch_to_tsquery('english', :question)
                ORDER BY lexical_score DESC, dc.chunk_index ASC
                LIMIT :limit
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("tenantId", tenantId)
                .addValue("embeddingProfile", embeddingProfile)
                .addValue("question", question)
                .addValue("limit", limit);

        return jdbcTemplate.query(sql, params, (rs, rowNum) -> new RetrievedChunk(
                rs.getObject("document_id", UUID.class),
                rs.getString("document_name"),
                rs.getInt("chunk_index"),
                rs.getString("section_title"),
                rs.getString("chunk_type"),
                rs.getString("chunk_text"),
                null,
                rs.getDouble("lexical_score"),
                0.0,
                0.0
        ));
    }
}
