package dev.dong4j.zeka.stack.idea.plugin.changelog.statusbar;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.StatusBarWidgetFactory;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import dev.dong4j.zeka.stack.idea.plugin.changelog.util.ChangelogBundle;

/**
 * Changelog 状态栏小部件工厂类
 * <p> 用于创建和管理 Changelog 状态栏小部件. 实现了 StatusBarWidgetFactory 接口, 提供了获取小部件 ID, 显示名称, 可用性检查, 创建和销毁小部件等功能.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.01
 * @since 1.0.0
 */
public class ChangelogStatusBarWidgetFactory implements StatusBarWidgetFactory {

    /**
     * 获取状态栏小部件的唯一标识符
     * <p> 返回与当前工厂关联的状态栏小部件的 ID
     *
     * @return 状态栏小部件的唯一标识符
     */
    @Override
    public @NonNls @NotNull String getId() {
        return ChangelogStatusBarWidget.WIDGET_ID;
    }

    /**
     * 获取状态栏显示名称
     * <p> 返回状态栏提供者的显示名称, 通过 ChangelogBundle 消息资源获取
     *
     * @return 状态栏提供者的显示名称
     */
    @Override
    public @NotNull String getDisplayName() {
        return ChangelogBundle.message("statusbar.provider.factory.name");
    }

    /**
     * 判断状态栏小部件是否可用
     * <p> 此方法用于确定状态栏小部件在给定项目中是否可用. 当前实现总是返回 true, 表示该小部件在任何项目中都可用.
     *
     * @param project 项目对象
     * @return 布尔值, 总是返回 true
     */
    @Override
    public boolean isAvailable(@NotNull Project project) {
        return true;
    }

    /**
     * 检查状态栏是否可以启用该组件
     * <p> 此方法用于确定当前的状态栏是否支持启用该组件. 在本实现中, 始终返回 true, 表示该组件可以在任何状态下启用.
     *
     * @param statusBar 状态栏对象
     * @return 布尔值, 始终返回 true
     */
    @Override
    public boolean canBeEnabledOn(@NotNull StatusBar statusBar) {
        return true;
    }

    /**
     * 创建状态栏小部件
     * <p> 根据给定的项目创建一个新的 ChangelogStatusBarWidget 实例
     *
     * @param project 项目实例
     * @return 返回创建的状态栏小部件
     */
    @Override
    public @NotNull StatusBarWidget createWidget(@NotNull Project project) {
        return new ChangelogStatusBarWidget(project);
    }

    /**
     * 释放状态栏小部件的资源
     * <p> 调用小部件的 dispose 方法来释放资源
     *
     * @param widget 状态栏小部件
     */
    @Override
    public void disposeWidget(@NotNull StatusBarWidget widget) {
        widget.dispose();
    }
}
