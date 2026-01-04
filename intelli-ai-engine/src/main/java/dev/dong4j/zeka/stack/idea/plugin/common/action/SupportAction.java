package dev.dong4j.zeka.stack.idea.plugin.common.action;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;

import org.jetbrains.annotations.NotNull;

import dev.dong4j.zeka.stack.idea.plugin.common.ui.dialog.SupportDialog;
import dev.dong4j.zeka.stack.idea.plugin.common.util.AICommonBundle;

/**
 * 支持操作类
 * <p> 继承自 DumbAwareAction, 提供一个支持对话框的操作功能
 * <p> 在执行动作时, 会显示一个支持对话框
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.04
 * @since 1.0.0
 */
public class SupportAction extends DumbAwareAction {

    /**
     * 构造函数, 初始化支持动作
     * <p> 设置该动作的名称, 描述和图标, 用于在 Help 菜单中显示支持对话框
     *
     */
    public SupportAction() {
        super(AICommonBundle.message("action.support.text"),
              AICommonBundle.message("action.support.description"),
              com.intellij.icons.AllIcons.General.Balloon);
    }

    /**
     * 获取动作更新线程
     * <p> 此方法返回一个非空的 {@link ActionUpdateThread} 对象, 表示该动作的更新线程类型
     *
     * @return 动作更新线程类型 BGT (Background Thread)
     */
    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    /**
     * 执行动作以显示支持对话框
     * <p> 当用户触发该动作时, 会创建并显示一个支持对话框, 用于提供帮助或联系支持服务
     *
     * @param event 动作事件对象, 包含触发动作的相关信息
     */
    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        new SupportDialog().show();
    }
}

