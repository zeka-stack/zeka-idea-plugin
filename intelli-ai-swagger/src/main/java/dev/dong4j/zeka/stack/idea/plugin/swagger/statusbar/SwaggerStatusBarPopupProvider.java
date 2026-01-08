package dev.dong4j.zeka.stack.idea.plugin.swagger.statusbar;

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
import dev.dong4j.zeka.stack.idea.plugin.swagger.settings.SettingsState;
import dev.dong4j.zeka.stack.idea.plugin.swagger.settings.SwaggerSettingsConfigurable;
import dev.dong4j.zeka.stack.idea.plugin.swagger.util.SwaggerBundle;
import icons.AICommonIcons;
import lombok.extern.slf4j.Slf4j;

/**
 * Swagger 状态栏弹出窗口提供者
 * <p> 该类实现了 AIStatusBarPopupProvider 接口, 用于在状态栏中提供与 AI 服务相关的弹出菜单.
 * <p> 主要功能包括:
 * <ul>
 * <li> 根据项目是否配置了 AI 提供者来动态创建不同的动作组.</li>
 * <li> 提供一个开关动作 (SwitchProviderAction), 用于切换当前项目的 AI 提供者.</li>
 * <li> 提供一个打开设置的动作 (OpenSettingsAction), 用于打开 AI 相关的设置界面.</li>
 * </ul>
 * <p> 具体实现如下:
 * <ul>
 * <li> 通过 {@link #getGroupName()} 方法返回弹出菜单的标题.</li>
 * <li> 通过 {@link #createActionGroup(Project, DataContext)} 方法创建并返回一个包含开关动作和打开设置动作的动作组.</li>
 * <li> 内部类 SwitchProviderAction 负责处理切换 AI 提供者的逻辑.</li>
 * <li> 内部类 ProviderSelectionActionGroup 负责根据可用的 AI 提供者列表创建相应的开关动作.</li>
 * <li> 内部类 OpenSettingsAction 负责打开 AI 设置界面.</li>
 * </ul>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.02
 * @since 1.0.0
 */
@Slf4j
public class SwaggerStatusBarPopupProvider implements AIStatusBarPopupProvider {
    /** 用于标识当前选项是否被选中的状态键 */
    private static final Key<Boolean> SELECTED_KEY = Key.create("selected");

    /**
     * 获取状态栏弹出窗口的组名
     * <p> 返回与状态栏提供者相关的组名, 该名称由 SwaggerBundle 提供.
     *
     * @return 状态栏提供者的组名
     */
    @Override
    public @NotNull String getGroupName() {
        return SwaggerBundle.message("statusbar.provider.popup.title");
    }

    /**
     * 创建状态栏弹出操作组
     * <p> 根据项目和数据上下文创建一个包含 AI 提供商选择和设置入口的操作组.
     *
     * @param project 当前项目实例, 不可为 null
     * @param context 数据上下文, 用于获取当前环境信息, 不可为 null
     * @return 构建好的操作组 (ActionGroup), 包含切换 AI 提供商, 分隔符和打开设置等操作
     * @since hello.world
     */
    @Override
    public @NotNull ActionGroup createActionGroup(@NotNull Project project, @NotNull DataContext context) {
        DefaultActionGroup group = new DefaultActionGroup();
        if (!AIProviderUtils.hasAIProvider(project,
                                           SwaggerBundle.message("settings.display.name"),
                                           SwaggerBundle.message("settings.ai.provider.selection"))) {
            group.add(new OpenSettingsAction(project));
            return group;
        }

        List<AIProviderConfig> providers = AIProviderUtils.getProviders();
        group.add(new ProviderSelectionActionGroup(project, providers));
        group.add(Separator.create(SwaggerBundle.message("statusbar.quick.settings.title")));
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
     * <p> 更新当前应用的默认 AI 服务提供商配置, 包括设置全局配置和应用级配置.
     * <p> 此方法会更新 {@link SettingsState} 中的 `providerConfig` 字段, 并调用全局设置服务更新默认配置.
     *
     * @param providerType 服务提供商类型, 不能为 null
     * @param config       服务提供商的具体配置, 不能为 null
     */
    private void switchDefaultProvider(@NotNull AIProviderType providerType, @NotNull AIProviderConfig config) {
        SettingsState settings = SettingsState.getInstance();
        settings.providerConfig = config;
        AIProviderSettings globalSettings = AIProviderSettings.getInstance();
        globalSettings.updateDefaultProviderConfig(providerType, config);
    }

    /**
     * 切换 AI 服务提供商的操作类
     * <p> 该类用于在 IDE 中提供一个切换默认 AI 服务提供商的菜单项操作, 支持在项目设置中选择不同的 AI 服务提供商并应用更改.
     * <p> 主要功能包括:
     * <ul>
     *   <li> 初始化操作时设置显示名称和图标 </li>
     *   <li> 执行操作时检查当前项目是否已配置 AI 服务提供商, 并在确认后切换默认服务商 </li>
     *   <li> 更新操作状态, 根据当前选中的 AI 服务提供商设置菜单项的选中状态 </li>
     * </ul>
     *
     * <pre>{@code
     * // 示例: 创建 SwitchProviderAction 实例
     * SwitchProviderAction action = new SwitchProviderAction(project, config);
     * }</pre>
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.02
     * @since 1.0.0
     */
    private class SwitchProviderAction extends AnAction {
        /**
         * 当前操作所属的项目实例
         *
         * @see Project
         */
        private final Project project;
        /**
         * AI 提供商配置对象
         * <p> 包含与 AI 提供商相关的配置信息, 例如提供商类型等
         *
         * @see AIProviderConfig
         */
        private final AIProviderConfig config;

        /**
         * 初始化 SwitchProviderAction 实例
         * <p> 通过传入的项目和配置信息初始化动作, 设置动作名称和图标
         *
         * @param project 项目对象, 不能为 null
         * @param config  AI 提供商配置对象, 不能为 null
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
         * <p> 当用户触发此操作时, 会检查当前项目和配置是否支持指定的 AI 服务提供商.
         * 如果支持, 则在后台线程中异步执行切换操作, 并确保在项目已关闭的情况下不会继续执行.</p>
         *
         * @param e 动作事件对象, 提供上下文信息
         * @see AIProviderUtils#hasAIProvider(Project, AIProviderConfig, String, String)
         * @see ApplicationManager#getApplication()* @see Project#isDisposed()
         * @see AIProviderConfig#copy()* @see #switchDefaultProvider(AIProviderType, AIProviderConfig)
         */
        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            if (!AIProviderUtils.hasAIProvider(project, config, SwaggerBundle.message("settings.display.name"), SwaggerBundle.message(
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
         * 更新动作的呈现状态
         * <p> 根据当前选择的服务商类型和配置的服务商类型, 更新动作的选中状态
         *
         * @param e 动作事件对象, 不能为 null
         * @since 1.0
         */
        @Override
        public void update(@NotNull AnActionEvent e) {
            AIProviderType currentType = getCurrentProviderType();
            boolean isSelected = config != null && config.providerType == currentType;
            e.getPresentation().putClientProperty(SELECTED_KEY, isSelected);
        }

        /**
         * 获取该动作的更新线程
         * <p>指定此动作的更新操作应在哪个线程上执行, 此处表示在事件调度线程 (BGT) 上执行.
         *
         * @return 返回动作更新线程类型, 始终返回 ActionUpdateThread.BGT
         */
        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.BGT;
        }
    }

    /**
     * 提供者选择操作组
     * <p> 用于在状态栏中展示 AI 服务提供者的切换选项, 继承自 {@link DefaultActionGroup}, 主要功能是动态添加各个 AI 提供者的切换动作.
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
         * <p> 创建一个用于选择 AI 提供者的操作组, 包含指定项目和提供者配置列表中的所有提供者选项
         * <p> 该操作组会为每个提供者配置创建一个切换提供者操作, 并添加到组中
         *
         * @param project   非空的项目对象, 用于创建切换提供者操作
         * @param providers 提供者配置列表, 如果为 null 则不添加任何操作
         */
        ProviderSelectionActionGroup(@NotNull Project project, List<AIProviderConfig> providers) {
            super(SwaggerBundle.message("statusbar.provider.selection.title"), true);
            if (providers != null) {
                for (AIProviderConfig config : providers) {
                    add(new SwitchProviderAction(project, config));
                }
            }
        }
    }

    /**
     * 打开设置动作类
     * <p> 用于在 IntelliJ 平台中提供打开 Swagger 插件设置界面的功能
     * <p> 该内部类继承自 AnAction, 负责响应用户触发的设置操作, 并通过 ShowSettingsUtil 显示配置对话框
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.02
     * @since 1.0.0
     */
    private static class OpenSettingsAction extends AnAction {
        /** 当前项目实例, 用于访问项目相关资源和状态 */
        private final Project project;

        /**
         * 构造函数, 初始化 OpenSettingsAction 对象
         * <p> 设置动作名称并绑定项目实例
         *
         * @param project 项目实例, 不能为 null
         */
        OpenSettingsAction(@NotNull Project project) {
            super(SwaggerBundle.message("statusbar.open.settings"));
            this.project = project;
        }

        /**
         * 处理打开设置对话框的动作
         * <p> 当用户触发该动作时, 检查项目是否已被释放, 若未被释放则显示设置对话框
         *
         * @param e 动作事件, 包含触发动作的上下文信息
         */
        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            if (project.isDisposed()) {
                return;
            }
            ShowSettingsUtil.getInstance().showSettingsDialog(project, SwaggerSettingsConfigurable.class);
        }

        /**
         * 获取动作更新线程
         * <p> 此方法重写自父类, 指定了动作更新的线程类型为后台线程 (BGT)
         *
         * @return 动作更新线程类型, 始终返回 BGT
         */
        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.BGT;
        }
    }
}
