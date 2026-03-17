package com.opspilot.assistant.service.retrieval;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class RerankerStartupValidator implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RerankerStartupValidator.class);

    private final RerankerProperties properties;
    private final RerankerService rerankerService;

    public RerankerStartupValidator(RerankerProperties properties, RerankerService rerankerService) {
        this.properties = properties;
        this.rerankerService = rerankerService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isEnabled()) {
            log.info("assistant_reranker_startup_validation_skipped enabled=false");
            return;
        }
        if (!properties.isValidateOnStartup()) {
            log.info("assistant_reranker_startup_validation_skipped enabled=true provider={}", rerankerService.providerName());
            return;
        }

        List<RerankResult> results = rerankerService.validateSample();
        if (results.isEmpty()) {
            throw new IllegalStateException("Reranker returned no results during startup validation");
        }
        if (results.getFirst().index() != 0) {
            throw new IllegalStateException("Reranker startup validation returned an unexpected top result");
        }

        log.info(
                "assistant_reranker_profile_validated provider={} model={} topScore={}",
                rerankerService.providerName(),
                rerankerService.modelName(),
                results.getFirst().score()
        );
    }
}
