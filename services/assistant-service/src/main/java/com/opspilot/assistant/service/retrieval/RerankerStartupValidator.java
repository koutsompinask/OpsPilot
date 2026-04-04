package com.opspilot.assistant.service.retrieval;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Validates the active reranker provider immediately after application startup.
 *
 * <p>Sends a fixed probe query ({@code "startup validation"}) with two known passages and
 * asserts that the top-ranked result is index 0 (the passage explicitly containing the
 * probe phrase). If the reranker is disabled or {@code validateOnStartup} is false, the
 * check is skipped entirely. A failed validation aborts startup to prevent silent
 * degradation where the reranker would accept calls but return nonsensical scores.</p>
 */
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
        // Skip entirely if the reranker feature is disabled in config
        if (!properties.isEnabled()) {
            log.info("assistant_reranker_startup_validation_skipped enabled=false");
            return;
        }
        // Allow skipping the live probe (e.g. in environments where TEI starts after this service)
        if (!properties.isValidateOnStartup()) {
            log.info("assistant_reranker_startup_validation_skipped enabled=true provider={}", rerankerService.providerName());
            return;
        }

        List<RerankResult> results = rerankerService.validateSample();
        if (results.isEmpty()) {
            throw new IllegalStateException("Reranker returned no results during startup validation");
        }
        // The sample is designed so index 0 should always win; anything else suggests model misconfiguration
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
