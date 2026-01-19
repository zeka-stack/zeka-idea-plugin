package dev.dong4j.zeka.stack.idea.javadoc.action;

import com.intellij.openapi.actionSystem.AnActionEvent;

import org.jetbrains.annotations.NotNull;

import dev.dong4j.zeka.stack.idea.plugin.common.statistics.StatisticsUserAction;
import lombok.extern.slf4j.Slf4j;

/**
 * 生成 Javadoc 生成动作类
 * <p>
 * 该类继承自 AbstractGenerateJavaDocAction, 用于处理生成 Javadoc 的 IDE 动作事件,
 * 当用户触发该动作时, 会执行 Javadoc 生成的相关处理逻辑.
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email mailto:zeka.stack@gmail.com
 * @date 2025.11.30
 * @since 1.0.0
 */
@Slf4j
public class GenerateJavadocGenerateAction extends AbstractGenerateJavaDocAction {
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

    /**
     * 解析用户操作类型
     * <p> 根据当前动作事件和编辑器是否存在, 返回对应的统计用户操作类型, 此处固定返回编辑器上下文菜单操作类型.
     *
     * @param e             动作事件对象, 包含事件相关信息
     * @param editorPresent 是否存在编辑器上下文
     * @return 固定返回 {@link StatisticsUserAction#EDITOR_CONTEXT_MENU}, 表示编辑器上下文菜单操作
     */
    @Override
    protected @NotNull StatisticsUserAction resolveUserAction(@NotNull AnActionEvent e, boolean editorPresent) {
        return StatisticsUserAction.EDITOR_CONTEXT_MENU;
    }
}
