package com.opspilot.assistant.repository;

import java.util.List;

/**
 * Utility that converts a {@code List<Double>} embedding vector into the PostgreSQL literal
 * string format required by the pgvector extension (e.g. {@code "[0.1,0.2,0.3]"}).
 *
 * JPA/JDBC does not natively understand the pgvector type, so embedding values must be
 * passed as a {@code CAST(:embedding AS vector)} SQL expression where the parameter value
 * is this bracket-delimited string.
 */
final class PgVectorLiteral {

    private PgVectorLiteral() {
    }

    /**
     * Formats the embedding vector as a pgvector-compatible literal string.
     *
     * @param embedding the embedding vector components
     * @return a string of the form {@code "[v0,v1,...,vN]"}
     */
    static String from(List<Double> embedding) {
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < embedding.size(); i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(embedding.get(i));
        }
        builder.append(']');
        return builder.toString();
    }
}
