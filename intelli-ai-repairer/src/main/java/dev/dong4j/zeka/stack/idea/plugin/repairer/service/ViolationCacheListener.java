package dev.dong4j.zeka.stack.idea.plugin.repairer.service;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import dev.dong4j.zeka.stack.idea.plugin.repairer.violation.CodeViolation;

/**
 * Listener for violation cache updates.
 */
public interface ViolationCacheListener {
    /**
     * 当违规信息缓存更新时触发该方法
     * <p> 用于通知监听器违规信息列表已发生变化, 可以执行相应的处理逻辑.
     *
     * @param violations 更新后的违规信息列表, 包含所有当前的代码违规项
     */
    void violationsUpdated(@NotNull List<CodeViolation> violations);
}
