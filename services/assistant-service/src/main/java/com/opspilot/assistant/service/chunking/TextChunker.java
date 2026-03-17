package com.opspilot.assistant.service.chunking;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TextChunker {

    private final int maxChunkChars;
    private final int overlapBlocks;

    public TextChunker(
            @Value("${assistant.chunking.chunk-size:420}") int maxChunkChars,
            @Value("${assistant.chunking.chunk-overlap:1}") int overlapBlocks
    ) {
        if (maxChunkChars < 120 || overlapBlocks < 0 || overlapBlocks > 5) {
            throw new IllegalArgumentException("Invalid chunking configuration");
        }
        this.maxChunkChars = maxChunkChars;
        this.overlapBlocks = overlapBlocks;
    }

    public List<TextChunk> chunk(String text) {
        String normalized = text == null ? "" : text.trim();
        if (normalized.isEmpty()) {
            return List.of();
        }

        List<Block> blocks = parseBlocks(normalized);
        if (blocks.isEmpty()) {
            return List.of();
        }

        List<TextChunk> chunks = new ArrayList<>();
        int cursor = 0;
        while (cursor < blocks.size()) {
            int endExclusive = cursor;
            StringBuilder content = new StringBuilder();
            String sectionTitle = blocks.get(cursor).sectionTitle();
            String chunkType = blocks.get(cursor).type();

            while (endExclusive < blocks.size()) {
                Block block = blocks.get(endExclusive);
                String candidateText = content.isEmpty() ? block.text() : content + "\n\n" + block.text();
                if (!content.isEmpty() && candidateText.length() > maxChunkChars) {
                    break;
                }
                if (!content.isEmpty()) {
                    content.append("\n\n");
                }
                content.append(block.text());
                sectionTitle = preferredSectionTitle(sectionTitle, block.sectionTitle());
                chunkType = mergeType(chunkType, block.type());
                endExclusive++;
            }

            if (!content.isEmpty()) {
                chunks.add(new TextChunk(content.toString().trim(), sectionTitle, chunkType));
            }

            if (endExclusive <= cursor) {
                endExclusive = cursor + 1;
            }
            cursor = Math.max(cursor + 1, endExclusive - overlapBlocks);
        }
        return chunks;
    }

    private List<Block> parseBlocks(String text) {
        List<Block> blocks = new ArrayList<>();
        String[] lines = text.split("\\R");
        String currentSectionTitle = "General";
        List<String> currentBlockLines = new ArrayList<>();
        String currentType = "paragraph";

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                flushBlock(blocks, currentBlockLines, currentSectionTitle, currentType);
                currentBlockLines.clear();
                currentType = "paragraph";
                continue;
            }

            if (isHeading(trimmed)) {
                flushBlock(blocks, currentBlockLines, currentSectionTitle, currentType);
                currentBlockLines.clear();
                currentSectionTitle = normalizeHeading(trimmed);
                blocks.add(new Block(trimmed, currentSectionTitle, "heading"));
                currentType = "paragraph";
                continue;
            }

            String lineType = classifyLine(trimmed);
            if (!currentBlockLines.isEmpty() && !lineType.equals(currentType)) {
                flushBlock(blocks, currentBlockLines, currentSectionTitle, currentType);
                currentBlockLines.clear();
            }
            currentType = lineType;
            currentBlockLines.add(trimmed);
        }

        flushBlock(blocks, currentBlockLines, currentSectionTitle, currentType);
        return blocks;
    }

    private void flushBlock(List<Block> blocks, List<String> lines, String sectionTitle, String type) {
        if (lines.isEmpty()) {
            return;
        }
        String delimiter = "list".equals(type) ? "\n" : " ";
        String text = String.join(delimiter, lines).trim();
        if (!text.isEmpty()) {
            blocks.add(new Block(text, sectionTitle, type));
        }
    }

    private boolean isHeading(String line) {
        if (line.startsWith("#")) {
            return true;
        }
        if (line.endsWith(":")) {
            return true;
        }
        if (!line.endsWith(".") && !line.endsWith("?") && !line.endsWith("!") && line.length() <= 80
                && line.matches("^[A-Z][A-Za-z0-9&/()' -]+$")) {
            return true;
        }
        return line.equals(line.toUpperCase(Locale.ROOT)) && line.length() <= 80;
    }

    private String normalizeHeading(String line) {
        String normalized = line.replaceFirst("^#+\\s*", "").trim();
        if (normalized.endsWith(":")) {
            normalized = normalized.substring(0, normalized.length() - 1).trim();
        }
        return normalized.isBlank() ? "General" : normalized;
    }

    private String classifyLine(String line) {
        if (line.startsWith("-") || line.matches("^\\d+[.)].*")) {
            return "list";
        }
        return "paragraph";
    }

    private String preferredSectionTitle(String current, String candidate) {
        if (current == null || current.isBlank() || "General".equals(current)) {
            return candidate;
        }
        return current;
    }

    private String mergeType(String left, String right) {
        if (left == null || left.isBlank()) {
            return right;
        }
        if (left.equals(right)) {
            return left;
        }
        return "mixed";
    }

    private record Block(String text, String sectionTitle, String type) {
    }
}
