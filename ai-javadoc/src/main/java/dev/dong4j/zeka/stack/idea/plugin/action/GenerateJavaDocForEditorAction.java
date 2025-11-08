package dev.dong4j.zeka.stack.idea.plugin.action;

import com.intellij.openapi.actionSystem.AnActionEvent;

import org.jetbrains.annotations.NotNull;

import lombok.extern.slf4j.Slf4j;

/**
 * 生成 JavaDoc 编辑器动作
 * <p>
 * 该类实现了 IDE 中的动作, 用于在编辑器中为当前文件或选中的代码块生成 JavaDoc 注释. 它继承自 {@link AbstractGenerateJavaDocAction}, 并通过 {@code process(e, true)} 触发生成逻辑.
 * <p>
 * 通过 Lombok 的 {@code @Slf4j} 注解提供日志支持, 方便在调试时记录动作执行过程.
 *
 * @author dong4j
 * @version 1.0.0
 * @date 2025.11.08
 * @since 1.0.0
 */
@Slf4j
public class GenerateJavaDocForEditorAction extends AbstractGenerateJavaDocAction {
    /**
     * 处理动作事件的回调方法
     * <p>
     * 该方法用于响应用户触发的动作事件, 调用内部的处理方法进行具体操作
     *
     * @param e 动作事件对象, 包含事件相关的信息
     */
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        process(e, true);
    }

}

