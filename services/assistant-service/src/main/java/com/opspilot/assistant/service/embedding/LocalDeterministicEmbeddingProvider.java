package com.opspilot.assistant.service.embedding;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Stub embedding provider that generates deterministic pseudo-random vectors entirely in-process.
 *
 * <p>Used for local development and integration tests where a real embedding server is not
 * available. Each text is hashed with SHA-256 to produce a reproducible seed, which is then
 * expanded into a vector using a 64-bit LCG (linear congruential generator) so that identical
 * inputs always produce the same vector. The output is scaled to the range {@code [-1, 1]} to
 * loosely mimic normalized embedding vectors. These vectors carry no semantic meaning and
 * should never be used in production.</p>
 */
@Component
public class LocalDeterministicEmbeddingProvider implements EmbeddingProvider {

    private final EmbeddingProperties properties;

    public LocalDeterministicEmbeddingProvider(EmbeddingProperties properties) {
        this.properties = properties;
    }

    @Override
    public EmbeddingProfile profile() {
        return new EmbeddingProfile(
                "stub:deterministic:" + properties.getStub().getDimensions(),
                "stub",
                "deterministic",
                properties.getStub().getDimensions()
        );
    }

    @Override
    public List<List<Double>> embed(List<String> inputs) {
        List<List<Double>> vectors = new ArrayList<>(inputs.size());
        for (String input : inputs) {
            vectors.add(toVector(input));
        }
        return vectors;
    }

    private List<Double> toVector(String input) {
        byte[] seed = sha256(input == null ? "" : input);
        int dimensions = profile().dimensions();
        List<Double> vector = new ArrayList<>(dimensions);

        // Fold all 32 seed bytes into a single 64-bit initial state via XOR-shift
        long state = 0;
        for (byte b : seed) {
            state = (state << 1) ^ (b & 0xff);
        }

        for (int i = 0; i < dimensions; i++) {
            // Knuth's multiplicative LCG constants (64-bit); produces a full-period sequence
            state = (state * 6364136223846793005L + 1442695040888963407L);
            // Extract 53 mantissa bits and normalise to [0, 1), then shift to [-1, 1)
            double value = ((state >>> 11) / (double) (1L << 53));
            vector.add((value * 2.0) - 1.0);
        }
        return vector;
    }

    private byte[] sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(text.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }
}
