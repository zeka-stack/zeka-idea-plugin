package dev.dong4j.zeka.stack.idea.plugin.action;

import com.intellij.openapi.actionSystem.AnActionEvent;

import org.jetbrains.annotations.NotNull;

import lombok.extern.slf4j.Slf4j;

/**
 * 生成 JavaDoc 编辑器动作类
 * <p>
 * 该类继承自 AbstractGenerateJavaDocAction, 用于在编辑器中执行 JavaDoc 生成操作,
 * 通过 actionPerformed 方法处理用户触发的动作事件, 调用父类的 process 方法进行具体的 JavaDoc 生成处理
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email mailto:zeka.stack@gmail.com
 * @date 2025.11.30
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

