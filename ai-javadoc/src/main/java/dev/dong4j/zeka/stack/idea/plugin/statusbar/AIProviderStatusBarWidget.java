package dev.dong4j.zeka.stack.idea.plugin.statusbar;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.impl.status.EditorBasedStatusBarPopup;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import javax.swing.Icon;

import dev.dong4j.zeka.stack.idea.plugin.ai.AIProviderType;
import dev.dong4j.zeka.stack.idea.plugin.settings.SettingsState;
import dev.dong4j.zeka.stack.idea.plugin.util.JavaDocBundle;
import dev.dong4j.zeka.stack.idea.plugin.util.NotificationUtil;
import icons.AIJicons;
import lombok.extern.slf4j.Slf4j;

/**
 * 状态栏默认服务商切换控件
 * <p>
 * 在 IDE 编辑器状态栏展示当前默认 AI 服务商，并支持在列表弹窗中快速切换。
 * 控件显示插件主图标和服务商名称，点击后展示 {@link SettingsState#availableProviders}
 * 中已验证的服务商列表，选中即可更新默认服务商配置。
 *
 * <p>线程模型：
 * <ul>
 *   <li>UI 更新通过 {@link ApplicationManager#getApplication()} 调用 {@code invokeLater}</li>
 *   <li>配置写入使用 {@code runWriteAction} 确保线程安全</li>
 * </ul>
 *
 * @author dong4j
 * @version 1.0.0
 * @see EditorBasedStatusBarPopup
 * @since 1.0.0
 */
@Slf4j
public class AIProviderStatusBarWidget extends EditorBasedStatusBarPopup {

    /**
     * 控件唯一标识
     *
     * <p>用于在状态栏系统中唯一标记该控件，便于刷新和更新。</p>
     */
    public static final String WIDGET_ID = "dev.dong4j.zeka.stack.idea.plugin.statusbar.AIProviderStatusBarWidget";

    private final Project project;
    private StatusBar statusBar;

    /**
     * 构造状态栏控件
     *
     * @param project 当前项目
     * @since 1.0.0
     */
    public AIProviderStatusBarWidget(@NotNull Project project) {
        super(project, false);
        this.project = project;
    }

    /**
     * 创建新的控件实例。
     *
     * @param project 项目上下文
     * @return 新实例
     */
    @Override
    protected @NotNull StatusBarWidget createInstance(@NotNull Project project) {
        return new AIProviderStatusBarWidget(project);
    }

    @Override
    protected void registerCustomListeners() {
        // 当前不存在需要订阅的事件，使用默认行为即可
    }

    /**
     * 返回控件标识符。
     *
     * <p>该标识符用于状态栏更新与控件管理。</p>
     *
     * @return 控件唯一标识符
     * @since 1.0.0
     */
    @Override
    public @NotNull String ID() {
        return WIDGET_ID;
    }

    /**
     * 安装控件到状态栏。
     *
     * @param statusBar 状态栏实例
     */
    @Override
    public void install(@NotNull StatusBar statusBar) {
        super.install(statusBar);
        this.statusBar = statusBar;
    }

    /**
     * 释放控件资源。
     */
    @Override
    public void dispose() {
        super.dispose();
        statusBar = null;
    }

    @Override
    protected @NotNull WidgetState getWidgetState(@Nullable VirtualFile file) {
        SettingsState settings = SettingsState.getInstance();
        String displayText = AIProviderStatusBarWidgetModel.getCurrentProviderDisplayName(settings);
        String tooltip = JavaDocBundle.message("statusbar.provider.tooltip", displayText);
        WidgetState state = new WidgetState(tooltip, displayText, true);

        // 获取当前提供商的图标
        AIProviderType providerType = settings.providerType != null ? settings.providerType : AIProviderType.QIANWEN;
        Icon providerIcon = providerType.getIcon();
        // 如果提供商有图标则使用，否则使用插件主图标
        state.setIcon(providerIcon != null ? providerIcon : AIJicons.AIJ_16);
        
        return state;
    }

    @Override
    protected @Nullable ListPopup createPopup(@NotNull DataContext context) {
        SettingsState settings = SettingsState.getInstance();
        List<SettingsState.ProviderConfig> providers = AIProviderStatusBarWidgetModel.buildProviderItems(settings);
        if (providers.isEmpty()) {
            Notification notification = new Notification(NotificationUtil.NOTIFICATION_GROUP_ID,
                                                         JavaDocBundle.message("statusbar.provider.switch.failed.title"),
                                                         JavaDocBundle.message("statusbar.provider.no.available"),
                                                         NotificationType.ERROR);
            // 添加设置动作
            NotificationUtil.addOpenConfigurablePanelAction(notification, project);
            return null;
        }

        int defaultIndex = AIProviderStatusBarWidgetModel.findCurrentProviderIndex(providers, settings);

        BaseListPopupStep<SettingsState.ProviderConfig> step =
            new BaseListPopupStep<>(JavaDocBundle.message("statusbar.provider.popup.title"), providers) {
                @Override
                public boolean isMnemonicsNavigationEnabled() {
                    return true;
                }

                @Override
                public boolean isSpeedSearchEnabled() {
                    return true;
                }

                @NotNull
                @Override
                public Icon getIconFor(SettingsState.ProviderConfig value) {
                    // 根据提供商类型获取对应的图标
                    if (value != null && value.providerType != null) {
                        Icon providerIcon = value.providerType.getIcon();
                        // 如果提供商有图标则使用，否则使用插件主图标
                        return providerIcon != null ? providerIcon : AIJicons.AIJ_16;
                    }
                    return AIJicons.AIJ_16;
                }

                @Override
                public @NotNull String getTextFor(SettingsState.ProviderConfig value) {
                    return AIProviderStatusBarWidgetModel.getProviderDisplayText(value);
                }

                @Override
                public PopupStep<?> onChosen(SettingsState.ProviderConfig selectedValue, boolean finalChoice) {
                    if (finalChoice && selectedValue != null) {
                        handleSelection(selectedValue);
                    }
                    return FINAL_CHOICE;
                }

                @Override
                public int getDefaultOptionIndex() {
                    return defaultIndex;
                }
            };

        return JBPopupFactory.getInstance().createListPopup(step);
    }

    /**
     * 处理服务商切换。
     *
     * <p>该方法会在 EDT 中调度写操作，更新默认服务商配置，并刷新状态栏显示。
     *
     * @param selectedConfig 被选中的服务商配置
     */
    private void handleSelection(@NotNull SettingsState.ProviderConfig selectedConfig) {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (project.isDisposed()) {
                return;
            }

            try {
                ApplicationManager.getApplication().runWriteAction(() -> {
                    SettingsState settings = SettingsState.getInstance();
                    AIProviderStatusBarWidgetModel.switchDefaultProvider(settings, selectedConfig);
                });
            } catch (Exception exception) {
                log.error("切换默认服务商失败", exception);
                NotificationUtil.notifyError(project,
                                             JavaDocBundle.message("statusbar.provider.switch.failed.title"),
                                             JavaDocBundle.message("statusbar.provider.switch.failed", exception.getMessage()));
            } finally {
                StatusBar currentStatusBar = statusBar;
                if (currentStatusBar != null) {
                    currentStatusBar.updateWidget(ID());
                }
                update();
            }
        }, ModalityState.NON_MODAL);
    }
}
