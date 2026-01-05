package dev.dong4j.zeka.stack.idea.plugin.common.autocomplete;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

final class OccurrenceCandidateFinder {
    private static final double MIN_SIMILARITY = 0.6;
    private static final int MAX_LINE_LENGTH = 400;

    @NotNull
    String buildAllowedOccurrences(@NotNull String fullText,
                                   @NotNull String oldText,
                                   @NotNull EditRecord lastEdit,
                                   int limit) {
        List<OccurrenceCandidate> candidates = findCandidates(fullText, oldText, lastEdit, limit);
        if (candidates.isEmpty()) {
            return "[]";
        }
        StringBuilder builder = new StringBuilder();
        for (OccurrenceCandidate candidate : candidates) {
            builder.append("{\"start_index\":").append(candidate.startIndex())
                .append(",\"end_index\":").append(candidate.endIndex())
                .append(",\"line\":").append(candidate.line())
                .append(",\"score\":").append(String.format(Locale.US, "%.2f", candidate.score()))
                .append(",\"preview\":\"").append(escapeJson(candidate.preview())).append("\"}");
            builder.append('\n');
        }
        return builder.toString();
    }

    @NotNull
    private List<OccurrenceCandidate> findCandidates(@NotNull String fullText,
                                                     @NotNull String oldText,
                                                     @NotNull EditRecord lastEdit,
                                                     int limit) {
        if (oldText.isBlank() || oldText.contains("\n")) {
            return List.of();
        }
        List<OccurrenceCandidate> results = new ArrayList<>();
        Set<Integer> seenStart = new HashSet<>();
        int lineStart = 0;
        int lineNumber = 0;
        int textLength = fullText.length();
        while (lineStart <= textLength) {
            int lineEnd = fullText.indexOf('\n', lineStart);
            if (lineEnd < 0) {
                lineEnd = textLength;
            }
            int lineLength = lineEnd - lineStart;
            if (lineLength > 0 && lineLength <= MAX_LINE_LENGTH) {
                String line = fullText.substring(lineStart, lineEnd);
                OccurrenceCandidate candidate = bestMatchInLine(line, lineStart, lineNumber, oldText, lastEdit);
                if (candidate != null && seenStart.add(candidate.startIndex())) {
                    results.add(candidate);
                }
            }
            if (lineEnd == textLength) {
                break;
            }
            lineStart = lineEnd + 1;
            lineNumber++;
        }
        results.sort(Comparator.comparingDouble(OccurrenceCandidate::score).reversed());
        if (results.size() > limit) {
            return results.subList(0, limit);
        }
        return results;
    }

    private OccurrenceCandidate bestMatchInLine(@NotNull String line,
                                                int lineStartOffset,
                                                int lineNumber,
                                                @NotNull String oldText,
                                                @NotNull EditRecord lastEdit) {
        String trimmedLine = line.trim();
        if (trimmedLine.isEmpty()) {
            return null;
        }
        int oldLen = oldText.length();
        if (line.length() < 1 || oldLen < 1) {
            return null;
        }
        double bestScore = 0.0;
        int bestStart = -1;
        int bestEnd = -1;
        for (int i = 0; i + oldLen <= line.length(); i++) {
            int end = i + oldLen;
            String candidate = line.substring(i, end);
            double score = similarity(oldText, candidate);
            if (score > bestScore) {
                int globalStart = lineStartOffset + i;
                int globalEnd = lineStartOffset + end;
                if (!overlaps(globalStart, globalEnd, lastEdit.startOffset(), lastEdit.endOffset())) {
                    bestScore = score;
                    bestStart = globalStart;
                    bestEnd = globalEnd;
                }
            }
        }
        if (bestScore < MIN_SIMILARITY || bestStart < 0) {
            return null;
        }
        String preview = line;
        if (preview.length() > 120) {
            preview = preview.substring(0, 120);
        }
        return new OccurrenceCandidate(bestStart, bestEnd, lineNumber, bestScore, preview);
    }

    private boolean overlaps(int start, int end, int lastStart, int lastEnd) {
        return start < lastEnd && end > lastStart;
    }

    private double similarity(@NotNull String a, @NotNull String b) {
        int dist = levenshtein(a, b);
        int max = Math.max(a.length(), b.length());
        if (max == 0) {
            return 1.0;
        }
        return 1.0 - ((double) dist / (double) max);
    }

    private int levenshtein(@NotNull String a, @NotNull String b) {
        int n = a.length();
        int m = b.length();
        int[] prev = new int[m + 1];
        int[] curr = new int[m + 1];
        for (int j = 0; j <= m; j++) {
            prev[j] = j;
        }
        for (int i = 1; i <= n; i++) {
            curr[0] = i;
            char ca = a.charAt(i - 1);
            for (int j = 1; j <= m; j++) {
                int cost = ca == b.charAt(j - 1) ? 0 : 1;
                curr[j] = Math.min(Math.min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] temp = prev;
            prev = curr;
            curr = temp;
        }
        return prev[m];
    }

    @NotNull
    private String escapeJson(@NotNull String text) {
        return text.replace("\\", "\\\\")
            .replace("\"", "\\\"");
    }
}
