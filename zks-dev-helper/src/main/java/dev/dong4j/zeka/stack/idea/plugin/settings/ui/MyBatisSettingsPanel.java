package dev.dong4j.zeka.stack.idea.plugin.settings.ui;

import com.intellij.ui.components.JBCheckBox;
import com.intellij.util.ui.FormBuilder;

import org.jetbrains.annotations.NotNull;

import javax.swing.JPanel;

import dev.dong4j.zeka.stack.idea.plugin.settings.state.MyBatisSettingsState;
import dev.dong4j.zeka.stack.idea.plugin.util.HelperBundle;
import lombok.Getter;

/**
 * My Batis Settings Panel
 *
 * @author dong4j
 * @version hello.world
 * @date 2026-01-02 03:01:46
 * @since hello.world
 */
public class MyBatisSettingsPanel {

    /**
     * 主界面主面板，用于承载主要功能组件和布局
     * -- GETTER --
     * 获取主面板组件
     * <p>
     * 返回应用程序的主面板，用于展示主要界面内容。
     */
    @Getter
    private final JPanel mainPanel;

    /** MyBatis 功能启用状态复选框 */
    private JBCheckBox enableMyBatisCheckBox;

    /**
     * 构造函数，初始化 MyBatis 设置面板
     * <p>
     * 调用初始化组件方法，完成面板的初始化工作
     */
    public MyBatisSettingsPanel() {
        mainPanel = createMainPanel();
    }

    /**
     * 创建主面板
     * <p>
     * 构建包含 MyBatis 配置选项的主面板
     *
     * @return 主面板组件
     */
    @NotNull
    private JPanel createMainPanel() {
        // 创建组件
        enableMyBatisCheckBox = new JBCheckBox(
            HelperBundle.message("settings.mybatis.enable.label"));
        enableMyBatisCheckBox.setToolTipText(
            HelperBundle.message("settings.mybatis.enable.hint"));

        // 设置默认值
        enableMyBatisCheckBox.setSelected(true);

        // 使用 FormBuilder 创建布局
        return FormBuilder.createFormBuilder()
            .addComponent(enableMyBatisCheckBox)
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
    public void reset(@NotNull MyBatisSettingsState settings) {
        enableMyBatisCheckBox.setSelected(settings.isEnableMyBatis());
    }

    /**
     * 应用面板中的设置
     * <p>
     * 将面板中的配置应用到设置状态中。
     *
     * @param settings 设置状态对象
     */
    public void apply(@NotNull MyBatisSettingsState settings) {
        settings.setEnableMyBatis(enableMyBatisCheckBox.isSelected());
    }

    /**
     * 检查设置是否被修改
     * <p>
     * 比较当前面板中的设置与已保存的设置，判断是否有修改。
     *
     * @param settings 设置状态对象
     * @return 如果设置被修改返回 true，否则返回 false
     */
    public boolean isModified(@NotNull MyBatisSettingsState settings) {
        return enableMyBatisCheckBox.isSelected() != settings.isEnableMyBatis();
    }
}

