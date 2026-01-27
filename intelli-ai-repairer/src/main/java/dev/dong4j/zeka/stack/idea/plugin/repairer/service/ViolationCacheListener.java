package dev.dong4j.zeka.stack.idea.plugin.repairer.service;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import dev.dong4j.zeka.stack.idea.plugin.repairer.violation.CodeViolation;

/**
 * Listener for violation cache updates.
 */
public interface ViolationCacheListener {
    void violationsUpdated(@NotNull List<CodeViolation> violations);
}
