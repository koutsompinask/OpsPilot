package com.opspilot.assistant.service.embedding;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmbeddingStartupValidatorTest {

    @Mock
    private EmbeddingService embeddingService;

    @Mock
    private EmbeddingProvider embeddingProvider;

    @InjectMocks
    private EmbeddingStartupValidator validator;

    @Test
    void runShouldFailWhenProviderDimensionsDoNotMatchProfile() {
        EmbeddingProperties properties = new EmbeddingProperties();
        validator = new EmbeddingStartupValidator(properties, embeddingService);

        when(embeddingService.provider()).thenReturn(embeddingProvider);
        when(embeddingService.profile()).thenReturn(new EmbeddingProfile("tei:test:384", "tei", "test", 384));
        when(embeddingProvider.embed(List.of("embedding startup validation"))).thenReturn(List.of(List.of(0.1, 0.2)));

        assertThatThrownBy(() -> validator.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("dimension mismatch");
    }
}
