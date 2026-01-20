package dev.dong4j.zeka.stack.idea.plugin.repairer.ai;

/**
 * AI 响应校验与清洗.
 */
public final class FixResponseValidator {
    private FixResponseValidator() {
    }

    public static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        String trimmed = raw.trim();
        int fenceStart = trimmed.indexOf("```");
        if (fenceStart >= 0) {
            int fenceEnd = trimmed.indexOf("```", fenceStart + 3);
            if (fenceEnd > fenceStart) {
                String inside = trimmed.substring(fenceStart + 3, fenceEnd);
                int firstLineBreak = inside.indexOf('\n');
                if (firstLineBreak >= 0) {
                    String firstLine = inside.substring(0, firstLineBreak).trim();
                    if (!firstLine.isEmpty() && firstLine.length() <= 10 && firstLine.matches("[a-zA-Z]+")) {
                        inside = inside.substring(firstLineBreak + 1);
                    }
                }
                trimmed = inside.trim();
            }
        }
        if (trimmed.contains("```") || trimmed.contains("<<<") || trimmed.contains(">>>")) {
            return "";
        }
        return trimmed;
    }
}
