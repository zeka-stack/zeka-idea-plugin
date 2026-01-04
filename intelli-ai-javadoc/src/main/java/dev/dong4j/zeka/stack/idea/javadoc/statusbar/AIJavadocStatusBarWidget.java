package dev.dong4j.zeka.stack.idea.javadoc.statusbar;

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

import dev.dong4j.zeka.stack.idea.javadoc.settings.JavadocSettingsConfigurable;
import dev.dong4j.zeka.stack.idea.javadoc.settings.OverrideMode;
import dev.dong4j.zeka.stack.idea.javadoc.settings.SettingsState;
import dev.dong4j.zeka.stack.idea.javadoc.util.JavadocBundle;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIProviderType;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderSettings;
import dev.dong4j.zeka.stack.idea.plugin.common.util.AIProviderUtils;
import icons.AICommonIcons;
import icons.AIJicons;
import lombok.extern.slf4j.Slf4j;

/**
 * AI Javadoc 状态栏组件
 * <p>
 * 该组件用于在 IDE 状态栏中显示当前使用的 AI 服务商信息, 并提供切换服务商的功能.
 * 继承自 EditorBasedStatusBarPopup, 实现了状态栏弹出菜单和状态显示功能.
 * 用户可以通过点击状态栏组件来查看可用的 AI 服务商列表并进行切换操作.
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email mailto:zeka.stack@gmail.com
 * @date 2025.11.30
 * @since 1.0.0
 */
@Slf4j
public class AIJavadocStatusBarWidget extends EditorBasedStatusBarPopup {
    /**
     * 状态栏小部件的唯一标识符
     * <p>
     * 用于标识 AI Javadoc 状态栏组件的唯一字符串常量, 确保在 IDE 状态栏中正确识别和定位该组件.
     *
     * @since 1.0.0
     */
    public static final String WIDGET_ID = "dev.dong4j.zeka.stack.idea.statusbar.javadoc.AIJavadocStatusBarWidget";
    /**
     * 选中状态的键值对标识
     * <p>
     * 用于在动作事件中标识当前选项是否被选中, 存储为客户端属性.
     */
    private static final Key<Boolean> SELECTED_KEY = Key.create("selected");
    /**
     * 项目实例, 表示当前操作的项目上下文.
     *
     * @since 1.0.0
     */
    private final Project project;
    /** 状态栏组件, 用于显示应用程序状态信息 */
    private StatusBar statusBar;

    /**
     * 构造 AI Javadoc 状态栏组件
     * <p>
     * 初始化 AI Javadoc 状态栏组件, 设置项目上下文
     *
     * @param project 项目实例, 不可为 null
     */
    public AIJavadocStatusBarWidget(@NotNull Project project) {
        super(project, false);
        this.project = project;
    }

    /**
     * 创建状态栏组件实例
     * <p>
     * 重写父类方法, 根据指定项目创建 AI Javadoc 状态栏组件实例
     *
     * @param project 项目实例, 不能为空
     * @return AI Javadoc 状态栏组件实例, 不为 null
     */
    @Override
    protected @NotNull StatusBarWidget createInstance(@NotNull Project project) {
        return new AIJavadocStatusBarWidget(project);
    }

    // registerCustomListeners() 方法已过时，不再需要重写
    // 如果将来需要注册自定义监听器，请使用其他方式

    /**
     * 获取组件的唯一标识符
     * <p>
     * 返回预定义的组件 ID 常量值
     *
     * @return 组件的唯一标识符, 不为 null
     */
    @Override
    public @NotNull String ID() {
        return WIDGET_ID;
    }

    /**
     * 安装状态栏组件
     * <p>
     * 调用父类的 {@code install} 方法, 并将传入的 {@link StatusBar} 对象保存到本实例中, 以便后续使用.
     *
     * @param statusBar 要安装的状态栏对象, 不能为空
     */
    @Override
    public void install(@NotNull StatusBar statusBar) {
        super.install(statusBar);
        this.statusBar = statusBar;
    }

    /**
     * 释放资源
     * <p>
     * 调用父类的 {@code dispose} 方法, 并将 {@code statusBar} 置为 {@code null}, 以便及时回收资源.
     *
     * @since 1.0.0
     */
    @Override
    public void dispose() {
        super.dispose();
        statusBar = null;
    }

    /**
     * 获取小部件状态
     * <p>
     * 根据虚拟文件获取小部件的当前状态, 包括显示文本, 工具提示和图标.
     * 状态栏只显示插件图标, 不显示模型名称.
     *
     * @param file 虚拟文件, 可为 null
     * @return 小部件状态对象, 不为 null
     */
    @Override
    protected @NotNull WidgetState getWidgetState(@Nullable VirtualFile file) {
        String displayText = getCurrentProviderModelName();
        String tooltip = JavadocBundle.message("statusbar.provider.tooltip", displayText);
        // 只显示图标，不显示文本
        WidgetState state = new WidgetState(tooltip, "", true);

        // 使用插件图标，不显示提供商图标
        Icon iconToUse = IconUtil.scale(AIJicons.AIJ_16, null, 0.8125f);
        state.setIcon(iconToUse);

        return state;
    }

    /**
     * 创建 AI 提供程序切换弹出菜单
     * <p>
     * 根据当前数据上下文创建一个包含可用 AI 提供程序列表的弹出菜单,
     * 如果没有可用的提供程序则显示错误通知并返回 null
     *
     * @param context 数据上下文, 包含当前操作的上下文信息
     * @return ListPopup 类型的弹出菜单, 如果没有可用提供程序则返回 null
     */
    @Override
    protected @Nullable ListPopup createPopup(@NotNull DataContext context) {
        // 检查 AI Provider 配置
        if (!AIProviderUtils.hasAIProvider(project, JavadocBundle.message("settings.display.name"), JavadocBundle.message("settings.ai" +
                                                                                                                          ".provider" +
                                                                                                                          ".selection"))) {
            return null;
        }

        // 获取可用的提供商列表
        List<AIProviderConfig> providers = AIProviderUtils.getProviders();
        // 创建 Action 组
        DefaultActionGroup group = new DefaultActionGroup();

        // 1. 添加 AI Provider 选择子菜单（放在最上面，作为模型选择）
        group.add(new ProviderSelectionActionGroup(providers));

        // 2. 添加快捷配置（用分隔符包裹，形成边框效果）
        group.add(Separator.create(JavadocBundle.message("statusbar.quick.settings.title")));

        // 2.1 生成配置（二级菜单，多选）
        group.add(new GenerationConfigActionGroup());

        // 2.2 覆写配置（包含启用开关和模式选择）
        group.add(createOverrideConfigActionGroup());

        // 2.3 添加上下文
        group.add(new EnableGenerationContextToggleAction());

        // 2.4 允许代码压缩
        group.add(new EnableCodeCompressionToggleAction());

        // 2.5 压缩成一行
        group.add(new CompressSingleLineJavaDocToggleAction());

        // 2.6 中英文标点符号替换
        group.add(new ReplaceChinesePunctuationToggleAction());

        // 2.7 性能模式
        group.add(new PerformanceModeToggleAction());

        // 2.8 显示生成 Javadoc 提示
        group.add(new ShowGenerateJavadocHintToggleAction());

        // 2.9 保存时生成注释
        group.add(new GenerateOnSaveToggleAction());

        // 2.10 允许删除 Javadoc
        group.add(new AllowDeleteJavadocToggleAction());

        group.add(Separator.create());

        // 3. 添加打开设置按钮
        group.add(new OpenSettingsAction());

        // 创建弹出菜单
        return JBPopupFactory.getInstance().createActionGroupPopup(
            JavadocBundle.message("statusbar.provider.popup.title"),
            group,
            context,
            JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
            true
                                                                  );
    }

    /**
     * 获取当前 AI 提供商类型
     * <p>
     * 从设置状态中获取当前配置的 AI 提供商类型, 如果未配置则默认返回千问类型
     *
     * @return 当前 AI 提供商类型, 如果未配置则返回默认的 QIANWEN 类型
     */
    @NotNull
    private AIProviderType getCurrentProviderType() {
        SettingsState settings = SettingsState.getInstance();
        return settings.providerConfig != null ? settings.providerConfig.providerType : AIProviderType.QIANWEN;
    }

    /**
     * 获取指定 AI 提供商类型的默认配置
     * <p>
     * 通过 {@link AIProviderSettings#getInstance()} 获取全局设置实例, 并返回对应
     * {@link AIProviderType} 的默认 {@link AIProviderConfig}.
     *
     * @param providerType AI 提供商类型, 不能为空
     * @return 对应类型的默认配置 (非 {@code null})
     */
    @NotNull
    private AIProviderConfig getDefaultProviderConfig(@NotNull AIProviderType providerType) {
        AIProviderSettings globalSettings = AIProviderSettings.getInstance();
        return globalSettings.getDefaultProviderConfig(providerType);
    }

    /**
     * 获取当前提供商的模型名称
     * <p>
     * 获取当前选中的 AI 提供商类型的默认配置, 并返回其模型名称
     *
     * @return 当前提供商的模型名称, 不为 null
     */
    @NotNull
    private String getCurrentProviderModelName() {
        AIProviderType providerType = getCurrentProviderType();
        AIProviderConfig defaultConfig = getDefaultProviderConfig(providerType);
        return defaultConfig.modelName;
    }

    /**
     * 切换默认 AI 提供商
     * <p>
     * 根据指定的提供商类型和配置更新默认的 AI 提供商设置
     *
     * @param providerType AI 提供商类型, 不能为空
     * @param config       AI 提供商配置, 不能为空
     */
    private void switchDefaultProvider(@NotNull AIProviderType providerType, @NotNull AIProviderConfig config) {
        // 更新插件配置中的默认提供商选择
        SettingsState settings = SettingsState.getInstance();
        settings.providerConfig = config;
        // 更新全局配置中的提供商配置
        AIProviderSettings globalSettings = AIProviderSettings.getInstance();
        globalSettings.updateDefaultProviderConfig(providerType, config);
    }

    /**
     * 切换 AI 服务商提供者动作类
     * <p>
     * 该类继承自 AnAction, 用于处理 AI 服务商的切换操作. 它允许用户在不同的 AI 服务商之间进行切换,
     * 并更新相关的配置和状态. 此类包含错误处理机制, 确保在切换过程中出现异常时能够正确处理
     * 并向用户显示相应的错误通知. 同时支持在状态栏中显示当前选中的服务商状态.
     *
     * @author zeka.stack.team
     * @version 1.0.0
     * @email "mailto:zeka.stack@gmail.com"
     * @date 2025.11.30
     * @since 1.0.0
     */
    private class SwitchProviderAction extends AnAction {
        /** AI 提供商配置 */
        private final AIProviderConfig config;

        /**
         * 构造函数, 创建切换提供者动作实例
         * <p>
         * 根据 AI 提供者配置初始化动作, 设置模型名称并配置提供者图标
         *
         * @param config AI 提供者配置对象, 包含提供者类型, 模型名称等信息
         */
        SwitchProviderAction(AIProviderConfig config) {
            super(config.modelName);
            this.config = config;

            // 设置图标
            if (config.providerType != null) {
                Icon providerIcon = AICommonIcons.getProviderIcon(config.providerType);
                if (providerIcon != null) {
                    getTemplatePresentation().setIcon(providerIcon);
                }
            }
        }

        /**
         * 处理动作事件, 切换默认 AI 服务商
         * <p>
         * 当用户触发切换服务商的动作时, 执行相应的切换逻辑,
         * 包括验证服务商类型, 在写操作中更新配置, 处理异常情况等
         *
         * @param e 动作事件对象, 包含事件的相关信息
         * @throws NullPointerException 当传入的事件对象为 null 时抛出
         */
        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            // 检查 AI Provider 配置
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
                } finally {
                    StatusBar currentStatusBar = statusBar;
                    if (currentStatusBar != null) {
                        currentStatusBar.updateWidget(ID());
                    }
                    // update() 方法被标记为 @ApiStatus.OverrideOnly，只能被重写，不应手动调用
                    // 框架会在需要时自动调用 update() 方法来更新动作状态
                }
            }, ModalityState.defaultModalityState());
        }

        /**
         * 更新动作事件的状态
         * <p>
         * 根据当前 AI 提供者类型和配置信息更新动作事件的展示状态, 将选中状态存储到客户端属性中
         *
         * @param e 动作事件对象, 不能为空
         */
        @Override
        public void update(@NotNull AnActionEvent e) {
            // 如果是当前选中的提供商,显示选中标记
            AIProviderType currentType = getCurrentProviderType();
            boolean isSelected = config != null && config.providerType == currentType;
            e.getPresentation().putClientProperty(SELECTED_KEY, isSelected);
        }

        /**
         * 获取动作更新线程
         * <p>
         * 返回动作更新所使用的线程类型, 固定返回后台线程 (BGT)
         *
         * @return 动作更新线程, 非空值
         * @since 1.0
         */
        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.BGT;
        }
    }

    /**
     * 生成配置动作组
     * <p>
     * 该类继承自 DefaultActionGroup, 用于在状态栏弹出菜单中提供生成配置的子菜单.
     * 包含类、方法、字段三个选项，支持多选（复选框）。
     *
     * @author zeka.stack.team
     * @version 1.0.0
     * @since 2.8.0
     */
    private static class GenerationConfigActionGroup extends DefaultActionGroup {
        GenerationConfigActionGroup() {
            super(JavadocBundle.message("statusbar.quick.settings.generate.config"), true);
            add(new GenerateForClassToggleAction());
            add(new GenerateForMethodToggleAction());
            add(new GenerateForFieldToggleAction());
        }
    }

    /**
     * 生成类级别 Javadoc 切换动作类
     * <p>
     * 该类继承自 ToggleAction, 用于控制是否为类生成 Javadoc 注释的开关动作.
     * 通过该动作可以切换生成类级别 Javadoc 注释的开关状态, 状态信息保存在 SettingsState 中.
     *
     * @author zeka.stack.team
     * @version 1.0.0
     * @email "mailto:zeka.stack@gmail.com"
     * @date 2025.11.30
     * @since 1.0.0
     */
    private static class GenerateForClassToggleAction extends com.intellij.openapi.actionSystem.ToggleAction {
        GenerateForClassToggleAction() {
            super(JavadocBundle.message("statusbar.quick.settings.generate.for.class"));
        }

        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.BGT;
        }

        @Override
        public boolean isSelected(@NotNull AnActionEvent e) {
            return SettingsState.getInstance().generateForClass;
        }

        @Override
        public void setSelected(@NotNull AnActionEvent e, boolean state) {
            SettingsState.getInstance().generateForClass = state;
        }
    }

    /**
     * 生成方法 Javadoc 切换动作类
     * <p>
     * 该类继承自 ToggleAction, 用于控制是否为方法生成 Javadoc 的功能开关,
     * 通过状态栏快速设置选项来切换生成方法 Javadoc 的开关状态
     *
     * @author zeka.stack.team
     * @version 1.0.0
     * @email mailto:zeka.stack@gmail.com
     * @date 2025.11.30
     * @since 1.0.0
     */
    private static class GenerateForMethodToggleAction extends com.intellij.openapi.actionSystem.ToggleAction {
        GenerateForMethodToggleAction() {
            super(JavadocBundle.message("statusbar.quick.settings.generate.for.method"));
        }

        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.BGT;
        }

        @Override
        public boolean isSelected(@NotNull AnActionEvent e) {
            return SettingsState.getInstance().generateForMethod;
        }

        @Override
        public void setSelected(@NotNull AnActionEvent e, boolean state) {
            SettingsState.getInstance().generateForMethod = state;
        }
    }

    /**
     * 字段生成开关动作类
     * <p>
     * 继承自 IntelliJ IDEA 的 ToggleAction, 用于控制字段生成功能的开关状态.
     * 该类提供了一个切换动作, 允许用户在 IDE 状态栏中快速启用或禁用字段生成功能.
     * 通过获取和设置 SettingsState 中的 generateForField 属性来管理字段生成的启用状态.
     *
     * @author zeka.stack.team
     * @version 1.0.0
     * @email "mailto:zeka.stack@gmail.com"
     * @date 2025.11.30
     * @since 1.0.0
     */
    private static class GenerateForFieldToggleAction extends com.intellij.openapi.actionSystem.ToggleAction {
        GenerateForFieldToggleAction() {
            super(JavadocBundle.message("statusbar.quick.settings.generate.for.field"));
        }

        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.BGT;
        }

        @Override
        public boolean isSelected(@NotNull AnActionEvent e) {
            return SettingsState.getInstance().generateForField;
        }

        @Override
        public void setSelected(@NotNull AnActionEvent e, boolean state) {
            SettingsState.getInstance().generateForField = state;
        }
    }

    /**
     * 创建覆写配置动作组
     * <p>
     * 动态创建动作组，包含启用覆写的开关和覆写模式选择。
     * 无论覆写是否启用，都显示模式选择选项。
     *
     * @return 覆写配置动作组
     * @since 2.8.0
     */
    @NotNull
    private static DefaultActionGroup createOverrideConfigActionGroup() {
        SettingsState settings = SettingsState.getInstance();
        String title = JavadocBundle.message("statusbar.quick.settings.override.config");

        // 如果覆写已启用，在标题中显示当前模式
        if (settings.overrideExisting) {
            String currentMode = settings.overrideMode == OverrideMode.FIX
                                 ? JavadocBundle.message("statusbar.quick.settings.override.mode.fix")
                                 : JavadocBundle.message("statusbar.quick.settings.override.mode.replace");
            title = title + " (" + currentMode + ")";
        }

        DefaultActionGroup group = new DefaultActionGroup(title, true);

        // 1. 添加启用覆写的开关
        group.add(new EnableOverrideToggleAction());

        // 2. 添加模式选择选项（无论覆写是否启用都显示）
        group.add(Separator.create());
        group.add(new OverrideModeFixAction());
        group.add(new OverrideModeReplaceAction());

        return group;
    }

    /**
     * 启用覆写切换动作类
     * <p>
     * 该类继承自 ToggleAction，用于控制是否启用覆写已有注释的功能。
     * 对应设置页面中的"覆写配置"复选框。
     *
     * @author zeka.stack.team
     * @version 1.0.0
     * @since 2.8.0
     */
    private static class EnableOverrideToggleAction extends com.intellij.openapi.actionSystem.ToggleAction {
        EnableOverrideToggleAction() {
            super(JavadocBundle.message("settings.override.existing"));
        }

        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.BGT;
        }

        @Override
        public boolean isSelected(@NotNull AnActionEvent e) {
            return SettingsState.getInstance().overrideExisting;
        }

        @Override
        public void setSelected(@NotNull AnActionEvent e, boolean state) {
            SettingsState settings = SettingsState.getInstance();
            settings.overrideExisting = state;
            // 如果禁用覆写，重置为默认模式（可选）
            // if (!state) {
            //     settings.overrideMode = OverrideMode.REPLACE;
            // }
        }
    }

    /**
     * 覆写模式：仅修复动作类
     * <p>
     * 该类继承自 AnAction, 用于选择"仅修复错误注释"的覆写模式.
     * 选择此模式时会自动启用覆写功能。
     *
     * @author zeka.stack.team
     * @version 1.0.0
     * @since 2.8.0
     */
    private static class OverrideModeFixAction extends AnAction {
        OverrideModeFixAction() {
            super(JavadocBundle.message("statusbar.quick.settings.override.mode.fix"));
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            SettingsState settings = SettingsState.getInstance();
            settings.overrideExisting = true; // 选择模式时自动启用覆写
            settings.overrideMode = OverrideMode.FIX;
        }

        @Override
        public void update(@NotNull AnActionEvent e) {
            SettingsState settings = SettingsState.getInstance();
            boolean isSelected = settings.overrideExisting && settings.overrideMode == OverrideMode.FIX;
            e.getPresentation().putClientProperty(SELECTED_KEY, isSelected);
        }

        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.BGT;
        }
    }

    /**
     * 覆写模式：删除并替换动作类
     * <p>
     * 该类继承自 AnAction, 用于选择"删除原注释并重新生成"的覆写模式.
     * 选择此模式时会自动启用覆写功能。
     *
     * @author zeka.stack.team
     * @version 1.0.0
     * @since 2.8.0
     */
    private static class OverrideModeReplaceAction extends AnAction {
        OverrideModeReplaceAction() {
            super(JavadocBundle.message("statusbar.quick.settings.override.mode.replace"));
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            SettingsState settings = SettingsState.getInstance();
            settings.overrideExisting = true; // 选择模式时自动启用覆写
            settings.overrideMode = OverrideMode.REPLACE;
        }

        @Override
        public void update(@NotNull AnActionEvent e) {
            SettingsState settings = SettingsState.getInstance();
            boolean isSelected = settings.overrideExisting && settings.overrideMode == OverrideMode.REPLACE;
            e.getPresentation().putClientProperty(SELECTED_KEY, isSelected);
        }

        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.BGT;
        }
    }

    /**
     * 启用生成上下文切换动作类
     * <p>
     * 该类继承自 ToggleAction, 用于控制是否为文档生成提供类级别上下文信息.
     *
     * @author zeka.stack.team
     * @version 1.0.0
     * @since 2.8.0
     */
    private static class EnableGenerationContextToggleAction extends com.intellij.openapi.actionSystem.ToggleAction {
        EnableGenerationContextToggleAction() {
            super(JavadocBundle.message("statusbar.quick.settings.enable.generation.context"));
        }

        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.BGT;
        }

        @Override
        public boolean isSelected(@NotNull AnActionEvent e) {
            return SettingsState.getInstance().enableGenerationContext;
        }

        @Override
        public void setSelected(@NotNull AnActionEvent e, boolean state) {
            SettingsState.getInstance().enableGenerationContext = state;
        }
    }

    /**
     * 允许代码压缩切换动作类
     * <p>
     * 该类继承自 ToggleAction, 用于控制是否启用代码压缩以减少 token 使用量.
     *
     * @author zeka.stack.team
     * @version 1.0.0
     * @since 2.8.0
     */
    private static class EnableCodeCompressionToggleAction extends com.intellij.openapi.actionSystem.ToggleAction {
        EnableCodeCompressionToggleAction() {
            super(JavadocBundle.message("statusbar.quick.settings.enable.code.compression"));
        }

        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.BGT;
        }

        @Override
        public boolean isSelected(@NotNull AnActionEvent e) {
            return SettingsState.getInstance().enableCodeCompression;
        }

        @Override
        public void setSelected(@NotNull AnActionEvent e, boolean state) {
            SettingsState.getInstance().enableCodeCompression = state;
        }
    }

    /**
     * 压缩成一行切换动作类
     * <p>
     * 该类继承自 ToggleAction, 用于控制是否将 Javadoc 压缩成一行.
     *
     * @author zeka.stack.team
     * @version 1.0.0
     * @since 2.8.0
     */
    private static class CompressSingleLineJavaDocToggleAction extends com.intellij.openapi.actionSystem.ToggleAction {
        CompressSingleLineJavaDocToggleAction() {
            super(JavadocBundle.message("statusbar.quick.settings.compress.single.line.javadoc"));
        }

        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.BGT;
        }

        @Override
        public boolean isSelected(@NotNull AnActionEvent e) {
            return SettingsState.getInstance().compressSingleLineJavaDoc;
        }

        @Override
        public void setSelected(@NotNull AnActionEvent e, boolean state) {
            SettingsState.getInstance().compressSingleLineJavaDoc = state;
        }
    }

    /**
     * 中英文标点符号替换切换动作类
     * <p>
     * 该类继承自 ToggleAction, 用于控制是否将中文标点符号转换为英文标点符号.
     *
     * @author zeka.stack.team
     * @version 1.0.0
     * @since 2.8.0
     */
    private static class ReplaceChinesePunctuationToggleAction extends com.intellij.openapi.actionSystem.ToggleAction {
        ReplaceChinesePunctuationToggleAction() {
            super(JavadocBundle.message("statusbar.quick.settings.replace.chinese.punctuation"));
        }

        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.BGT;
        }

        @Override
        public boolean isSelected(@NotNull AnActionEvent e) {
            return SettingsState.getInstance().replaceChinesePunctuation;
        }

        @Override
        public void setSelected(@NotNull AnActionEvent e, boolean state) {
            SettingsState.getInstance().replaceChinesePunctuation = state;
        }
    }

    /**
     * 性能模式切换动作类
     * <p>
     * 继承自 IntelliJ IDEA 的 ToggleAction, 用于控制性能模式的开启和关闭状态.
     * 该类提供了一个切换按钮, 允许用户在性能模式和普通模式之间进行切换,
     * 通过 SettingsState 管理性能模式的状态持久化.
     *
     * @author zeka.stack.team
     * @version 1.0.0
     * @email "mailto:zeka.stack@gmail.com"
     * @date 2025.11.30
     * @since 1.0.0
     */
    private static class PerformanceModeToggleAction extends com.intellij.openapi.actionSystem.ToggleAction {
        PerformanceModeToggleAction() {
            super(JavadocBundle.message("settings.performance.mode"));
        }

        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.BGT;
        }

        @Override
        public boolean isSelected(@NotNull AnActionEvent e) {
            return SettingsState.getInstance().performanceMode;
        }

        @Override
        public void setSelected(@NotNull AnActionEvent e, boolean state) {
            SettingsState.getInstance().performanceMode = state;
        }
    }

    /**
     * 显示生成 Javadoc 提示切换动作类
     * <p>
     * 该类继承自 ToggleAction, 用于控制是否显示生成 Javadoc 的 Code Vision 提示.
     * 通过该动作可以切换显示提示的开关状态, 状态信息保存在 SettingsState 中.
     *
     * @author zeka.stack.team
     * @version 1.0.0
     * @since 2.6.0
     */
    private static class ShowGenerateJavadocHintToggleAction extends com.intellij.openapi.actionSystem.ToggleAction {
        ShowGenerateJavadocHintToggleAction() {
            super(JavadocBundle.message("statusbar.quick.settings.show.generate.javadoc.hint"));
        }

        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.BGT;
        }

        @Override
        public boolean isSelected(@NotNull AnActionEvent e) {
            return SettingsState.getInstance().showGenerateJavadocHint;
        }

        @Override
        public void setSelected(@NotNull AnActionEvent e, boolean state) {
            SettingsState.getInstance().showGenerateJavadocHint = state;
        }
    }

    /**
     * 保存时生成注释切换动作类
     * <p>
     * 该类继承自 ToggleAction, 用于控制是否在保存文件时自动生成 Javadoc 注释.
     * 通过该动作可以切换保存时生成注释的开关状态, 状态信息保存在 SettingsState 中.
     *
     * @author zeka.stack.team
     * @version 1.0.0
     * @since 2.8.0
     */
    private static class GenerateOnSaveToggleAction extends com.intellij.openapi.actionSystem.ToggleAction {
        GenerateOnSaveToggleAction() {
            super(JavadocBundle.message("statusbar.quick.settings.generate.on.save"));
        }

        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.BGT;
        }

        @Override
        public boolean isSelected(@NotNull AnActionEvent e) {
            return SettingsState.getInstance().generateOnSave;
        }

        @Override
        public void setSelected(@NotNull AnActionEvent e, boolean state) {
            SettingsState.getInstance().generateOnSave = state;
        }
    }

    /**
     * 允许删除 Javadoc 切换动作类
     * <p>
     * 该类继承自 ToggleAction, 用于控制是否允许删除已存在的 Javadoc 注释.
     * 通过该动作可以切换允许删除 Javadoc 的开关状态, 状态信息保存在 SettingsState 中.
     *
     * @author zeka.stack.team
     * @version 1.0.0
     * @since 2.7.0
     */
    private static class AllowDeleteJavadocToggleAction extends com.intellij.openapi.actionSystem.ToggleAction {
        AllowDeleteJavadocToggleAction() {
            super(JavadocBundle.message("settings.allow.delete.javadoc"));
        }

        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.BGT;
        }

        @Override
        public boolean isSelected(@NotNull AnActionEvent e) {
            return SettingsState.getInstance().allowDeleteJavadoc;
        }

        @Override
        public void setSelected(@NotNull AnActionEvent e, boolean state) {
            SettingsState.getInstance().allowDeleteJavadoc = state;
        }
    }

    /**
     * AI Provider 选择动作组
     * <p>
     * 该类继承自 DefaultActionGroup, 用于在状态栏弹出菜单中提供 AI Provider 选择的子菜单.
     * 从可用服务商列表中选择，每个服务商配置包含模型信息，因此这就是模型选择.
     *
     * @author zeka.stack.team
     * @version 1.0.0
     * @since 2.6.0
     */
    private class ProviderSelectionActionGroup extends DefaultActionGroup {
        ProviderSelectionActionGroup(List<AIProviderConfig> providers) {
            super(JavadocBundle.message("statusbar.provider.list.title"), true);

            // 添加所有可用的服务商（每个服务商配置都包含模型信息）
            for (AIProviderConfig config : providers) {
                add(new SwitchProviderAction(config));
            }
        }
    }

    /**
     * 打开设置页面动作类
     * <p>
     * 该类继承自 AnAction, 用于在状态栏弹出菜单中提供快速打开插件设置页面的功能.
     * 点击该动作后会打开 IntelliAI Javadoc 的设置配置页面.
     *
     * @author zeka.stack.team
     * @version 1.0.0
     * @email "mailto:zeka.stack@gmail.com"
     * @date 2025.11.30
     * @since 1.0.0
     */
    private class OpenSettingsAction extends AnAction {
        /**
         * 构造函数, 创建打开设置动作实例
         * <p>
         * 使用国际化的文本标签初始化动作
         */
        OpenSettingsAction() {
            super(JavadocBundle.message("statusbar.quick.settings.open.settings"));
        }

        /**
         * 处理动作事件, 打开插件设置页面
         * <p>
         * 当用户点击该动作时, 打开 IntelliAI Javadoc 的设置配置页面
         *
         * @param e 动作事件对象, 包含事件的相关信息
         */
        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            ShowSettingsUtil.getInstance().showSettingsDialog(
                project, JavadocSettingsConfigurable.class
                                                             );
        }

        /**
         * 获取动作更新线程
         * <p>
         * 返回动作更新所使用的线程类型, 固定返回后台线程 (BGT)
         *
         * @return 动作更新线程, 非空值
         * @since 1.0
         */
        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.BGT;
        }
    }
}
