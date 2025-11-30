package dev.dong4j.zeka.stack.idea.plugin.action;

import com.intellij.openapi.actionSystem.AnActionEvent;

import org.jetbrains.annotations.NotNull;

import lombok.extern.slf4j.Slf4j;

/**
 * 生成 JavaDoc 快捷操作类
 * <p>
 * 该类继承自 AbstractGenerateJavaDocAction, 用于处理 JavaDoc 生成的快捷操作,
 * 通过 actionPerformed 方法响应用户操作事件, 调用父类的 process 方法执行具体的 JavaDoc 生成逻辑
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email mailto:zeka.stack@gmail.com
 * @date 2025.11.30
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
