package dev.dong4j.zeka.stack.idea.plugin.settings.ui;

import com.intellij.util.ui.UIUtil;

import org.jetbrains.annotations.NotNull;

import java.awt.BorderLayout;
import java.awt.Color;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;

import dev.dong4j.zeka.stack.idea.plugin.util.JavadocBundle;

/**
 * 面板工具类
 * <p>
 * 提供创建和配置 UI 面板的通用工具方法。
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @since 1.4.0
 */
public final class PanelUtil {

    /**
     * 私有构造函数，禁止实例化
     */
    private PanelUtil() {
        // 工具类，禁止实例化
    }

    /**
     * 创建一个带标题边框的面板
     * <p>
     * 该方法将传入的 {@code contentPanel} 放置在中心位置, 并为其设置一个
     * 具有 {@link BorderFactory#createEtchedBorder() EtchedBorder} 样式的标题边框.
     * 标题文本从资源文件中获取. 随后调用 {@link #configureTitledBorder(TitledBorder)}
     * 对边框进行进一步配置.
     *
     * @param contentPanel 需要被包装的内容面板
     * @param bundleKey    用于从资源文件中获取标题文本的键
     * @return 包含标题边框的 {@link JPanel}
     */
    @NotNull
    public static JPanel createBorderPanel(@NotNull JPanel contentPanel, @NotNull String bundleKey) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(contentPanel, BorderLayout.CENTER);

        // 添加带标题的边框
        TitledBorder titledBorder = BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(),
            JavadocBundle.message(bundleKey)
                                                                    );
        configureTitledBorder(titledBorder);
        panel.setBorder(titledBorder);

        return panel;
    }

    /**
     * 配置 TitledBorder 的字体和颜色
     * <p>
     * 显式设置字体和颜色，确保在 2025 版本中正常显示。
     * 使用 UIUtil 获取主题感知的文本颜色，自动适配浅色和深色主题。
     *
     * @param titledBorder 要配置的 TitledBorder
     */
    public static void configureTitledBorder(@NotNull TitledBorder titledBorder) {
        titledBorder.setTitleFont(UIManager.getFont("Label.font"));
        Color titleColor = UIUtil.getLabelForeground();
        titledBorder.setTitleColor(titleColor);
    }
}

