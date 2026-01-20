package dev.dong4j.zeka.stack.idea.plugin.repairer.checkstyle;

/**
 * Checkstyle AI 修复上下文.
 */

import dev.dong4j.zeka.stack.idea.plugin.repairer.violation.CodeViolation;

/**
 * 修复上下文记录类
 * <p>用于封装代码违规项 (CodeViolation) 及其目标文本(targetText), 常用于代码修复工具中, 作为修复操作的上下文数据载体.
 * 该类为不可变数据类, 适用于传递与代码修复相关的结构化信息.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.20
 * @since 1.0.0
 */
record FixContext(CodeViolation violation, String targetText) {
}
