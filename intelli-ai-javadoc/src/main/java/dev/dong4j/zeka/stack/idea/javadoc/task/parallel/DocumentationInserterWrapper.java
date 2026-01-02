package dev.dong4j.zeka.stack.idea.javadoc.task.parallel;

import org.jetbrains.annotations.NotNull;

import dev.dong4j.zeka.stack.idea.javadoc.task.DocumentationInserterHelper;
import dev.dong4j.zeka.stack.idea.javadoc.task.DocumentationTask;
import lombok.RequiredArgsConstructor;

/**
 * 文档插入器适配器
 * <p>
 * 直接调用 DocumentationInserterHelper 的 insertDocumentation 方法.
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

    /**
     * 插入文档内容
     * <p> 通过内部的文档插入辅助类执行文档插入操作
     *
     * @param task           文档任务对象, 不能为 null
     * @param documentation  要插入的文档内容, 不能为 null
     * @param verboseLogging 是否启用详细日志记录
     * @since 1.0.0
     */
    @Override
    public void insertDocumentation(@NotNull DocumentationTask task,
                                    @NotNull String documentation,
                                    boolean verboseLogging) {
        inserterHelper.insertDocumentation(task, documentation, verboseLogging);
    }
}

