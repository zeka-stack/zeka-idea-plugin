package dev.dong4j.zeka.stack.idea.plugin.task.parallel;

import org.jetbrains.annotations.NotNull;

import dev.dong4j.zeka.stack.idea.plugin.task.DocumentationTask;
import dev.dong4j.zeka.stack.idea.plugin.task.TaskExecutor;
import lombok.RequiredArgsConstructor;

/**
 * TaskExecutor 文档插入器适配器
 * <p>
 * 直接调用 TaskExecutor 的 insertDocumentation 方法。
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email mailto:zeka.stack@gmail.com
 * @date 2025.12.01
 * @since 1.0.0
 */
@RequiredArgsConstructor
public class TaskExecutorDocumentationInserter implements DocumentationInserter {
    /** TaskExecutor 实例 */
    @NotNull
    private final TaskExecutor taskExecutor;

    @Override
    public void insertDocumentation(@NotNull DocumentationTask task,
                                    @NotNull String documentation,
                                    boolean verboseLogging) {
        // 直接调用 TaskExecutor 的 insertDocumentation 方法
        taskExecutor.insertDocumentation(task, documentation, verboseLogging);
    }
}

