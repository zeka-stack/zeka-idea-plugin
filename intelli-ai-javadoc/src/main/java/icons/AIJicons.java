package icons;

import com.intellij.openapi.util.IconLoader;

import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

/**
 * AIJ 图标工具类
 * <p>
 * 用于集中管理插件中使用的所有图标资源.
 * 图标文件应放置在 {@code src/main/resources/icons/} 目录下.
 * <p>
 * 图标尺寸说明:
 * <ul>
 *   <li>16x16 - Toolbar/Action/Menu/ToolWindow(工具栏, 动作, 菜单, 工具窗口)</li>
 *   <li>24x24 - Notifications(通知图标)</li>
 *   <li>32x32 - Dialog/Settings(对话框, 设置面板)</li>
 * </ul>
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email mailto:zeka.stack@gmail.com
 * @date 2025.11.30
 * @since 1.0.0
 */
public class AIJicons {
    /**
     * 加载图标
     * <p>
     * 用于加载位于 {@code/icons/} 目录下的图标文件.
     * 路径必须以 {@code/icons/} 开头.
     *
     * @param iconPath 图标文件路径, 相对于 resources 根目录(例如:"/icons/icon.svg")
     * @return 加载的图标
     */
    @NotNull
    private static Icon load(@NotNull String iconPath) {
        return IconLoader.getIcon(iconPath, AIJicons.class);
    }

    // ========== 16x16 图标 - 用于 Toolbar/Action/Menu/ToolWindow ==========

    /**
     * IntelliJAI Javadoc 主图标 (16x16)
     * <p>
     * 用于: 工具栏按钮, 动作图标, 菜单项, 工具窗口标签
     *
     */
    public static final Icon AIJ_16 = load("/icons/aij_16.svg");
    /**
     * InteliAI Javadoc 主图标 (24x24)
     * <p> 用于: 通知图标
     */
    public static final Icon AIJ_24 = load("/icons/aij_24.svg");
    /**
     * IntelliAI Javadoc 主图标 (32x32)
     * <p> 用于: 对话框, 设置面板等界面元素
     */
    public static final Icon AIJ_32 = load("/icons/aij_32.svg");

}
