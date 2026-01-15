package icons;

import com.intellij.openapi.util.IconLoader;

import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

/**
 * 变更日志图标类
 * <p>
 * 提供变更日志相关的图标资源, 包含各种日志视图和操作的图标, 如变更日志, 日志, 周报, 日报, 差异对比等图标
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email "mailto:zeka.stack@gmail.com"
 * @date 2025.11.30
 * @since 1.0.0
 */
public class ChangelogIcons {
    /**
     * 加载图标
     * <p>
     * 用于加载位于 {@code /icons/} 目录下的图标文件。
     * 路径必须以 {@code /icons/} 开头。
     *
     * @param iconPath 图标文件路径，相对于 resources 根目录（例如："/icons/icon.svg"）
     * @return 加载的图标
     */
    @NotNull
    private static Icon load(@NotNull String iconPath) {
        return IconLoader.getIcon(iconPath, ChangelogIcons.class);
    }

    // ========== 16x16 图标 - 用于 Toolbar/Action/Menu/ToolWindow ==========

    /**
     * IntelliAI Changelog 主图标 (16x16)
     * <p>
     * 用于：工具栏按钮、动作图标、菜单项、工具窗口标签
     */
    public static final Icon CHANGELOG_16 = load("/icons/changelog_16.svg");
    /** 日志图标, 用于显示日志相关操作或视图的界面元素 */
    public static final Icon LOGS = load("/icons/logs.svg");
    /** 周报图标 (16x16), 用于工具栏按钮, 动作图标, 菜单项, 工具窗口标签 */
    public static final Icon WEEKLY = load("/icons/weekly.svg");
    /** 日报图标, 用于显示日报相关操作或视图的界面元素 */
    public static final Icon DAILY = load("/icons/daily.svg");
    /** 差异对比图标 (16x16), 用于工具栏按钮, 动作图标, 菜单项, 工具窗口标签 */
    public static final Icon DIFF = load("/icons/diff.svg");
    /** IntelliJ AI 变更日志发布图标 (16x16)<br> 用于工具栏按钮, 动作图标, 菜单项, 工具窗口标签 */
    public static final Icon RELEASE = load("/icons/release.svg");
    /** IntelliJ AI 变更日志主图标 (16x16), 用于工具栏按钮, 动作图标, 菜单项及工具窗口标签 */
    public static final Icon CHANGELOG = load("/icons/change-logo.svg");

}

