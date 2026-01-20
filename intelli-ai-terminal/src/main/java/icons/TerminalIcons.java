package icons;

import com.intellij.openapi.util.IconLoader;

import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

/**
 * 图标工具类
 * <p> 提供常用图标资源的加载和访问功能, 主要用于应用程序中图形界面元素的展示
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.02
 * @since 1.0.0
 */
public class TerminalIcons {
    /**
     * 加载图标
     * <p> 用于加载位于资源包路径下的图标文件. 路径应与插件包路径保持一致.</p>
     *
     * @param iconPath 图标文件路径, 相对于 resources 根目录
     * @return 加载的图标
     */
    @NotNull
    private static Icon load(@NotNull String iconPath) {
        return IconLoader.getIcon(iconPath, TerminalIcons.class);
    }

    // ========== 16x16 图标 - 用于 Toolbar/Action/Menu/ToolWindow ==========

    /**
     * 插件主图标 (16x16)
     * <p> 用于: 工具栏按钮, 动作图标, 菜单项, 工具窗口标签
     */
    public static final Icon TERMINAL_16 =
        load("/icons/terminal_16.svg");
}
