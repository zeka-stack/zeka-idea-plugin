package dev.dong4j.zeka.stack.idea.plugin.common.statusbar;

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Separator;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.impl.status.EditorBasedStatusBarPopup;
import com.intellij.util.IconUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import javax.swing.Icon;

import dev.dong4j.zeka.stack.idea.plugin.common.EngineContents;
import dev.dong4j.zeka.stack.idea.plugin.common.settings.AICommonSettingsConfigurable;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderSettings;
import dev.dong4j.zeka.stack.idea.plugin.common.config.IntelliAgentSettings;
import dev.dong4j.zeka.stack.idea.plugin.common.config.ResponseLanguage;
import dev.dong4j.zeka.stack.idea.plugin.common.agent.IntelliAgentManager;
import dev.dong4j.zeka.stack.idea.plugin.common.agent.IntelliAgentUpdateChecker;
import dev.dong4j.zeka.stack.idea.plugin.common.util.AICommonBundle;
import dev.dong4j.zeka.stack.idea.plugin.common.util.NotificationUtil;
import dev.dong4j.zeka.stack.idea.plugin.common.whatsnew.WhatsNewEditorOpener;
import dev.dong4j.zeka.stack.idea.plugin.kit.SettingsUtil;
import icons.AICommonIcons;

/**
 * AI 状态栏小部件类
 * <p> 用于在 IDEA 编辑器的状态栏中显示 AI 相关功能的快捷入口和状态信息, 支持动态加载引擎功能, 扩展插件菜单以及配置项的快速访问.
 * <p> 该小部件通过插件系统集成多种 AI 功能, 包括语言设置, 日志级别, 自动更新, 代理启动与停止等操作, 并支持通过右键菜单扩展第三方功能.
 * <p> 使用示例:
 * <pre>{@code
 * // 该类通常由插件框架自动注册, 无需手动创建实例
 * // 通过 IDEA 的工具栏或状态栏自动显示
 * }</pre>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.02
 * @since 1.0.0
 */
public class AIStatusBarWidget extends EditorBasedStatusBarPopup {
    /** 状态栏小部件的唯一标识符 */
    public static final String WIDGET_ID =
        "dev.dong4j.zeka.stack.idea.plugin.common.statusbar.AIStatusBarWidget";

    /**
     * 构造一个新的 AIStatusBarWidget 实例
     * <p> 该状态栏小部件基于指定的项目初始化, 并且默认不可折叠显示
     *
     * @param project 所属的项目对象, 不能为 null
     */
    public AIStatusBarWidget(@NotNull Project project) {
        super(project, false);
    }

    /**
     * 创建状态栏小部件实例
     * <p> 返回一个新的 {@link AIStatusBarWidget} 实例, 用于在状态栏中显示插件相关信息
     *
     * @param project 当前项目对象, 不可为 null
     * @return 返回新的 AIStatusBarWidget 实例, 不会为 null
     */
    @Override
    protected @NotNull StatusBarWidget createInstance(@NotNull Project project) {
        return new AIStatusBarWidget(project);
    }

    /**
     * 返回状态栏小部件的唯一标识符
     *
     * @return 小部件的 ID 字符串, 用于在系统中唯一标识该状态栏组件
     */
    @Override
    public @NotNull String ID() {
        return WIDGET_ID;
    }

    /**
     * 获取状态栏组件的显示状态
     * <p> 根据传入的文件对象生成状态栏组件的显示状态, 包括提示信息, 图标等.
     *
     * @param file 与状态栏组件相关的文件对象, 可以为 null
     * @return 状态栏组件的显示状态对象, 包含提示信息和图标
     */
    @Override
    protected @NotNull WidgetState getWidgetState(@Nullable VirtualFile file) {
        String tooltip = AICommonBundle.message("statusbar.engine.tooltip");
        WidgetState state = new WidgetState(tooltip, "", true);
        Icon icon = IconUtil.scale(AICommonIcons.TOOL_ICON, null, 0.8125f);
        state.setIcon(icon);
        return state;
    }

    /**
     * 创建弹出菜单
     * <p> 根据当前上下文创建一个包含引擎操作和扩展插件的弹出菜单
     *
     * @param context 数据上下文, 用于获取当前编辑器状态等信息
     * @return 弹出菜单对象, 如果无法创建则返回 null
     */
    @Override
    protected @Nullable ListPopup createPopup(@NotNull DataContext context) {
        DefaultActionGroup root = new DefaultActionGroup();
        DefaultActionGroup engineActions = buildEngineActionGroup();
        for (com.intellij.openapi.actionSystem.AnAction action
            : engineActions.getChildren(ActionManager.getInstance())) {
            root.add(action);
        }

        List<AIStatusBarPopupProvider> providers = AIStatusBarPopupProvider.EP_NAME.getExtensionList();
        boolean hasExtensions = false;
        if (!providers.isEmpty()) {
            root.add(Separator.create());
            root.add(Separator.create(AICommonBundle.message("statusbar.engine.extensions.group.title")));
        }
        for (AIStatusBarPopupProvider provider : providers) {
            if (!provider.isAvailable(getProject())) {
                continue;
            }
            ActionGroup group = provider.createActionGroup(getProject(), context);
            if (group == null) {
                continue;
            }
            DefaultActionGroup submenu = new DefaultActionGroup(provider.getGroupName(), true);
            if (group instanceof DefaultActionGroup) {
                for (com.intellij.openapi.actionSystem.AnAction action
                    : ((DefaultActionGroup) group).getChildren(ActionManager.getInstance())) {
                    submenu.add(action);
                }
            } else {
                submenu.add(group);
            }
            root.add(submenu);
            hasExtensions = true;
        }

        root.add(Separator.create());
        root.add(new OpenEngineSettingsAction(getProject()));
        root.add(new OpenPersonalInfoDialogAction(getProject()));

        return JBPopupFactory.getInstance().createActionGroupPopup(
            AICommonBundle.message("statusbar.engine.popup.title"),
            root,
            context,
            JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
            true
                                                                  );
    }

    /**
     * 构建引擎功能操作组
     * <p> 用于创建包含各种引擎相关操作的默认动作组, 包括语言设置, 日志级别, 自动更新检查, 代理启动 / 停止等功能.
     *
     * @return 包含引擎相关操作的默认动作组
     */
    @NotNull
    private DefaultActionGroup buildEngineActionGroup() {
        DefaultActionGroup engineGroup = new DefaultActionGroup();
        engineGroup.add(new OutputLanguageActionGroup());
        engineGroup.add(new VerboseLoggingToggleAction());
        engineGroup.add(new AutoUpdateCheckToggleAction());
        engineGroup.add(new AutoStartAgentToggleAction());
        engineGroup.add(new AutoUpdateAgentToggleAction());
        engineGroup.add(new StartStopAgentAction());
        engineGroup.add(new WhatsNewAction());
        return engineGroup;
    }

    /**
     * 打开引擎设置的动作类
     * <p> 继承自 IntelliJ IDEA 的 AnAction 类, 用于在项目中打开特定的引擎设置对话框
     * <p> 当用户触发此动作时, 会检查当前项目是否已被销毁, 若未销毁, 则显示引擎设置对话框
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.02
     * @since 1.0.0
     */
    private static class OpenEngineSettingsAction extends com.intellij.openapi.actionSystem.AnAction {
        /** 项目实例, 用于访问项目相关的资源和功能 */
        private final Project project;

        /**
         * 构造函数, 初始化 OpenEngineSettingsAction 对象
         * <p> 设置动作名称为从 AICommonBundle 中获取的 "statusbar.engine.open.settings" 消息, 并将项目实例赋值给成员变量 project
         *
         * @param project 项目实例, 不能为 null
         */
        OpenEngineSettingsAction(@NotNull Project project) {
            super(AICommonBundle.message("statusbar.engine.open.settings"));
            this.project = project;
        }

        /**
         * 处理动作事件以打开工程设置对话框
         * <p> 检查项目是否已销毁, 如果未销毁则显示设置对话框
         *
         * @param e 动作事件对象, 不能为 null
         * @since 1.0
         */
        @Override
        public void actionPerformed(@NotNull com.intellij.openapi.actionSystem.AnActionEvent e) {
            if (project.isDisposed()) {
                return;
            }
            SettingsUtil.openSettings(project, AICommonSettingsConfigurable.class);
        }
    }

    /**
     * 输出语言操作组类
     * <p> 用于在 IDE 的上下文菜单中动态生成与响应语言相关的操作项, 支持根据当前配置的语言设置动态构建菜单项.
     * <p> 该类继承自 DefaultActionGroup, 用于在用户界面中展示语言选择功能, 允许用户切换响应语言.
     * <p> 使用示例:
     * <pre>{@code
     * OutputLanguageActionGroup group = new OutputLanguageActionGroup();
     * // 该组将包含所有 ResponseLanguage 枚举值对应的操作项
     * }</pre>
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.02
     * @since 1.0.0
     */
    private static class OutputLanguageActionGroup extends DefaultActionGroup {
        /**
         * 构造函数, 初始化 OutputLanguageActionGroup 对象
         * <p> 该构造函数继承自父类 DefaultActionGroup, 并根据 ResponseLanguage 的所有枚举值创建相应的 OutputLanguageAction 实例并添加到组中
         *
         * @since 1.0
         */
        OutputLanguageActionGroup() {
            super(getTitle(), true);
            for (ResponseLanguage language : ResponseLanguage.values()) {
                add(new OutputLanguageAction(language));
            }
        }

        /**
         * 获取当前设置的注释语言标题
         * <p> 根据当前 AI 提供商设置中的响应语言, 返回对应的注释语言标题, 格式为 "设置注释语言 (语言描述)".
         * <p> 如果未设置响应语言, 则默认使用中文 (ZH).
         *
         * @return 当前配置的注释语言标题, 格式为 "设置注释语言 (语言描述)", 始终不为 null
         */
        @NotNull
        private static String getTitle() {
            AIProviderSettings settings = AIProviderSettings.getInstance();
            ResponseLanguage current = settings.responseLanguage != null ? settings.responseLanguage : ResponseLanguage.ZH;
            return AICommonBundle.message("settings.comment.language") + " (" + current.getDesc() + ")";
        }
    }

    /**
     * 输出语言操作类
     * <p> 用于在 IntelliJ IDEA 插件中实现响应语言的切换功能, 支持用户在不同语言之间进行选择和切换.
     * <p> 该类继承自 IntelliJ 的 AnAction, 用于创建可切换的 UI 按钮或菜单项, 允许用户设置 AI 响应的输出语言.
     * <p> 当用户点击该操作时, 会更新全局配置中的响应语言设置.
     * <p> 使用示例:
     * <pre>{@code
     * OutputLanguageAction action = new OutputLanguageAction(ResponseLanguage.CHINESE);
     * action.actionPerformed(event);
     * }</pre>
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.02
     * @since 1.0.0
     */
    private static class OutputLanguageAction extends com.intellij.openapi.actionSystem.AnAction {
        /** 语言设置, 用于指定响应的语言类型 */
        private final ResponseLanguage language;

        /**
         * 初始化语言选择操作
         * <p> 创建一个用于设置响应语言的操作项, 该操作项会更新 AI 提供商的设置中的响应语言
         *
         * @param language 要设置的响应语言, 不能为 null
         */
        OutputLanguageAction(@NotNull ResponseLanguage language) {
            super(language.getDesc());
            this.language = language;
        }

        /**
         * 处理动作事件, 设置响应语言
         * <p> 当用户选择某个语言选项时, 将该语言设置为 AI 提供商的响应语言
         * <p> 此方法通常在用户界面中点击某个语言选项时被调用
         *
         * @param e 动作事件对象, 包含事件的上下文信息, 不能为 null
         * @since 1.0
         */
        @Override
        public void actionPerformed(@NotNull com.intellij.openapi.actionSystem.AnActionEvent e) {
            AIProviderSettings settings = AIProviderSettings.getInstance();
            settings.responseLanguage = language;
        }

        /**
         * 更新动作状态以反映当前响应语言的选择情况
         * <p> 此方法用于根据当前设置的响应语言更新动作的显示状态, 例如选中状态
         *
         * @param e 动作事件对象, 包含与动作触发相关的上下文信息
         */
        @Override
        public void update(@NotNull com.intellij.openapi.actionSystem.AnActionEvent e) {
            AIProviderSettings settings = AIProviderSettings.getInstance();
            boolean isSelected = settings.responseLanguage == language;
            e.getPresentation().putClientProperty(
                com.intellij.openapi.actionSystem.Toggleable.SELECTED_PROPERTY, isSelected);
        }

        /**
         * 获取此操作的更新线程类型
         * <p>该方法用于指定在哪个线程中执行动作的更新逻辑. 返回后台线程 (BGT) 表示适合执行较轻量的操作, 不会阻塞 UI.
         *
         * @return 返回 {@link com.intellij.openapi.actionSystem.ActionUpdateThread#BGT} 表示使用后台线程进行更新
         */
        @Override
        public @NotNull com.intellij.openapi.actionSystem.ActionUpdateThread getActionUpdateThread() {
            return com.intellij.openapi.actionSystem.ActionUpdateThread.BGT;
        }
    }

    /**
     * 自动更新检查切换动作类
     * <p> 用于在 IDE 中提供一个切换自动更新检查功能的按钮, 用户可以通过该按钮开启或关闭自动检查更新的功能.
     * <p> 该动作继承自 ToggleAction, 支持状态切换, 并根据当前设置状态决定按钮是否被选中.
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.02
     * @since 1.0.0
     */
    private static class AutoUpdateCheckToggleAction extends com.intellij.openapi.actionSystem.ToggleAction {
        /**
         * 构造一个自动更新检查切换操作
         * <p> 该构造函数初始化一个 ToggleAction, 用于控制是否启用自动更新检查功能.
         * <p> 按钮的显示文本通过 AICommonBundle.message("settings.auto.update") 获取国际化消息.
         */
        AutoUpdateCheckToggleAction() {
            super(AICommonBundle.message("settings.auto.update"));
        }

        /**
         * 获取动作更新线程
         * <p>返回此动作的更新线程类型, 指定该动作的更新操作应在后台线程 (BGT) 中执行
         *
         * @return 动作更新线程类型, 始终返回 {@link com.intellij.openapi.actionSystem.ActionUpdateThread#BGT}
         */
        @Override
        public @NotNull com.intellij.openapi.actionSystem.ActionUpdateThread getActionUpdateThread() {
            return com.intellij.openapi.actionSystem.ActionUpdateThread.BGT;
        }

        /**
         * 判断当前操作是否被选中
         * <p> 用于 ToggleAction 显示当前动作的状态, 返回 AIProviderSettings 中的 lastUpdateCheck 值
         *
         * @param e AnActionEvent 事件对象, 提供上下文信息
         * @return 如果自动更新检查功能已启用则返回 true, 否则返回 false
         */
        @Override
        public boolean isSelected(@NotNull com.intellij.openapi.actionSystem.AnActionEvent e) {
            return AIProviderSettings.getInstance().lastUpdateCheck;
        }

        /**
         * 设置自动更新检查的状态
         * <p> 根据给定的状态更新自动更新检查的开关状态
         *
         * @param e     AnActionEvent 事件对象, 不能为 null
         * @param state 自动更新检查的新状态
         */
        @Override
        public void setSelected(@NotNull com.intellij.openapi.actionSystem.AnActionEvent e, boolean state) {
            AIProviderSettings.getInstance().lastUpdateCheck = state;
        }
    }

    /**
     * 日志详细模式切换操作类
     * <p> 用于在 IDE 中切换 AI 提供者日志的详细模式, 控制是否显示更详细的日志信息
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.02
     * @since 1.0.0
     */
    private static class VerboseLoggingToggleAction extends com.intellij.openapi.actionSystem.ToggleAction {
        /**
         * 构造函数, 初始化 VerboseLoggingToggleAction 对象
         * <p> 调用父类构造函数, 传入日志设置的显示文本
         *
         */
        VerboseLoggingToggleAction() {
            super(AICommonBundle.message("settings.verbose.logging"));
        }

        /**
         * 返回动作更新线程
         * <p> 此方法重写自父类, 指定该动作在后台线程中进行更新
         *
         * @return 动作更新线程, 始终返回 BGT (Background Thread)
         */
        @Override
        public @NotNull com.intellij.openapi.actionSystem.ActionUpdateThread getActionUpdateThread() {
            return com.intellij.openapi.actionSystem.ActionUpdateThread.BGT;
        }

        /**
         * 判断当前动作是否被选中
         * <p> 根据 AI 提供商设置中的详细日志记录选项状态, 返回当前动作是否处于选中状态
         *
         * @param e 动作事件, 包含当前操作上下文信息, 不能为 null
         * @return 如果详细日志记录选项已启用, 则返回 true; 否则返回 false
         */
        @Override
        public boolean isSelected(@NotNull com.intellij.openapi.actionSystem.AnActionEvent e) {
            return AIProviderSettings.getInstance().verboseLogging;
        }

        /**
         * 设置“详细日志”功能的启用状态
         * <p> 根据传入的布尔值 state 更新 AIProviderSettings 中的 verboseLogging 配置项, 用于控制是否开启详细日志输出
         *
         * @param e     AnActionEvent 事件对象, 包含触发此操作的上下文信息
         * @param state 如果为 true, 则启用详细日志; 如果为 false, 则禁用详细日志
         */
        @Override
        public void setSelected(@NotNull com.intellij.openapi.actionSystem.AnActionEvent e, boolean state) {
            AIProviderSettings.getInstance().verboseLogging = state;
        }
    }

    /**
     * 自动启动代理切换操作类
     * <p> 用于在 IntelliJ IDEA 中控制 AI 代理的自动启动功能, 提供一个可切换的 UI 操作项.
     * <p> 该类继承自 IntelliJ 平台的 ToggleAction, 允许用户在设置中启用或禁用代理的自动启动行为.
     * <p> 当用户切换此选项时, 会更新全局的 IntelliAgentSettings 配置, 影响代理服务的启动行为.
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.02
     * @since 1.0.0
     */
    private static class AutoStartAgentToggleAction extends com.intellij.openapi.actionSystem.ToggleAction {
        /**
         * 初始化自动启动代理的切换动作
         * <p> 该构造函数用于初始化一个自动启动代理的切换动作, 设置其显示名称为“设置 - 代理 - 自动启动”.
         *
         */
        AutoStartAgentToggleAction() {
            super(AICommonBundle.message("settings.agent.auto.start"));
        }

        /**
         * 获取动作更新线程
         * <p>返回此动作的更新线程, 用于指定动作更新操作在哪个线程中执行
         * <p>此方法返回 <code>ActionUpdateThread.BGT</code>, 表示动作更新在后台线程 (Background Thread) 中执行
         *
         * @return 动作更新线程类型, 始终返回 <code>ActionUpdateThread.BGT</code>
         */
        @Override
        public @NotNull com.intellij.openapi.actionSystem.ActionUpdateThread getActionUpdateThread() {
            return com.intellij.openapi.actionSystem.ActionUpdateThread.BGT;
        }

        /**
         * 判断自动启动代理功能是否被选中
         * <p> 检查当前配置中是否启用了自动启动代理功能
         *
         * @param e 动作事件, 包含当前操作的上下文信息, 不能为 null
         * @return 如果自动启动代理功能已启用则返回 true, 否则返回 false
         */
        @Override
        public boolean isSelected(@NotNull com.intellij.openapi.actionSystem.AnActionEvent e) {
            IntelliAgentSettings settings = AIProviderSettings.getInstance().intelliAgentSettings;
            return settings != null && settings.autoStart;
        }

        /**
         * 设置自动启动代理的开关状态
         * <p> 根据传入的状态参数更新智能代理设置中的 autoStart 字段
         *
         * @param e     Action 事件对象, 用于上下文信息
         * @param state 新的开关状态,true 表示启用自动启动,false 表示禁用
         */
        @Override
        public void setSelected(@NotNull com.intellij.openapi.actionSystem.AnActionEvent e, boolean state) {
            IntelliAgentSettings settings = AIProviderSettings.getInstance().intelliAgentSettings;
            if (settings != null) {
                settings.autoStart = state;
            }
        }
    }

    /**
     * 自动更新代理开关操作类
     * <p> 用于在 IDE 中提供一个切换自动更新代理功能的按钮或菜单项, 控制是否启用自动更新代理功能.
     * <p> 该类继承自 ToggleAction, 实现了状态切换逻辑, 并与 IntelliAgentSettings 进行交互以读取和设置自动更新代理的状态.
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.02
     * @since 1.0.0
     */
    private static class AutoUpdateAgentToggleAction extends com.intellij.openapi.actionSystem.ToggleAction {
        /**
         * 构造函数, 初始化自动更新代理的动作
         * <p> 设置动作名称为从 AICommonBundle 中获取的 "settings.agent.auto.update" 消息
         *
         * @since 1.0
         */
        AutoUpdateAgentToggleAction() {
            super(AICommonBundle.message("settings.agent.auto.update"));
        }

        /**
         * 获取此操作的更新线程类型
         * <p>指定此动作的更新操作应在哪个线程上执行, 此处设置为在后台线程 (BGT) 上执行.
         *
         * @return 返回 {@link com.intellij.openapi.actionSystem.ActionUpdateThread#BGT} 表示在后台线程上执行
         */
        @Override
        public @NotNull com.intellij.openapi.actionSystem.ActionUpdateThread getActionUpdateThread() {
            return com.intellij.openapi.actionSystem.ActionUpdateThread.BGT;
        }

        /**
         * 判断自动更新功能是否被选中
         * <p> 检查当前智能代理设置中是否启用了自动更新功能
         *
         * @param e 动作事件对象, 包含当前操作的上下文信息, 不能为 null
         * @return 如果智能代理设置存在且自动更新功能已启用, 则返回 true, 否则返回 false
         */
        @Override
        public boolean isSelected(@NotNull com.intellij.openapi.actionSystem.AnActionEvent e) {
            IntelliAgentSettings settings = AIProviderSettings.getInstance().intelliAgentSettings;
            return settings != null && settings.autoUpdate;
        }

        /**
         * 设置智能代理的自动更新状态
         * <p> 根据给定的状态更新智能代理设置中的自动更新选项, 并启动或停止自动更新检查器
         *
         * @param e     AnActionEvent 事件对象, 提供上下文信息
         * @param state 布尔值, 表示是否启用自动更新
         *              <p>
         *              使用示例:
         *              <pre>{@code
         *                           // 启用自动更新
         *                           setSelected(e, true);
         *
         *                           // 禁用自动更新
         *                           setSelected(e, false);
         *                           }</pre>
         */
        @Override
        public void setSelected(@NotNull com.intellij.openapi.actionSystem.AnActionEvent e, boolean state) {
            IntelliAgentSettings settings = AIProviderSettings.getInstance().intelliAgentSettings;
            if (settings == null) {
                return;
            }
            settings.autoUpdate = state;
            IntelliAgentUpdateChecker updateChecker = IntelliAgentUpdateChecker.getInstance();
            if (state) {
                updateChecker.start();
            } else {
                updateChecker.stop();
            }
        }
    }

    /**
     * 启动 / 停止代理动作类
     * <p> 用于在 IDE 中提供启动或停止智能代理的功能, 根据当前代理运行状态切换按钮文本并执行相应操作.
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.02
     * @since 1.0.0
     */
    private static class StartStopAgentAction extends com.intellij.openapi.actionSystem.AnAction {
        /**
         * 初始化启动 / 停止代理动作
         * <p> 创建一个用于启动或停止代理的 IntelliJ IDEA 操作按钮, 按钮文本根据代理当前状态动态显示
         * <p> 当代理正在运行时, 按钮文本为“停止”, 否则为“启动”
         *
         * @since 2024
         */
        StartStopAgentAction() {
            super(AICommonBundle.message("settings.agent.start"));
        }

        /**
         * 处理启动或停止智能代理的动作
         * <p> 根据当前代理状态执行相应的操作: 如果代理正在运行, 则停止代理; 否则, 启动代理
         * <p> 在后台线程中执行代理的启动操作, 以避免阻塞 UI 线程
         *
         * @param e AnActionEvent 对象, 包含动作事件的相关信息
         * @since 1.0
         */
        @Override
        public void actionPerformed(@NotNull com.intellij.openapi.actionSystem.AnActionEvent e) {
            Project project = e.getProject();
            IntelliAgentManager manager = IntelliAgentManager.getInstance();
            IntelliAgentSettings settings = AIProviderSettings.getInstance().intelliAgentSettings;
            if (settings == null) {
                return;
            }

            if (manager.isRunningQuick()) {
                manager.stopAgent();
                return;
            }

            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                try {
                    manager.startAgent(settings);
                } catch (Exception ex) {
                    NotificationUtil.showError(project, ex.getMessage());
                }
            });
        }

        /**
         * 更新动作的显示文本
         * <p> 根据智能代理的运行状态更新动作的显示文本, 如果智能代理正在快速运行, 则显示停止文本, 否则显示启动文本
         *
         * @param e 动作事件, 不能为 null
         */
        @Override
        public void update(@NotNull com.intellij.openapi.actionSystem.AnActionEvent e) {
            IntelliAgentManager manager = IntelliAgentManager.getInstance();
            String text = manager.isRunningQuick()
                          ? AICommonBundle.message("settings.agent.stop")
                          : AICommonBundle.message("settings.agent.start");
            e.getPresentation().setText(text);
        }

        /**
         * 获取此操作的更新线程类型
         * <p>该方法用于指定执行动作更新 (如菜单项状态刷新) 时所使用的线程.
         * <p>返回后台线程(BGT), 表示更新操作应在非 UI 线程中进行, 以避免阻塞界面.
         *
         * @return 返回 {@link com.intellij.openapi.actionSystem.ActionUpdateThread#BGT}, 表示使用后台线程
         */
        @Override
        public @NotNull com.intellij.openapi.actionSystem.ActionUpdateThread getActionUpdateThread() {
            return com.intellij.openapi.actionSystem.ActionUpdateThread.BGT;
        }
    }

    /**
     * "新功能" 操作类
     * <p> 该内部类用于在 IntelliJ 平台中创建一个显示新功能的系统动作 (Action), 通常用于引导用户查看最新更新或功能介绍.
     * <p> 通过继承 AnAction 类, 实现 actionPerformed 和 getActionUpdateThread 方法, 控制动作执行逻辑和线程策略.
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.02
     * @since 1.0.0
     */
    private static class WhatsNewAction extends com.intellij.openapi.actionSystem.AnAction {
        /**
         * 初始化 "最新动态" 操作
         * <p> 创建一个用于显示最新动态的菜单操作项, 该操作项会使用指定的文本标签
         *
         */
        WhatsNewAction() {
            super(AICommonBundle.message("whatsnew.action.text"));
        }

        /**
         * 执行“查看更新”操作, 用于打开新版本编辑器
         * <p> 该方法在用户触发“查看更新”动作时被调用, 会获取当前项目并调用 WhatsNewEditorOpener.open 方法打开新版本编辑器界面
         *
         * @param e 动作事件对象, 包含触发动作的相关信息
         */
        @Override
        public void actionPerformed(@NotNull com.intellij.openapi.actionSystem.AnActionEvent e) {
            Project project = e.getProject();
            if (project != null) {
                WhatsNewEditorOpener.open(project);
            }
        }

        /**
         * 返回操作更新线程
         * <p> 此方法重写父类的方法, 指定此动作的操作更新线程为后台线程 (BGT)
         *
         * @return 操作更新线程, 固定返回 BGT
         */
        @Override
        public @NotNull com.intellij.openapi.actionSystem.ActionUpdateThread getActionUpdateThread() {
            return com.intellij.openapi.actionSystem.ActionUpdateThread.BGT;
        }
    }

    /**
     * 打开个人信息对话框操作类
     * <p> 该内部类用于在 IntelliJ IDEA 插件中创建一个动作, 点击后会弹出显示用户个人信息的对话框.
     * <p> 此动作通常被添加到状态栏或工具栏中, 为用户提供快速访问其信息的方式.
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.02
     * @since 1.0.0
     */
    private static class OpenPersonalInfoDialogAction extends com.intellij.openapi.actionSystem.AnAction {
        /** 项目实例, 用于访问 IDE 的项目相关功能 */
        private final Project project;

        /**
         * 初始化个人信息对话框操作
         * <p> 创建一个用于打开个人信息对话框的动作, 该动作会显示一个包含用户个人信息的对话框
         * <p> 当用户点击此动作时, 会在指定的项目上下文中显示个人信息对话框
         *
         * @param project 非空的项目实例, 用于创建和显示个人信息对话框
         */
        OpenPersonalInfoDialogAction(@NotNull Project project) {
            super(AICommonBundle.message("statusbar.engine.personal.info"));
            this.project = project;
        }

        /**
         * 处理动作事件, 显示个人资料对话框
         * <p> 当用户点击相关操作按钮时, 创建并显示个人资料对话框, 用于展示或编辑用户个人信息
         * <p> 如果项目已销毁, 则直接返回, 不执行任何操作
         *
         * @param e 动作事件对象, 包含事件上下文信息, 不能为 null
         */
        @Override
        public void actionPerformed(@NotNull com.intellij.openapi.actionSystem.AnActionEvent e) {
            if (project.isDisposed()) {
                return;
            }
            new dev.dong4j.zeka.stack.idea.plugin.common.ui.PersonalInfoDialog(project).show();
        }

        /**
         * 获取动作更新线程
         * <p> 返回此动作的更新线程, 用于指定动作更新操作应在哪个线程中执行
         * <p> 此实现返回后台线程 (BGT), 表示动作更新应在后台线程中执行, 以避免阻塞 UI 线程
         *
         * @return 动作更新线程类型, 此处返回 {@link com.intellij.openapi.actionSystem.ActionUpdateThread#BGT}
         */
        @Override
        public @NotNull com.intellij.openapi.actionSystem.ActionUpdateThread getActionUpdateThread() {
            return com.intellij.openapi.actionSystem.ActionUpdateThread.BGT;
        }
    }
}
