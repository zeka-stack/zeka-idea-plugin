package dev.dong4j.zeka.stack.idea.plugin.action;

import com.intellij.openapi.actionSystem.AnActionEvent;

import org.jetbrains.annotations.NotNull;

import lombok.extern.slf4j.Slf4j;

/**
 * 生成 JavaDoc 的操作类
 * <p>
 * 该类用于执行生成 JavaDoc 的具体操作, 继承自 AbstractGenerateJavaDocAction, 主要负责处理生成逻辑.
 * 支持对 Java 代码片段进行注释生成, 适用于 IDE 插件或代码工具中的 JavaDoc 自动生成功能.
 *
 * @author dong4j
 * @version 1.0.0
 * @date 2025.10.24
 * @since 1.0.0
 */
@Slf4j
public class GenerateJavaDocGenerateAction extends AbstractGenerateJavaDocAction {
    /**
     * 处理动作事件的回调方法
     * <p>
     * 该方法用于响应用户触发的动作事件, 调用内部的处理方法进行具体操作
     *
     * @param e 动作事件对象, 包含事件相关信息
     */
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        process(e, true);
    }
}

