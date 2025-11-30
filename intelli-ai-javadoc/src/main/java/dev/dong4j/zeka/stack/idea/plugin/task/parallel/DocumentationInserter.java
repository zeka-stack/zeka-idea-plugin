package dev.dong4j.zeka.stack.idea.plugin.task.parallel;

import org.jetbrains.annotations.NotNull;

import dev.dong4j.zeka.stack.idea.plugin.task.DocumentationTask;

/**
 * 文档插入接口
 * <p>
 * 定义插入文档的标准接口，用于解耦文档插入逻辑。
 * 允许不同的实现方式（Document API 或 PSI API）。
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email mailto:zeka.stack@gmail.com
 * @date 2025.12.01
 * @since 1.0.0
 */
public interface DocumentationInserter {
    /**
     * 插入文档到代码中
     *
     * @param task           文档生成任务
     * @param documentation  生成的文档内容
     * @param verboseLogging 是否启用详细日志
     */
    void insertDocumentation(@NotNull DocumentationTask task,
                             @NotNull String documentation,
                             boolean verboseLogging);
}

