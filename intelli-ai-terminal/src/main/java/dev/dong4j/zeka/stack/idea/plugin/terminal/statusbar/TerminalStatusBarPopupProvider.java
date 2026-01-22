package dev.dong4j.zeka.stack.idea.plugin.terminal.statusbar;

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Separator;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;

import org.jetbrains.annotations.NotNull;

import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderSettings;
import dev.dong4j.zeka.stack.idea.plugin.common.statusbar.AIProviderSelectionActionGroupFactory;
import dev.dong4j.zeka.stack.idea.plugin.common.statusbar.AIStatusBarPopupProvider;
import dev.dong4j.zeka.stack.idea.plugin.common.util.AIProviderUtils;
import dev.dong4j.zeka.stack.idea.plugin.terminal.settings.SettingsState;
import dev.dong4j.zeka.stack.idea.plugin.terminal.settings.TerminalSettingsConfigurable;
import dev.dong4j.zeka.stack.idea.plugin.terminal.util.TerminalBundle;
import lombok.extern.slf4j.Slf4j;

/**
 * 终端状态栏弹窗提供者实现类
 * <p>用于在终端状态栏中提供 AI 服务提供商选择弹窗, 支持切换默认 AI 服务提供商及跳转到设置页面.
 * 该类实现了 {@link AIStatusBarPopupProvider} 接口, 通过构建动作组 (ActionGroup) 动态展示可用的 AI 服务提供商选项,
 * 并支持用户选择后更新全局默认配置. 同时, 提供"打开设置"动作以引导用户进入相关配置界面.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.20
 * @since 1.0.0
 */
@Slf4j
public class TerminalStatusBarPopupProvider implements AIStatusBarPopupProvider {
    /** 用于标识菜单项是否被选中的状态键 */
    private static final Key<Boolean> SELECTED_KEY = Key.create("selected");
    /** 预设的触发前缀选项 */
    private static final String[] TRIGGER_PREFIX_OPTIONS = {"#", "::", "??"};

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
     *   <li> 若已配置 AI 提供者, 将添加提供商选择操作组, 快速设置分隔符, 触发前缀选择, 以及打开设置操作项.</li>
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
        if (!AIProviderUtils.hasAIProvider(project, TerminalBundle.message("settings.display.name"),
                                           TerminalBundle.message("settings.ai.provider.selection"))) {
            group.add(new OpenSettingsAction(project));
            return group;
        }

        group.add(AIProviderSelectionActionGroupFactory.createActionGroup(
            project,
            TerminalBundle.message("statusbar.provider.selection.title"),
            TerminalBundle.message("settings.display.name"),
            TerminalBundle.message("settings.ai.provider.selection"),
            () -> SettingsState.getInstance().providerConfig,
            (providerType, config) -> {
                SettingsState settings = SettingsState.getInstance();
                settings.providerConfig = config;
                AIProviderSettings globalSettings = AIProviderSettings.getInstance();
                globalSettings.updateDefaultProviderConfig(providerType, config);
            }
                                                                         ));
        group.add(Separator.create(TerminalBundle.message("statusbar.quick.settings.title")));
        group.add(new TriggerPrefixActionGroup());
        group.add(new EnableTerminalAIToggleAction());
        group.add(new EnableStreamResponseToggleAction());
        group.add(new EnableTerminalContextToggleAction());
        group.add(new OpenSettingsAction(project));
        return group;
    }

    /**
     * 触发前缀选择操作组
     * <p> 用于在状态栏中显示和切换不同的触发前缀选项, 标题中展示当前选中的前缀.
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.22
     * @since 1.0.0
     */
    private static class TriggerPrefixActionGroup extends DefaultActionGroup {
        /**
         * 初始化触发前缀选择操作组
         * <p> 创建一个包含多个触发前缀选项的动作组, 用于在状态栏中显示和切换不同的触发前缀
         */
        TriggerPrefixActionGroup() {
            super(buildTitle(), true);
            for (String prefix : TRIGGER_PREFIX_OPTIONS) {
                add(new TriggerPrefixAction(prefix));
            }
        }

        /**
         * 更新动作事件的显示文本
         * <p> 根据当前设置的触发前缀构建标题文本, 并设置到动作事件的呈现对象中, 用于在状态栏中显示当前选中的前缀选项
         *
         * @param e 动作事件对象, 包含当前上下文信息
         */
        @Override
        public void update(@NotNull AnActionEvent e) {
            e.getPresentation().setText(buildTitle());
        }

        /**
         * 构建标题文本
         * <p> 根据当前设置的前缀值生成标题, 格式为 "Trigger Prefix (当前前缀)"
         *
         * @return 标题文本
         */
        private static @NotNull String buildTitle() {
            SettingsState settings = SettingsState.getInstance();
            String currentPrefix = settings.triggerPrefix != null && !settings.triggerPrefix.isEmpty()
                                   ? settings.triggerPrefix : "#";
            return TerminalBundle.message("settings.terminal.trigger.prefix") + " (" + currentPrefix + ")";
        }
    }

    /**
     * 触发前缀切换动作
     * <p> 在状态栏菜单中显示不同的触发前缀选项, 并通过选中状态提示当前值.
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.22
     * @since 1.0.0
     */
    private static class TriggerPrefixAction extends AnAction {
        /** 触发前缀值 */
        private final String prefix;

        /**
         * 初始化触发前缀动作
         *
         * @param prefix 触发前缀值
         */
        TriggerPrefixAction(@NotNull String prefix) {
            super(prefix);
            this.prefix = prefix;
        }

        /**
         * 执行触发前缀切换操作
         * <p> 获取当前的设置状态, 并将指定的触发前缀值更新到设置中.
         *
         * @param e Action 事件对象, 包含与动作相关的上下文信息
         */
        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            SettingsState settings = SettingsState.getInstance();
            settings.triggerPrefix = prefix;
        }

        /**
         * 更新操作事件的状态
         * <p> 根据当前设置的触发前缀值来更新操作事件的选中状态
         *
         * @param e 操作事件对象
         */
        @Override
        public void update(@NotNull AnActionEvent e) {
            SettingsState settings = SettingsState.getInstance();
            String currentPrefix = settings.triggerPrefix != null ? settings.triggerPrefix : "#";
            boolean isSelected = prefix.equals(currentPrefix);
            e.getPresentation().putClientProperty(SELECTED_KEY, isSelected);
        }

        /**
         * 获取动作更新线程
         * <p> 返回用于更新动作的线程类型
         *
         * @return 动作更新线程类型, 固定为 BGT
         */
        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.BGT;
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
        /**
         * 构造函数, 初始化 Terminal AI 启用 / 禁用切换动作
         * <p> 使用 TerminalBundle 中的本地化消息设置动作的显示文本
         *
         * @see TerminalBundle#message(String)
         */
        EnableTerminalAIToggleAction() {
            super(TerminalBundle.message("settings.terminal.enable"));
        }

        /**
         * 获取动作更新线程的执行上下文
         * <p>返回该动作在后台线程 (BGT) 中更新, 确保不阻塞主线程
         *
         * @return 动作更新线程类型, 固定返回 {@link ActionUpdateThread#BGT}
         */
        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.BGT;
        }

        /**
         * 判断 Terminal AI 是否已启用
         * <p> 根据当前设置状态返回 Terminal AI 的启用情况
         *
         * @param e Action 事件对象
         * @return 如果 Terminal AI 已启用则返回 true, 否则返回 false
         */
        @Override
        public boolean isSelected(@NotNull AnActionEvent e) {
            return SettingsState.getInstance().enableTerminalAI;
        }

        /**
         * 设置 Terminal AI 的启用状态
         * <p> 更新设置状态中 Terminal AI 的启用标志
         *
         * @param e     触发动作的事件对象, 不能为 null
         * @param state 新的启用状态 (true 表示启用,false 表示禁用)
         */
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
        /**
         * 初始化启用流式响应切换动作
         * <p> 通过调用父类构造函数, 使用终端设置中定义的本地化消息作为动作标题
         *
         */
        EnableStreamResponseToggleAction() {
            super(TerminalBundle.message("settings.terminal.stream.enable"));
        }

        /**
         * 获取动作更新线程的执行上下文
         * <p>返回动作更新操作应在后台线程 (BGT) 中执行, 确保不阻塞主线程
         *
         * @return 动作更新线程类型, 固定为 {@code ActionUpdateThread.BGT}
         */
        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.BGT;
        }

        /**
         * 判断当前是否启用了流式响应功能
         * <p> 根据设置状态返回流式响应功能的启用状态
         *
         * @param e 动作事件对象
         * @return 如果启用了流式响应功能则返回 true, 否则返回 false
         */
        @Override
        public boolean isSelected(@NotNull AnActionEvent e) {
            return SettingsState.getInstance().enableStreamResponse;
        }

        /**
         * 设置流式响应开关的状态
         * <p> 根据传入的状态更新设置中的流式响应开关状态
         *
         * @param e     事件对象
         * @param state 新的状态,true 表示开启流式响应,false 表示关闭
         */
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
        /**
         * 构造函数, 用于初始化启用终端上下文切换动作
         * <p> 该构造函数调用父类构造函数并传入从资源文件中获取的国际化字符串作为动作名称
         *
         */
        EnableTerminalContextToggleAction() {
            super(TerminalBundle.message("settings.terminal.context.enable"));
        }

        /**
         * 获取动作更新线程
         * <p>返回后台线程 (BGT) 作为动作更新线程, 确保动作状态更新在后台执行
         *
         * @return 动作更新线程, 始终返回 {@link ActionUpdateThread#BGT}
         */
        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.BGT;
        }

        /**
         * 判断当前动作是否被选中
         * <p> 根据全局设置状态判断终端上下文检测功能是否启用
         *
         * @param e 动作事件对象, 包含当前操作的上下文信息
         * @return 如果终端上下文检测功能已启用则返回 true, 否则返回 false
         */
        @Override
        public boolean isSelected(@NotNull AnActionEvent e) {
            return SettingsState.getInstance().enableTerminalContext;
        }

        /**
         * 设置终端上下文检测的启用状态
         * <p> 根据传入的布尔值更新全局设置中的终端上下文检测开关状态
         *
         * @param e     动作事件对象, 用于获取上下文信息
         * @param state 是否启用终端上下文检测的布尔值
         */
        @Override
        public void setSelected(@NotNull AnActionEvent e, boolean state) {
            SettingsState.getInstance().enableTerminalContext = state;
        }
    }
}
