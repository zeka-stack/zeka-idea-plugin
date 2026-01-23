package dev.dong4j.zeka.stack.idea.plugin.common.action;

import com.intellij.icons.AllIcons;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.ActionUpdateThreadAware;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;

import org.jetbrains.annotations.NotNull;

import dev.dong4j.zeka.stack.idea.plugin.common.util.AICommonBundle;
import dev.dong4j.zeka.stack.idea.plugin.kit.SiteContents;

/**
 * 快速启动操作类
 * <p> 继承自 DumbAwareAction 并实现 ActionUpdateThreadAware 接口, 用于在 IDE 中提供一个快速启动功能按钮, 点击后会打开指定的网页地址 (如 ZEKA_STACK_HOME).</p>
 * <p> 该操作在后台线程中更新, 确保 UI 线程不阻塞.</p>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.23
 * @since x.x.x
 */
public class QuickStartAction extends DumbAwareAction implements ActionUpdateThreadAware {

    /**
     * 初始化快速启动动作, 设置动作文本, 描述和图标
     * <p> 调用父类构造函数, 传入动作文本, 描述和图标资源
     *
     */
    public QuickStartAction() {
        super(AICommonBundle.message("action.quick.start.text"),
              AICommonBundle.message("action.quick.start.description"),
              AllIcons.General.Web);
    }

    /**
     * 处理动作事件, 打开指定网站主页
     * <p> 通过 BrowserUtil 工具类打开常量 SiteContents.ZEKA_STACK_HOME 所指向的网页地址
     *
     * @param event 动作事件对象, 包含触发该动作的上下文信息
     */
    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        BrowserUtil.browse(SiteContents.ZEKA_STACK_HOME);
    }

    /**
     * 获取动作更新线程的执行上下文
     * <p>返回动作更新操作应在后台线程 (BGT) 中执行
     *
     * @return 动作更新线程类型, 固定返回 {@link ActionUpdateThread#BGT}
     */
    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

}
