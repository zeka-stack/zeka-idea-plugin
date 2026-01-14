package dev.dong4j.zeka.stack.idea.plugin.swagger.util;

import org.jetbrains.annotations.NotNull;

/**
 * Swagger 注解文本处理工具
 */
public final class SwaggerAnnotationWriterUtil {

    private SwaggerAnnotationWriterUtil() {
    }

    @NotNull
    public static String cleanAnnotationText(@NotNull String text) {
        String content = text.trim();
        if (content.isEmpty()) {
            return "";
        }

        content = removeCodeFence(content).trim();
        if (content.isEmpty()) {
            return "";
        }

        int firstAt = content.indexOf('@');
        if (firstAt > 0) {
            content = content.substring(firstAt).trim();
        }

        String[] lines = content.split("\n");
        StringBuilder sb = new StringBuilder();
        boolean inAnnotation = false;
        for (String line : lines) {
            String trimmed = line.trim();
            if (looksLikeMethodSignature(trimmed)) {
                break;
            }
            if (trimmed.startsWith("@")) {
                inAnnotation = true;
            } else if (inAnnotation && !looksLikeAnnotationContinuation(trimmed)) {
                break;
            } else if (!inAnnotation) {
                continue;
            }

            if (!sb.isEmpty()) {
                sb.append("\n");
            }
            sb.append(line);
        }

        String result = sb.toString().trim();
        return result.startsWith("@") ? result : "";
    }

    @NotNull
    public static String indentLines(@NotNull String text, @NotNull String indent) {
        String[] lines = text.split("\n");
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            String trimmed = line.trim();
            if (!sb.isEmpty()) {
                sb.append("\n");
            }
            if (trimmed.isEmpty()) {
            } else {
                sb.append(indent).append(trimmed);
            }
        }
        return sb.toString();
    }

    private static boolean looksLikeMethodSignature(@NotNull String line) {
        if (line.contains("(") && line.contains(")") && line.endsWith("{")) {
            return true;
        }
        return line.startsWith("public ")
               || line.startsWith("protected ")
               || line.startsWith("private ")
               || line.startsWith("static ")
               || line.startsWith("default ");
    }

    private static boolean looksLikeAnnotationContinuation(@NotNull String trimmed) {
        if (trimmed.isEmpty()) {
            return true;
        }
        if (trimmed.startsWith("(") || trimmed.startsWith(")") || trimmed.startsWith(",")) {
            return true;
        }
        return trimmed.contains("=") || trimmed.startsWith("\"");
    }

    @NotNull
    private static String removeCodeFence(@NotNull String content) {
        String result = content;
        if (result.startsWith("```")) {
            int firstNewline = result.indexOf('\n');
            if (firstNewline != -1) {
                result = result.substring(firstNewline + 1);
            } else {
                result = result.replaceFirst("^```+\\s*", "");
            }
        }
        result = result.trim();
        if (result.endsWith("```")) {
            result = result.replaceAll("\\s*```+$", "").trim();
        }
        return result;
    }
}
