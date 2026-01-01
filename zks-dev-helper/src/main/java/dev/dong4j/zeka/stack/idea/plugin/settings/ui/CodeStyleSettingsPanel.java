package dev.dong4j.zeka.stack.idea.plugin.settings.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

import dev.dong4j.zeka.stack.idea.plugin.codestyle.CodeStyleDownloadManager;
import dev.dong4j.zeka.stack.idea.plugin.settings.state.CodeStyleSettingsState;
import dev.dong4j.zeka.stack.idea.plugin.util.HelperBundle;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * 代码样式设置面板
 * <p>
 * 该类用于展示和管理代码样式相关的设置界面，提供代码样式模块（文件模板、Live Template、代码风格）等功能的开关配置。
 * 用户可以通过该面板对插件的各项功能进行启用或禁用设置，并将配置保存或恢复到指定的设置对象中。
 * 该类位于 {@code settings.ui} 包中，作为二级菜单的设置面板。
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
 * @email "mailto:dong4j@gmail.com"
 * @date 2025.10.25
 * @since 1.0.0
 * @see dev.dong4j.zeka.stack.idea.plugin.settings.configurable.CodeStyleSettingsConfigurable
 * @see dev.dong4j.zeka.stack.idea.plugin.settings.state.CodeStyleSettingsState
 */
@Slf4j
@Data
public class CodeStyleSettingsPanel {

    /**
     * 主界面主面板，用于承载主要功能组件和布局
     * -- GETTER --
     * 获取主面板组件
     * <p>
     * 返回应用程序的主面板，用于展示主要界面内容。
     */
    private JPanel mainPanel;
    /** 文件模板启用状态复选框 */
    private JBCheckBox enableFileTemplatesCheckBox;
    /** 启用活模板的复选框 */
    private JBCheckBox enableLiveTemplatesCheckBox;
    /** 启用代码样式检查的复选框 */
    private JBCheckBox enableCodeStyleCheckBox;

    // ========== 代码样式更新配置组件 ==========
    /** 自动更新代码样式复选框 */
    private JBCheckBox autoUpdateCodeStyleCheckBox;
    /** 下载地址输入框 */
    private JBTextField downloadUrlField;
    /** 设置为全局方案复选框 */
    private JBCheckBox useGlobalSchemeCheckBox;
    /** 手动下载按钮 */
    private JButton downloadButton;
    /** 下载进度条 */
    private JProgressBar downloadProgressBar;
    /** 状态标签 */
    private JBLabel statusLabel;
    /** 是否正在下载 */
    private final AtomicBoolean isDownloading = new AtomicBoolean(false);

    /**
     * 构造函数，初始化统一格式设置面板
     * <p>
     * 调用初始化组件方法，完成面板的初始化工作
     */
    public CodeStyleSettingsPanel() {
        initializeComponents();
    }

    /**
     * 初始化插件的组件和布局
     * <p>
     * 该方法用于创建并配置插件所需的各个组件，包括复选框和描述标签，并使用 FormBuilder
     * 构建最终的布局面板。所有组件默认状态为选中。
     *
     * @author 插件开发人员
     * @since 1.0
     */
    private void initializeComponents() {
        // 创建组件（使用国际化）
        enableFileTemplatesCheckBox = new JBCheckBox(
            HelperBundle.message("settings.codestyle.enable.file.templates.label"));
        enableLiveTemplatesCheckBox = new JBCheckBox(
            HelperBundle.message("settings.codestyle.enable.live.templates.label"));
        enableCodeStyleCheckBox = new JBCheckBox(
            HelperBundle.message("settings.codestyle.enable.code.style.label"));

        // 设置默认值
        enableFileTemplatesCheckBox.setSelected(true);
        enableLiveTemplatesCheckBox.setSelected(true);
        enableCodeStyleCheckBox.setSelected(true);

        // 创建代码样式更新配置面板
        JPanel codeStyleUpdatePanel = createCodeStyleUpdatePanel();

        // 使用 FormBuilder 创建布局
        mainPanel = FormBuilder.createFormBuilder()
            .addComponent(enableFileTemplatesCheckBox)
            .addComponent(enableLiveTemplatesCheckBox)
            .addComponent(enableCodeStyleCheckBox)
            .addSeparator()
            .addComponent(codeStyleUpdatePanel)
            .addComponentFillVertically(new JPanel(), 0)
            .getPanel();
    }

    /**
     * 创建代码样式在线更新配置面板
     *
     * @return 代码样式更新配置面板
     */
    private JPanel createCodeStyleUpdatePanel() {
        // 创建组件
        autoUpdateCodeStyleCheckBox = new JBCheckBox(
            HelperBundle.message("settings.codestyle.update.auto.label"));
        autoUpdateCodeStyleCheckBox.setToolTipText(
            HelperBundle.message("settings.codestyle.update.auto.hint"));

        downloadUrlField = new JBTextField();
        downloadUrlField.setToolTipText(
            HelperBundle.message("settings.codestyle.update.download.url.hint"));
        downloadUrlField.setPreferredSize(new java.awt.Dimension(500, downloadUrlField.getPreferredSize().height));

        useGlobalSchemeCheckBox = new JBCheckBox(
            HelperBundle.message("settings.codestyle.update.use.global.scheme.label"));
        useGlobalSchemeCheckBox.setToolTipText(
            HelperBundle.message("settings.codestyle.update.use.global.scheme.hint"));

        downloadButton = new JButton(
            HelperBundle.message("settings.codestyle.update.download.button"));

        downloadProgressBar = new JProgressBar(0, 100);
        downloadProgressBar.setStringPainted(false);
        downloadProgressBar.setVisible(false);
        downloadProgressBar.setPreferredSize(new java.awt.Dimension(500, JBUI.scale(3)));

        statusLabel = new JBLabel(HelperBundle.message("settings.codestyle.update.status.ready"));

        // 设置初始状态
        downloadUrlField.setEnabled(false);
        downloadButton.setEnabled(false);

        // 自动更新复选框控制下载地址输入框的可用性
        autoUpdateCodeStyleCheckBox.addActionListener(e -> {
            boolean enabled = autoUpdateCodeStyleCheckBox.isSelected();
            downloadUrlField.setEnabled(enabled);
            downloadButton.setEnabled(enabled);
        });

        // 手动下载按钮
        downloadButton.addActionListener(e -> {
            String url = downloadUrlField.getText().trim();
            if (url.isEmpty()) {
                statusLabel.setText(HelperBundle.message("settings.codestyle.update.download.failed", "请先输入下载地址"));
                return;
            }
            triggerCodeStyleDownload(url);
        });

        return FormBuilder.createFormBuilder()
            .addComponent(autoUpdateCodeStyleCheckBox)
            .addLabeledComponent(new JBLabel(HelperBundle.message("settings.codestyle.update.download.url.label")), downloadUrlField)
            .addComponent(useGlobalSchemeCheckBox)
            .addComponent(downloadButton)
            .addComponent(downloadProgressBar)
            .addComponent(statusLabel)
            .getPanel();
    }

    /**
     * 触发代码样式下载
     *
     * @param downloadUrl 下载地址
     */
    private void triggerCodeStyleDownload(@NotNull String downloadUrl) {
        // 防止重复下载
        if (!isDownloading.compareAndSet(false, true)) {
            log.warn("Download already in progress, skipping");
            return;
        }

        // 重置状态
        downloadProgressBar.setVisible(true);
        downloadProgressBar.setIndeterminate(true);
        downloadProgressBar.setValue(0);
        statusLabel.setText(HelperBundle.message("settings.codestyle.update.status.checking"));
        downloadButton.setEnabled(false);

        // 后台下载
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            ProgressIndicator indicator = new EmptyProgressIndicator();
            try {
                // 检查并更新代码样式
                CodeStyleDownloadManager.checkAndUpdate(
                    null, // 项目对象，在设置页面中可以为 null
                    downloadUrl, // baseUrl
                    indicator,
                    (downloaded, total) -> {
                        // 更新进度条
                        SwingUtilities.invokeLater(() -> {
                            // 确保进度条和状态标签可见
                            if (!downloadProgressBar.isVisible()) {
                                downloadProgressBar.setVisible(true);
                            }
                            if (!statusLabel.isVisible()) {
                                statusLabel.setVisible(true);
                            }

                            if (total > 0) {
                                int percent = (int) Math.min(100, Math.round(downloaded * 100.0 / total));
                                downloadProgressBar.setIndeterminate(false);
                                downloadProgressBar.setValue(percent);
                                statusLabel.setText(
                                    HelperBundle.message("settings.codestyle.update.status.downloading") + " (" + percent + "%)");
                            } else {
                                downloadProgressBar.setIndeterminate(true);
                                statusLabel.setText(HelperBundle.message("settings.codestyle.update.status.downloading"));
                            }
                            // 强制刷新
                            downloadProgressBar.revalidate();
                            downloadProgressBar.repaint();
                            statusLabel.revalidate();
                            statusLabel.repaint();
                        });
                    }
                                                       );

                // 下载成功
                SwingUtilities.invokeLater(() -> {
                    downloadProgressBar.setVisible(false);
                    downloadProgressBar.setValue(0);
                    statusLabel.setText(HelperBundle.message("settings.codestyle.update.download.success"));
                    downloadButton.setEnabled(autoUpdateCodeStyleCheckBox.isSelected());
                });
            } catch (Exception e) {
                log.error("Failed to download code style", e);
                // 下载失败
                SwingUtilities.invokeLater(() -> {
                    downloadProgressBar.setVisible(false);
                    downloadProgressBar.setValue(0);
                    statusLabel.setText(HelperBundle.message("settings.codestyle.update.download.failed", e.getMessage()));
                    downloadButton.setEnabled(autoUpdateCodeStyleCheckBox.isSelected());
                });
            } finally {
                isDownloading.set(false);
            }
        });
    }

    /**
     * 判断当前设置是否与给定的设置状态不同
     * <p>
     * 比较当前设置项与传入的设置状态，若任一设置项的选中状态不同，则返回 true。
     *
     * @param settings 要比较的设置状态对象
     * @return 如果当前设置与给定设置状态不同，返回 true；否则返回 false
     */
    public boolean isModified(CodeStyleSettingsState settings) {
        boolean basicModified = enableFileTemplatesCheckBox.isSelected() != settings.isEnableFileTemplates() ||
                                enableLiveTemplatesCheckBox.isSelected() != settings.isEnableLiveTemplates() ||
                                enableCodeStyleCheckBox.isSelected() != settings.isEnableCodeStyle();

        // 检查代码样式更新配置
        CodeStyleSettingsState.CodeStyleUpdateSettings currentUpdateSettings = getCodeStyleUpdateSettings();
        CodeStyleSettingsState.CodeStyleUpdateSettings savedUpdateSettings = settings.getCodeStyleUpdateSettings();

        boolean updateModified = false;
        if (currentUpdateSettings != null && savedUpdateSettings != null) {
            updateModified = currentUpdateSettings.isAutoUpdate() != savedUpdateSettings.isAutoUpdate() ||
                             !java.util.Objects.equals(
                                 currentUpdateSettings.getDownloadUrl() != null ? currentUpdateSettings.getDownloadUrl().trim() : "",
                                 savedUpdateSettings.getDownloadUrl() != null ? savedUpdateSettings.getDownloadUrl().trim() : ""
                                                      ) ||
                             currentUpdateSettings.isUseGlobalScheme() != savedUpdateSettings.isUseGlobalScheme();
        } else if (currentUpdateSettings != null || savedUpdateSettings != null) {
            updateModified = true;
        }

        return basicModified || updateModified;
    }

    /**
     * 应用格式设置状态到指定的设置对象
     * <p>
     * 将复选框的状态应用到统一格式设置状态对象中，用于配置文件模板、实时模板、代码样式功能的启用状态。
     *
     * @param settings 格式设置状态对象，用于存储配置信息
     */
    public void apply(CodeStyleSettingsState settings) {
        settings.setEnableFileTemplates(enableFileTemplatesCheckBox.isSelected());
        settings.setEnableLiveTemplates(enableLiveTemplatesCheckBox.isSelected());
        settings.setEnableCodeStyle(enableCodeStyleCheckBox.isSelected());

        // 应用代码样式更新配置
        CodeStyleSettingsState.CodeStyleUpdateSettings updateSettings = getCodeStyleUpdateSettings();
        settings.setCodeStyleUpdateSettings(updateSettings);
    }

    /**
     * 重置统一格式设置状态
     * <p>
     * 根据传入的设置状态，更新相关复选框的选中状态。
     *
     * @param settings 统一格式设置状态对象
     */
    public void reset(CodeStyleSettingsState settings) {
        enableFileTemplatesCheckBox.setSelected(settings.isEnableFileTemplates());
        enableLiveTemplatesCheckBox.setSelected(settings.isEnableLiveTemplates());
        enableCodeStyleCheckBox.setSelected(settings.isEnableCodeStyle());

        // 重置代码样式更新配置
        CodeStyleSettingsState.CodeStyleUpdateSettings updateSettings = settings.getCodeStyleUpdateSettings();
        if (updateSettings != null) {
            autoUpdateCodeStyleCheckBox.setSelected(updateSettings.isAutoUpdate());
            downloadUrlField.setText(updateSettings.getDownloadUrl() != null ? updateSettings.getDownloadUrl() : "");
            downloadUrlField.setEnabled(updateSettings.isAutoUpdate());
            downloadButton.setEnabled(updateSettings.isAutoUpdate());
            useGlobalSchemeCheckBox.setSelected(updateSettings.isUseGlobalScheme());
        } else {
            autoUpdateCodeStyleCheckBox.setSelected(false);
            downloadUrlField.setText("");
            downloadUrlField.setEnabled(false);
            downloadButton.setEnabled(false);
            useGlobalSchemeCheckBox.setSelected(false);
        }
        downloadProgressBar.setVisible(false);
        downloadProgressBar.setValue(0);
        statusLabel.setText(HelperBundle.message("settings.codestyle.update.status.ready"));
    }

    /**
     * 获取代码样式更新配置
     *
     * @return 代码样式更新配置
     */
    @NotNull
    private CodeStyleSettingsState.CodeStyleUpdateSettings getCodeStyleUpdateSettings() {
        CodeStyleSettingsState.CodeStyleUpdateSettings updateSettings =
            new CodeStyleSettingsState.CodeStyleUpdateSettings();
        updateSettings.setAutoUpdate(autoUpdateCodeStyleCheckBox.isSelected());
        updateSettings.setDownloadUrl(downloadUrlField.getText().trim());
        updateSettings.setUseGlobalScheme(useGlobalSchemeCheckBox.isSelected());
        return updateSettings;
    }

}
