package dev.dong4j.zeka.stack.idea.plugin.common.statusbar;

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
import com.intellij.util.IconUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import javax.swing.Icon;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIProviderType;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.icons.AICommonIcons;
import lombok.extern.slf4j.Slf4j;

/**
 * AI 服务提供商状态栏小部件
 * <p>
 * 该类用于在 IDE 的状态栏中展示当前 AI 服务提供商的信息, 并提供切换服务商的下拉菜单功能. 支持动态更新状态栏显示内容和图标, 并处理用户选择服务商后的相关操作.
 * <p>
 * 该类继承自 EditorBasedStatusBarPopup, 用于创建和管理状态栏弹出窗口, 支持弹出菜单的创建, 显示和交互.
 *
 * @author 作者
 * @version 1.0.0
 * @date 2025.10.24
 * @since 1.0.0
 */
@Slf4j
public class AIProviderStatusBarWidget extends EditorBasedStatusBarPopup {

    /** 项目对象, 用于表示当前操作所关联的项目信息 */
    private final Project project;
    /**
     * 状态栏适配器
     * <p>
     * 用于管理 AI 提供商状态栏的显示和更新
     *
     * @see AIProviderStatusBarAdapter
     */
    private final AIProviderStatusBarAdapter adapter;
    /** 状态栏组件, 用于显示应用运行状态和相关信息 */
    private StatusBar statusBar;

    /**
     * 构造状态栏控件
     *
     * @param project 当前项目
     * @param adapter 状态栏适配器
     * @since 1.0.0
     */
    public AIProviderStatusBarWidget(@NotNull Project project, @NotNull AIProviderStatusBarAdapter adapter) {
        super(project, false);
        this.project = project;
        this.adapter = adapter;
    }

    /**
     * 创建新的控件实例。
     *
     * @param project 项目上下文
     * @return 新实例
     */
    @Override
    protected @NotNull StatusBarWidget createInstance(@NotNull Project project) {
        return new AIProviderStatusBarWidget(project, adapter);
    }

    /**
     * 注册自定义监听器
     * <p>
     * 用于注册自定义的监听器, 以实现特定的业务逻辑或事件处理.
     *
     * @since 1.0
     */
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
        return adapter.getClass().getName() + ".AIProviderStatusBarWidget";
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

    /**
     * 获取状态栏小部件的当前状态.
     * <p>
     * 根据当前 AI 提供者信息构造 {@link WidgetState}, 包括显示文本, 工具提示和图标.
     * 该方法不使用传入的 {@code file} 参数, 仅根据适配器内部状态生成状态.
     *
     * @param file 当前文件 (可为空), 此方法不使用该参数
     * @return 包含当前 AI 提供者信息的 {@link WidgetState}, 永不为 {@code null}
     */
    @Override
    protected @NotNull WidgetState getWidgetState(@Nullable VirtualFile file) {
        String displayText = AIProviderStatusBarWidgetModel.getCurrentProviderDisplayName(adapter);
        String tooltip = adapter.getMessage("statusbar.provider.tooltip", displayText);
        WidgetState state = new WidgetState(tooltip, " " + displayText, true);

        // 获取当前提供商的图标（已缩放为 13x13）
        AIProviderType providerType = adapter.getCurrentProviderType();
        Icon providerIcon = AICommonIcons.getProviderIcon(providerType);
        // 如果提供商有图标则使用（已缩放），否则缩放主图标
        Icon iconToUse = providerIcon != null ? providerIcon : scaleIconForStatusBar(adapter.getMainIcon());
        state.setIcon(iconToUse);

        return state;
    }

    /**
     * 创建用于选择 AI 提供商的弹出窗口
     * <p>
     * 根据当前数据上下文构建 AI 提供商列表, 并创建一个可交互的弹出窗口供用户选择.
     * 如果没有可用的提供商, 则显示错误通知并返回 null.
     *
     * @param context 数据上下文, 用于获取相关数据
     * @return 创建的弹出窗口, 若没有可用提供商则返回 null
     * @since 1.0
     */
    @Override
    protected @Nullable ListPopup createPopup(@NotNull DataContext context) {
        List<AIProviderConfig> providers = AIProviderStatusBarWidgetModel.buildProviderItems(adapter);
        if (providers.isEmpty()) {
            adapter.showErrorNotification(
                project,
                adapter.getMessage("statusbar.provider.switch.failed.title"),
                adapter.getMessage("statusbar.provider.no.available")
                                         );
            return null;
        }

        int defaultIndex = AIProviderStatusBarWidgetModel.findCurrentProviderIndex(providers, adapter);

        BaseListPopupStep<AIProviderConfig> step =
            new BaseListPopupStep<>(adapter.getMessage("statusbar.provider.popup.title"), providers) {
                /**
                 * 判断是否启用了助记符导航功能
                 * <p>
                 * 该方法返回一个布尔值, 表示当前是否启用了通过助记符进行导航的功能.
                 *
                 * @return 如果启用了助记符导航则返回 true, 否则返回 false
                 */
                @Override
                public boolean isMnemonicsNavigationEnabled() {
                    return true;
                }

                /**
                 * 判断是否启用速度搜索功能
                 * <p>
                 * 返回一个布尔值, 表示速度搜索功能是否已启用
                 *
                 * @return 是否启用速度搜索功能
                 */
                @Override
                public boolean isSpeedSearchEnabled() {
                    return true;
                }

                /**
                 * 根据 AIProviderConfig 获取对应的图标
                 * <p>
                 * 根据传入的 AIProviderConfig 对象的 providerType 获取对应的图标, 若图标不存在则返回适配器的默认图标
                 *
                 * @param value AIProviderConfig 对象, 用于获取 providerType
                 * @return 对应的图标, 若图标不存在则返回适配器的默认图标
                 */
                @NotNull
                @Override
                public Icon getIconFor(AIProviderConfig value) {
                    // 根据提供商类型获取对应的图标（已缩放为 13x13）
                    if (value != null && value.providerType != null) {
                        Icon providerIcon = AICommonIcons.getProviderIcon(value.providerType);
                        // 如果提供商有图标则使用（已缩放），否则缩放主图标
                        return providerIcon != null ? providerIcon : scaleIconForStatusBar(adapter.getMainIcon());
                    }
                    // 使用主图标并缩放
                    return scaleIconForStatusBar(adapter.getMainIcon());
                }

                /**
                 * 根据 AI 提供者配置获取对应的文本显示内容
                 * <p>
                 * 该方法用于根据传入的 AI 提供者配置对象, 获取其在状态栏中显示的文本描述.
                 *
                 * @param value AI 提供者配置对象
                 * @return AI 提供者对应的文本显示内容
                 * @since 1.0
                 */
                @Override
                public @NotNull String getTextFor(AIProviderConfig value) {
                    // return AIProviderStatusBarWidgetModel.getProviderDisplayText(value);
                    return AIProviderStatusBarWidgetModel.getProviderModelName(value);
                }

                /**
                 * 处理用户选择的 AI 提供者配置
                 * <p>
                 * 当用户最终选择了一个 AI 提供者配置时, 执行相应的处理逻辑.
                 *
                 * @param selectedValue 用户选择的 AI 提供者配置
                 * @param finalChoice   是否为最终选择
                 * @return 返回 {@code FINAL_CHOICE} 表示处理完成
                 */
                @Override
                public PopupStep<?> onChosen(AIProviderConfig selectedValue, boolean finalChoice) {
                    if (finalChoice && selectedValue != null) {
                        handleSelection(selectedValue);
                    }
                    return FINAL_CHOICE;
                }

                /**
                 * 获取默认选项的索引值
                 * <p>
                 * 返回配置中默认选项对应的索引值
                 *
                 * @return 默认选项的索引值
                 */
                @Override
                public int getDefaultOptionIndex() {
                    return defaultIndex;
                }
            };

        return JBPopupFactory.getInstance().createListPopup(step);
    }

    /**
     * 缩放图标以适应状态栏显示
     * <p>
     * 状态栏图标通常使用 13x13 的尺寸，该方法将图标缩放到适合状态栏显示的尺寸。
     * 主要用于缩放主图标，因为提供商图标已在 {@link AICommonIcons#getProviderIcon} 中缩放。
     * 如果图标为 null，则直接返回 null。
     *
     * @param icon 原始图标
     * @return 缩放后的图标，如果输入为 null 则返回 null
     */
    @Nullable
    private Icon scaleIconForStatusBar(@Nullable Icon icon) {
        if (icon == null) {
            return null;
        }
        // 状态栏图标通常使用 13x13 尺寸，将 16x16 的图标缩放到 13x13
        // 缩放比例：13/16 = 0.8125
        return IconUtil.scale(icon, null, 0.8125f);
    }

    /**
     * 处理服务商切换。
     *
     * <p>该方法会在 EDT 中调度写操作，更新默认服务商配置，并刷新状态栏显示。
     *
     * @param selectedConfig 被选中的服务商配置
     */
    private void handleSelection(@NotNull AIProviderConfig selectedConfig) {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (project.isDisposed()) {
                return;
            }

            try {
                ApplicationManager.getApplication().runWriteAction(() -> {
                    AIProviderStatusBarWidgetModel.switchDefaultProvider(adapter, selectedConfig);
                });
            } catch (Exception exception) {
                log.error("切换默认服务商失败", exception);
                adapter.showErrorNotification(
                    project,
                    adapter.getMessage("statusbar.provider.switch.failed.title"),
                    adapter.getMessage("statusbar.provider.switch.failed", exception.getMessage())
                                             );
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

