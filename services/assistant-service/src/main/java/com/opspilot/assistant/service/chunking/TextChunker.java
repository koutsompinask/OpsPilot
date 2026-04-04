package com.opspilot.assistant.service.chunking;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Block-aware text chunker that splits documents into semantically coherent chunks
 * suitable for embedding and retrieval.
 *
 * <p>The algorithm works in two stages:
 * <ol>
 *   <li><b>Block parsing</b> — the raw text is first split into logical blocks: headings,
 *       list items, and prose paragraphs. Blank lines act as block separators. Each block
 *       carries a {@code sectionTitle} (inherited from the most recent heading) and a
 *       {@code type} ({@code heading}, {@code list}, {@code paragraph}, or {@code mixed}).</li>
 *   <li><b>Block packing</b> — consecutive blocks are greedily packed into a chunk until
 *       the chunk would exceed {@code maxChunkChars}. The cursor then advances by
 *       {@code (endBlock - overlapBlocks)}, allowing the last {@code overlapBlocks} blocks
 *       of the current chunk to appear at the start of the next chunk. This overlap ensures
 *       that context spanning a chunk boundary is not lost during retrieval.</li>
 * </ol>
 *
 * <p>Configuration properties:
 * <ul>
 *   <li>{@code assistant.chunking.chunk-size} (default 420): maximum characters per chunk.
 *       420 chars corresponds to roughly 100–120 tokens, a good fit for most embedding
 *       models with a 512-token context window.</li>
 *   <li>{@code assistant.chunking.chunk-overlap} (default 1): number of trailing blocks
 *       repeated at the start of the next chunk to preserve cross-chunk context.</li>
 * </ul>
 * </p>
 */
@Component
public class TextChunker {

    private final int maxChunkChars;
    private final int overlapBlocks;

    public TextChunker(
            @Value("${assistant.chunking.chunk-size:420}") int maxChunkChars,
            @Value("${assistant.chunking.chunk-overlap:1}") int overlapBlocks
    ) {
        // Minimum of 120 chars prevents pathologically tiny chunks that carry no useful context;
        // overlap must be in [0, 5] to avoid consuming more blocks than fit in a single chunk.
        if (maxChunkChars < 120 || overlapBlocks < 0 || overlapBlocks > 5) {
            throw new IllegalArgumentException("Invalid chunking configuration");
        }
        this.maxChunkChars = maxChunkChars;
        this.overlapBlocks = overlapBlocks;
    }

    /**
     * Splits the given document text into a list of semantically coherent {@link TextChunk}s.
     *
     * @param text the raw document text to split; {@code null} returns an empty list
     * @return an ordered list of chunks ready for embedding; never {@code null}
     */
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
            // The section title and type for the chunk are taken from its first block and
            // updated as additional blocks are packed in.
            String sectionTitle = blocks.get(cursor).sectionTitle();
            String chunkType = blocks.get(cursor).type();

            while (endExclusive < blocks.size()) {
                Block block = blocks.get(endExclusive);
                String candidateText = content.isEmpty() ? block.text() : content + "\n\n" + block.text();
                // Stop packing once adding this block would push the chunk over the size limit.
                // The first block is always included even if it alone exceeds maxChunkChars,
                // to ensure very long paragraphs are not silently dropped.
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
                // Safety guard: if no block advanced the end pointer, move forward by 1 to
                // avoid an infinite loop (can happen only with a degenerate block list).
                endExclusive = cursor + 1;
            }
            // Overlap: rewind the cursor by overlapBlocks so the trailing blocks of this chunk
            // are repeated at the start of the next chunk, preserving cross-boundary context.
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
        // Markdown-style headings: lines starting with one or more '#' characters
        if (line.startsWith("#")) {
            return true;
        }
        // Lines ending with ':' are treated as section labels (e.g. "Check-in Policy:")
        if (line.endsWith(":")) {
            return true;
        }
        // Title-case lines of ≤80 chars that don't end with sentence punctuation and
        // look like a heading (starts with uppercase, only safe characters)
        if (!line.endsWith(".") && !line.endsWith("?") && !line.endsWith("!") && line.length() <= 80
                && line.matches("^[A-Z][A-Za-z0-9&/()' -]+$")) {
            return true;
        }
        // All-caps lines of ≤80 chars (common in plain-text docs, e.g. "GENERAL INFORMATION")
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
