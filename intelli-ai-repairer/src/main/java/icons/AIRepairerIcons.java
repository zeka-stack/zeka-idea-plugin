package icons;

import com.intellij.openapi.util.IconLoader;

import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

/**
 * Repairer 图标工具类.
 */
public class AIRepairerIcons {
    @NotNull
    private static Icon load(@NotNull String iconPath) {
        return IconLoader.getIcon(iconPath, AIRepairerIcons.class);
    }

    public static final Icon REPAIRER_16 = load("/icons/repairer_16.svg");
    public static final Icon REPAIRER_24 = load("/icons/repairer_24.svg");
    public static final Icon REPAIRER_32 = load("/icons/repairer_32.svg");
}
