package com.opspilot.assistant.service.embedding;

/**
 * Immutable snapshot of an embedding provider's identity and vector dimensionality.
 *
 * <p>The {@code id} field is stored on every {@code Document} row so that
 * {@link com.opspilot.assistant.service.DocumentEmbeddingMaintenanceService} can detect
 * when the active profile has changed and re-index stale documents accordingly.</p>
 *
 * @param id         stable composite identifier in the form {@code provider:model:dimensions}
 * @param provider   short provider name (e.g. {@code tei}, {@code openai}, {@code stub})
 * @param model      model name or identifier used for this embedding
 * @param dimensions number of dimensions in each output vector
 */
public record EmbeddingProfile(
        String id,
        String provider,
        String model,
        int dimensions
) {
}
