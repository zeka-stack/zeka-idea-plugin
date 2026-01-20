package dev.dong4j.zeka.stack.idea.plugin.repairer.violation;

/**
 * 统一的代码违规模型.
 */
public class CodeViolation {
    public String tool;
    public String ruleId;
    public String message;
    public int severity;
    public String filePath;
    public int startLine;
    public int startColumn;
    public int endLine;
    public int endColumn;
}
