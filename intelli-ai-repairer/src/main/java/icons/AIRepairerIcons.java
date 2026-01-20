package icons;

import com.intellij.openapi.util.IconLoader;

import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

/**
 * AI 维修图标管理类
 * <p> 该类用于加载和管理 AI 维修相关的图标资源. 通过静态方法加载不同尺寸的图标, 并提供对应的静态成员变量供访问.
 * 图标路径由相对路径指定, 加载过程中确保传入的图标路径不为空.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.20
 * @since 1.0.0
 */
public class AIRepairerIcons {
    /**
     * 加载指定路径的图标
     * <p> 根据给定的图标路径加载图标资源
     *
     * @param iconPath 图标文件的路径
     * @return 加载的图标对象
     */
    @NotNull
    private static Icon load(@NotNull String iconPath) {
        return IconLoader.getIcon(iconPath, AIRepairerIcons.class);
    }

    /** 16x16 大小的 Repairer 图标 */
    public static final Icon REPAIRER_16 = load("/icons/repairer_16.svg");
    /** Repairer 图标, 尺寸为 24x24 像素 */
    public static final Icon REPAIRER_24 = load("/icons/repairer_24.svg");
    /** Repairer 32x32 像素图标 */
    public static final Icon REPAIRER_32 = load("/icons/repairer_32.svg");
}
