package dev.dong4j.zeka.stack.idea.plugin.common.action;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;

import org.jetbrains.annotations.NotNull;

import dev.dong4j.zeka.stack.idea.plugin.common.ui.dialog.SupportDialog;
import dev.dong4j.zeka.stack.idea.plugin.common.util.AICommonBundle;

/**
 * 捐赠动作类
 * <p> 继承自 DumbAwareAction, 用于在通知中显示支持对话框, 提供用户触发捐赠操作的支持功能
 * <p> 通过指定的文本初始化捐赠动作, 并设置相应的描述和图标
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.04
 * @since 1.0.0
 */
public class DonateAction extends DumbAwareAction {

    /**
     * 初始化捐赠动作
     * <p> 使用指定的文本创建一个捐赠动作实例, 并设置描述和图标
     *
     * @param text 动作显示的文本, 不能为 null
     */
    public DonateAction(String text) {
        super(text,
              AICommonBundle.message("action.donate.description"),
              com.intellij.icons.AllIcons.General.Balloon);
    }

    /**
     * 获取动作更新线程
     * <p> 返回此动作在哪个线程中更新. 该动作将在后台线程 (BGT) 中更新.
     *
     * @return 动作更新线程类型, 此处返回 {@link ActionUpdateThread#BGT}
     */
    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    /**
     * 处理动作执行事件
     * <p> 在用户触发捐赠动作时, 显示支持对话框以提供帮助或支持选项
     *
     * @param event 动作事件对象, 包含触发动作的上下文信息, 不能为 null
     */
    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        new SupportDialog().show();
    }
}

