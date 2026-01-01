package dev.dong4j.zeka.stack.idea.plugin.settings.ui;

import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;

import org.jetbrains.annotations.NotNull;

import javax.swing.JPanel;

import dev.dong4j.zeka.stack.idea.plugin.settings.state.ZksDevHelperMainState;
import dev.dong4j.zeka.stack.idea.plugin.util.HelperBundle;
import lombok.Getter;

/**
 * ZKS Dev Helper 主设置面板
 * <p>
 * 作为一级菜单的设置面板，显示插件的描述信息。后续会添加全局配置选项。
 * 该类位于 {@code settings.ui} 包中，负责 UI 组件的创建和布局。
 * <p>
 * 目录结构说明：
 * <ul>
 *   <li>{@code settings.configurable} - 配置类（Configurable 接口实现）</li>
 *   <li>{@code settings.ui} - UI 面板类（Panel 类）</li>
 *   <li>{@code settings.state} - 状态类（PersistentStateComponent 实现）</li>
 * </ul>
 *
 * @author dong4j
 * @version 1.0.0
 * @date 2026.01.02
 * @see dev.dong4j.zeka.stack.idea.plugin.settings.configurable.ZksDevHelperMainConfigurable
 * @see dev.dong4j.zeka.stack.idea.plugin.settings.state.ZksDevHelperMainState
 * @since 1.0.0
 */
public class ZksDevHelperMainPanel {

    /**
     * 主界面主面板，用于承载主要功能组件和布局
     * -- GETTER --
     * 获取主面板组件
     * <p>
     * 返回应用程序的主面板，用于展示主要界面内容。
     */
    @Getter
    private final JPanel mainPanel;

    /**
     * 构造函数，初始化主设置面板
     * <p>
     * 创建并配置主设置面板的 UI 组件，包括插件描述信息等。
     */
    public ZksDevHelperMainPanel() {
        mainPanel = createMainPanel();
    }

    /**
     * 创建主面板
     * <p>
     * 构建包含插件描述信息的主面板
     *
     * @return 主面板组件
     */
    @NotNull
    private JPanel createMainPanel() {
        // 构建描述文本（使用国际化）
        String descriptionHtml = "<html>" +
                                 "<p>" + HelperBundle.message("settings.description.title") + "</p>" +
                                 "<p><b>" + HelperBundle.message("settings.description.current.features") + "</b></p>" +
                                 "<ul>" +
                                 "<li><b>" + HelperBundle.message("settings.description.file.templates") + "</b></li>" +
                                 "<li><b>" + HelperBundle.message("settings.description.live.templates") + "</b></li>" +
                                 "<li><b>" + HelperBundle.message("settings.description.code.style") + "</b></li>" +
                                 "</ul>" +
                                 "<p><b>" + HelperBundle.message("settings.description.planned.features") + "</b></p>" +
                                 "<ul>" +
                                 "<li>" + HelperBundle.message("settings.description.planned.mybatis") + "</li>" +
                                 "<li>" + HelperBundle.message("settings.description.planned.proxyer") + "</li>" +
                                 "<li>" + HelperBundle.message("settings.description.planned.more") + "</li>" +
                                 "</ul>" +
                                 "</html>";
        JBLabel descriptionLabel = new JBLabel(descriptionHtml);
        descriptionLabel.setAllowAutoWrapping(true);

        JPanel panel = FormBuilder.createFormBuilder()
            .addComponent(descriptionLabel)
            .addComponentFillVertically(new JPanel(), 0)
            .getPanel();
        panel.setBorder(JBUI.Borders.empty(10));
        return panel;
    }

    /**
     * 重置面板状态
     * <p>
     * 将面板恢复到初始状态，从设置状态中加载配置。
     * 后续添加全局配置后，可以在此方法中从状态中加载配置项。
     *
     * @param settings 设置状态对象
     */
    public void reset(@NotNull ZksDevHelperMainState settings) {
        // 主配置页面目前不包含可修改的设置
        // 后续添加全局配置后，可以在此方法中从 settings 中加载配置项
    }

    /**
     * 应用面板中的设置
     * <p>
     * 将面板中的配置应用到设置状态中。目前主配置页面不包含可修改的设置，此方法为空实现。
     * 后续添加全局配置后，可以在此方法中保存配置到 settings。
     *
     * @param settings 设置状态对象
     */
    public void apply(@NotNull ZksDevHelperMainState settings) {
        // 主配置页面目前不包含可修改的设置
        // 后续添加全局配置后，可以在此方法中保存配置到 settings
    }

    /**
     * 检查设置是否被修改
     * <p>
     * 比较当前面板中的设置与已保存的设置，判断是否有修改。
     * 目前主配置页面不包含可修改的设置，始终返回 false。
     *
     * @param settings 设置状态对象
     * @return 如果设置被修改返回 true，否则返回 false
     */
    public boolean isModified(@NotNull ZksDevHelperMainState settings) {
        // 主配置页面目前不包含可修改的设置
        return false;
    }
}

