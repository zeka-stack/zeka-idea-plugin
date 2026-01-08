package dev.dong4j.zeka.stack.idea.plugin.changelog.statusbar;

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
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.impl.status.EditorBasedStatusBarPopup;
import com.intellij.util.IconUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import javax.swing.Icon;

import dev.dong4j.zeka.stack.idea.plugin.changelog.git.GitCliffDownloadManager;
import dev.dong4j.zeka.stack.idea.plugin.changelog.settings.ChangelogSettingsConfigurable;
import dev.dong4j.zeka.stack.idea.plugin.changelog.settings.ReleaseLogProvider;
import dev.dong4j.zeka.stack.idea.plugin.changelog.settings.SettingsState;
import dev.dong4j.zeka.stack.idea.plugin.changelog.util.ChangelogBundle;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIProviderType;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderSettings;
import dev.dong4j.zeka.stack.idea.plugin.common.util.AIProviderUtils;
import icons.AICommonIcons;
import icons.ChangelogIcons;
import lombok.extern.slf4j.Slf4j;

/**
 * Changelog 状态栏小部件
 * <p> 此小部件继承自 EditorBasedStatusBarPopup, 用于在 IDEA 的状态栏中显示变更日志相关信息. 它提供了多种操作选项, 包括切换 AI 提供商, 选择变更日志的起始点以及打开设置等.
 * <p> 通过扩展和重写父类的方法, 实现了状态栏小部件的创建, 安装, 卸载以及弹出菜单的构建.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.01
 * @since 1.0.0
 */
@Slf4j
public class ChangelogStatusBarWidget extends EditorBasedStatusBarPopup {
    /** 状态栏组件的唯一标识符 */
    public static final String WIDGET_ID =
        "dev.dong4j.zeka.stack.idea.plugin.changelog.statusbar.ChangelogStatusBarWidget";
    /**
     * 用于标识动作是否被选中的键
     *
     * @see AnActionEvent#getPresentation()
     */
    private static final Key<Boolean> SELECTED_KEY = Key.create("selected");
    /**
     * 当前项目的引用
     *
     * @see Project
     */
    private final Project project;
    /** 状态栏实例, 用于更新和管理状态栏显示内容 */
    private StatusBar statusBar;

    /**
     * 创建 Changelog 状态栏小部件实例
     * <p> 初始化状态栏小部件, 用于显示和管理 Changelog 相关功能入口 </p>
     *
     * @param project 非空的项目实例, 用于关联状态栏小部件与当前 IDE 项目
     */
    public ChangelogStatusBarWidget(@NotNull Project project) {
        super(project, false);
        this.project = project;
    }

    /**
     * 创建该状态栏组件的实例
     * <p> 用于创建 ChangelogStatusBarWidget 的新实例, 传入指定的 Project 对象.
     *
     * @param project 当前项目对象, 用于初始化组件
     * @return 新创建的 ChangelogStatusBarWidget 实例
     */
    @Override
    protected @NotNull StatusBarWidget createInstance(@NotNull Project project) {
        return new ChangelogStatusBarWidget(project);
    }

    /**
     * 返回该状态栏组件的唯一标识符
     * <p> 该标识符用于在 IDE 中唯一标识此状态栏组件
     *
     * @return 状态栏组件的唯一 ID
     */
    @Override
    public @NotNull String ID() {
        return WIDGET_ID;
    }

    /**
     * 安装状态栏组件
     * <p> 将当前组件安装到指定的状态栏中, 并保存对状态栏的引用, 以便后续更新组件内容
     *
     * @param statusBar 要安装组件的目标状态栏实例
     */
    @Override
    public void install(@NotNull StatusBar statusBar) {
        super.install(statusBar);
        this.statusBar = statusBar;
    }

    /**
     * 释放资源并清理状态栏引用
     * <p> 在组件销毁时调用, 释放相关资源并将状态栏引用置为 null
     *
     */
    @Override
    public void dispose() {
        super.dispose();
        statusBar = null;
    }

    /**
     * 构建并返回状态栏组件的显示状态
     * <p> 该方法用于创建状态栏中展示的 WidgetState 对象, 包含提示信息和图标.
     *
     * @param file 当前文件 (可为 null, 不使用)
     * @return WidgetState 实例, 包含状态栏组件的标题, 文本和图标
     */
    @Override
    protected @NotNull WidgetState getWidgetState(@Nullable VirtualFile file) {
        String tooltip = ChangelogBundle.message("statusbar.provider.popup.title");
        WidgetState state = new WidgetState(tooltip, "", true);
        state.setIcon(IconUtil.scale(ChangelogIcons.CHANGELOG_16, null, 0.8125f));
        return state;
    }

    /**
     * 创建并返回一个包含各种操作选项的列表弹出框
     * <p> 该方法根据当前项目是否配置了 AI 提供商来决定是否创建弹出框. 如果未配置, 则返回 null. 否则, 构建一个包含提供商选择, 发布日志起点设置, 发布日志提供者选择以及打开设置选项的默认动作组, 并创建一个带有快速搜索功能的弹出框.
     *
     * @param context 数据上下文, 用于确定弹出框的位置和行为
     * @return 包含操作选项的列表弹出框, 如果没有配置 AI 提供商则返回 null
     */
    @Override
    protected @Nullable ListPopup createPopup(@NotNull DataContext context) {
        if (!AIProviderUtils.hasAIProvider(project, ChangelogBundle.message("settings.display.name"), ChangelogBundle.message("settings" +
                                                                                                                              ".ai.provider.selection"))) {
            return null;
        }

        List<AIProviderConfig> providers = AIProviderUtils.getProviders();
        DefaultActionGroup group = new DefaultActionGroup();

        // 1. AI Provider 选择
        group.add(new ProviderSelectionActionGroup(providers));

        // 2. 快捷设置
        group.add(Separator.create(ChangelogBundle.message("statusbar.quick.settings.title")));
        group.add(new CommitMessageContextToggleAction());
        group.add(new ReleaseLogStartPointActionGroup());
        group.add(createReleaseLogProviderActionGroup(context));

        group.add(Separator.create());
        group.add(new OpenSettingsAction());

        return JBPopupFactory.getInstance().createActionGroupPopup(
            ChangelogBundle.message("statusbar.provider.popup.title"),
            group,
            context,
            JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
            true
                                                                  );
    }

    /**
     * 提交消息上下文切换动作类
     * <p> 该类继承自 AnAction, 用于在状态栏中切换提交消息的上下文显示状态.
     * <p> 通过点击该动作, 用户可以在提交消息的输入框与上下文之间进行切换.
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.04
     * @since 1.0.0
     */
    private static class CommitMessageContextToggleAction extends AnAction {
        CommitMessageContextToggleAction() {
            super(ChangelogBundle.message("statusbar.commit.context.toggle"));
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            SettingsState settings = SettingsState.getInstance();
            settings.useCommitMessageInputAsContext = !settings.useCommitMessageInputAsContext;
        }

        @Override
        public void update(@NotNull AnActionEvent e) {
            boolean isSelected = SettingsState.getInstance().useCommitMessageInputAsContext;
            e.getPresentation().putClientProperty(SELECTED_KEY, isSelected);
        }

        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.BGT;
        }
    }

    /**
     * 获取当前默认的 AI 服务商类型
     * <p>从全局设置中获取当前配置的 AI 服务商类型, 如果未配置则返回默认的通义千问 (Qianwen) 类型
     *
     * @return 当前默认的 AI 服务商类型
     */
    @NotNull
    private AIProviderType getCurrentProviderType() {
        SettingsState settings = SettingsState.getInstance();
        return settings.providerConfig != null ? settings.providerConfig.providerType : AIProviderType.QIANWEN;
    }

    /**
     * 获取指定 AI 提供商类型的默认配置
     * <p> 根据给定的 AI 提供商类型从全局设置中获取默认配置
     *
     * @param providerType AI 提供商类型
     * @return 默认的 AI 提供商配置
     */
    @NotNull
    private AIProviderConfig getDefaultProviderConfig(@NotNull AIProviderType providerType) {
        AIProviderSettings globalSettings = AIProviderSettings.getInstance();
        return globalSettings.getDefaultProviderConfig(providerType);
    }

    /**
     * 获取当前 AI 服务商的模型名称
     * <p> 根据当前选择的 AI 服务商类型, 获取其默认配置中的模型名称
     *
     * @return 当前 AI 服务商的模型名称
     */
    @NotNull
    private String getCurrentProviderModelName() {
        AIProviderType providerType = getCurrentProviderType();
        AIProviderConfig defaultConfig = getDefaultProviderConfig(providerType);
        return defaultConfig.modelName;
    }

    /**
     * 获取当前 AI 服务提供商的图标
     * <p> 根据当前配置的 AI 服务提供商类型, 获取对应的图标. 如果图标不存在, 则使用默认的 Changelog 图标.
     *
     * @return AI 服务提供商的图标, 若未找到则返回默认图标
     */
    @NotNull
    private Icon getProviderIcon() {
        AIProviderType providerType = getCurrentProviderType();
        Icon providerIcon = AICommonIcons.getProviderIcon(providerType);
        return providerIcon != null ? providerIcon : IconUtil.scale(ChangelogIcons.CHANGELOG_16, null, 0.8125f);
    }

    /**
     * 切换默认的 AI 服务提供商配置
     * <p> 更新当前项目的默认 AI 服务提供商配置, 将指定的配置设为默认值.
     *
     * @param providerType 服务提供商类型
     * @param config       服务提供商配置信息
     */
    private void switchDefaultProvider(@NotNull AIProviderType providerType, @NotNull AIProviderConfig config) {
        SettingsState settings = SettingsState.getInstance();
        settings.providerConfig = config;
        AIProviderSettings globalSettings = AIProviderSettings.getInstance();
        globalSettings.updateDefaultProviderConfig(providerType, config);
    }

    /**
     * 切换提供者动作类
     * <p> 用于在 IntelliJ IDEA 中切换 AI 提供者的默认设置. 该类继承自 AnAction, 并通过配置文件中的信息来设置图标和执行切换操作.
     * 在动作被触发时, 会检查当前项目是否具有指定的 AI 提供者, 并在确认后进行默认提供者的切换.
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.01
     * @since 1.0.0
     */
    private class SwitchProviderAction extends AnAction {
        /**
         * AI 提供商配置信息
         * <p> 包含模型名称, 提供商类型等配置项
         *
         * @see AIProviderConfig
         */
        private final AIProviderConfig config;

        /**
         * 初始化切换服务商操作
         * <p> 创建一个用于切换 AI 服务商的 Action, 设置操作名称和图标
         *
         * @param config AI 服务商配置信息
         */
        SwitchProviderAction(AIProviderConfig config) {
            super(config.modelName);
            this.config = config;
            if (config.providerType != null) {
                Icon providerIcon = AICommonIcons.getProviderIcon(config.providerType);
                if (providerIcon != null) {
                    getTemplatePresentation().setIcon(providerIcon);
                }
            }
        }

        /**
         * 处理动作执行逻辑, 用于切换默认的 AI 服务提供商
         * <p> 检查当前项目是否已配置 AI 服务提供商, 若存在则异步执行切换操作.
         * 切换过程中会创建配置副本并更新默认服务提供商类型.
         *
         * @param e 动作事件, 包含当前操作的上下文信息
         */
        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            if (!AIProviderUtils.hasAIProvider(project, config, ChangelogBundle.message("settings.display.name"),
                                               ChangelogBundle.message("settings.ai.provider.selection"))) {
                return;
            }

            ApplicationManager.getApplication().invokeLater(() -> {
                if (project.isDisposed()) {
                    return;
                }
                try {
                    ApplicationManager.getApplication().runWriteAction(() -> {
                        AIProviderConfig configCopy = config.copy();
                        configCopy.providerType = config.providerType;
                        switchDefaultProvider(config.providerType, configCopy);
                    });
                } catch (Exception exception) {
                    log.debug("切换默认服务商失败", exception);
                } finally {
                    StatusBar currentStatusBar = statusBar;
                    if (currentStatusBar != null) {
                        currentStatusBar.updateWidget(ID());
                    }
                }
            }, ModalityState.defaultModalityState());
        }

        /**
         * 更新动作的显示状态, 根据当前配置和选中的服务提供商类型设置是否选中
         * <p> 此方法用于在用户界面中更新该动作的显示状态, 根据当前配置的提供商类型与实际选中的类型进行比较, 设置对应的状态属性.
         *
         * @param e 动作事件对象, 包含当前动作的上下文信息
         */
        @Override
        public void update(@NotNull AnActionEvent e) {
            AIProviderType currentType = getCurrentProviderType();
            boolean isSelected = config != null && config.providerType == currentType;
            e.getPresentation().putClientProperty(SELECTED_KEY, isSelected);
        }

        /**
         * 获取此操作的更新线程类型
         * <p>指定该操作在后台线程 (BGT) 中进行更新, 以避免阻塞 UI 线程
         *
         * @return 返回操作更新所使用的线程类型, 此处返回 {@link ActionUpdateThread#BGT}
         */
        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.BGT;
        }
    }

    /**
     * 提供者选择动作组
     * <p> 用于在状态栏中展示可切换的 AI 提供者配置选项, 支持根据提供的 AIProviderConfig 列表动态添加切换动作
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.01
     * @since 1.0.0
     */
    private class ProviderSelectionActionGroup extends DefaultActionGroup {
        /**
         * 构造函数, 用于创建提供者选择动作组
         * <p> 初始化动作组标题为指定的国际化消息, 并设置为可折叠状态. 如果提供非空的提供者列表, 则为每个提供者配置添加一个切换提供者动作.
         *
         * @param providers 提供者配置列表, 用于添加对应的切换动作, 如果为 null 则不添加任何动作
         */
        ProviderSelectionActionGroup(List<AIProviderConfig> providers) {
            super(ChangelogBundle.message("statusbar.provider.selection.title"), true);
            if (providers != null) {
                for (AIProviderConfig config : providers) {
                    add(new SwitchProviderAction(config));
                }
            }
        }
    }

    /**
     * 用于构建发布日志起始点操作组的类
     * <p>该类继承自 DefaultActionGroup, 用于在 IDE 的状态栏中提供选择发布日志起始点的下拉菜单选项. 支持基于标签 (tag) 或哈希值 (hash) 两种方式作为起始点, 并根据用户设置动态显示对应的文本标签.</p>
     * <p>主要功能包括:</p>
     * <ul>
     *   <li>动态生成菜单标题, 根据用户设置显示“tag”或“hash”作为起始点类型</li>
     *   <li>添加两个操作项: 一个用于基于标签的起始点操作, 另一个用于基于哈希值的起始点操作</li>
     * </ul>
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.01
     * @since 1.0.0
     */
    private static class ReleaseLogStartPointActionGroup extends DefaultActionGroup {
        /**
         * 初始化释放日志起点操作组, 包含基于标签或哈希的两个操作选项
         * <p> 构造函数创建一个操作组, 其中包含两个释放日志起点操作: 一个使用标签作为起点, 另一个使用哈希作为起点
         *
         * @see ReleaseLogStartPointAction 释放日志起点操作类
         * @see SettingsState 设置状态类, 用于获取当前配置
         * @see ChangelogBundle 国际化资源包, 用于获取本地化消息
         */
        ReleaseLogStartPointActionGroup() {
            super(getTitle(), true);
            add(new ReleaseLogStartPointAction(true));
            add(new ReleaseLogStartPointAction(false));
        }

        /**
         * 获取当前使用的发布日志起点标识符
         * <p> 根据设置状态返回发布日志起点的标题文本, 包含具体的标识符 (标签或哈希)</p>
         *
         * @return 当前使用的发布日志起点标识符文本, 不能为空
         */
        @NotNull
        private static String getTitle() {
            SettingsState settings = SettingsState.getInstance();
            String tagText = ChangelogBundle.message("statusbar.release.log.start.point.tag");
            String hashText = ChangelogBundle.message("statusbar.release.log.start.point.hash");
            String current = settings.useTagAsStart ? tagText : hashText;
            return ChangelogBundle.message("statusbar.release.log.start.point") + " (" + current + ")";
        }
    }

    /**
     * 发布日志起点操作类
     * <p> 用于在状态栏中切换发布日志的起点显示方式, 可以选择以标签或哈希值作为起点
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.01
     * @since 1.0.0
     */
    private static class ReleaseLogStartPointAction extends AnAction {
        /** 是否使用标签作为起始点 */
        private final boolean useTag;

        /**
         * 构造一个 ReleaseLogStartPointAction 实例, 用于设置版本日志起始点的显示方式
         * <p>根据传入的 useTag 参数决定是使用标签 (tag) 还是哈希值 (hash) 作为起始点标识</p>
         *
         * @param useTag 如果为 true, 则使用标签; 否则使用哈希值
         */
        ReleaseLogStartPointAction(boolean useTag) {
            super(useTag
                  ? ChangelogBundle.message("statusbar.release.log.start.point.tag")
                  : ChangelogBundle.message("statusbar.release.log.start.point.hash"));
            this.useTag = useTag;
        }

        /**
         * 当操作被触发时, 更新设置中是否使用标签作为发布日志的起始点
         *
         * @param e 动作事件对象, 包含触发动作的相关信息
         */
        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            SettingsState.getInstance().useTagAsStart = useTag;
        }

        /**
         * 更新操作按钮的选中状态
         * <p> 根据当前设置中的 useTagAsStart 值与该动作的 useTag 值比较, 设置按钮的选中状态
         *
         * @param e 事件对象, 包含操作上下文信息
         */
        @Override
        public void update(@NotNull AnActionEvent e) {
            boolean isSelected = SettingsState.getInstance().useTagAsStart == useTag;
            e.getPresentation().putClientProperty(SELECTED_KEY, isSelected);
        }

        /**
         * 获取该操作的更新线程类型
         * <p>指定此操作应在哪个线程上执行更新, 此处返回在后台线程 (BGT) 上执行
         *
         * @return 操作的更新线程类型, 始终返回 ActionUpdateThread.BGT
         */
        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.BGT;
        }
    }

    /**
     * 创建发布日志提供程序选择动作组
     * <p> 根据当前设置创建包含 AI 和 GitCliff 两种发布日志提供程序选项的动作组
     * 动作组标题会根据当前选择的提供程序动态显示, 例如:'发布日志提供程序 (🤖 AI (Qianwen:qwen-turbo))' 或 '发布日志提供程序 (🪨 GitCliff (2.11.0))'
     *
     * @param context 数据上下文, 用于创建动作
     * @return 包含发布日志提供程序选择选项的动作组
     */
    @NotNull
    private DefaultActionGroup createReleaseLogProviderActionGroup(@NotNull DataContext context) {
        SettingsState settings = SettingsState.getInstance();
        String current;
        if (settings.releaseLog == ReleaseLogProvider.GIT_CLIFF) {
            // 获取 git-cliff 版本号
            String version = GitCliffDownloadManager.getInstalledVersion();
            String gitCliffText = ChangelogBundle.message("statusbar.release.log.provider.gitcliff");
            if (version != null && !version.isEmpty()) {
                current = "🪨 " + gitCliffText + " (" + version + ")";
            } else {
                current = "🪨 " + gitCliffText;
            }
        } else {
            // 获取 AI 提供商和模型名称
            String aiText = ChangelogBundle.message("statusbar.release.log.provider.ai");
            AIProviderConfig providerConfig = settings.providerConfig;
            if (providerConfig != null && providerConfig.providerType != null) {
                String providerName = providerConfig.providerType.getDisplayName();
                String modelName = providerConfig.modelName != null && !providerConfig.modelName.isEmpty()
                                   ? providerConfig.modelName : "";
                if (!modelName.isEmpty()) {
                    current = "🤖 " + aiText + " (" + providerName + ":" + modelName + ")";
                } else {
                    current = "🤖 " + aiText + " (" + providerName + ")";
                }
            } else {
                current = "🤖 " + aiText;
            }
        }
        String title = ChangelogBundle.message("statusbar.release.log.provider") + " (" + current + ")";
        DefaultActionGroup group = new DefaultActionGroup(title, true);
        group.add(new ReleaseLogProviderAction(ReleaseLogProvider.AI));
        group.add(new ReleaseLogProviderAction(ReleaseLogProvider.GIT_CLIFF));
        return group;
    }

    /**
     * 释放日志提供者操作类
     * <p> 用于在 IDE 状态栏中提供不同的释放日志生成方式, 支持 Git Cliffs 和 AI 两种模式.
     * 用户可以通过状态栏选择不同的日志生成方式, 该类作为 UI 按钮的事件处理器, 负责更新设置并同步 UI 状态.
     * <p> AI 选项会动态显示当前选择的服务商名称和模型名称, git-cliff 选项会动态显示版本号.
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.01
     * @since 1.0.0
     */
    private static class ReleaseLogProviderAction extends AnAction {
        /** 用于标识此操作对应的发布日志提供方 */
        private final ReleaseLogProvider provider;

        /**
         * 构造函数, 用于创建一个 ReleaseLogProviderAction 实例
         * <p> 根据传入的 ReleaseLogProvider 类型设置动作的显示名称, 并初始化 provider 字段.
         * 对于 AI 选项, 会动态获取当前选择的服务商名称和模型名称.
         * 对于 git-cliff 选项, 会动态获取版本号.
         *
         * @param provider 用于确定显示名称的 ReleaseLogProvider 实例
         */
        ReleaseLogProviderAction(ReleaseLogProvider provider) {
            super(getProviderDisplayTextStatic(provider));
            this.provider = provider;
        }

        /**
         * 获取提供程序的显示文本（静态方法）
         * <p> 根据提供程序类型动态生成显示文本:
         * <ul>
         *   <li>AI: 显示 "🤖 AI (提供商名称:模型名称)"</li>
         *   <li>GitCliff: 显示 "🪨 GitCliff (版本号)"</li>
         * </ul>
         *
         * @param provider 发布日志提供程序类型
         * @return 格式化后的显示文本
         */
        @NotNull
        private static String getProviderDisplayTextStatic(@NotNull ReleaseLogProvider provider) {
            if (provider == ReleaseLogProvider.GIT_CLIFF) {
                // 获取 git-cliff 版本号
                String baseText = ChangelogBundle.message("statusbar.release.log.provider.gitcliff");
                String version = GitCliffDownloadManager.getInstalledVersion();
                if (version != null && !version.isEmpty()) {
                    return "🪨 " + baseText + " (" + version + ")";
                } else {
                    return "🪨 " + baseText;
                }
            } else {
                // 获取 AI 提供商和模型名称
                String baseText = ChangelogBundle.message("statusbar.release.log.provider.ai");
                SettingsState settings = SettingsState.getInstance();
                AIProviderConfig providerConfig = settings.providerConfig;
                if (providerConfig != null && providerConfig.providerType != null) {
                    String providerName = providerConfig.providerType.getDisplayName();
                    String modelName = providerConfig.modelName != null && !providerConfig.modelName.isEmpty()
                                       ? providerConfig.modelName : "";
                    if (!modelName.isEmpty()) {
                        return "🤖 " + baseText + " (" + providerName + ":" + modelName + ")";
                    } else {
                        return "🤖 " + baseText + " (" + providerName + ")";
                    }
                } else {
                    return "🤖 " + baseText;
                }
            }
        }

        /**
         * 处理用户在界面上触发的动作事件
         * <p> 将当前选定的发布日志提供者设置到应用的状态中
         *
         * @param e 包含动作事件信息的对象
         */
        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            SettingsState.getInstance().releaseLog = provider;
        }

        /**
         * 更新操作按钮的选中状态和显示文本
         * <p> 根据当前设置中的 releaseLog 是否等于 provider, 更新动作按钮的选中状态.
         * 同时动态更新显示文本, 以反映当前选择的 AI 提供商和模型名称, 或 git-cliff 版本号.
         *
         * @param e 事件对象, 包含动作执行上下文信息
         */
        @Override
        public void update(@NotNull AnActionEvent e) {
            boolean isSelected = SettingsState.getInstance().releaseLog == provider;
            e.getPresentation().putClientProperty(SELECTED_KEY, isSelected);
            // 动态更新显示文本
            e.getPresentation().setText(getProviderDisplayTextStatic(provider));
        }

        /**
         * 获取此操作的更新线程
         * <p> 指定用于更新操作状态的线程, 返回后台线程 (BGT)
         *
         * @return 始终返回 {@link ActionUpdateThread#BGT} 表示后台线程
         */
        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.BGT;
        }
    }

    /**
     * 打开设置操作类
     * <p> 用于在状态栏中触发打开设置对话框的操作, 当项目未被释放时, 会调用显示设置对话框的方法
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.01
     * @since 1.0.0
     */
    private class OpenSettingsAction extends AnAction {
        /**
         * 构造一个用于打开设置的 Action 对象
         * <p> 该构造函数使用国际化消息 "statusbar.open.settings" 作为 Action 的显示名称 </p>
         *
         */
        OpenSettingsAction() {
            super(ChangelogBundle.message("statusbar.open.settings"));
        }

        /**
         * 处理动作执行事件, 显示设置对话框
         * <p> 检查项目是否已销毁, 若未销毁则通过 ShowSettingsUtil 显示 Changelog 设置配置对话框 </p>
         *
         * @param e 动作事件, 包含执行上下文信息
         */
        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            if (project.isDisposed()) {
                return;
            }
            ShowSettingsUtil.getInstance().showSettingsDialog(project, ChangelogSettingsConfigurable.class);
        }

        /**
         * 获取此操作的更新线程
         * <p>指定此动作在哪个线程上执行更新, 返回后台线程 (BGT) 以确保 UI 线程不会被阻塞
         *
         * @return 返回 ActionUpdateThread.BGT, 表示在后台线程执行更新
         */
        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.BGT;
        }
    }
}
