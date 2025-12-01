package icons;

import com.intellij.openapi.util.IconLoader;
import com.intellij.util.IconUtil;

import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

/**
 * 图标管理器
 * <p>
 * 负责集中管理 IntelliAI Tracer 插件的所有图标资源。
 * 所有 SVG 文件应存放在 {@code src/main/resources/icons/} 目录下，并通过
 * 该类暴露为静态常量，方便动作、工具窗口等位置复用。
 *
 * @author dong4j
 * @version 1.0.0
 * @since 1.0.0
 */
public final class TracerIcons {
    private TracerIcons() {
        // utility class
    }

    /**
     * 通用图标加载入口。
     *
     * @param iconPath 相对 {@code resources} 根目录的路径（例如 {@code "/icons/pluginIcon_16.svg"}）
     * @return {@link Icon}
     */
    @NotNull
    private static Icon load(@NotNull String iconPath) {
        return IconLoader.getIcon(iconPath, TracerIcons.class);
    }

    /**
     * 工作流解释动作默认图标 (16x16)。
     * <p>
     * 用于：右键菜单、工具栏按钮等基于 {@link com.intellij.openapi.actionSystem.AnAction} 的入口。
     */
    public static final Icon WORKFLOW_ACTION = load("/icons/pluginIcon_16.svg");
    /**
     * 工具图标, 用于表示工作流操作
     * <p>
     * 该图标通过 {@link IconUtil#scale} 方法对原始图标进行缩放处理, 缩放比例为 0.8125
     *
     * @see IconUtil
     */
    public static final Icon TOOL_ICON = IconUtil.scale(WORKFLOW_ACTION, null, 0.8125f);
}

