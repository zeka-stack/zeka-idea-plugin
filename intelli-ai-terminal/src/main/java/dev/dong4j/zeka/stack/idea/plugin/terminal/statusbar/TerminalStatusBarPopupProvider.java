package dev.dong4j.zeka.stack.idea.plugin.terminal.statusbar;

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Separator;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIProviderType;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderSettings;
import dev.dong4j.zeka.stack.idea.plugin.common.statusbar.AIStatusBarPopupProvider;
import dev.dong4j.zeka.stack.idea.plugin.common.util.AIProviderUtils;
import dev.dong4j.zeka.stack.idea.plugin.terminal.settings.SettingsState;
import dev.dong4j.zeka.stack.idea.plugin.terminal.settings.TerminalSettingsConfigurable;
import dev.dong4j.zeka.stack.idea.plugin.terminal.util.TerminalBundle;
import icons.AICommonIcons;
import lombok.extern.slf4j.Slf4j;

/**
 * 终端状态栏弹窗提供者实现类
 * <p>用于在终端状态栏中提供 AI 服务提供商选择弹窗, 支持切换默认 AI 服务提供商及跳转到设置页面.
 * 该类实现了 {@link AIStatusBarPopupProvider} 接口, 通过构建动作组 (ActionGroup) 动态展示可用的 AI 服务提供商选项,
 * 并支持用户选择后更新全局默认配置. 同时, 提供“打开设置”动作以引导用户进入相关配置界面.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.20
 * @since 1.0.0
 */
@Slf4j
public class TerminalStatusBarPopupProvider implements AIStatusBarPopupProvider {
    /** 标记当前提供者是否被选中的状态键 */
    private static final Key<Boolean> SELECTED_KEY = Key.create("selected");

    /**
     * 获取状态栏弹出组的名称
     * <p> 返回与状态栏弹出相关的组名称, 用于在 UI 中显示该弹出菜单的标题.
     *
     * @return 组名称, 不能为空
     */
    @Override
    public @NotNull String getGroupName() {
        return TerminalBundle.message("statusbar.provider.popup.title");
    }

    /**
     * 创建状态栏弹出菜单的动作组
     * <p> 根据当前项目是否配置 AI 提供者, 动态构造包含相关操作的 {@link ActionGroup}.
     * <ul>
     *   <li> 若项目中没有配置任何 AI 提供者, 则仅添加打开设置的操作项.</li>
     *   <li> 若已配置 AI 提供者, 将添加提供商选择操作组, 快速设置分隔符以及打开设置操作项.</li>
     * </ul>
     * 该动作组使用 {@link DefaultActionGroup} 包装各个子操作, 并在构造完成后返回给调用方.
     *
     * @param project 当前 IntelliJ IDEA 项目上下文
     * @param context 当前 UI 事件上下文 (此实现未使用)
     * @return 包含所有需要显示在状态栏弹出菜单中的 {@link ActionGroup}
     */
    @Override
    public @NotNull ActionGroup createActionGroup(@NotNull Project project, @NotNull DataContext context) {
        DefaultActionGroup group = new DefaultActionGroup();
        if (!AIProviderUtils.hasAIProvider(project, TerminalBundle.message("settings.display.name"), TerminalBundle.message("settings.ai" +
                                                                                                                            ".provider" +
                                                                                                                            ".selection"))) {
            group.add(new OpenSettingsAction(project));
            return group;
        }

        List<AIProviderConfig> providers = AIProviderUtils.getProviders();
        group.add(new ProviderSelectionActionGroup(project, providers));
        group.add(Separator.create(TerminalBundle.message("statusbar.quick.settings.title")));
        group.add(new EnableTerminalAIToggleAction());
        group.add(new EnableStreamResponseToggleAction());
        group.add(new EnableTerminalContextToggleAction());
        group.add(new OpenSettingsAction(project));
        return group;
    }

    /**
     * 获取当前默认的 AI 提供商类型
     * <p> 从全局设置中获取当前配置的 AI 提供商类型, 如果未配置则返回默认的 QIANWEN 类型
     *
     * @return 当前的 AI 提供商类型, 如果设置中未指定则返回 {@link AIProviderType#QIANWEN}
     */
    @NotNull
    private AIProviderType getCurrentProviderType() {
        SettingsState settings = SettingsState.getInstance();
        return settings.providerConfig != null ? settings.providerConfig.providerType : AIProviderType.QIANWEN;
    }

    /**
     * 切换默认的 AI 服务提供商配置
     * <p> 该方法用于更新当前项目的默认 AI 提供商配置, 并同步到全局设置中.
     *
     * @param providerType AI 提供商类型, 不能为 null
     * @param config       要切换的 AI 提供商配置对象, 不能为 null
     */
    private void switchDefaultProvider(@NotNull AIProviderType providerType, @NotNull AIProviderConfig config) {
        SettingsState settings = SettingsState.getInstance();
        settings.providerConfig = config;
        AIProviderSettings globalSettings = AIProviderSettings.getInstance();
        globalSettings.updateDefaultProviderConfig(providerType, config);
    }

    /**
     * 切换 AI 服务商动作类
     * <p> 该内部类用于在 IDE 中提供一个切换默认 AI 服务商的操作, 通常作为 UI 操作按钮或菜单项使用.
     * <p> 主要功能包括:
     * <ul>
     * <li> 根据配置显示对应的服务商图标和名称 </li>
     * <li> 执行切换 AI 服务商的异步操作 </li>
     * <li> 更新当前操作状态 (是否被选中)</li>
     * </ul>
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.20
     * @since 1.0.0
     */
    private class SwitchProviderAction extends AnAction {
        /** 项目实例, 表示当前操作所在的 IntelliJ 项目上下文 */
        private final Project project;
        /** AI 提供商配置信息, 包含提供商类型, 模型名称等配置项, 用于切换默认 AI 服务商. */
        private final AIProviderConfig config;

        /**
         * 构造函数, 初始化 SwitchProviderAction 对象
         * <p> 设置动作名称为 AIProviderConfig 中的 modelName, 并根据 providerType 设置图标
         *
         * @param project 当前项目实例
         * @param config  AI 提供商配置对象
         */
        SwitchProviderAction(@NotNull Project project, @NotNull AIProviderConfig config) {
            super(config.modelName);
            this.project = project;
            this.config = config;
            if (config.providerType != null) {
                getTemplatePresentation().setIcon(AICommonIcons.getProviderIcon(config.providerType));
            }
        }

        /**
         * 执行切换默认 AI 服务提供商的操作
         * <p> 当用户触发此操作时, 会检查当前项目中是否配置了有效的 AI 提供商. 如果有效, 则在后台线程中执行切换操作.
         *
         * @param e AnActionEvent 事件对象, 提供上下文信息
         */
        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            if (!AIProviderUtils.hasAIProvider(project, config, TerminalBundle.message("settings.display.name"), TerminalBundle.message(
                "settings.ai.provider.selection"))) {
                return;
            }

            ApplicationManager.getApplication().invokeLater(() -> {
                if (project.isDisposed()) {
                    return;
                }
                try {
                    ApplicationManager.getApplication().runWriteAction(() -> {
                        AIProviderConfig copy = config.copy();
                        copy.providerType = config.providerType;
                        switchDefaultProvider(config.providerType, copy);
                    });
                } catch (Exception exception) {
                    log.debug("切换默认服务商失败", exception);
                }
            }, ModalityState.defaultModalityState());
        }

        /**
         * 更新动作的显示状态, 根据当前配置和选中的提供者类型设置是否选中
         * <p> 此方法用于在用户界面中更新该动作的显示状态, 判断当前配置的提供者类型是否与系统当前选中的提供者类型一致, 并据此设置动作的选中状态.
         *
         * @param e 动作事件对象, 包含事件相关信息
         */
        @Override
        public void update(@NotNull AnActionEvent e) {
            AIProviderType currentType = getCurrentProviderType();
            boolean isSelected = config != null && config.providerType == currentType;
            e.getPresentation().putClientProperty(SELECTED_KEY, isSelected);
        }

        /**
         * 获取动作更新线程
         * <p> 返回此动作更新应在哪个线程中执行. 此实现返回后台线程 (BGT), 表示动作更新应在后台线程中进行.
         *
         * @return 动作更新线程类型, 此处返回 {@link ActionUpdateThread#BGT}
         */
        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.BGT;
        }
    }

    /**
     * 提供者选择操作组类
     * <p> 继承自 DefaultActionGroup, 用于创建一组动作, 每个动作对应一个 AI 提供商配置.
     * <p> 在构造函数中, 根据传入的项目和提供商配置列表, 动态地添加切换提供商的动作.
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.20
     * @since 1.0.0
     */
    private class ProviderSelectionActionGroup extends DefaultActionGroup {
        /**
         * 初始化提供者选择操作组
         * <p> 创建一个包含多个提供者切换动作的动作组, 用于在状态栏中显示和切换不同的 AI 提供者
         * <p> 该构造函数会根据传入的提供者列表动态添加相应的切换动作
         *
         * @param project   当前项目实例, 用于创建切换动作时的上下文
         * @param providers 提供者配置列表, 包含所有可用的 AI 提供者配置, 如果为 null 则不添加任何动作
         */
        ProviderSelectionActionGroup(@NotNull Project project, List<AIProviderConfig> providers) {
            super(TerminalBundle.message("statusbar.provider.selection.title"), true);
            if (providers != null) {
                for (AIProviderConfig config : providers) {
                    add(new SwitchProviderAction(project, config));
                }
            }
        }
    }

    /**
     * 状态栏打开设置操作类
     * <p> 用于在状态栏中创建一个动作, 点击后可以打开项目的设置对话框, 支持在后台线程中更新状态, 避免阻塞主线程.
     * <p> 该类继承自 {@link AnAction}, 提供标准的 UI 动作行为, 包括动作名称设置, 事件处理和更新线程配置.
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.20
     * @since 1.0.0
     */
    private static class OpenSettingsAction extends AnAction {
        /** 项目实例, 用于访问项目相关资源和功能 */
        private final Project project;

        /**
         * 初始化打开设置操作
         * <p> 构造函数用于初始化打开设置操作, 设置操作名称并绑定项目实例
         *
         * @param project 项目实例, 不能为 null
         */
        OpenSettingsAction(@NotNull Project project) {
            super(TerminalBundle.message("statusbar.open.settings"));
            this.project = project;
        }

        /**
         * 处理动作事件以打开设置对话框
         * <p> 检查项目是否已销毁, 如果未销毁则显示设置对话框
         *
         * @param e 动作事件对象, 不能为 null
         * @since 1.0
         */
        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            if (project.isDisposed()) {
                return;
            }
            ShowSettingsUtil.getInstance().showSettingsDialog(project, TerminalSettingsConfigurable.class);
        }

        /**
         * 获取此操作的更新线程类型
         * <p>指定在后台线程 (BGT) 中执行操作的更新逻辑, 以避免阻塞 UI 线程
         *
         * @return 返回 {@link ActionUpdateThread#BGT} 表示该操作应在后台线程中更新
         */
        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.BGT;
        }
    }

    /**
     * 启用 Terminal AI 的切换动作
     * <p> 使用状态栏快捷设置控制 Terminal AI 开关
     */
    private static class EnableTerminalAIToggleAction extends com.intellij.openapi.actionSystem.ToggleAction {
        EnableTerminalAIToggleAction() {
            super(TerminalBundle.message("settings.terminal.enable"));
        }

        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.BGT;
        }

        @Override
        public boolean isSelected(@NotNull AnActionEvent e) {
            return SettingsState.getInstance().enableTerminalAI;
        }

        @Override
        public void setSelected(@NotNull AnActionEvent e, boolean state) {
            SettingsState.getInstance().enableTerminalAI = state;
        }
    }

    /**
     * 启用流式输出的切换动作
     * <p> 使用状态栏快捷设置控制流式响应开关
     */
    private static class EnableStreamResponseToggleAction extends com.intellij.openapi.actionSystem.ToggleAction {
        EnableStreamResponseToggleAction() {
            super(TerminalBundle.message("settings.terminal.stream.enable"));
        }

        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.BGT;
        }

        @Override
        public boolean isSelected(@NotNull AnActionEvent e) {
            return SettingsState.getInstance().enableStreamResponse;
        }

        @Override
        public void setSelected(@NotNull AnActionEvent e, boolean state) {
            SettingsState.getInstance().enableStreamResponse = state;
        }
    }

    /**
     * 启用上下文检测的切换动作
     * <p> 使用状态栏快捷设置控制上下文检测开关
     */
    private static class EnableTerminalContextToggleAction extends com.intellij.openapi.actionSystem.ToggleAction {
        EnableTerminalContextToggleAction() {
            super(TerminalBundle.message("settings.terminal.context.enable"));
        }

        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.BGT;
        }

        @Override
        public boolean isSelected(@NotNull AnActionEvent e) {
            return SettingsState.getInstance().enableTerminalContext;
        }

        @Override
        public void setSelected(@NotNull AnActionEvent e, boolean state) {
            SettingsState.getInstance().enableTerminalContext = state;
        }
    }
}
