package dev.dong4j.zeka.stack.idea.plugin.changelog.statusbar;

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Separator;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import dev.dong4j.zeka.stack.idea.plugin.changelog.git.GitCliffDownloadManager;
import dev.dong4j.zeka.stack.idea.plugin.changelog.settings.ChangelogSettingsConfigurable;
import dev.dong4j.zeka.stack.idea.plugin.changelog.settings.ReleaseLogProvider;
import dev.dong4j.zeka.stack.idea.plugin.changelog.settings.SettingsState;
import dev.dong4j.zeka.stack.idea.plugin.changelog.util.ChangelogBundle;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIProviderType;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderSettings;
import dev.dong4j.zeka.stack.idea.plugin.common.statusbar.AIStatusBarPopupProvider;
import dev.dong4j.zeka.stack.idea.plugin.common.util.AIProviderUtils;
import dev.dong4j.zeka.stack.idea.plugin.kit.SettingsUtil;
import icons.AICommonIcons;
import lombok.extern.slf4j.Slf4j;

/**
 * 状态栏弹出菜单提供者类
 * <p> 该类用于在状态栏中提供与版本日志相关的弹出菜单, 支持切换 AI 服务商, 设置日志起始点等功能.
 * <p> 主要功能包括:
 * - 创建包含操作组的弹出菜单
 * - 支持 AI 服务商切换
 * - 支持版本日志起始点设置 (标签或哈希)
 * - 提供打开设置的功能
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.02
 * @since 1.0.0
 */
@Slf4j
public class ChangelogStatusBarPopupProvider implements AIStatusBarPopupProvider {
    /** 用于标识菜单项是否被选中的状态键 */
    private static final Key<Boolean> SELECTED_KEY = Key.create("selected");

    /**
     * 获取状态栏弹出菜单的组名称
     * <p> 返回与状态栏弹出菜单相关的标题文本
     *
     * @return 状态栏弹出菜单的组名称
     */
    @Override
    public @NotNull String getGroupName() {
        return ChangelogBundle.message("statusbar.provider.popup.title");
    }

    /**
     * 创建状态栏弹出菜单的动作组
     * <p> 根据当前项目配置和可用的 AI 提供商, 构建包含多种操作选项的菜单项.
     * <p> 如果当前项目未配置有效的 AI 提供商, 则仅显示打开设置的选项.
     * <p> 否则, 添加以下菜单项:
     * <ul>
     *   <li>AI 提供商选择组 (允许用户切换不同的 AI 提供商)</li>
     *   <li> 快速设置分隔符 </li>
     *   <li> 发布日志起始点选择组 (支持基于标签或哈希值)</li>
     *   <li> 发布日志提供者选择组 (支持 GitCliff 或 AI 模型)</li>
     *   <li> 分隔符 </li>
     *   <li> 再次打开设置的选项 </li>
     * </ul>
     *
     * @param project 当前项目, 用于获取项目特定的配置和上下文
     * @param context 数据上下文, 用于提供操作执行时的环境信息
     * @return 包含所有菜单项的动作组, 用于在状态栏弹出菜单中展示
     */
    @Override
    public @NotNull ActionGroup createActionGroup(@NotNull Project project, @NotNull DataContext context) {
        DefaultActionGroup group = new DefaultActionGroup();
        if (!AIProviderUtils.hasAIProvider(project, ChangelogBundle.message("settings.display.name"), ChangelogBundle.message("settings" +
                                                                                                                              ".ai.provider.selection"))) {
            group.add(new OpenSettingsAction(project));
            return group;
        }

        List<AIProviderConfig> providers = AIProviderUtils.getProviders();
        group.add(new ProviderSelectionActionGroup(project, providers));
        group.add(Separator.create(ChangelogBundle.message("statusbar.quick.settings.title")));
        group.add(new ReleaseLogStartPointActionGroup());
        group.add(createReleaseLogProviderActionGroup());
        group.add(Separator.create());
        group.add(new OpenSettingsAction(project));
        return group;
    }

    /**
     * 获取当前默认的 AI 服务商类型
     * <p>根据全局设置获取当前配置的 AI 服务商类型, 如果未配置则返回默认的千问 (Qianwen) 类型
     *
     * @return 当前配置的 AI 服务商类型, 如果配置为空则返回 {@link AIProviderType#QIANWEN}
     */
    @NotNull
    private AIProviderType getCurrentProviderType() {
        SettingsState settings = SettingsState.getInstance();
        return settings.providerConfig != null ? settings.providerConfig.providerType : AIProviderType.QIANWEN;
    }

    /**
     * 创建发布日志提供者操作组
     * <p> 根据当前配置生成包含 AI 和 GitCliff 两种日志提供者的操作组, 用于状态栏菜单选择
     *
     * @return 包含可用日志提供者选项的操作组实例
     */
    @NotNull
    private DefaultActionGroup createReleaseLogProviderActionGroup() {
        SettingsState settings = SettingsState.getInstance();
        String current;
        if (settings.releaseLog == ReleaseLogProvider.GIT_CLIFF) {
            String version = GitCliffDownloadManager.getInstalledVersion();
            String gitCliffText = ChangelogBundle.message("statusbar.release.log.provider.gitcliff");
            if (version != null && !version.isEmpty()) {
                current = "🪨 " + gitCliffText + " (" + version + ")";
            } else {
                current = "🪨 " + gitCliffText;
            }
        } else {
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
     * 切换默认的 AI 提供商配置
     * <p> 更新当前项目的 AI 提供商配置, 并同步到全局设置
     *
     * @param project      当前项目
     * @param providerType 提供商类型
     * @param config       提供商配置
     */
    private void switchDefaultProvider(@NotNull Project project,
                                       @NotNull AIProviderType providerType,
                                       @NotNull AIProviderConfig config) {
        SettingsState settings = SettingsState.getInstance();
        settings.providerConfig = config;
        AIProviderSettings globalSettings = AIProviderSettings.getInstance();
        globalSettings.updateDefaultProviderConfig(providerType, config);
    }

    /**
     * 切换提供者动作类
     * <p> 该类继承自 AnAction, 用于在 IntelliJ IDEA 插件中实现切换 AI 提供者的功能.
     * <p> 通过调用 `actionPerformed` 方法, 可以切换指定项目的默认 AI 提供者.
     * <p> 在 `update` 方法中, 根据当前选择的提供者类型更新动作的呈现状态.
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.02
     * @since 1.0.0
     */
    private class SwitchProviderAction extends AnAction {
        /** 项目实例, 用于访问项目相关的资源和功能 */
        private final Project project;
        /** AI 服务提供商配置信息 */
        private final AIProviderConfig config;

        /**
         * 初始化 SwitchProviderAction 对象
         * <p> 用于创建一个切换默认 AI 服务提供商的 Action 实例, 设置项目和配置信息, 并根据配置类型设置图标
         *
         * @param project 项目对象, 不能为 null
         * @param config  AI 服务提供商配置对象, 不能为 null
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
         * 执行切换默认 AI 服务商的操作
         * <p> 当用户触发该操作时, 首先检查项目中是否已配置对应的服务商, 若未配置则直接返回.
         * <p> 如果配置有效, 则在后台线程中执行切换默认服务商的逻辑. 切换过程会复制当前配置并调用 switchDefaultProvider 方法完成切换.
         * <p> 所有操作会在 UI 线程上异步执行, 并确保项目处于可用状态.
         *
         * @param e 动作事件对象, 提供上下文信息
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
                        switchDefaultProvider(project, config.providerType, configCopy);
                    });
                } catch (Exception exception) {
                    log.error("切换默认服务商失败", exception);
                }
            }, ModalityState.defaultModalityState());
        }

        /**
         * 更新动作的显示状态
         * <p> 根据当前配置的提供者类型, 更新动作在 UI 中的选中状态
         *
         * @param e 动作事件, 包含当前动作的上下文信息
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
     * 提供者选择动作组类
     * <p> 用于在状态栏中创建一个动作组, 允许用户切换不同的 AI 提供者配置
     * <p> 该类继承自 DefaultActionGroup, 并根据传入的 AI 提供者配置列表动态添加切换提供者动作
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.02
     * @since 1.0.0
     */
    private class ProviderSelectionActionGroup extends DefaultActionGroup {
        /**
         * 构造函数, 用于创建一个包含 AI 提供商切换操作的 ActionGroup
         * <p> 该分组会根据传入的 AIProviderConfig 列表生成对应的 SwitchProviderAction 操作项, 并添加到菜单中.
         *
         * @param project   当前项目对象, 不能为 null
         * @param providers AI 提供商配置列表, 如果为 null 则不添加任何操作项
         */
        ProviderSelectionActionGroup(@NotNull Project project, List<AIProviderConfig> providers) {
            super(ChangelogBundle.message("statusbar.provider.selection.title"), true);
            if (providers != null) {
                for (AIProviderConfig config : providers) {
                    add(new SwitchProviderAction(project, config));
                }
            }
        }
    }

    /**
     * 用于在状态栏中显示发布日志起始点选择的 ActionGroup
     * <p>该 ActionGroup 提供了两种选择: 基于标签 (tag) 或基于哈希值 (hash) 作为发布日志的起始点
     * <p>根据用户设置, 动态显示当前选择的类型(标签或哈希)
     * <p>支持通过快捷键或右键菜单访问, 用于快速切换发布日志的起始点
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.02
     * @since 1.0.0
     */
    private static class ReleaseLogStartPointActionGroup extends DefaultActionGroup {
        /**
         * 构造函数, 初始化 ReleaseLogStartPointActionGroup 对象
         * <p> 创建一个包含两个 ReleaseLogStartPointAction 的动作组
         * <p> 第一个动作使用 true 参数, 第二个动作使用 false 参数
         *
         * @since 1.0
         */
        ReleaseLogStartPointActionGroup() {
            super(getTitle(), true);
            add(new ReleaseLogStartPointAction(true));
            add(new ReleaseLogStartPointAction(false));
        }

        /**
         * 获取状态栏中释放日志起始点的标题文本
         * <p> 根据设置状态生成状态栏显示的标题, 用于标识当前使用的标签或哈希值作为起始点
         *
         * @return 标题文本, 保证不为 null
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
     * 释放日志起始点操作类
     * <p>用于在状态栏中切换使用标签 (tag) 或哈希 (hash) 作为生成变更日志的起始点
     * <p>该内部类继承自 {@link AnAction}, 主要功能是根据用户选择更新配置, 并控制 UI 的选中状态
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.02
     * @since 1.0.0
     */
    private static class ReleaseLogStartPointAction extends AnAction {
        /** 用于标识是否使用标签作为发布日志的起始点 */
        private final boolean useTag;

        /**
         * 初始化 ReleaseLogStartPointAction 对象
         * <p> 根据传入的 useTag 参数设置动作的显示名称, 并保存 useTag 值用于后续逻辑处理
         *
         * @param useTag 是否使用标签作为起始点,true 表示使用标签,false 表示使用哈希值
         */
        ReleaseLogStartPointAction(boolean useTag) {
            super(useTag
                  ? ChangelogBundle.message("statusbar.release.log.start.point.tag")
                  : ChangelogBundle.message("statusbar.release.log.start.point.hash"));
            this.useTag = useTag;
        }

        /**
         * 处理动作事件, 设置使用标签作为起始点的选项
         * <p> 根据构造时指定的 useTag 标志, 将 SettingsState 中的 useTagAsStart 属性设置为对应的值
         * <p> 该方法通常在用户界面中点击某个选项时被调用, 用于切换使用标签或哈希作为发布日志的起始点
         *
         * @param e 动作事件对象, 包含事件的上下文信息, 不能为 null
         */
        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            SettingsState.getInstance().useTagAsStart = useTag;
        }

        /**
         * 更新动作事件的呈现状态
         * <p> 根据当前设置的状态更新动作事件的呈现, 设置选中状态
         *
         * @param e 动作事件对象, 不能为 null
         * @since 1.0
         */
        @Override
        public void update(@NotNull AnActionEvent e) {
            boolean isSelected = SettingsState.getInstance().useTagAsStart == useTag;
            e.getPresentation().putClientProperty(SELECTED_KEY, isSelected);
        }

        /**
         * 获取该操作的线程更新策略
         * <p> 指定此操作应在哪个线程上更新, 此处返回 BGT(Background Graphics Thread) 表示在后台图形线程上执行更新操作.
         *
         * @return 返回 ActionUpdateThread.BGT, 表示在后台图形线程上执行更新
         */
        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.BGT;
        }
    }

    /**
     * 释放日志提供者操作类
     * <p>用于在状态栏中显示和切换不同的释放日志生成提供者(如 GitCliff 或 AI 提供者), 并处理用户选择操作
     * <p>支持动态更新显示文本, 根据当前选择的提供者类型和配置信息 (如 AI 提供者的模型名称) 生成合适的显示文本
     * <p>该类继承自 IntelliJ 平台的 AnAction, 用于在 UI 中作为可执行的动作项, 允许用户通过点击或快捷键选择不同的日志生成方式
     * <p>当用户选择某个提供者时, 会更新全局设置中的 releaseLog 字段, 以持久化用户的选择
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.02
     * @since 1.0.0
     */
    private static class ReleaseLogProviderAction extends AnAction {
        /**
         * 用于表示当前选择的发布日志提供者
         * <p> 该字段用于在 UI 中标识用户选择的发布日志生成器, 如 GitCliff 或 AI 提供者 </p>
         *
         * @see ReleaseLogProvider
         */
        private final ReleaseLogProvider provider;

        /**
         * 初始化一个 ReleaseLogProviderAction 实例
         * <p> 通过指定的 ReleaseLogProvider 创建对应的 Action, 用于在 IDE 中提供释放日志的选项
         *
         * @param provider 用于创建 Action 的 ReleaseLogProvider 实例, 不能为 null
         */
        ReleaseLogProviderAction(ReleaseLogProvider provider) {
            super(getProviderDisplayTextStatic(provider));
            this.provider = provider;
        }

        /**
         * 获取指定发布日志提供者的显示文本
         * <p> 根据传入的发布日志提供者类型, 返回相应的显示文本. 对于 GitCliff 提供者, 会附加已安装版本号; 对于 AI 提供者, 会附加提供商类型和模型名称.
         *
         * @param provider 发布日志提供者实例
         * @return 显示文本, 包含提供商名称和相关信息
         * @since 1.0
         */
        @NotNull
        private static String getProviderDisplayTextStatic(@NotNull ReleaseLogProvider provider) {
            if (provider == ReleaseLogProvider.GIT_CLIFF) {
                String baseText = ChangelogBundle.message("statusbar.release.log.provider.gitcliff");
                String version = GitCliffDownloadManager.getInstalledVersion();
                if (version != null && !version.isEmpty()) {
                    return "🪨 " + baseText + " (" + version + ")";
                } else {
                    return "🪨 " + baseText;
                }
            }

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

        /**
         * 处理动作事件, 设置当前选择的发布日志提供者
         * <p> 当用户选择某个发布日志提供者时, 将该提供者设置为当前的默认提供者
         * <p> 此方法通常在用户界面中被调用, 用于响应用户的交互操作
         *
         * @param e 动作事件对象, 包含事件的上下文信息, 不能为 null
         * @since 1.0
         */
        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            SettingsState.getInstance().releaseLog = provider;
        }

        /**
         * 更新动作的显示状态
         * <p> 根据当前设置的状态更新动作的选中状态和文本显示
         *
         * @param e 动作事件对象
         * @since 1.0
         */
        @Override
        public void update(@NotNull AnActionEvent e) {
            boolean isSelected = SettingsState.getInstance().releaseLog == provider;
            e.getPresentation().putClientProperty(SELECTED_KEY, isSelected);
            e.getPresentation().setText(getProviderDisplayTextStatic(provider));
        }

        /**
         * 获取该动作的更新线程
         * <p> 指定此动作的更新线程为 BGT(Background Graphics Thread), 确保在后台线程中执行更新操作, 以避免阻塞 UI 线程.
         *
         * @return 返回 ActionUpdateThread.BGT 常量, 表示在后台图形线程中执行更新
         */
        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.BGT;
        }
    }

    /**
     * 打开设置动作类
     * <p> 继承自 AnAction, 用于在 IntelliJ IDEA 的状态栏中添加一个打开设置的动作.
     * <p> 该动作会在项目未被销毁的情况下, 显示项目的设置对话框.
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.02
     * @since 1.0.0
     */
    private static class OpenSettingsAction extends AnAction {
        /** 项目实例, 用于访问当前项目的上下文和资源 */
        private final Project project;

        /**
         * 构造一个用于打开设置对话框的操作对象
         * <p> 初始化操作时指定所属的项目上下文, 并设置操作名称为 "statusbar.open.settings" 的本地化字符串
         *
         * @param project 所属项目, 不能为 null
         */
        OpenSettingsAction(@NotNull Project project) {
            super(ChangelogBundle.message("statusbar.open.settings"));
            this.project = project;
        }

        /**
         * 处理动作事件, 用于打开设置对话框
         * <p> 当用户触发该动作时, 检查项目是否已释放, 若未释放则显示设置对话框
         *
         * @param e 动作事件对象, 不能为 null
         */
        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            if (project.isDisposed()) {
                return;
            }
            SettingsUtil.openSettings(project, ChangelogSettingsConfigurable.class);
        }

        /**
         * 获取动作更新线程
         * <p> 返回此动作更新应在哪个线程中执行. 此实现返回后台线程 (BGT), 表示动作更新应在后台线程中进行, 以避免阻塞 UI 线程.
         *
         * @return 动作更新线程类型, 此处返回 {@link ActionUpdateThread#BGT}
         */
        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.BGT;
        }
    }
}
