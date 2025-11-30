package dev.dong4j.zeka.stack.idea.plugin.task.parallel;

import org.jetbrains.annotations.NotNull;

import dev.dong4j.zeka.stack.idea.plugin.task.DocumentationInserterHelper;
import dev.dong4j.zeka.stack.idea.plugin.task.DocumentationTask;
import lombok.RequiredArgsConstructor;

/**
 * TaskExecutor 文档插入器适配器
 * <p>
 * 直接调用 DocumentationInserterHelper 的 insertDocumentation 方法。
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email mailto:zeka.stack@gmail.com
 * @date 2025.12.01
 * @since 1.0.0
 */
@RequiredArgsConstructor
public class DocumentationInserterWrapper implements DocumentationInserter {
    /** 文档插入辅助类实例 */
    @NotNull
    private final DocumentationInserterHelper inserterHelper;

    @Override
    public void insertDocumentation(@NotNull DocumentationTask task,
                                    @NotNull String documentation,
                                    boolean verboseLogging) {
        inserterHelper.insertDocumentation(task, documentation, verboseLogging);
    }
}

