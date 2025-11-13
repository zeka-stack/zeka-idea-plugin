package dev.dong4j.zeka.stack.idea.plugin.statusbar;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIProviderType;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.statusbar.AIProviderStatusBarAdapter;
import dev.dong4j.zeka.stack.idea.plugin.settings.JavaDocSettingsConfigurable;
import dev.dong4j.zeka.stack.idea.plugin.settings.SettingsState;
import dev.dong4j.zeka.stack.idea.plugin.util.JavaDocBundle;
import dev.dong4j.zeka.stack.idea.plugin.util.NotificationUtil;
import icons.AIJicons;

/**
 * AI 提供商状态栏适配器实现
 * <p>
 * 将 ai-javadoc 的配置和资源桥接到通用的状态栏组件。
 */
public class AIProviderStatusBarAdapterImpl implements AIProviderStatusBarAdapter {

    private static final AIProviderStatusBarAdapterImpl INSTANCE = new AIProviderStatusBarAdapterImpl();

    private AIProviderStatusBarAdapterImpl() {
    }

    public static AIProviderStatusBarAdapterImpl getInstance() {
        return INSTANCE;
    }

    @Override
    @NotNull
    public AIProviderType getCurrentProviderType() {
        SettingsState settings = SettingsState.getInstance();
        return settings.providerSettings.providerType != null ? settings.providerSettings.providerType : AIProviderType.QIANWEN;
    }

    @Override
    @NotNull
    public AIProviderConfig getDefaultProviderConfig(@NotNull AIProviderType providerType) {
        SettingsState settings = SettingsState.getInstance();
        return settings.providerSettings.getDefaultProviderConfig(providerType);
    }

    @Override
    @NotNull
    public List<AIProviderConfig> getAvailableProviders() {
        SettingsState settings = SettingsState.getInstance();
        return new ArrayList<>(settings.providerSettings.getVerifiedProviders());
    }

    @Override
    public void switchDefaultProvider(@NotNull AIProviderType providerType, @NotNull AIProviderConfig config) {
        SettingsState settings = SettingsState.getInstance();
        settings.providerSettings.providerType = providerType;
        settings.providerSettings.updateDefaultProviderConfig(providerType, config);
    }

    @Override
    @Nullable
    public Icon getProviderIcon(@Nullable AIProviderType providerType) {
        return AIJicons.getProviderIcon(providerType);
    }

    @Override
    @NotNull
    public Icon getMainIcon() {
        return AIJicons.AIJ_16;
    }

    @Override
    @NotNull
    public String getMessage(@NotNull String key, Object... params) {
        return JavaDocBundle.message(key, params);
    }

    @Override
    @NotNull
    public String getNotificationGroupId() {
        return "AI Javadoc Notifications";
    }

    @Override
    public void openSettingsPanel(@NotNull Project project) {
        ShowSettingsUtil.getInstance().showSettingsDialog(project, JavaDocSettingsConfigurable.class);
    }

    @Override
    public void showErrorNotification(@NotNull Project project, @NotNull String title, @NotNull String content) {
        Notification notification = new Notification(
            NotificationUtil.NOTIFICATION_GROUP_ID,
            title,
            content,
            NotificationType.ERROR
        );
        NotificationUtil.addOpenConfigurablePanelAction(notification, project);
        notification.notify(project);
    }
}

