package dev.dong4j.zeka.stack.idea.plugin.example.statusbar;

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
import dev.dong4j.zeka.stack.idea.plugin.example.settings.ExampleSettingsConfigurable;
import dev.dong4j.zeka.stack.idea.plugin.example.settings.SettingsState;
import dev.dong4j.zeka.stack.idea.plugin.example.util.ExampleBundle;
import icons.AICommonIcons;
import lombok.extern.slf4j.Slf4j;

/**
 * 状态栏弹出菜单提供者
 * <p> 该类用于为状态栏提供弹出菜单功能, 主要负责构建与 AI 相关配置和操作的菜单项, 包括服务商选择, 设置打开等功能.
 * <p> 实现 {@link AIStatusBarPopupProvider} 接口, 提供动态生成菜单组的能力, 支持根据当前项目配置展示不同的菜单选项.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.02
 * @since 1.0.0
 */
@Slf4j
public class ExampleStatusBarPopupProvider implements AIStatusBarPopupProvider {
    /** 标记当前提供者是否被选中的状态键 */
    private static final Key<Boolean> SELECTED_KEY = Key.create("selected");

    /**
     * 获取状态栏弹出组的名称
     * <p> 返回与状态栏弹出相关的组名称
     *
     * @return 组名称, 不能为空
     */
    @Override
    public @NotNull String getGroupName() {
        return ExampleBundle.message("statusbar.provider.popup.title");
    }

    /**
     * 创建一个包含操作项的动作组
     * <p> 根据项目是否具有 AI 提供者来构建动作组. 如果没有 AI 提供者, 则仅添加打开设置的操作项;
     * 如果有多个 AI 提供者, 则添加提供者选择的操作组, 并在最后添加快速设置分隔符和打开设置的操作项.
     *
     * @param project 当前项目
     * @param context 当前上下文数据
     * @return 包含操作项的动作组
     */
    @Override
    public @NotNull ActionGroup createActionGroup(@NotNull Project project, @NotNull DataContext context) {
        DefaultActionGroup group = new DefaultActionGroup();
        if (!AIProviderUtils.hasAIProvider(project, ExampleBundle.message("settings.display.name"), ExampleBundle.message("settings.ai.provider.selection"))) {
            group.add(new OpenSettingsAction(project));
            return group;
        }

        List<AIProviderConfig> providers = AIProviderUtils.getProviders();
        group.add(new ProviderSelectionActionGroup(project, providers));
        group.add(Separator.create(ExampleBundle.message("statusbar.quick.settings.title")));
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
     *   <li> 根据配置显示对应的服务商图标和名称 </li>
     *   <li> 执行切换 AI 服务商的异步操作 </li>
     *   <li> 更新当前操作状态 (是否被选中)</li>
     * </ul>
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.02
     * @since 1.0.0
     */
    private class SwitchProviderAction extends AnAction {
        /** 项目实例, 表示当前操作所在的 IntelliJ 项目上下文 */
        private final Project project;
        /**
         * AI 提供商配置信息
         * <p> 包含提供商类型, 模型名称等配置项
         *
         * @see AIProviderConfig
         */
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
            if (!AIProviderUtils.hasAIProvider(project, config, ExampleBundle.message("settings.display.name"), ExampleBundle.message("settings.ai.provider.selection"))) {
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
     * 提供商选择操作组类
     * <p> 继承自 DefaultActionGroup, 用于创建一组动作, 每个动作对应一个 AI 提供商配置.
     * <p> 在构造函数中, 根据传入的项目和提供商配置列表, 动态地添加切换提供商的动作.
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.02
     * @since 1.0.0
     */
    private class ProviderSelectionActionGroup extends DefaultActionGroup {
        /**
         * 初始化提供者选择操作组
         * <p> 创建一个包含多个提供者切换操作的动作组, 用于在状态栏中显示和切换不同的 AI 提供者
         * <p> 该构造函数会根据传入的提供者列表动态添加相应的切换动作
         *
         * @param project   当前项目实例, 用于创建切换动作时的上下文
         * @param providers 提供者配置列表, 包含所有可用的 AI 提供者配置, 如果为 null 则不添加任何动作
         */
        ProviderSelectionActionGroup(@NotNull Project project, List<AIProviderConfig> providers) {
            super(ExampleBundle.message("statusbar.provider.selection.title"), true);
            if (providers != null) {
                for (AIProviderConfig config : providers) {
                    add(new SwitchProviderAction(project, config));
                }
            }
        }
    }

    /**
     * 状态栏打开设置操作类
     * <p> 用于在状态栏中创建一个动作, 点击后可以打开项目的设置对话框
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.02
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
            super(ExampleBundle.message("statusbar.open.settings"));
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
            ShowSettingsUtil.getInstance().showSettingsDialog(project, ExampleSettingsConfigurable.class);
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
}
