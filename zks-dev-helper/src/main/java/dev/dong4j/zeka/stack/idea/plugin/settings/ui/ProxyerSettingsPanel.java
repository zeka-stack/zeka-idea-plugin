package dev.dong4j.zeka.stack.idea.plugin.settings.ui;

import com.intellij.ui.components.JBCheckBox;
import com.intellij.util.ui.FormBuilder;

import org.jetbrains.annotations.NotNull;

import javax.swing.JPanel;

import dev.dong4j.zeka.stack.idea.plugin.settings.state.ProxyerSettingsState;
import dev.dong4j.zeka.stack.idea.plugin.util.HelperBundle;
import lombok.Getter;

/**
 * Proxyer Settings Panel
 *
 * @author dong4j
 * @version hello.world
 * @date 2026-01-02 03:02:01
 * @since hello.world
 */
public class ProxyerSettingsPanel {

    /**
     * 主界面主面板，用于承载主要功能组件和布局
     * -- GETTER --
     * 获取主面板组件
     * <p>
     * 返回应用程序的主面板，用于展示主要界面内容。
     */
    @Getter
    private final JPanel mainPanel;

    /** Proxyer 功能启用状态复选框 */
    private JBCheckBox enableProxyerCheckBox;

    /**
     * 构造函数，初始化 Proxyer 设置面板
     * <p>
     * 调用初始化组件方法，完成面板的初始化工作
     */
    public ProxyerSettingsPanel() {
        mainPanel = createMainPanel();
    }

    /**
     * 创建主面板
     * <p>
     * 构建包含 Proxyer 配置选项的主面板
     *
     * @return 主面板组件
     */
    @NotNull
    private JPanel createMainPanel() {
        // 创建组件
        enableProxyerCheckBox = new JBCheckBox(
            HelperBundle.message("settings.proxyer.enable.label"));
        enableProxyerCheckBox.setToolTipText(
            HelperBundle.message("settings.proxyer.enable.hint"));

        // 设置默认值
        enableProxyerCheckBox.setSelected(true);

        // 使用 FormBuilder 创建布局
        return FormBuilder.createFormBuilder()
            .addComponent(enableProxyerCheckBox)
            .addComponentFillVertically(new JPanel(), 0)
            .getPanel();
    }

    /**
     * 重置面板状态
     * <p>
     * 将面板恢复到初始状态，从设置状态中加载配置。
     *
     * @param settings 设置状态对象
     */
    public void reset(@NotNull ProxyerSettingsState settings) {
        enableProxyerCheckBox.setSelected(settings.isEnableProxyer());
    }

    /**
     * 应用面板中的设置
     * <p>
     * 将面板中的配置应用到设置状态中。
     *
     * @param settings 设置状态对象
     */
    public void apply(@NotNull ProxyerSettingsState settings) {
        settings.setEnableProxyer(enableProxyerCheckBox.isSelected());
    }

    /**
     * 检查设置是否被修改
     * <p>
     * 比较当前面板中的设置与已保存的设置，判断是否有修改。
     *
     * @param settings 设置状态对象
     * @return 如果设置被修改返回 true，否则返回 false
     */
    public boolean isModified(@NotNull ProxyerSettingsState settings) {
        return enableProxyerCheckBox.isSelected() != settings.isEnableProxyer();
    }
}

