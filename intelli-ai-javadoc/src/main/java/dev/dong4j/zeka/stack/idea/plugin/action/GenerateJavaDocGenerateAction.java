package dev.dong4j.zeka.stack.idea.plugin.action;

import com.intellij.openapi.actionSystem.AnActionEvent;

import org.jetbrains.annotations.NotNull;

import lombok.extern.slf4j.Slf4j;

/**
 * 生成 Javadoc 生成动作类
 * <p>
 * 该类继承自 AbstractGenerateJavaDocAction, 用于处理生成 Javadoc 的 IDE 动作事件,
 * 当用户触发该动作时, 会执行 Javadoc 生成的相关处理逻辑
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email mailto:zeka.stack@gmail.com
 * @date 2025.11.30
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

