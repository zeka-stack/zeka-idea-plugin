package dev.dong4j.zeka.stack.idea.javadoc.action;

import com.intellij.openapi.actionSystem.AnActionEvent;

import org.jetbrains.annotations.NotNull;

import dev.dong4j.zeka.stack.idea.plugin.common.statistics.StatisticsUserAction;
import lombok.extern.slf4j.Slf4j;

/**
 * 生成 Javadoc 编辑器动作类
 * <p>
 * 该类继承自 AbstractGenerateJavaDocAction, 用于在编辑器中执行 Javadoc 生成操作,
 * 通过 actionPerformed 方法处理用户触发的动作事件, 调用父类的 process 方法进行具体的 Javadoc 生成处理.
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email mailto:zeka.stack@gmail.com
 * @date 2025.11.30
 * @since 1.0.0
 */
@Slf4j
public class GenerateJavadocForEditorAction extends AbstractGenerateJavaDocAction {
    /**
     * 处理动作事件的回调方法
     * <p>
     * 该方法用于响应用户触发的动作事件, 调用内部的处理方法进行具体操作.
     *
     * @param e 动作事件对象, 包含事件相关的信息
     */
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        process(e, true);
    }

    /**
     * 解析用户操作类型, 用于标识当前操作发生在编辑器上下文菜单中
     * <p>
     * 该方法重写父类方法, 固定返回 {@link StatisticsUserAction.EDITOR_CONTEXT_MENU}, 表示当前操作来自编辑器右键菜单.
     *
     * @param e             动作事件对象, 包含触发事件的上下文信息
     * @param editorPresent 是否存在编辑器上下文, 该参数在当前实现中未被使用
     * @return 固定返回 {@link StatisticsUserAction.EDITOR_CONTEXT_MENU}, 表示编辑器上下文菜单操作
     */
    @Override
    protected @NotNull StatisticsUserAction resolveUserAction(@NotNull AnActionEvent e, boolean editorPresent) {
        return StatisticsUserAction.EDITOR_CONTEXT_MENU;
    }
}
