package dev.dong4j.zeka.stack.idea.plugin.common.statusbar;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.StatusBarWidgetFactory;

import org.jetbrains.annotations.NotNull;

import dev.dong4j.zeka.stack.idea.plugin.common.util.AICommonBundle;

/**
 * AI 状态栏小部件工厂类
 * <p> 实现了 `StatusBarWidgetFactory` 接口, 用于创建和管理 AI 相关的状态栏小部件.
 * <p> 该工厂类的主要职责是提供状态栏小部件的标识, 显示名称, 可用性检查以及小部件的创建和销毁.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.02
 * @since 1.0.0
 */
public class AIStatusBarWidgetFactory implements StatusBarWidgetFactory {
    /**
     * 获取状态栏小部件的唯一标识符
     * <p> 返回与当前工厂关联的状态栏小部件的 ID
     *
     * @return 状态栏小部件的唯一标识符
     */
    @Override
    public @NotNull String getId() {
        return AIStatusBarWidget.WIDGET_ID;
    }

    /**
     * 获取状态栏小部件的显示名称
     * <p> 返回状态栏小部件的显示名称, 该名称由资源文件提供
     *
     * @return 状态栏小部件的显示名称
     */
    @Override
    public @NotNull String getDisplayName() {
        return AICommonBundle.message("statusbar.engine.factory.name");
    }

    /**
     * 检查状态栏小部件是否可用
     * <p> 此方法总是返回 true, 表示该状态栏小部件在任何项目中都可用
     *
     * @param project 当前项目
     * @return 常量 true, 表示小部件始终可用
     */
    @Override
    public boolean isAvailable(@NotNull Project project) {
        return true;
    }

    /**
     * 创建 AI 状态栏小部件
     * <p> 根据给定的项目创建一个新的 AI 状态栏小部件实例
     *
     * @param project 项目对象, 不能为 null
     * @return AI 状态栏小部件实例
     */
    @Override
    public @NotNull StatusBarWidget createWidget(@NotNull Project project) {
        return new AIStatusBarWidget(project);
    }

    /**
     * 释放状态栏小部件资源
     * <p> 此方法用于释放指定的状态栏小部件资源. 在实现中, 可能会执行清理操作, 例如取消订阅事件或释放内存.
     *
     * @param widget 要释放的状态栏小部件, 不能为 null
     */
    @Override
    public void disposeWidget(@NotNull StatusBarWidget widget) {
    }
}
