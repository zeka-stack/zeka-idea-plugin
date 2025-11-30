package dev.dong4j.zeka.stack.idea.plugin.statusbar;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.StatusBarWidgetFactory;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import dev.dong4j.zeka.stack.idea.plugin.util.JavaDocBundle;

/**
 * AI JavaDoc 状态栏组件工厂类
 * <p>
 * 该类实现了 StatusBarWidgetFactory 接口, 用于创建和管理 AI JavaDoc 相关的状态栏组件.
 * 负责创建 AIJavadocStatusBarWidget 实例, 并提供组件的标识符, 显示名称等信息.
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email mailto:zeka.stack@gmail.com
 * @date 2025.11.30
 * @since 1.0.0
 */
public class AIJavadocStatusBarWidgetFactory implements StatusBarWidgetFactory {

    /**
     * 返回状态栏控件工厂标识
     *
     * @return 控件工厂唯一标识
     */
    @Override
    public @NonNls @NotNull String getId() {
        return AIJavadocStatusBarWidget.WIDGET_ID;
    }

    /**
     * 返回控件在设置界面中的显示名称
     *
     * @return 显示名称
     */
    @Override
    public @NotNull String getDisplayName() {
        return JavaDocBundle.message("statusbar.provider.factory.name");
    }

    /**
     * 判断当前项目是否可用
     *
     * @param project 当前项目
     * @return 永远返回 true
     */
    @Override
    public boolean isAvailable(@NotNull Project project) {
        return true;
    }

    /**
     * 判断控件是否可以在指定状态栏启用
     *
     * @param statusBar 状态栏对象
     * @return 永远返回 true
     */
    @Override
    public boolean canBeEnabledOn(@NotNull StatusBar statusBar) {
        return true;
    }

    /**
     * 创建状态栏控件实例
     *
     * @param project 当前项目
     * @return 控件实例
     */
    @Override
    public @NotNull StatusBarWidget createWidget(@NotNull Project project) {
        return new AIJavadocStatusBarWidget(project);
    }

    /**
     * 释放控件资源
     *
     * @param widget 控件实例
     */
    @Override
    public void disposeWidget(@NotNull StatusBarWidget widget) {
        widget.dispose();
    }
}
