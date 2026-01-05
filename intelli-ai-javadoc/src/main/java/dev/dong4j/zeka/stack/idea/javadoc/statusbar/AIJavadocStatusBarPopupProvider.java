package dev.dong4j.zeka.stack.idea.javadoc.statusbar;

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

import dev.dong4j.zeka.stack.idea.javadoc.settings.JavadocSettingsConfigurable;
import dev.dong4j.zeka.stack.idea.javadoc.settings.OverrideMode;
import dev.dong4j.zeka.stack.idea.javadoc.settings.SettingsState;
import dev.dong4j.zeka.stack.idea.javadoc.util.JavadocBundle;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIProviderType;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderSettings;
import dev.dong4j.zeka.stack.idea.plugin.common.statusbar.AIStatusBarPopupProvider;
import dev.dong4j.zeka.stack.idea.plugin.common.util.AIProviderUtils;
import dev.dong4j.zeka.stack.idea.plugin.kit.SettingsUtil;
import icons.AICommonIcons;
import lombok.extern.slf4j.Slf4j;

/**
 * AI 文档状态栏弹出提供者类
 * <p> 该类用于在 IntelliJ IDEA 状态栏中提供与 AI 生成文档相关的弹出菜单功能, 包括 AI 服务商切换, 生成配置设置, 代码压缩选项等. 它主要负责构建和管理状态栏上的操作组, 以支持用户快速访问和调整 AI 生成文档的相关设置.
 *
 * <p> 主要功能包括:
 * - 提供 AI 服务商的切换选项
 * - 支持生成文档的配置项 (如生成类, 方法, 字段)
 * - 控制是否覆盖已有注释及覆盖模式 (修复或替换)
 * - 启用或禁用生成上下文, 代码压缩, 单行注释压缩, 中文标点替换等功能
 * - 控制性能模式和提示信息显示
 * - 允许删除已有的 Javadoc 注释
 *
 * <p> 使用示例:
 * <pre>{@code
 * AIJavadocStatusBarPopupProvider provider = new AIJavadocStatusBarPopupProvider();
 * ActionGroup actionGroup = provider.createActionGroup(project, context);
 * // 将 actionGroup 添加到状态栏中
 * }</pre>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.02
 * @since 1.0.0
 */
@Slf4j
public class AIJavadocStatusBarPopupProvider implements AIStatusBarPopupProvider {
    /** 用于标识操作项是否被选中的状态键 */
    private static final Key<Boolean> SELECTED_KEY = Key.create("selected");

    /**
     * 获取状态栏弹出菜单的组名
     * <p> 该方法返回用于在状态栏中显示 AI Javadoc 相关操作的弹出菜单组标题.
     *
     * @return 状态栏弹出菜单的组名字符串, 不可为 null
     */
    @Override
    public @NotNull String getGroupName() {
        return JavadocBundle.message("statusbar.provider.popup.title");
    }

    /**
     * 创建状态栏弹出操作组
     * <p> 根据项目和上下文信息构建一个包含 AI 服务配置相关操作的 ActionGroup, 用于在状态栏中显示快捷设置选项.
     *
     * @param project 当前项目对象
     * @param context 数据上下文, 提供额外的环境信息
     * @return 构建好的操作组 (ActionGroup), 包含各种配置选项和切换动作
     */
    @Override
    public @NotNull ActionGroup createActionGroup(@NotNull Project project, @NotNull DataContext context) {
        DefaultActionGroup group = new DefaultActionGroup();
        if (!AIProviderUtils.hasAIProvider(project, JavadocBundle.message("settings.display.name"),
                                           JavadocBundle.message("settings.ai.provider.selection"))) {
            group.add(new OpenSettingsAction(project));
            return group;
        }

        List<AIProviderConfig> providers = AIProviderUtils.getProviders();
        group.add(new ProviderSelectionActionGroup(project, providers));
        group.add(Separator.create(JavadocBundle.message("statusbar.quick.settings.title")));
        group.add(new GenerationConfigActionGroup());
        group.add(createOverrideConfigActionGroup());
        group.add(new EnableGenerationContextToggleAction());
        group.add(new EnableCodeCompressionToggleAction());
        group.add(new CompressSingleLineJavaDocToggleAction());
        group.add(new ReplaceChinesePunctuationToggleAction());
        group.add(new PerformanceModeToggleAction());
        group.add(new ShowGenerateJavadocHintToggleAction());
        group.add(new AllowDeleteJavadocToggleAction());
        group.add(Separator.create());
        group.add(new OpenSettingsAction(project));
        return group;
    }

    /**
     * 获取当前选中的 AI 提供商类型
     * <p> 从全局设置中获取当前配置的 AI 提供商信息, 若未配置则返回默认值 QIANWEN
     *
     * @return 当前选中的 AI 提供商类型
     */
    @NotNull
    private AIProviderType getCurrentProviderType() {
        SettingsState settings = SettingsState.getInstance();
        return settings.providerConfig != null ? settings.providerConfig.providerType : AIProviderType.QIANWEN;
    }

    /**
     * 切换默认的 AI 服务提供商配置
     * <p> 将指定的 AI 服务提供商配置设置为默认配置, 并更新全局设置中的默认提供商信息.
     *
     * @param providerType 服务提供商类型, 不能为空
     * @param config       服务提供商配置对象, 不能为空
     */
    private void switchDefaultProvider(@NotNull AIProviderType providerType, @NotNull AIProviderConfig config) {
        SettingsState settings = SettingsState.getInstance();
        settings.providerConfig = config;
        AIProviderSettings globalSettings = AIProviderSettings.getInstance();
        globalSettings.updateDefaultProviderConfig(providerType, config);
    }

    /**
     * 切换提供商的操作类
     * <p> 用于在指定项目中切换 AI 提供商. 该类继承自 AnAction, 提供了切换提供商的具体逻辑.
     * <p> 通过构造函数初始化项目和提供商配置, 并根据配置设置图标.
     * <p> 在执行动作时, 检查当前项目是否存在指定的 AI 提供商, 如果不存在则不进行任何操作.
     * <p> 如果存在, 则在事件调度线程中调用写入操作, 更新提供商类型.
     * <p> 更新操作会复制当前配置, 并确保提供商类型正确切换.
     * <p> 在更新动作时, 根据当前提供商类型和配置, 设置动作选择状态.
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.02
     * @since 1.0.0
     */
    private class SwitchProviderAction extends AnAction {
        /** 项目实例, 用于访问 IDE 的项目相关功能 */
        private final Project project;
        /** AI 服务提供商配置信息 */
        private final AIProviderConfig config;

        /**
         * 初始化切换服务商动作
         * <p> 用于创建一个可以切换默认 AI 服务商的动作对象, 设置项目和配置信息, 并根据配置类型设置图标
         *
         * @param project 项目对象, 不能为 null
         * @param config  AI 服务商配置对象, 不能为 null
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
         * 处理用户选择切换默认 AI 提供商的动作
         * <p> 在用户触发动作时, 检查项目是否已废弃, 并尝试切换默认提供商
         * <p> 如果当前项目没有启用 AI 提供商, 则直接返回不做任何操作
         * <p> 切换操作会在事件调度线程中异步执行, 确保 UI 响应流畅
         * <p> 如果切换过程中发生异常, 会记录错误日志
         *
         * @param e 表示用户动作的事件对象, 不能为 null
         */
        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            if (!AIProviderUtils.hasAIProvider(project, config, JavadocBundle.message("settings.display.name"), JavadocBundle.message(
                "settings.ai.provider.selection"))) {
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
                    log.error("切换默认服务商失败", exception);
                }
            }, ModalityState.defaultModalityState());
        }

        /**
         * 更新动作呈现状态
         * <p> 根据当前选择的服务提供商类型更新动作的选中状态
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
         * 返回操作更新线程类型
         * <p> 此方法重写自父类, 指定此动作的操作更新线程为后台线程 (BGT)
         *
         * @return 操作更新线程类型, 始终返回 ActionUpdateThread.BGT
         */
        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.BGT;
        }
    }

    /**
     * 提供程序选择动作组
     * <p> 用于在 IDE 状态栏中显示 AI 提供商选择菜单, 支持动态添加和切换不同的 AI 提供商配置
     * <p> 该动作组继承自默认动作组, 用于构建上下文菜单项, 允许用户在多个 AI 提供商之间进行切换
     * <p> 使用示例:
     * <pre>{@code
     * List<AIProviderConfig> providers = Arrays.asList(config1, config2);
     * ProviderSelectionActionGroup group = new ProviderSelectionActionGroup(project, providers);
     * }</pre>
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
         * <p> 创建一个包含指定 AI 提供者配置的操作组, 用于在状态栏中切换不同的 AI 提供者
         * <p> 该操作组会为每个提供者配置添加一个切换动作, 允许用户在不同提供者之间进行切换
         *
         * @param project   当前项目实例, 用于传递给切换动作
         * @param providers 提供者配置列表, 包含所有可用的 AI 提供者配置
         */
        ProviderSelectionActionGroup(@NotNull Project project, List<AIProviderConfig> providers) {
            super(JavadocBundle.message("statusbar.provider.list.title"), true);
            for (AIProviderConfig config : providers) {
                add(new SwitchProviderAction(project, config));
            }
        }
    }

    /**
     * 生成配置动作组类
     * <p> 用于在状态栏快速设置中提供生成配置的动作选项, 支持为类, 方法和字段分别启用或禁用生成操作
     * <p> 该类继承自 DefaultActionGroup, 用于构建包含多个生成配置切换动作的工具栏或菜单项
     * <p> 主要功能包括:
     * <ul>
     *   <li> 为类生成配置 </li>
     *   <li> 为方法生成配置 </li>
     *   <li> 为字段生成配置 </li>
     * </ul>
     * <p> 通过组合不同的 ToggleAction 实例, 实现灵活的生成行为控制
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.02
     * @since 1.0.0
     */
    private static class GenerationConfigActionGroup extends DefaultActionGroup {
        /**
         * 初始化生成配置操作组
         * <p> 创建一个包含类, 方法和字段生成选项的操作组, 用于快速生成 Javadoc 配置
         * <p> 该操作组包含三个子项:
         * <ul>
         *   <li>{@link GenerateForClassToggleAction}: 用于生成类级别的 Javadoc</li>
         *   <li>{@link GenerateForMethodToggleAction}: 用于生成方法级别的 Javadoc</li>
         *   <li>{@link GenerateForFieldToggleAction}: 用于生成字段级别的 Javadoc</li>
         * </ul>
         *
         * @see GenerateForClassToggleAction
         * @see GenerateForMethodToggleAction
         * @see GenerateForFieldToggleAction
         */
        GenerationConfigActionGroup() {
            super(JavadocBundle.message("statusbar.quick.settings.generate.config"), true);
            add(new GenerateForClassToggleAction());
            add(new GenerateForMethodToggleAction());
            add(new GenerateForFieldToggleAction());
        }
    }

    /**
     * 生成类切换动作类
     * <p> 继承自 IntelliJ IDEA 的 ToggleAction 类, 用于在状态栏快速设置中启用或禁用“为类生成”功能.
     * <p> 该类通过重写父类的方法来控制选中状态和动作更新线程.
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.02
     * @since 1.0.0
     */
    private static class GenerateForClassToggleAction extends com.intellij.openapi.actionSystem.ToggleAction {
        /**
         * 初始化生成对于类的切换操作动作
         * <p> 通过调用父类构造函数初始化操作名称, 该名称从资源文件中获取
         *
         */
        GenerateForClassToggleAction() {
            super(JavadocBundle.message("statusbar.quick.settings.generate.for.class"));
        }

        /**
         * 获取此操作的更新线程
         * <p> 返回用于更新此操作状态的线程类型, 当前指定为后台线程 (BGT)
         *
         * @return 返回 {@link ActionUpdateThread#BGT} 表示在后台线程中更新操作状态
         */
        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.BGT;
        }

        /**
         * 判断当前动作是否被选中
         * <p> 该方法用于检查生成设置中是否启用了针对类的生成选项
         *
         * @param e 事件对象, 包含动作执行上下文信息
         * @return 如果启用针对类的生成选项则返回 true, 否则返回 false
         */
        @Override
        public boolean isSelected(@NotNull AnActionEvent e) {
            return SettingsState.getInstance().generateForClass;
        }

        /**
         * 设置生成类文档的选中状态
         * <p> 根据指定的事件和状态更新设置, 控制是否为类生成文档
         *
         * @param e     动作事件, 不能为 null
         * @param state 选中状态,true 表示启用,false 表示禁用
         */
        @Override
        public void setSelected(@NotNull AnActionEvent e, boolean state) {
            SettingsState.getInstance().generateForClass = state;
        }
    }

    /**
     * 方法生成切换操作类
     * <p> 用于在 IDE 状态栏中提供一个切换选项, 控制是否为方法生成文档注释的功能
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.02
     * @since 1.0.0
     */
    private static class GenerateForMethodToggleAction extends com.intellij.openapi.actionSystem.ToggleAction {
        /**
         * 构造函数, 初始化 GenerateForMethodToggleAction 对象
         * <p> 调用父类构造函数并设置动作名称
         *
         * @since 1.0
         */
        GenerateForMethodToggleAction() {
            super(JavadocBundle.message("statusbar.quick.settings.generate.for.method"));
        }

        /**
         * 获取此操作的更新线程
         * <p> 返回在后台线程中更新操作状态, 以避免阻塞 UI 线程
         *
         * @return 操作更新所使用的线程, 本方法始终返回
         */
        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.BGT;
        }

        /**
         * 判断当前操作是否被选中
         * <p> 返回一个布尔值表示该切换动作当前是否处于激活状态, 通常用于 UI 状态同步
         *
         * @param e 动作事件对象, 提供上下文信息, 不能为 null
         * @return 如果 generateForMethod 设置为 true, 则返回 true, 表示该操作被选中; 否则返回 false
         */
        @Override
        public boolean isSelected(@NotNull AnActionEvent e) {
            return SettingsState.getInstance().generateForMethod;
        }

        /**
         * 设置是否为方法生成注释的状态
         * <p> 根据传入的事件和状态更新是否为方法生成注释的配置项
         *
         * @param e     事件对象, 用于获取当前操作上下文
         * @param state 新的状态值, 表示是否为方法生成注释
         */
        @Override
        public void setSelected(@NotNull AnActionEvent e, boolean state) {
            SettingsState.getInstance().generateForMethod = state;
        }
    }

    /**
     * 用于控制是否为字段生成文档注释的切换操作类
     * <p> 该类继承自 {@code ToggleAction}, 用于在 IDE 状态栏中提供一个开关选项, 用户可选择是否自动为字段生成 Javadoc 注释
     * <p> 主要功能包括:
     * <ul>
     *     <li> 根据当前设置状态更新按钮显示 </li>
     *     <li> 将用户的操作结果保存到全局配置中 </li>
     * </ul>
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.02
     * @since 1.0.0
     */
    private static class GenerateForFieldToggleAction extends com.intellij.openapi.actionSystem.ToggleAction {
        /**
         * 初始化生成字段注释的切换动作
         * <p> 构造函数, 用于创建一个用于控制是否为字段生成注释的切换动作
         * <p> 该动作会绑定到 IDE 的状态设置中, 用于控制是否为字段生成注释
         *
         */
        GenerateForFieldToggleAction() {
            super(JavadocBundle.message("statusbar.quick.settings.generate.for.field"));
        }

        /**
         * 获取该操作的线程更新策略
         * <p> 此方法用于指定该动作在哪个线程上更新, 返回值为 BGT(Background Thread) 表示在后台线程上执行更新操作.
         *
         * @return 返回 ActionUpdateThread.BGT, 表示在后台线程上更新
         */
        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.BGT;
        }

        /**
         * 判断操作是否被选中
         * <p> 检查当前设置中是否启用了为字段生成代码的功能
         *
         * @param e 操作事件, 不能为 null
         * @return 如果设置中启用了为字段生成代码功能则返回 true, 否则返回 false
         */
        @Override
        public boolean isSelected(@NotNull AnActionEvent e) {
            return SettingsState.getInstance().generateForField;
        }

        /**
         * 设置生成字段注释的开关状态
         * <p> 该方法用于更新配置项 {@code generateForField} 的值, 控制是否为字段自动生成文档注释
         *
         * @param e     Action 事件对象, 不可为 null
         * @param state 新的状态值,true 表示启用字段注释生成,false 表示禁用
         */
        @Override
        public void setSelected(@NotNull AnActionEvent e, boolean state) {
            SettingsState.getInstance().generateForField = state;
        }
    }

    /**
     * 创建覆盖配置操作组
     * <p>根据当前设置生成包含覆盖选项的操作组, 包括启用覆盖, 覆盖模式 (修复或替换) 等选项
     * <p>如果启用了覆盖功能, 则在标题中显示当前的覆盖模式(修复或替换)
     * <p>使用示例:
     * <pre>{@code
     * DefaultActionGroup overrideGroup = createOverrideConfigActionGroup();
     * }</pre>
     *
     * @return 覆盖配置操作组, 包含启用覆盖和覆盖模式选项
     */
    @NotNull
    private static DefaultActionGroup createOverrideConfigActionGroup() {
        SettingsState settings = SettingsState.getInstance();
        String title = JavadocBundle.message("statusbar.quick.settings.override.config");
        if (settings.overrideExisting) {
            String currentMode = settings.overrideMode == OverrideMode.FIX
                                 ? JavadocBundle.message("statusbar.quick.settings.override.mode.fix")
                                 : JavadocBundle.message("statusbar.quick.settings.override.mode.replace");
            title = title + " (" + currentMode + ")";
        }

        DefaultActionGroup group = new DefaultActionGroup(title, true);
        group.add(new EnableOverrideToggleAction());
        group.add(Separator.create());
        group.add(new OverrideModeFixAction());
        group.add(new OverrideModeReplaceAction());
        return group;
    }

    /**
     * 切换覆盖现有设置的工具类
     * <p> 用于在 IntelliJ IDEA 插件中实现一个可切换的选项, 控制是否覆盖现有设置.
     * <p> 该类继承自 IntelliJ 的 ToggleAction, 提供一个可选的 UI 控件, 允许用户在启用和禁用覆盖模式之间切换.
     * <p> 当启用时, 新设置将覆盖已存在的设置; 当禁用时, 新设置将保留为独立项.
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.02
     * @since 1.0.0
     */
    private static class EnableOverrideToggleAction extends com.intellij.openapi.actionSystem.ToggleAction {
        /**
         * 构造函数, 初始化 EnableOverrideToggleAction 对象
         * <p> 调用父类构造函数并传入本地化的消息字符串作为动作名称
         *
         * @since 1.0
         */
        EnableOverrideToggleAction() {
            super(JavadocBundle.message("settings.override.existing"));
        }

        /**
         * 获取此操作的线程更新策略
         * <p> 指定此动作在哪个线程上更新, 确保 UI 操作在正确的线程执行
         *
         * @return 返回 {@link ActionUpdateThread#BGT}, 表示在后台线程上更新
         */
        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.BGT;
        }

        /**
         * 判断当前设置是否启用了覆盖现有功能
         * <p> 根据 SettingsState 的实例状态返回是否启用覆盖现有功能的标志
         *
         * @param e AnActionEvent 事件对象
         * @return 是否启用覆盖现有功能
         */
        @Override
        public boolean isSelected(@NotNull AnActionEvent e) {
            return SettingsState.getInstance().overrideExisting;
        }

        /**
         * 设置是否覆盖现有设置
         * <p> 根据指定的事件和状态更新设置状态, 用于控制是否覆盖现有设置
         *
         * @param e     动作事件对象, 不能为 null
         * @param state 是否覆盖现有设置的布尔值
         */
        @Override
        public void setSelected(@NotNull AnActionEvent e, boolean state) {
            SettingsState.getInstance().overrideExisting = state;
        }
    }

    /**
     * 重写模式修复操作类
     * <p> 用于在 IDE 状态栏中提供一个快速设置选项, 允许用户启用“修复模式”以覆盖现有设置.
     * <p> 该操作通常与 IDE 的设置状态管理相关联, 用于在用户选择时更新全局设置.
     * <p> 使用示例:
     * <pre>{@code
     * OverrideModeFixAction action = new OverrideModeFixAction();
     * action.actionPerformed(event);
     * }</pre>
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.02
     * @since 1.0.0
     */
    private static class OverrideModeFixAction extends AnAction {
        /**
         * 构造一个 OverrideModeFixAction 实例
         * <p> 初始化时设置操作的显示名称为 "statusbar.quick.settings.override.mode.fix" 的本地化消息
         */
        OverrideModeFixAction() {
            super(JavadocBundle.message("statusbar.quick.settings.override.mode.fix"));
        }

        /**
         * 执行覆盖模式修复操作
         * <p> 设置覆盖模式为修复模式, 并启用覆盖现有项功能
         *
         * @param e 动作事件, 包含触发该动作的上下文信息
         */
        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            SettingsState settings = SettingsState.getInstance();
            settings.overrideExisting = true;
            settings.overrideMode = OverrideMode.FIX;
        }

        /**
         * 更新操作动作的状态
         * <p> 根据当前设置状态决定该操作是否被选中, 并更新其在 UI 中的显示状态
         *
         * @param e 动作事件, 包含用于更新操作状态的 Presentation 对象, 不能为 null
         */
        @Override
        public void update(@NotNull AnActionEvent e) {
            SettingsState settings = SettingsState.getInstance();
            boolean isSelected = settings.overrideExisting && settings.overrideMode == OverrideMode.FIX;
            e.getPresentation().putClientProperty(SELECTED_KEY, isSelected);
        }

        /**
         * 获取该操作的更新线程类型
         * <p>指定此操作在哪个线程上执行更新, 此处返回在后台线程 (BGT) 上执行
         *
         * @return 返回 {@link ActionUpdateThread#BGT}, 表示在后台线程上执行
         */
        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.BGT;
        }
    }

    /**
     * 重写模式替换操作类
     * <p> 该类继承自 AnAction, 用于在状态栏快速设置中启用替换模式. 当此操作被触发时, 会将当前设置的状态更改为替换模式, 并设置为覆盖现有项目.
     * <p> 具体功能如下:
     * <ul>
     * <li> 初始化时, 通过父类构造函数设置操作名称.</li>
     * <li> 当操作被执行时, 更新设置状态以启用替换模式.</li>
     * <li> 在更新操作时, 根据当前设置状态调整操作的可用性或选中状态.</li>
     * </ul>
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.02
     * @since 1.0.0
     */
    private static class OverrideModeReplaceAction extends AnAction {
        /**
         * 构造函数, 初始化 OverrideModeReplaceAction 对象
         * <p> 调用父类构造函数, 并设置状态栏快速设置中的覆盖模式为替换模式
         *
         * @since 1.0
         */
        OverrideModeReplaceAction() {
            super(JavadocBundle.message("statusbar.quick.settings.override.mode.replace"));
        }

        /**
         * 执行替换模式操作, 设置覆盖现有项为 true 并将覆盖模式设为替换
         * <p> 此方法用于在用户界面中执行替换模式操作, 更新设置状态以启用替换行为.
         *
         * @param e 事件对象, 包含触发该操作的上下文信息
         */
        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            SettingsState settings = SettingsState.getInstance();
            settings.overrideExisting = true;
            settings.overrideMode = OverrideMode.REPLACE;
        }

        /**
         * 更新动作状态, 根据当前设置决定是否选中该操作
         * <p> 此方法用于更新动作的显示状态, 根据配置的覆盖模式和是否覆盖现有内容来判断是否选中该操作
         *
         * @param e 事件对象, 包含当前动作的上下文信息
         */
        @Override
        public void update(@NotNull AnActionEvent e) {
            SettingsState settings = SettingsState.getInstance();
            boolean isSelected = settings.overrideExisting && settings.overrideMode == OverrideMode.REPLACE;
            e.getPresentation().putClientProperty(SELECTED_KEY, isSelected);
        }

        /**
         * 返回操作更新线程
         * <p>此方法重写父类的方法, 返回一个后台线程 (BGT) 来更新操作.
         * <p>后台线程用于在后台执行更新操作, 避免阻塞主线程.
         *
         * @return 操作更新线程, 固定返回 ActionUpdateThread.BGT
         */
        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.BGT;
        }
    }

    /**
     * 用于切换生成上下文功能的按钮动作类
     * <p> 该类继承自 ToggleAction, 用于在状态栏中创建一个可切换的按钮, 控制是否启用生成上下文功能.
     * <p> 当用户点击该按钮时, 会更新设置中的 enableGenerationContext 状态, 从而影响后续生成逻辑的行为.
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.02
     * @since 1.0.0
     */
    private static class EnableGenerationContextToggleAction extends com.intellij.openapi.actionSystem.ToggleAction {
        /**
         * 初始化启用生成上下文切换操作
         * <p> 创建一个切换动作, 用于控制是否启用生成上下文功能
         * <p> 该动作会绑定到状态设置中的 enableGenerationContext 标志位
         *
         */
        EnableGenerationContextToggleAction() {
            super(JavadocBundle.message("statusbar.quick.settings.enable.generation.context"));
        }

        /**
         * 返回操作更新线程类型
         * <p> 此方法重写自父类, 用于指定该动作在后台线程中进行更新
         *
         * @return 操作更新线程类型, 始终返回 BGT (Background Thread)
         */
        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.BGT;
        }

        /**
         * 判断是否选中
         * <p> 检查当前设置状态, 返回是否启用生成上下文功能
         *
         * @param e 操作事件, 包含当前操作的上下文信息, 不能为 null
         * @return 如果启用生成上下文功能则返回 true, 否则返回 false
         */
        @Override
        public boolean isSelected(@NotNull AnActionEvent e) {
            return SettingsState.getInstance().enableGenerationContext;
        }

        /**
         * 设置生成上下文功能的启用状态
         * <p> 根据传入的布尔值设置是否启用生成上下文的功能, 该状态保存在 SettingsState 中
         *
         * @param e     动作事件对象, 用于获取当前上下文信息
         * @param state true 表示启用生成上下文功能,false 表示禁用
         */
        @Override
        public void setSelected(@NotNull AnActionEvent e, boolean state) {
            SettingsState.getInstance().enableGenerationContext = state;
        }
    }

    /**
     * 代码压缩功能切换动作类
     * <p> 用于在 IntelliJ IDEA 的状态栏快速设置中控制代码压缩功能的启用与禁用
     * <p> 该类继承自 IntelliJ 平台的 ToggleAction, 实现状态切换功能, 通过 SettingsState 获取和设置当前的代码压缩状态
     * <p> 当用户点击状态栏中的相关选项时, 会触发此动作, 更新 SettingsState 中的 enableCodeCompression 标志位
     * <p> 该功能通常用于在开发过程中快速切换代码的压缩显示状态, 以提升代码可读性或调试效率
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.02
     * @since 1.0.0
     */
    private static class EnableCodeCompressionToggleAction extends com.intellij.openapi.actionSystem.ToggleAction {
        /**
         * 构造函数, 用于创建启用代码压缩的切换操作
         * <p> 初始化时设置操作名称为 "statusbar.quick.settings.enable.code.compression" 的本地化字符串
         */
        EnableCodeCompressionToggleAction() {
            super(JavadocBundle.message("statusbar.quick.settings.enable.code.compression"));
        }

        /**
         * 获取该操作的线程更新策略
         * <p> 此方法用于指定该动作在哪个线程中更新状态, 默认为 BGT(Background Thread)
         *
         * @return 返回 ActionUpdateThread 类型, 表示该动作的线程更新策略
         */
        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.BGT;
        }

        /**
         * 判断当前操作是否被选中
         * <p> 该方法用于返回代码压缩功能的当前启用状态, 由设置中的 enableCodeCompression 控制
         *
         * @param e Action 事件对象, 不能为 null
         * @return 如果代码压缩功能已启用则返回 true, 否则返回 false
         */
        @Override
        public boolean isSelected(@NotNull AnActionEvent e) {
            return SettingsState.getInstance().enableCodeCompression;
        }

        /**
         * 设置代码压缩功能的启用状态
         * <p> 根据传入的布尔值设置代码压缩功能是否启用, 该功能状态存储在 SettingsState 单例中
         *
         * @param e     事件对象, 用于获取当前动作事件的信息
         * @param state 是否启用代码压缩功能
         */
        @Override
        public void setSelected(@NotNull AnActionEvent e, boolean state) {
            SettingsState.getInstance().enableCodeCompression = state;
        }
    }

    /**
     * 压缩单行 JavaDoc 动作类
     * <p> 此类继承自 IntelliJ IDEA 的 ToggleAction, 用于在状态栏中切换压缩单行 JavaDoc 的设置.
     * <p> 该动作通过读取和修改 SettingsState 中的 compressSingleLineJavaDoc 属性来控制是否启用压缩单行 JavaDoc 功能.
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.02
     * @since 1.0.0
     */
    private static class CompressSingleLineJavaDocToggleAction extends com.intellij.openapi.actionSystem.ToggleAction {
        /**
         * 构造函数, 初始化 CompressSingleLineJavaDocToggleAction 对象
         * <p> 调用父类 ToggleAction 的构造函数, 并传入状态栏快速设置中的压缩单行 Javadoc 提示信息
         *
         * @since 该构造函数用于创建 CompressSingleLineJavaDocToggleAction 实例
         */
        CompressSingleLineJavaDocToggleAction() {
            super(JavadocBundle.message("statusbar.quick.settings.compress.single.line.javadoc"));
        }

        /**
         * 获取动作更新线程
         * <p> 返回此动作更新应运行的线程类型
         *
         * @return 动作更新线程类型, 此处返回后台线程 (BGT)
         */
        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.BGT;
        }

        /**
         * 判断是否选中压缩单行 JavaDoc 的设置
         * <p> 根据当前设置状态返回是否启用压缩单行 JavaDoc 功能
         *
         * @param e 操作事件, 包含当前操作的上下文信息, 不能为 null
         * @return 如果启用压缩单行 JavaDoc 功能则返回 true, 否则返回 false
         */
        @Override
        public boolean isSelected(@NotNull AnActionEvent e) {
            return SettingsState.getInstance().compressSingleLineJavaDoc;
        }

        /**
         * 设置是否压缩单行 Javadoc 的状态
         * <p> 根据传入的事件和状态更新设置中是否压缩单行 Javadoc 的配置项
         *
         * @param e     事件对象, 用于获取动作上下文信息
         * @param state 新的状态值, 表示是否压缩单行 Javadoc
         */
        @Override
        public void setSelected(@NotNull AnActionEvent e, boolean state) {
            SettingsState.getInstance().compressSingleLineJavaDoc = state;
        }
    }

    /**
     * 中文标点替换切换动作类
     * <p> 该内部类用于在 IDE 状态栏中提供一个可切换的按钮, 控制是否启用中文标点替换功能.
     * <p> 通过继承 ToggleAction 实现状态切换逻辑, 与用户设置中的 replaceChinesePunctuation 字段绑定.
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.02
     * @since 1.0.0
     */
    private static class ReplaceChinesePunctuationToggleAction extends com.intellij.openapi.actionSystem.ToggleAction {
        /**
         * 初始化替换中文标点符号切换操作
         * <p> 创建一个用于切换是否替换中文标点符号的 UI 操作项, 该操作项会绑定到设置状态
         *
         */
        ReplaceChinesePunctuationToggleAction() {
            super(JavadocBundle.message("statusbar.quick.settings.replace.chinese.punctuation"));
        }

        /**
         * 返回操作更新线程
         * <p> 此方法重写自父类, 指定此操作在后台线程进行更新
         *
         * @return 操作更新线程, 始终返回 ActionUpdateThread.BGT 表示后台线程
         */
        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.BGT;
        }

        /**
         * 判断当前操作是否被选中
         * <p> 返回用于控制中文标点替换功能的开关状态
         *
         * @param e Action 事件对象, 不能为 null
         * @return 如果中文标点替换功能处于启用状态则返回 true, 否则返回 false
         */
        @Override
        public boolean isSelected(@NotNull AnActionEvent e) {
            return SettingsState.getInstance().replaceChinesePunctuation;
        }

        /**
         * 设置是否替换中文标点符号的选中状态
         * <p> 根据指定的事件和状态更新设置状态中的替换中文标点符号选项
         *
         * @param e     动作事件对象, 不能为 null
         * @param state 选中状态,true 表示启用替换,false 表示禁用替换
         */
        @Override
        public void setSelected(@NotNull AnActionEvent e, boolean state) {
            SettingsState.getInstance().replaceChinesePunctuation = state;
        }
    }

    /**
     * 性能模式切换操作类
     * <p> 用于在 IDE 中切换性能模式的切换动作, 该模式可以控制是否启用性能优化相关的功能.
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.02
     * @since 1.0.0
     */
    private static class PerformanceModeToggleAction extends com.intellij.openapi.actionSystem.ToggleAction {
        /**
         * 构造函数, 初始化性能模式切换操作
         * <p> 使用指定的文本作为操作名称, 该文本通过 JavadocBundle 加载国际化资源
         */
        PerformanceModeToggleAction() {
            super(JavadocBundle.message("settings.performance.mode"));
        }

        /**
         * 获取该动作的更新线程
         * <p>此方法用于指定该动作在哪个线程上更新, 返回值表示该动作应在后台线程 (BGT) 上更新.
         *
         * @return 返回 ActionUpdateThread.BGT, 表示该动作应在后台线程上更新
         */
        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.BGT;
        }

        /**
         * 判断当前性能模式是否被选中
         * <p> 该方法用于检查设置中的性能模式是否处于激活状态, 返回对应的布尔值
         *
         * @param e 事件对象, 包含与动作相关的上下文信息
         * @return 如果性能模式已启用则返回 true, 否则返回 false
         */
        @Override
        public boolean isSelected(@NotNull AnActionEvent e) {
            return SettingsState.getInstance().performanceMode;
        }

        /**
         * 设置性能模式的选中状态
         * <p> 根据指定的事件和状态更新性能模式的选中状态
         * <p> 此方法通常由 IDE 的动作系统调用, 用于响应用户界面中的切换操作
         *
         * @param e     动作事件对象, 包含当前操作的上下文信息, 不能为 null
         * @param state 性能模式的选中状态,true 表示启用性能模式,false 表示禁用
         */
        @Override
        public void setSelected(@NotNull AnActionEvent e, boolean state) {
            SettingsState.getInstance().performanceMode = state;
        }
    }

    /**
     * 显示生成 Javadoc 提示的切换动作类
     * <p> 该类继承自 com.intellij.openapi.actionSystem.ToggleAction, 用于在 IntelliJ IDEA 的状态栏中切换显示生成 Javadoc 提示的功能
     * <p> 通过调用父类的构造函数设置动作名称, 并重写相关方法来控制动作的状态和更新线程
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.02
     * @since 1.0.0
     */
    private static class ShowGenerateJavadocHintToggleAction extends com.intellij.openapi.actionSystem.ToggleAction {
        /**
         * 构造函数, 用于初始化 "显示生成 Javadoc 提示" 切换操作
         * <p> 该操作用于控制是否在状态栏中显示快速设置中的 Javadoc 生成提示开关
         *
         */
        ShowGenerateJavadocHintToggleAction() {
            super(JavadocBundle.message("statusbar.quick.settings.show.generate.javadoc.hint"));
        }

        /**
         * 获取该操作的线程更新策略
         * <p> 返回此操作应在其上执行的线程类型, 用于控制操作在 IDE 中的执行上下文.
         *
         * @return 操作的线程更新策略, 始终返回 {@code ActionUpdateThread.BGT}
         */
        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.BGT;
        }

        /**
         * 判断是否选中生成 Javadoc 提示
         * <p> 检查当前设置中是否启用了显示生成 Javadoc 提示功能
         *
         * @param e 操作事件, 包含当前操作的上下文信息, 不能为 null
         * @return 如果设置中启用了显示生成 Javadoc 提示, 则返回 true, 否则返回 false
         */
        @Override
        public boolean isSelected(@NotNull AnActionEvent e) {
            return SettingsState.getInstance().showGenerateJavadocHint;
        }

        /**
         * 设置是否显示生成 Javadoc 提示
         * <p> 更新全局设置中是否显示生成 Javadoc 的提示状态
         *
         * @param e     动作事件对象, 包含当前操作的上下文信息, 不能为 null
         * @param state 新的提示显示状态,true 表示显示,false 表示隐藏
         */
        @Override
        public void setSelected(@NotNull AnActionEvent e, boolean state) {
            SettingsState.getInstance().showGenerateJavadocHint = state;
        }
    }

    /**
     * 允许删除 Javadoc 切换操作类
     * <p> 该类用于在 IDE 中提供一个切换选项, 控制是否允许删除 Javadoc 注释的功能.
     * <p> 此操作通过继承 ToggleAction 实现, 用户可以通过菜单或快捷键切换该功能的状态.
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.02
     * @since 1.0.0
     */
    private static class AllowDeleteJavadocToggleAction extends com.intellij.openapi.actionSystem.ToggleAction {
        /**
         * 构造函数, 初始化允许删除 Javadoc 的切换动作
         * <p> 该构造函数调用父类的构造函数, 并设置动作名称为从 JavadocBundle 获取的消息 "settings.allow.delete.javadoc"
         *
         * @since 1.0
         */
        AllowDeleteJavadocToggleAction() {
            super(JavadocBundle.message("settings.allow.delete.javadoc"));
        }

        /**
         * 获取此操作的更新线程类型
         * <p> 指定该操作在后台线程中更新其状态, 避免阻塞 UI 线程
         *
         * @return 返回 {@link ActionUpdateThread#BGT} 表示在后台线程中更新
         */
        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.BGT;
        }

        /**
         * 判断操作是否被选中
         * <p> 检查当前设置中是否允许删除 Javadoc 注释
         *
         * @param e 操作事件, 不能为 null
         * @return 如果允许删除 Javadoc 则返回 true, 否则返回 false
         */
        @Override
        public boolean isSelected(@NotNull AnActionEvent e) {
            return SettingsState.getInstance().allowDeleteJavadoc;
        }

        /**
         * 设置允许删除 Javadoc 的状态
         * <p> 该方法用于更新用户设置中是否允许删除 Javadoc 的布尔值
         *
         * @param e     动作事件对象, 提供上下文信息, 不可为 null
         * @param state 新的状态值,true 表示允许删除 Javadoc,false 表示不允许
         */
        @Override
        public void setSelected(@NotNull AnActionEvent e, boolean state) {
            SettingsState.getInstance().allowDeleteJavadoc = state;
        }
    }

    /**
     * 打开设置动作类
     * <p> 用于在 IntelliJ IDEA 中创建一个可执行的动作, 点击后会打开项目设置对话框, 允许用户配置与 Javadoc 相关的设置项.
     * <p> 该动作通常出现在状态栏或右键菜单中, 为用户提供快速访问设置的入口.
     * <p> 使用示例:
     * <pre>{@code
     * OpenSettingsAction action = new OpenSettingsAction(project);
     * action.actionPerformed(event);
     * }</pre>
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
         * 初始化打开设置动作
         * <p> 创建一个用于打开设置对话框的动作, 绑定到指定的项目上下文
         *
         * @param project 非空的项目实例, 用于在设置对话框中显示项目相关的配置
         */
        OpenSettingsAction(@NotNull Project project) {
            super(JavadocBundle.message("statusbar.quick.settings.open.settings"));
            this.project = project;
        }

        /**
         * 处理动作事件以显示设置对话框
         * <p> 当用户触发此动作时, 会调用该方法以显示与 Javadoc 设置相关的配置对话框
         *
         * @param e 动作事件对象, 不能为 null
         */
        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            SettingsUtil.openSettings(project, JavadocSettingsConfigurable.class);
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
}
