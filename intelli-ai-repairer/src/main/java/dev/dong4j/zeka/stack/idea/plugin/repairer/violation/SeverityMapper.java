package dev.dong4j.zeka.stack.idea.plugin.repairer.violation;

/**
 * 严重级别映射.
 */
public final class SeverityMapper {
    private SeverityMapper() {
    }

    public static int fromCheckstyle(String severity) {
        if (severity == null) {
            return 3;
        }
        return switch (severity.toLowerCase()) {
            case "error" -> 1;
            case "warning" -> 2;
            case "info" -> 4;
            default -> 3;
        };
    }

    public static int fromPmdPriority(String priority) {
        if (priority == null) {
            return 3;
        }
        try {
            return Integer.parseInt(priority);
        } catch (NumberFormatException e) {
            return 3;
        }
    }
}
