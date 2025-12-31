package dev.dong4j.zeka.stack.idea.plugin.common.whatsnew;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;

import org.jetbrains.annotations.NotNull;

import icons.AICommonIcons;
import dev.dong4j.zeka.stack.idea.plugin.common.util.AICommonBundle;

/**
 * 提供插件中“查看更新内容”功能的实现类
 * <p> 该类用于处理查看插件更新内容的用户操作, 包括设置动作的图标, 文本和描述, 以及执行查看更新内容的逻辑
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2025.10.24
 * @since 1.0.0
 */
public class WhatsNewAction extends DumbAwareAction {
    /**
     * 构造函数, 用于初始化 "What's New" 操作的展示信息
     * <p> 设置该操作的图标, 文本和描述信息, 用于在 IDE 中显示
     */
    public WhatsNewAction() {
        getTemplatePresentation().setIcon(AICommonIcons.PLUGIN);
        getTemplatePresentation().setText(AICommonBundle.message("whatsnew.action.text"));
        getTemplatePresentation().setDescription(AICommonBundle.message("whatsnew.action.description"));
    }

    /**
     * 获取此操作的更新线程
     * <p> 指定用于更新操作状态的线程, 此处返回事件调度线程 (EDT)
     *
     * @return 返回事件调度线程 (EDT)
     */
    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

    /**
     * 处理动作事件, 用于打开 Whats New 编辑器
     * <p> 当接收到动作事件时, 检查项目是否为空, 若不为空则调用 WhatsNewEditorOpener 打开编辑器
     *
     * @param e 动作事件对象, 包含触发动作的上下文信息
     */
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        if (e.getProject() != null) {
            WhatsNewEditorOpener.open(e.getProject());
        }
    }
}
