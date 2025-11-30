package dev.dong4j.zeka.stack.idea.plugin.statusbar;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Separator;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
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

import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIProviderType;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderSettings;
import dev.dong4j.zeka.stack.idea.plugin.common.icons.AICommonIcons;
import dev.dong4j.zeka.stack.idea.plugin.settings.SettingsState;
import dev.dong4j.zeka.stack.idea.plugin.util.JavaDocBundle;
import dev.dong4j.zeka.stack.idea.plugin.util.NotificationUtil;
import icons.AIJicons;
import lombok.extern.slf4j.Slf4j;

/**
 * AI JavaDoc 状态栏组件
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
     * 控件唯一标识
     *
     * <p>用于在状态栏系统中唯一标记该控件，便于刷新和更新。</p>
     */
    public static final String WIDGET_ID = "dev.dong4j.zeka.stack.idea.plugin.statusbar.AIJavadocStatusBarWidget";

    /**
     * 提供商选中状态 Key
     *
     * <p>用于在 Presentation 中标记当前选中的提供商。</p>
     */
    private static final Key<Boolean> SELECTED_KEY = Key.create("selected");

    /** 项目对象 */
    private final Project project;
    /** 状态栏组件 */
    private StatusBar statusBar;

    /**
     * 构造状态栏控件
     *
     * @param project 当前项目
     * @since 1.0.0
     */
    public AIJavadocStatusBarWidget(@NotNull Project project) {
        super(project, false);
        this.project = project;
    }

    /**
     * 创建新的控件实例
     *
     * @param project 项目上下文
     * @return 新实例
     */
    @Override
    protected @NotNull StatusBarWidget createInstance(@NotNull Project project) {
        return new AIJavadocStatusBarWidget(project);
    }

    /**
     * 注册自定义监听器
     */
    @Override
    protected void registerCustomListeners() {
        // 当前不存在需要订阅的事件，使用默认行为即可
    }

    /**
     * 返回控件标识符
     *
     * @return 控件唯一标识符
     */
    @Override
    public @NotNull String ID() {
        return WIDGET_ID;
    }

    /**
     * 安装控件到状态栏
     *
     * @param statusBar 状态栏实例
     */
    @Override
    public void install(@NotNull StatusBar statusBar) {
        super.install(statusBar);
        this.statusBar = statusBar;
    }

    /**
     * 释放控件资源
     */
    @Override
    public void dispose() {
        super.dispose();
        statusBar = null;
    }

    /**
     * 获取状态栏小部件的当前状态
     *
     * @param file 当前文件 (可为空)
     * @return 包含当前 AI 提供者信息的 WidgetState
     */
    @Override
    protected @NotNull WidgetState getWidgetState(@Nullable VirtualFile file) {
        String displayText = getCurrentProviderModelName();
        String tooltip = JavaDocBundle.message("statusbar.provider.tooltip", displayText);
        WidgetState state = new WidgetState(tooltip, " " + displayText, true);

        // 获取当前提供商的图标（已缩放为 13x13）
        AIProviderType providerType = getCurrentProviderType();
        Icon providerIcon = AICommonIcons.getProviderIcon(providerType);
        // 如果提供商有图标则使用（已缩放），否则缩放主图标
        Icon iconToUse = providerIcon != null ? providerIcon : scaleIconForStatusBar(AIJicons.AIJ_16);
        state.setIcon(iconToUse);

        return state;
    }

    /**
     * 创建状态栏弹出菜单
     *
     * @param context 数据上下文
     * @return 弹出菜单, 如果创建失败则返回 null
     */
    @Override
    protected @Nullable ListPopup createPopup(@NotNull DataContext context) {
        // 获取可用的提供商列表
        List<AIProviderConfig> providers = getAvailableProviders();
        if (providers.isEmpty()) {
            showErrorNotification(
                JavaDocBundle.message("statusbar.provider.switch.failed.title"),
                JavaDocBundle.message("statusbar.provider.no.available")
                                 );
            return null;
        }

        // 创建 Action 组
        DefaultActionGroup group = new DefaultActionGroup();

        // 1. 添加提供商切换选项
        for (AIProviderConfig config : providers) {
            group.add(new SwitchProviderAction(config));
        }

        // 2. 添加分隔符
        group.add(Separator.create());

        // 3. 添加快捷配置 ToggleAction
        group.add(new GenerateForClassToggleAction());
        group.add(new GenerateForMethodToggleAction());
        group.add(new GenerateForFieldToggleAction());

        // 创建弹出菜单
        return JBPopupFactory.getInstance().createActionGroupPopup(
            JavaDocBundle.message("statusbar.provider.popup.title"),
            group,
            context,
            JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
            true
                                                                  );
    }

    /**
     * 获取当前默认提供商类型
     *
     * @return 当前默认提供商类型
     */
    @NotNull
    private AIProviderType getCurrentProviderType() {
        SettingsState settings = SettingsState.getInstance();
        return settings.providerConfig != null ? settings.providerConfig.providerType : AIProviderType.QIANWEN;
    }

    /**
     * 获取当前默认提供商配置
     *
     * @param providerType 提供商类型
     * @return 提供商配置
     */
    @NotNull
    private AIProviderConfig getDefaultProviderConfig(@NotNull AIProviderType providerType) {
        AIProviderSettings globalSettings = AIProviderSettings.getInstance();
        return globalSettings.getDefaultProviderConfig(providerType);
    }

    /**
     * 获取可用的提供商配置列表
     *
     * @return 可用提供商配置列表
     */
    @NotNull
    private List<AIProviderConfig> getAvailableProviders() {
        AIProviderSettings globalSettings = AIProviderSettings.getInstance();
        return new ArrayList<>(globalSettings.getVerifiedProviders());
    }

    /**
     * 获取当前提供商模型名称
     *
     * @return 模型名称
     */
    @NotNull
    private String getCurrentProviderModelName() {
        AIProviderType providerType = getCurrentProviderType();
        AIProviderConfig defaultConfig = getDefaultProviderConfig(providerType);
        return defaultConfig.modelName;
    }

    /**
     * 切换默认提供商
     *
     * @param providerType 提供商类型
     * @param config       提供商配置
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
     * 显示错误通知
     *
     * @param title   通知标题
     * @param content 通知内容
     */
    private void showErrorNotification(@NotNull String title, @NotNull String content) {
        Notification notification = new Notification(
            NotificationUtil.NOTIFICATION_GROUP_ID,
            title,
            content,
            NotificationType.ERROR
        );
        NotificationUtil.addOpenConfigurablePanelAction(notification, project);
        notification.notify(project);
    }

    /**
     * 缩放图标以适应状态栏显示
     *
     * @param icon 原始图标
     * @return 缩放后的图标
     */
    @Nullable
    private Icon scaleIconForStatusBar(@Nullable Icon icon) {
        if (icon == null) {
            return null;
        }
        // 状态栏图标通常使用 13x13 尺寸，将 16x16 的图标缩放到 13x13
        return IconUtil.scale(icon, null, 0.8125f);
    }

    /**
     * 切换提供商 Action
     */
    private class SwitchProviderAction extends AnAction {
        private final AIProviderConfig config;

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

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            if (config.providerType == null) {
                showErrorNotification(
                    JavaDocBundle.message("statusbar.provider.switch.failed.title"),
                    JavaDocBundle.message("statusbar.provider.error.missing.type")
                                     );
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
                    showErrorNotification(
                        JavaDocBundle.message("statusbar.provider.switch.failed.title"),
                        JavaDocBundle.message("statusbar.provider.switch.failed", exception.getMessage())
                                         );
                } finally {
                    StatusBar currentStatusBar = statusBar;
                    if (currentStatusBar != null) {
                        currentStatusBar.updateWidget(ID());
                    }
                    update(e);
                }
            }, ModalityState.NON_MODAL);
        }

        @Override
        public void update(@NotNull AnActionEvent e) {
            // 如果是当前选中的提供商,显示选中标记
            AIProviderType currentType = getCurrentProviderType();
            boolean isSelected = config != null && config.providerType == currentType;
            e.getPresentation().putClientProperty(SELECTED_KEY, isSelected);
        }

        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.BGT;
        }
    }

    /**
     * "为类生成文档" 切换 Action
     */
    private static class GenerateForClassToggleAction extends com.intellij.openapi.actionSystem.ToggleAction {
        GenerateForClassToggleAction() {
            super(JavaDocBundle.message("statusbar.quick.settings.generate.for.class"));
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
     * "为方法生成文档" 切换 Action
     */
    private static class GenerateForMethodToggleAction extends com.intellij.openapi.actionSystem.ToggleAction {
        GenerateForMethodToggleAction() {
            super(JavaDocBundle.message("statusbar.quick.settings.generate.for.method"));
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
     * "为字段生成文档" 切换 Action
     */
    private static class GenerateForFieldToggleAction extends com.intellij.openapi.actionSystem.ToggleAction {
        GenerateForFieldToggleAction() {
            super(JavaDocBundle.message("statusbar.quick.settings.generate.for.field"));
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
}
