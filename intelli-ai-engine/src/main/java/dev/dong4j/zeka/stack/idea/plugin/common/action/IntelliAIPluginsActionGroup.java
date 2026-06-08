package dev.dong4j.zeka.stack.idea.plugin.common.action;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import dev.dong4j.zeka.stack.idea.plugin.common.util.AICommonBundle;
import dev.dong4j.zeka.stack.idea.plugin.common.whatsnew.WhatsNewAction;
import org.jetbrains.annotations.NotNull;

/**
 * 智能 AI 插件动作组类
 * <p> 继承自 DefaultActionGroup, 用于定义一组智能 AI 相关的操作动作
 * <p> 该类初始化时会创建一个新的 "What's New" 动作和一个支持动作, 并将它们添加到动作组中
 * <p> 重写了 getActionUpdateThread 方法, 指定动作更新线程为后台线程 (BGT)
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.04
 * @since 1.0.0
 */
public class IntelliAIPluginsActionGroup extends DefaultActionGroup {

    /**
     * 构造函数, 初始化 IntelliAIPluginsActionGroup 菜单组
     * <p> 创建一个包含 "What's New"和"Support" 子菜单的一级菜单项, 并设置为可弹出菜单形式显示
     *
     * @since 1.0.0
     */
    public IntelliAIPluginsActionGroup() {
        super(AICommonBundle.message("settings.display.name"), true);
        add(new WhatsNewAction());
        add(new SupportAction());
        add(new FeedbackTestAction());
        add(new ShowLogAction());
    }

    /**
     * 返回动作更新线程
     * <p> 此方法重写自父类, 用于指定动作更新使用的线程类型
     * <p> 返回 BGT (Background Thread), 表示动作更新将在后台线程中进行
     *
     * @return 动作更新线程类型, 固定为 BGT
     */
    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
