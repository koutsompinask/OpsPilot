package com.opspilot.assistant.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.opspilot.assistant.service.chunking.TextChunk;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@ExtendWith(MockitoExtension.class)
class DocumentChunkRepositoryTest {

    @Mock
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Test
    void replaceForDocumentShouldDeleteExistingChunksThenBatchInsert() {
        DocumentChunkRepository repository = new DocumentChunkRepository(jdbcTemplate);
        UUID documentId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        List<TextChunk> chunks = List.of(
                new TextChunk("first chunk", "General", "paragraph"),
                new TextChunk("second chunk", "Policies", "list")
        );
        List<List<Double>> embeddings = List.of(
                List.of(0.1, 0.2),
                List.of(0.3, 0.4)
        );
        when(jdbcTemplate.batchUpdate(any(String.class), any(MapSqlParameterSource[].class))).thenReturn(new int[] {1, 1});

        repository.replaceForDocument(documentId, tenantId, chunks, embeddings);

        verify(jdbcTemplate).update(
                eq("DELETE FROM assistant.document_chunks WHERE document_id = :documentId AND tenant_id = :tenantId"),
                any(MapSqlParameterSource.class)
        );

        ArgumentCaptor<MapSqlParameterSource[]> batchCaptor = ArgumentCaptor.forClass(MapSqlParameterSource[].class);
        verify(jdbcTemplate).batchUpdate(any(String.class), batchCaptor.capture());

        MapSqlParameterSource[] batch = batchCaptor.getValue();
        assertThat(batch).hasSize(2);
        assertThat(batch[0].getValue("documentId")).isEqualTo(documentId);
        assertThat(batch[0].getValue("tenantId")).isEqualTo(tenantId);
        assertThat(batch[0].getValue("chunkIndex")).isEqualTo(0);
        assertThat(batch[0].getValue("chunkText")).isEqualTo("first chunk");
        assertThat(batch[0].getValue("sectionTitle")).isEqualTo("General");
        assertThat(batch[0].getValue("chunkType")).isEqualTo("paragraph");
        assertThat(batch[0].getValue("embedding")).isEqualTo("[0.1,0.2]");
        assertThat(batch[1].getValue("chunkIndex")).isEqualTo(1);
        assertThat(batch[1].getValue("chunkText")).isEqualTo("second chunk");
        assertThat(batch[1].getValue("sectionTitle")).isEqualTo("Policies");
        assertThat(batch[1].getValue("chunkType")).isEqualTo("list");
        assertThat(batch[1].getValue("embedding")).isEqualTo("[0.3,0.4]");
    }

    @Test
    void replaceForDocumentShouldSkipBatchInsertWhenNoChunksExist() {
        DocumentChunkRepository repository = new DocumentChunkRepository(jdbcTemplate);

        repository.replaceForDocument(UUID.randomUUID(), UUID.randomUUID(), List.of(), List.of());

        verify(jdbcTemplate).update(
                eq("DELETE FROM assistant.document_chunks WHERE document_id = :documentId AND tenant_id = :tenantId"),
                any(MapSqlParameterSource.class)
        );
        verify(jdbcTemplate, never()).batchUpdate(any(String.class), any(MapSqlParameterSource[].class));
    }
}
