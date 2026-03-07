package com.opspilot.aiorchestrator.service.embedding;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class LocalDeterministicEmbeddingProvider implements EmbeddingProvider {

    public static final int DIMENSIONS = 1536;

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
        List<Double> vector = new ArrayList<>(DIMENSIONS);

        long state = 0;
        for (byte b : seed) {
            state = (state << 1) ^ (b & 0xff);
        }

        for (int i = 0; i < DIMENSIONS; i++) {
            state = (state * 6364136223846793005L + 1442695040888963407L);
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
