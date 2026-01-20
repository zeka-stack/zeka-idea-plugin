package dev.dong4j.zeka.stack.idea.plugin.repairer.checkstyle;

/**
 * Checkstyle AI 修复上下文.
 */

import dev.dong4j.zeka.stack.idea.plugin.repairer.violation.CodeViolation;

record FixContext(CodeViolation violation, String targetText) {
}
