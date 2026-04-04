package com.opspilot.assistant.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * JDBC repository that executes the hybrid vector + lexical retrieval queries against
 * {@code assistant.document_chunks}.
 *
 * <ul>
 *   <li>{@link #searchTopVectorChunks} — uses pgvector cosine distance ({@code <=>}) for semantic retrieval</li>
 *   <li>{@link #searchTopLexicalChunks} — uses PostgreSQL full-text search ({@code ts_rank_cd / websearch_to_tsquery}) for keyword retrieval</li>
 * </ul>
 *
 * Results from both queries are merged by {@link com.opspilot.assistant.service.retrieval.ChunkRetrievalService}
 * using Reciprocal Rank Fusion before reranking.
 */
@Repository
public class DocumentChunkSearchRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public DocumentChunkSearchRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Retrieves the top-{@code limit} chunks by cosine distance to the query embedding.
     *
     * Only chunks from {@code READY} documents matching the active {@code embeddingProfile}
     * are considered, ensuring vector space consistency.
     *
     * @param tenantId         the owning tenant (enforces isolation)
     * @param embeddingProfile the active embedding model profile name
     * @param queryEmbedding   the query vector to search against
     * @param limit            maximum number of results to return
     * @return candidate chunks ordered by ascending cosine distance (closest first)
     */
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

    /**
     * Retrieves the top-{@code limit} chunks by full-text relevance score.
     *
     * Section titles are weighted 'A' (highest), document filenames 'B', and body text 'C',
     * using PostgreSQL's {@code ts_rank_cd} with weighted tsvectors.
     *
     * @param tenantId         the owning tenant (enforces isolation)
     * @param embeddingProfile the active embedding model profile name
     * @param question         the user's question, passed to {@code websearch_to_tsquery}
     * @param limit            maximum number of results to return
     * @return candidate chunks ordered by descending lexical relevance score
     */
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
