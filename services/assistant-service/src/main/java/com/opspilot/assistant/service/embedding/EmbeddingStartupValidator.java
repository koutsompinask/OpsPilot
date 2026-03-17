package com.opspilot.assistant.service.embedding;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class EmbeddingStartupValidator implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingStartupValidator.class);

    private final EmbeddingProperties properties;
    private final EmbeddingService embeddingService;

    public EmbeddingStartupValidator(EmbeddingProperties properties, EmbeddingService embeddingService) {
        this.properties = properties;
        this.embeddingService = embeddingService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isValidateOnStartup()) {
            log.info("assistant_embedding_startup_validation_skipped provider={}", embeddingService.profile().provider());
            return;
        }

        List<List<Double>> result = embeddingService.provider().embed(List.of("embedding startup validation"));
        if (result.isEmpty() || result.getFirst() == null) {
            throw new IllegalStateException("Embedding provider returned no vectors during startup validation");
        }

        int actualDimensions = result.getFirst().size();
        int expectedDimensions = embeddingService.profile().dimensions();
        if (actualDimensions != expectedDimensions) {
            throw new IllegalStateException(
                    "Embedding provider dimension mismatch. expected=" + expectedDimensions + " actual=" + actualDimensions
            );
        }

        log.info(
                "assistant_embedding_profile_validated provider={} model={} profile={} dimensions={}",
                embeddingService.profile().provider(),
                embeddingService.profile().model(),
                embeddingService.profile().id(),
                actualDimensions
        );
    }
}
