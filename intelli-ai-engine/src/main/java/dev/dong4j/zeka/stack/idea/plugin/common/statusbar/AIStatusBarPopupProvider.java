package dev.dong4j.zeka.stack.idea.plugin.common.statusbar;

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

/**
 * AI 状态栏弹出窗口提供者接口
 * <p> 该接口定义了用于提供 AI 状态栏弹出窗口的相关方法. 通过实现此接口, 可以自定义状态栏中的弹出窗口行为.
 * <p> 主要方法包括获取组名, 创建动作组以及检查可用性.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.02
 * @since 1.0.0
 */
public interface AIStatusBarPopupProvider {
    /**
     * 扩展点名称, 用于标识状态栏聚合入口的扩展点.
     * <p> 此字段用于定义和注册插件扩展点, 允许其他插件通过实现 {@link AIStatusBarPopupProvider} 接口来扩展状态栏功能.</p>
     *
     * @see ExtensionPointName
     */
    ExtensionPointName<AIStatusBarPopupProvider> EP_NAME =
        ExtensionPointName.create("dev.dong4j.zeka.stack.idea.plugin.common.ai.statusBarPopupProvider");

    /**
     * 获取该分组的显示名称.
     * <p> 返回状态栏中该分组的标题.
     *
     * @return 分组标题
     */
    @NotNull
    String getGroupName();

    /**
     * 构建该分组的动作组.
     * <p> 根据当前项目和状态栏数据上下文, 创建并返回一个动作组.</p>
     *
     * @param project 当前项目
     * @param context 状态栏数据上下文
     * @return 动作组, 不能为 null
     */
    @NotNull
    ActionGroup createActionGroup(@NotNull Project project, @NotNull DataContext context);

    /**
     * 判断当前扩展点是否可用
     * <p> 此方法用于确定该扩展点是否应在状态栏中显示, 默认返回 true 表示可用.
     *
     * @param project 当前项目
     * @return true 表示在状态栏中显示,false 表示不可见
     */
    default boolean isAvailable(@NotNull Project project) {
        return true;
    }
}
