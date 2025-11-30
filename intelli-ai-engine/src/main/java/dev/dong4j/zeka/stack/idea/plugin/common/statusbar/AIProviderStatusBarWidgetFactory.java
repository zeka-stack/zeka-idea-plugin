package dev.dong4j.zeka.stack.idea.plugin.common.statusbar;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.StatusBarWidgetFactory;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

/**
 * AI 提供商状态栏组件工厂类
 * <p>
 * 该类实现了 StatusBarWidgetFactory 接口, 用于创建和管理 AI 提供商状态栏组件.
 * 负责创建 AIProviderStatusBarWidget 实例, 并提供组件的标识符, 显示名称等信息.
 * 该工厂类通过适配器模式将 AI 提供商功能集成到 IDE 的状态栏中.
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email "mailto:zeka.stack@gmail.com"
 * @date 2025.11.30
 * @since 1.0.0
 */
public class AIProviderStatusBarWidgetFactory implements StatusBarWidgetFactory {

    /** AI 提供商状态栏适配器, 用于管理 AI 提供商在状态栏中的显示和交互逻辑 */
    private final AIProviderStatusBarAdapter adapter;
    /** 界面组件唯一标识符 */
    private final String widgetId;
    /** 显示名称 */
    private final String displayName;

    /**
     * 构造工厂
     *
     * @param adapter     状态栏适配器
     * @param widgetId    控件 ID
     * @param displayName 显示名称
     */
    public AIProviderStatusBarWidgetFactory(@NotNull AIProviderStatusBarAdapter adapter,
                                            @NotNull String widgetId,
                                            @NotNull String displayName) {
        this.adapter = adapter;
        this.widgetId = widgetId;
        this.displayName = displayName;
    }

    /**
     * 返回状态栏控件工厂标识。
     *
     * @return 控件工厂唯一标识
     */
    @Override
    public @NonNls @NotNull String getId() {
        return widgetId;
    }

    /**
     * 返回控件在设置界面中的显示名称。
     *
     * @return 显示名称
     */
    @Override
    public @NotNull String getDisplayName() {
        return displayName;
    }

    /**
     * 判断当前项目是否可用。
     *
     * <p>该控件面向所有项目开放，不受额外约束。</p>
     *
     * @param project 当前项目
     * @return 永远返回 {@code true}
     */
    @Override
    public boolean isAvailable(@NotNull Project project) {
        return true;
    }

    /**
     * 判断控件是否可以在指定状态栏启用。
     *
     * @param statusBar 状态栏对象
     * @return 永远返回 {@code true}
     */
    @Override
    public boolean canBeEnabledOn(@NotNull StatusBar statusBar) {
        return true;
    }

    /**
     * 创建状态栏控件实例。
     *
     * @param project 当前项目
     * @return 控件实例
     */
    @Override
    public @NotNull StatusBarWidget createWidget(@NotNull Project project) {
        return new AIProviderStatusBarWidget(project, adapter);
    }

    /**
     * 释放控件资源。
     *
     * @param widget 控件实例
     */
    @Override
    public void disposeWidget(@NotNull StatusBarWidget widget) {
        widget.dispose();
    }
}

