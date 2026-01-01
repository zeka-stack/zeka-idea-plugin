package icons;

import com.intellij.openapi.util.IconLoader;

import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

/**
 * IntelliAI Swagger 插件图标管理类
 * <p>
 * 用于集中管理插件中使用的所有图标资源。
 * 图标文件应放置在与插件包路径一致的资源目录下。
 * <p>
 * 图标尺寸说明：
 * <ul>
 *   <li>16x16 - Toolbar/Action/Menu/ToolWindow（工具栏、动作、菜单、工具窗口）</li>
 *   <li>24x24 - Notifications（通知图标）</li>
 *   <li>32x32 - Dialog/Settings（对话框、设置面板）</li>
 * </ul>
 *
 * @author dong4j
 * @since 1.0.0
 */
public class SwaggerIcons {
    /**
     * 加载图标
     * <p>
     * 用于加载位于资源包路径下的图标文件。
     * 路径应与插件包路径保持一致。
     *
     * @param iconPath 图标文件路径，相对于 resources 根目录
     * @return 加载的图标
     */
    @NotNull
    private static Icon load(@NotNull String iconPath) {
        return IconLoader.getIcon(iconPath, SwaggerIcons.class);
    }

    // ========== 16x16 图标 - 用于 Toolbar/Action/Menu/ToolWindow ==========

    /**
     * 插件主图标 (16x16)
     * <p>
     * 用于：工具栏按钮、动作图标、菜单项、工具窗口标签
     */
    public static final Icon SWAGGER_16 =
        load("/dev/dong4j/zeka/stack/idea/plugin/swagger/icons/swagger_16.svg");
}
