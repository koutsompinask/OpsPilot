package com.opspilot.assistant.repository;

import java.util.List;

final class PgVectorLiteral {

    private PgVectorLiteral() {
    }

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
