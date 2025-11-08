package dev.dong4j.zeka.stack.idea.plugin.action;

import com.intellij.openapi.actionSystem.AnActionEvent;

import org.jetbrains.annotations.NotNull;

import lombok.extern.slf4j.Slf4j;

/**
 * 生成 JavaDoc 注释的快捷操作类
 * <p>
 * 该类继承自 AbstractGenerateJavaDocAction, 用于在 IDE 中提供生成 JavaDoc 注释的快捷操作功能, 主要处理用户触发相关操作时的逻辑流程.
 *
 * @author 未知
 * @version 1.0.0
 * @date 2025.10.24
 * @since 1.0.0
 */
@Slf4j
public class GenerateJavaDocShortcutAction extends AbstractGenerateJavaDocAction {
    /**
     * 处理动作事件
     * <p>
     * 该方法用于处理用户触发的动作事件, 调用内部的 process 方法进行具体处理.
     *
     * @param e 动作事件对象
     */
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        process(e, false);
    }
}
