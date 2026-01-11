package dev.dong4j.zeka.stack.idea.plugin.changelog.settings.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;

import org.jetbrains.annotations.NotNull;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ItemEvent;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;

import dev.dong4j.zeka.stack.idea.plugin.changelog.PluginContents;
import dev.dong4j.zeka.stack.idea.plugin.changelog.git.GitCliffDownloadManager;
import dev.dong4j.zeka.stack.idea.plugin.changelog.settings.ReleaseLogProvider;
import dev.dong4j.zeka.stack.idea.plugin.changelog.settings.SettingsState;
import dev.dong4j.zeka.stack.idea.plugin.changelog.util.ChangelogBundle;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.ui.AIProviderSelectionPanel;
import dev.dong4j.zeka.stack.idea.plugin.common.ui.FeedbackPanel;
import lombok.Getter;

/**
 * 变更日志设置面板
 * <p>
 * 该面板提供了一个用户界面, 用于配置变更日志相关的设置, 包括 AI 提供商选择,
 * 系统提示模板, 变更日志模板, 日报模板, 周报模板和提交消息模板等高级设置.
 * 支持动态刷新 AI 提供商配置, 并提供模板重置功能.
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email mailto:zeka.stack@gmail.com
 * @date 2025.11.30
 * @since 1.0.0
 */
public class ChangelogSettingsPanel {

    /** 主面板 */
    @Getter
    private final JPanel mainPanel;
    /** AI 提供商选择面板（使用 engine 插件中的通用类） */
    private final AIProviderSelectionPanel aiProviderSelectionPanel;

    // 高级设置
    /** 显示高级设置的复选框 */
    private final JBCheckBox showAdvancedSettingsCheckBox;
    /** 高级设置容器面板（用于控制可见性） */
    private final JPanel advancedSettingsPanel;

    // Release Log 配置
    /** Release Log 生成方式：AI */
    private final JRadioButton releaseLogByAiRadioButton;
    /** Release Log 生成方式：git-cliff */
    private final JRadioButton releaseLogByGitCliffRadioButton;
    /** 使用 tag 作为起点单选框 */
    private final JRadioButton useTagAsStartRadioButton;
    /** 使用 hash 作为起点单选框 */
    private final JRadioButton useHashAsStartRadioButton;
    /** 清除 tag 和 hash 按钮 */
    private final JButton clearTagAndHashButton;
    /** Git-cliff 配置文本区域 */
    private final JBTextArea gitCliffConfigTextArea;
    /** 重置 Git-cliff 配置按钮 */
    private JButton resetGitCliffConfigButton;
    /** 显示 Git-cliff 配置复选框 */
    private final JBCheckBox showGitCliffConfigCheckBox;
    /** Git-cliff 配置容器面板（用于控制可见性） */
    private final JPanel gitCliffConfigPanel;
    /** 显示 AI Release Log 提示词复选框 */
    private final JBCheckBox showAiReleaseLogPromptCheckBox;
    /** AI Release Log 提示词容器面板（用于控制可见性） */
    private final JPanel aiReleaseLogPromptPanel;
    /** AI Release Log 提示词文本区域 */
    private final JBTextArea aiReleaseLogPromptTextArea;
    /** 重置 AI Release Log 提示词按钮 */
    private JButton resetAiReleaseLogPromptButton;
    /** Git-cliff 下载进度条 */
    private final JProgressBar gitCliffDownloadProgressBar;
    /** Git-cliff 下载状态标签 */
    private final JBLabel gitCliffDownloadStatusLabel;
    /** Git-cliff 下载进度面板 */
    private final JPanel gitCliffDownloadProgressPanel;
    /** 是否正在下载 git-cliff */
    private volatile boolean isDownloadingGitCliff = false;

    // Prompt 配置 - 创建文本区域（将在 Tab 页中使用）
    /** 系统提示文本区域, 用于显示或编辑系统提示信息 */
    public final JBTextArea systemPromptTextArea = new JBTextArea(15, 50);
    /** 日志变更模板文本区域, 用于显示和编辑日志变更模板内容 */
    public final JBTextArea changelogTemplateTextArea = new JBTextArea(15, 50);
    /**
     * 用于输入每日报告模板的文本区域
     * <p>
     * 提供 15 行 50 列的编辑空间, 供用户编辑或查看每日报告模板内容
     */
    public final JBTextArea dailyReportTemplateTextArea = new JBTextArea(15, 50);
    /** 周报模板文本区域, 用于显示和编辑周报模板内容 */
    public final JBTextArea weeklyReportTemplateTextArea = new JBTextArea(15, 50);
    /** 提交记录模板文本区域, 用于显示和编辑提交记录模板内容 */
    public final JBTextArea commitMessageTemplateTextArea = new JBTextArea(15, 50);
    /** 提交消息系统提示词文本区域, 用于显示和编辑系统提示词 */
    public final JBTextArea commitMessageSystemPromptTextArea = new JBTextArea(15, 50);
    /** 是否将提交说明作为上下文 */
    private final JBCheckBox useCommitMessageInputAsContextCheckBox;
    /** 是否显示提交消息提示词设置 */
    private final JBCheckBox showCommitMessagePromptCheckBox;
    /** 是否启用多 Git 仓库提交检查 */
    private final JBCheckBox enableCommitMultiRepoCheckBox;
    /** 提交消息 diff 生成方式 */
    private final JComboBox<SettingsState.CommitMessageDiffProvider> commitMessageDiffProviderComboBox;
    /** 提交消息提示词容器面板（用于控制可见性） */
    private final JPanel commitMessagePromptPanel;
    /** 是否显示排除模式设置 */
    private final JBCheckBox showExcludePatternsCheckBox;
    /** 排除模式容器面板（用于控制可见性） */
    private final JPanel excludePatternsPanel;
    /** 排除模式文本区域 */
    private final JBTextArea excludePatternsTextArea;

    /**
     * 构造函数, 初始化变更日志设置面板.
     * <p>
     * 创建并布局所有 UI 组件, 包括高级设置复选框,AI 提供者选择面板以及提示模板面板.
     * 同时注册提供者设置监听器并设置相关事件监听器.
     *
     * @since 1.0
     */
    public ChangelogSettingsPanel() {
        // 创建高级设置复选框
        showAdvancedSettingsCheckBox = new JBCheckBox(ChangelogBundle.message("settings.prompt.settings.show"));

        // 创建高级设置容器面板
        advancedSettingsPanel = new JPanel(new BorderLayout());

        // 构建高级设置面板内容（只包含 Prompt 模板）
        JPanel advancedSettingsContent = FormBuilder.createFormBuilder()
            // Prompt 模板与提示词
            .addComponent(createPromptTemplatesPanel())
            .getPanel();

        advancedSettingsPanel.add(advancedSettingsContent, BorderLayout.NORTH);

        // 初始化 AI 提供商选择面板（使用 engine 插件中的通用类）
        aiProviderSelectionPanel = new AIProviderSelectionPanel(
            ChangelogBundle::message,
            () -> {
                // 面板刷新后的回调：恢复选中的供应商
                SettingsState settings = SettingsState.getInstance();
                reset(settings);
            }
        );

        // 初始化 Release Log 配置组件
        releaseLogByAiRadioButton = new JRadioButton(ChangelogBundle.message("settings.release.log.generator.ai"));
        releaseLogByGitCliffRadioButton =
            new JRadioButton(ChangelogBundle.message("settings.release.log.generator.gitcliff"));
        ButtonGroup releaseLogGeneratorButtonGroup = new ButtonGroup();
        releaseLogGeneratorButtonGroup.add(releaseLogByAiRadioButton);
        releaseLogGeneratorButtonGroup.add(releaseLogByGitCliffRadioButton);
        useTagAsStartRadioButton = new JRadioButton(ChangelogBundle.message("settings.gitcliff.use.tag"));
        useHashAsStartRadioButton = new JRadioButton(ChangelogBundle.message("settings.gitcliff.use.hash"));

        // 创建按钮组
        ButtonGroup gitCliffStartPointButtonGroup = new ButtonGroup();
        gitCliffStartPointButtonGroup.add(useTagAsStartRadioButton);
        gitCliffStartPointButtonGroup.add(useHashAsStartRadioButton);

        // 创建清除按钮
        clearTagAndHashButton = new JButton(ChangelogBundle.message("settings.gitcliff.clear.tag.and.hash"));
        clearTagAndHashButton.addActionListener(e -> clearTagAndHash());

        // 创建显示 Git-cliff 配置复选框
        showGitCliffConfigCheckBox = new JBCheckBox(ChangelogBundle.message("settings.gitcliff.config.show"));

        // 创建 Git-cliff 配置容器面板
        gitCliffConfigPanel = new JPanel(new BorderLayout());

        // 创建 Git-cliff 配置文本区域
        gitCliffConfigTextArea = new JBTextArea(15, 50);
        gitCliffConfigTextArea.setLineWrap(true);
        gitCliffConfigTextArea.setWrapStyleWord(true);
        gitCliffConfigTextArea.setToolTipText(ChangelogBundle.message("settings.gitcliff.config.tooltip"));

        // 构建 Git-cliff 配置面板内容
        JPanel gitCliffConfigContent = createGitCliffConfigContentPanel();
        gitCliffConfigPanel.add(gitCliffConfigContent, BorderLayout.NORTH);
        gitCliffConfigPanel.setVisible(false); // 默认隐藏

        // 创建显示 AI Release Log 提示词复选框
        showAiReleaseLogPromptCheckBox = new JBCheckBox(ChangelogBundle.message("settings.ai.release.log.prompt.show"));

        // 创建 AI Release Log 提示词容器面板
        aiReleaseLogPromptPanel = new JPanel(new BorderLayout());

        // 创建 AI Release Log 提示词文本区域
        aiReleaseLogPromptTextArea = new JBTextArea(15, 50);
        aiReleaseLogPromptTextArea.setLineWrap(true);
        aiReleaseLogPromptTextArea.setWrapStyleWord(true);
        aiReleaseLogPromptTextArea.setToolTipText(ChangelogBundle.message("settings.ai.release.log.prompt.tooltip"));

        // 构建 AI Release Log 提示词面板内容
        JPanel aiReleaseLogPromptContent = createAiReleaseLogPromptContentPanel();
        aiReleaseLogPromptPanel.add(aiReleaseLogPromptContent, BorderLayout.NORTH);
        aiReleaseLogPromptPanel.setVisible(false); // 默认隐藏

        // 创建 Git-cliff 下载进度条和状态标签
        gitCliffDownloadProgressBar = new JProgressBar(0, 100);
        gitCliffDownloadProgressBar.setStringPainted(false);
        gitCliffDownloadProgressBar.setVisible(true); // 默认可见，由面板控制
        gitCliffDownloadProgressBar.setPreferredSize(new Dimension(420, JBUI.scale(3)));

        gitCliffDownloadStatusLabel = new JBLabel("");
        gitCliffDownloadStatusLabel.setVisible(true); // 默认可见，由面板控制

        // 创建下载进度面板
        gitCliffDownloadProgressPanel = new JPanel(new BorderLayout(0, 5));
        gitCliffDownloadProgressPanel.add(gitCliffDownloadProgressBar, BorderLayout.CENTER);
        gitCliffDownloadProgressPanel.add(gitCliffDownloadStatusLabel, BorderLayout.SOUTH);
        gitCliffDownloadProgressPanel.setVisible(false); // 默认隐藏，下载时显示

        // 初始化子配置的可用状态
        // use tag 和 use hash 始终可用，不受 provider 选择影响
        useTagAsStartRadioButton.setEnabled(true);
        useHashAsStartRadioButton.setEnabled(true);
        clearTagAndHashButton.setEnabled(true);
        updateGitCliffConfigAvailability(false);
        updateAiReleaseLogPromptAvailability(true); // 默认选择 AI，所以 AI 提示词可用

        // 默认选择 AI
        releaseLogByAiRadioButton.setSelected(true);

        // 更新 git-cliff 单选框文本，如果已安装则显示版本号
        updateGitCliffRadioButtonText();

        // 创建提交消息上下文开关
        useCommitMessageInputAsContextCheckBox =
            new JBCheckBox(ChangelogBundle.message("settings.commit.context.enabled"));
        useCommitMessageInputAsContextCheckBox.setToolTipText(
            ChangelogBundle.message("settings.commit.context.tooltip"));

        // 创建显示提交消息提示词复选框
        showCommitMessagePromptCheckBox = new JBCheckBox(ChangelogBundle.message("settings.commit.message.prompt.show"));

        // 创建多 Git 仓库提交检查开关
        enableCommitMultiRepoCheckBox = new JBCheckBox(ChangelogBundle.message("settings.commit.multi.repo.check.enable"));
        commitMessageDiffProviderComboBox = new ComboBox<>(SettingsState.CommitMessageDiffProvider.values());
        commitMessageDiffProviderComboBox.setRenderer(new javax.swing.DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(javax.swing.JList<?> list,
                                                                   Object value,
                                                                   int index,
                                                                   boolean isSelected,
                                                                   boolean cellHasFocus) {
                java.awt.Component component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof SettingsState.CommitMessageDiffProvider provider) {
                    String key = switch (provider) {
                        case AUTO -> "settings.commit.message.diff.provider.auto";
                        case IDEA_PATCH -> "settings.commit.message.diff.provider.idea.patch";
                        default -> "settings.commit.message.diff.provider.code.diff";
                    };
                    setText(ChangelogBundle.message(key));
                }
                return component;
            }
        });

        // 创建提交消息提示词容器面板
        commitMessagePromptPanel = new JPanel(new BorderLayout());

        // 构建提交消息提示词面板内容
        JPanel commitMessagePromptContent = createCommitMessagePromptContentPanel();
        commitMessagePromptPanel.add(commitMessagePromptContent, BorderLayout.NORTH);
        commitMessagePromptPanel.setVisible(false); // 默认隐藏

        // 创建显示排除模式设置复选框
        showExcludePatternsCheckBox = new JBCheckBox(ChangelogBundle.message("settings.commit.exclude.patterns.show"));

        // 创建排除模式容器面板
        excludePatternsPanel = new JPanel(new BorderLayout());

        // 创建排除模式文本区域
        excludePatternsTextArea = new JBTextArea(15, 50);
        excludePatternsTextArea.setLineWrap(true);
        excludePatternsTextArea.setWrapStyleWord(true);
        excludePatternsTextArea.setToolTipText(ChangelogBundle.message("settings.commit.exclude.patterns.tooltip"));

        // 构建排除模式面板内容
        JPanel excludePatternsContent = createExcludePatternsContentPanel();
        excludePatternsPanel.add(excludePatternsContent, BorderLayout.NORTH);
        excludePatternsPanel.setVisible(false); // 默认隐藏

        // 初始化反馈面板
        FeedbackPanel feedbackPanel = new FeedbackPanel(
            null, // 应用级设置，project 为 null
            PluginContents.PLUGIN_ID, // 插件 ID
            PluginContents.PLUGIN_NAME, // 插件名称
            "zeka-stack-changelog-plugin" // 签名密钥
        );

        // 构建主面板
        mainPanel = FormBuilder.createFormBuilder()
            // 第一组：AI 提供商选择
            .addComponent(aiProviderSelectionPanel.getPanel())
            .addSeparator(10)

            // 第二组：Release Log 配置（带边框的独立面板）
            .addComponent(createReleaseLogPanel())
            .addSeparator(10)

            // 第三组：提交消息设置
            .addComponent(createCommitMessagePanel())
            .addSeparator(10)

            // 第四组：变更日志设置
            .addComponent(createChangelogSettingsPanel())
            .addSeparator(10)

            // 填充垂直空间，使反馈面板固定在底部
            .addComponentFillVertically(new JPanel(), 0)

            // 第三组：反馈面板（固定在底部）
            .addComponent(feedbackPanel.getContent())
            .getPanel();

        // 设置边框
        mainPanel.setBorder(JBUI.Borders.empty(10));

        // 设置高级设置复选框的监听器
        setupListeners();

        // 初始化时更新 AI 单选按钮文本
        SwingUtilities.invokeLater(this::updateAiRadioButtonText);
    }

    /**
     * 判断当前设置是否与给定的设置状态发生修改
     * <p>
     * 比较当前界面中的系统提示, 变更日志模板, 日报模板, 周报模板以及高级设置显示状态
     * 是否与传入的设置状态对象不同, 若不同则返回 true, 表示设置已修改.
     * 同时检查 AI 提供者下拉框是否为空或不可用, 若为空或不可用则返回 false.
     * 如果选中的 AI 配置不为空且与 providerSettings 不一致, 则返回 true.
     *
     * @param settings         当前设置状态对象
     * @param providerSettings AI 提供者配置对象
     * @return 如果设置已修改, 返回 true; 否则返回 false
     */
    public boolean isModified(SettingsState settings, AIProviderConfig providerSettings) {
        if (!systemPromptTextArea.getText().equals(settings.systemPrompt)
            || !changelogTemplateTextArea.getText().equals(settings.changelogTemplate)
            || !dailyReportTemplateTextArea.getText().equals(settings.dailyReportTemplate)
            || !weeklyReportTemplateTextArea.getText().equals(settings.weeklyReportTemplate)
            || !commitMessageTemplateTextArea.getText().equals(settings.commitMessageTemplate)
            || !commitMessageSystemPromptTextArea.getText().equals(settings.commitMessageSystemPrompt)
            || useCommitMessageInputAsContextCheckBox.isSelected() != settings.useCommitMessageInputAsContext
            || commitMessageDiffProviderComboBox.getSelectedItem() != settings.commitMessageDiffProvider
            || showCommitMessagePromptCheckBox.isSelected() != settings.showCommitMessagePrompt
            || enableCommitMultiRepoCheckBox.isSelected() != settings.enableCommitMultiRepoCheck
            || showAdvancedSettingsCheckBox.isSelected() != settings.showPromptSettings
            || releaseLogByGitCliffRadioButton.isSelected() != (settings.releaseLog == ReleaseLogProvider.GIT_CLIFF)
            || useTagAsStartRadioButton.isSelected() != settings.useTagAsStart
            || showGitCliffConfigCheckBox.isSelected() != settings.showGitCliffConfig
            || !gitCliffConfigTextArea.getText().equals(settings.gitCliffConfig)
            || showAiReleaseLogPromptCheckBox.isSelected() != settings.showAiReleaseLogPrompt
            || !aiReleaseLogPromptTextArea.getText().equals(settings.aiReleaseLogPrompt)
            || !getExcludePatternsFromTextArea().equals(settings.excludePatterns)) {
            return true;
        }
        AIProviderConfig selectedConfig = aiProviderSelectionPanel != null ? aiProviderSelectionPanel.getSelectedProvider() : null;
        if (selectedConfig == null) {
            return providerSettings != null;
        }
        if (providerSettings == null) {
            return true;
        }
        return !providerSettings.contentEquals(selectedConfig);
    }

    /**
     * 将界面中的设置项应用到给定的 SettingsState 对象中
     * <p>
     * 该方法从文本框和复选框中读取当前用户界面的设置值, 并将其赋值给 SettingsState 对象的相应属性.
     *
     * @param settings 要应用设置的 SettingsState 对象
     */
    public void apply(SettingsState settings) {
        settings.systemPrompt = systemPromptTextArea.getText();
        settings.changelogTemplate = changelogTemplateTextArea.getText();
        settings.dailyReportTemplate = dailyReportTemplateTextArea.getText();
        settings.weeklyReportTemplate = weeklyReportTemplateTextArea.getText();
        settings.commitMessageTemplate = commitMessageTemplateTextArea.getText();
        settings.commitMessageSystemPrompt = commitMessageSystemPromptTextArea.getText();
        settings.useCommitMessageInputAsContext = useCommitMessageInputAsContextCheckBox.isSelected();
        settings.commitMessageDiffProvider = (SettingsState.CommitMessageDiffProvider) commitMessageDiffProviderComboBox.getSelectedItem();
        settings.showCommitMessagePrompt = showCommitMessagePromptCheckBox.isSelected();
        settings.enableCommitMultiRepoCheck = enableCommitMultiRepoCheckBox.isSelected();
        settings.showPromptSettings = showAdvancedSettingsCheckBox.isSelected();
        settings.releaseLog = releaseLogByGitCliffRadioButton.isSelected() ? ReleaseLogProvider.GIT_CLIFF : ReleaseLogProvider.AI;
        settings.useTagAsStart = useTagAsStartRadioButton.isSelected();
        settings.showGitCliffConfig = showGitCliffConfigCheckBox.isSelected();
        settings.gitCliffConfig = gitCliffConfigTextArea.getText();
        settings.showAiReleaseLogPrompt = showAiReleaseLogPromptCheckBox.isSelected();
        settings.aiReleaseLogPrompt = aiReleaseLogPromptTextArea.getText();
        settings.excludePatterns = getExcludePatternsFromTextArea();
        // 注意：lastUsedTag 和 lastUsedHash 不需要在这里更新，它们应该在使用时更新
        if (aiProviderSelectionPanel != null) {
            AIProviderConfig selectedConfig = aiProviderSelectionPanel.getSelectedProvider();
            if (selectedConfig != null) {
                settings.providerConfig = selectedConfig.copy();
            }
        }
    }

    /**
     * 重置界面设置为指定的配置状态
     * <p>
     * 根据传入的设置状态对象, 将界面中的各个文本区域和控件的值更新为对应的配置值.
     *
     * @param settings 包含配置信息的设置状态对象
     * @throws NullPointerException 如果传入的 settings 为 null, 可能导致空指针异常
     */
    public void reset(SettingsState settings) {
        systemPromptTextArea.setText(settings.systemPrompt);
        changelogTemplateTextArea.setText(settings.changelogTemplate);
        dailyReportTemplateTextArea.setText(settings.dailyReportTemplate);
        weeklyReportTemplateTextArea.setText(settings.weeklyReportTemplate);
        commitMessageTemplateTextArea.setText(settings.commitMessageTemplate);
        commitMessageSystemPromptTextArea.setText(settings.commitMessageSystemPrompt);
        useCommitMessageInputAsContextCheckBox.setSelected(settings.useCommitMessageInputAsContext);
        commitMessageDiffProviderComboBox.setSelectedItem(settings.commitMessageDiffProvider);
        showCommitMessagePromptCheckBox.setSelected(settings.showCommitMessagePrompt);
        enableCommitMultiRepoCheckBox.setSelected(settings.enableCommitMultiRepoCheck);
        commitMessagePromptPanel.setVisible(settings.showCommitMessagePrompt);
        showExcludePatternsCheckBox.setSelected(false); // 不持久化，默认为 false
        excludePatternsPanel.setVisible(false); // 默认隐藏
        List<String> excludePatterns = settings.excludePatterns != null && !settings.excludePatterns.isEmpty()
                                       ? settings.excludePatterns
                                       : SettingsState.getDefaultExcludePatterns();
        excludePatternsTextArea.setText(String.join("\n", excludePatterns));
        showAdvancedSettingsCheckBox.setSelected(settings.showPromptSettings);
        advancedSettingsPanel.setVisible(settings.showPromptSettings);

        // 重置 Release Log 生成方式（默认选择 AI）
        if (settings.releaseLog == ReleaseLogProvider.GIT_CLIFF) {
            releaseLogByGitCliffRadioButton.setSelected(true);
        } else {
            // 默认选择 AI（如果 releaseLog 为 AI 或未设置）
            releaseLogByAiRadioButton.setSelected(true);
        }

        // 更新 git-cliff 配置区域的可用状态
        boolean isGitCliffEnabled = releaseLogByGitCliffRadioButton.isSelected();
        updateGitCliffConfigAvailability(isGitCliffEnabled);
        updateAiReleaseLogPromptAvailability(!isGitCliffEnabled);

        // 重置显示 git-cliff 配置复选框
        // 只有在选择 git-cliff 时才显示配置面板
        if (isGitCliffEnabled) {
            showGitCliffConfigCheckBox.setSelected(settings.showGitCliffConfig);
            gitCliffConfigPanel.setVisible(settings.showGitCliffConfig);
            // 选择 git-cliff 时，隐藏 AI 提示词面板
            showAiReleaseLogPromptCheckBox.setSelected(false);
            aiReleaseLogPromptPanel.setVisible(false);
        } else {
            // 选择 AI 时，隐藏 git-cliff 配置面板，显示/隐藏 AI 提示词面板
            showGitCliffConfigCheckBox.setSelected(false);
            gitCliffConfigPanel.setVisible(false);
            showAiReleaseLogPromptCheckBox.setSelected(settings.showAiReleaseLogPrompt);
            aiReleaseLogPromptPanel.setVisible(settings.showAiReleaseLogPrompt);
        }

        // 重置 AI Release Log 提示词文本
        aiReleaseLogPromptTextArea.setText(settings.aiReleaseLogPrompt);

        // 动态更新单选框文本，将最近使用的 tag/hash 拼接到描述中
        updateRadioButtonTexts(settings);

        // 更新 git-cliff 单选框文本，添加版本号
        updateGitCliffRadioButtonText();

        // 重置 Git-cliff 配置文本
        gitCliffConfigTextArea.setText(settings.gitCliffConfig);

        if (settings.useTagAsStart) {
            useTagAsStartRadioButton.setSelected(true);
        } else {
            useHashAsStartRadioButton.setSelected(true);
        }

        if (aiProviderSelectionPanel != null) {
            // 设置选中的提供商配置
            aiProviderSelectionPanel.setSelectedProvider(settings.providerConfig);
            // 更新 AI 单选按钮文本
            updateAiRadioButtonText();
        }
    }

    /**
     * 设置监听器
     * <p>
     * 为高级设置和 Release Log 相关控件添加监听器。
     */
    private void setupListeners() {
        showAdvancedSettingsCheckBox.addActionListener(e -> {
            advancedSettingsPanel.setVisible(showAdvancedSettingsCheckBox.isSelected());
        });

        // Release Log 生成方式切换时，控制配置区可用状态
        releaseLogByAiRadioButton.addActionListener(e -> {
            updateGitCliffConfigAvailability(false);
            updateAiReleaseLogPromptAvailability(true);
            // 当选择 AI 时，隐藏 git-cliff 配置面板，显示/隐藏 AI 提示词面板
            gitCliffConfigPanel.setVisible(false);
            showGitCliffConfigCheckBox.setSelected(false);
            aiReleaseLogPromptPanel.setVisible(showAiReleaseLogPromptCheckBox.isSelected());
            // 隐藏下载进度面板
            gitCliffDownloadProgressBar.setVisible(false);
            gitCliffDownloadStatusLabel.setVisible(false);
            gitCliffDownloadProgressPanel.setVisible(false);
        });
        releaseLogByGitCliffRadioButton.addActionListener(e -> {
            updateGitCliffConfigAvailability(true);
            updateAiReleaseLogPromptAvailability(false);
            // 当选择 git-cliff 时，根据复选框状态显示/隐藏配置面板，隐藏 AI 提示词面板
            gitCliffConfigPanel.setVisible(showGitCliffConfigCheckBox.isSelected());
            aiReleaseLogPromptPanel.setVisible(false);
            showAiReleaseLogPromptCheckBox.setSelected(false);
            // 检查是否需要下载 git-cliff
            checkAndDownloadGitCliff();
        });

        // 显示 Git-cliff 配置复选框控制配置面板的显示/隐藏
        showGitCliffConfigCheckBox.addActionListener(e -> {
            gitCliffConfigPanel.setVisible(showGitCliffConfigCheckBox.isSelected());
        });

        // 显示 AI Release Log 提示词复选框控制提示词面板的显示/隐藏
        showAiReleaseLogPromptCheckBox.addActionListener(e -> {
            aiReleaseLogPromptPanel.setVisible(showAiReleaseLogPromptCheckBox.isSelected());
        });

        // 显示提交消息提示词复选框控制提示词面板的显示/隐藏
        showCommitMessagePromptCheckBox.addActionListener(e -> {
            commitMessagePromptPanel.setVisible(showCommitMessagePromptCheckBox.isSelected());
        });

        // 显示排除模式复选框控制排除模式面板的显示/隐藏
        showExcludePatternsCheckBox.addActionListener(e -> {
            excludePatternsPanel.setVisible(showExcludePatternsCheckBox.isSelected());
        });

        // 监听 AI 提供商选择变化，动态更新 AI 单选按钮文本
        setupAiProviderSelectionListener();
    }

    /**
     * 创建 Release Log 配置面板（带边框）
     *
     * @return 带边框的 Release Log 配置面板
     */
    private JPanel createReleaseLogPanel() {
        // 创建子配置面板
        JPanel subConfigPanel = new JPanel(new BorderLayout());

        JPanel subConfigContent = FormBuilder.createFormBuilder()
            .addComponent(createStartPointPanel())
            .addComponent(createReleaseLogProviderPanel())
            .getPanel();

        subConfigPanel.add(subConfigContent, BorderLayout.CENTER);

        JPanel contentPanel = FormBuilder.createFormBuilder()
            .addComponent(subConfigPanel)
            .getPanel();

        // 创建带边框的面板
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(contentPanel, BorderLayout.CENTER);

        // 添加带标题的边框
        TitledBorder titledBorder = BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(),
            ChangelogBundle.message("settings.release.log.title"));
        configureTitledBorder(titledBorder);
        panel.setBorder(titledBorder);

        return panel;
    }

    /**
     * 创建提交消息设置面板（带边框）
     *
     * @return 提交消息设置面板
     */
    private JPanel createCommitMessagePanel() {
        // 创建带缩进的提交消息提示词复选框面板
        JPanel commitMessagePromptCheckBoxPanel = new JPanel(new BorderLayout());
        commitMessagePromptCheckBoxPanel.setBorder(JBUI.Borders.emptyLeft(0));
        commitMessagePromptCheckBoxPanel.add(showCommitMessagePromptCheckBox, BorderLayout.WEST);

        // 创建带缩进的排除模式复选框面板
        JPanel excludePatternsCheckBoxPanel = new JPanel(new BorderLayout());
        excludePatternsCheckBoxPanel.setBorder(JBUI.Borders.emptyLeft(0));
        excludePatternsCheckBoxPanel.add(showExcludePatternsCheckBox, BorderLayout.WEST);

        JPanel contentPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent(ChangelogBundle.message("settings.commit.message.diff.provider.label"),
                                 commitMessageDiffProviderComboBox)
            .addComponent(useCommitMessageInputAsContextCheckBox)
            .addComponent(enableCommitMultiRepoCheckBox)
            .addComponent(commitMessagePromptCheckBoxPanel)
            .addComponent(commitMessagePromptPanel)
            .addComponent(excludePatternsCheckBoxPanel)
            .addComponent(excludePatternsPanel)
            .getPanel();

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(contentPanel, BorderLayout.CENTER);

        TitledBorder titledBorder = BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(),
            ChangelogBundle.message("settings.commit.message.settings.title"));
        configureTitledBorder(titledBorder);
        panel.setBorder(titledBorder);

        return panel;
    }

    /**
     * 创建提交消息提示词内容面板
     * <p>
     * 创建包含提示词 Tab 页的面板。
     *
     * @return 提交消息提示词内容面板
     */
    private JPanel createCommitMessagePromptContentPanel() {
        JPanel contentPanel = FormBuilder.createFormBuilder()
            .addComponent(new JBLabel("  " + ChangelogBundle.message("settings.prompt.commit.message.hint")))
            .addComponent(createCommitMessagePromptTabbedPane())
            .getPanel();

        // 创建带边框的面板
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(contentPanel, BorderLayout.CENTER);

        // 添加无标题的边框
        panel.setBorder(BorderFactory.createEtchedBorder());

        return panel;
    }

    /**
     * 创建提交消息提示词 Tab 页面板
     * <p>
     * 初始化一个包含 System 和 User 选项卡的 JBTabbedPane。
     *
     * @return 包含提交消息提示词配置选项卡的 JBTabbedPane 实例
     */
    private JBTabbedPane createCommitMessagePromptTabbedPane() {
        JBTabbedPane promptTabbedPane = new JBTabbedPane();
        // 设置 Tab 页的尺寸
        promptTabbedPane.setPreferredSize(new Dimension(600, 400));

        // 创建各个 Tab 页
        promptTabbedPane.addTab(ChangelogBundle.message("settings.prompt.tab.system"),
                                createPromptTab(commitMessageSystemPromptTextArea, "commit.message.system"));
        promptTabbedPane.addTab(ChangelogBundle.message("settings.prompt.tab.user"),
                                createPromptTab(commitMessageTemplateTextArea, "commit.message"));

        return promptTabbedPane;
    }

    /**
     * 创建排除模式内容面板
     * <p>
     * 创建包含排除模式文本区域和重置按钮的面板。
     *
     * @return 排除模式内容面板
     */
    private JPanel createExcludePatternsContentPanel() {
        JPanel contentPanel = FormBuilder.createFormBuilder()
            .addComponent(new JBLabel("  " + ChangelogBundle.message("settings.commit.exclude.patterns.hint")))
            .addComponent(createExcludePatternsTab())
            .getPanel();

        // 创建带边框的面板
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(contentPanel, BorderLayout.CENTER);

        // 添加无标题的边框
        panel.setBorder(BorderFactory.createEtchedBorder());

        return panel;
    }

    /**
     * 创建排除模式 Tab 页面板
     * <p>
     * 创建一个包含文本区域和重置按钮的标签页面板。
     *
     * @return 包含文本区域和重置按钮的面板
     */
    private JPanel createExcludePatternsTab() {
        JPanel tabPanel = new JPanel(new BorderLayout());

        // 创建文本区域
        excludePatternsTextArea.setLineWrap(true);
        excludePatternsTextArea.setWrapStyleWord(true);

        // 创建滚动面板，并添加边框以在四周留出空间
        JBScrollPane scrollPane = new JBScrollPane(excludePatternsTextArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        // 添加边框，在四周留出10像素的空间
        scrollPane.setBorder(JBUI.Borders.empty(10));

        tabPanel.add(scrollPane, BorderLayout.CENTER);

        // 创建重置按钮
        JButton resetButton = new JButton(ChangelogBundle.message("settings.commit.exclude.patterns.reset"));
        resetButton.addActionListener(e -> resetExcludePatternsToDefault());
        tabPanel.add(resetButton, BorderLayout.SOUTH);

        return tabPanel;
    }

    /**
     * 重置排除模式为默认值
     * <p>
     * 将排除模式文本区域重置为默认的排除模式列表。
     */
    private void resetExcludePatternsToDefault() {
        List<String> defaultPatterns = SettingsState.getDefaultExcludePatterns();
        excludePatternsTextArea.setText(String.join("\n", defaultPatterns));
    }

    /**
     * 从文本区域获取排除模式列表
     * <p>
     * 将文本区域的内容按行分割，过滤空行，返回排除模式列表。
     *
     * @return 排除模式列表
     */
    private List<String> getExcludePatternsFromTextArea() {
        String text = excludePatternsTextArea.getText();
        if (text == null || text.isBlank()) {
            return SettingsState.getDefaultExcludePatterns();
        }
        return Arrays.stream(text.split("\n"))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toList());
    }

    /**
     * 创建起点选择面板（带边框，无标题）
     * <p>
     * 包含 use tag、use hash 单选按钮和清除按钮。
     *
     * @return 带边框的起点选择面板
     */
    private JPanel createStartPointPanel() {
        // 创建清除按钮和提示文本的面板（同一行显示）
        JPanel clearButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        clearButtonPanel.setBorder(JBUI.Borders.emptyLeft(20)); // 缩进 20 像素
        clearButtonPanel.add(clearTagAndHashButton);

        // 创建提示文本标签
        JBLabel clearButtonHint = new JBLabel(ChangelogBundle.message("settings.gitcliff.clear.tag.and.hash.hint"));
        clearButtonHint.setForeground(UIUtil.getLabelForeground());
        clearButtonHint.setFont(UIManager.getFont("Label.font").deriveFont(UIManager.getFont("Label.font").getSize() - 1f));
        clearButtonPanel.add(clearButtonHint);

        // 创建内容面板，使用 FormBuilder 确保与 provider 面板对齐
        JPanel contentPanel = FormBuilder.createFormBuilder()
            .addComponent(useTagAsStartRadioButton)
            .addComponent(useHashAsStartRadioButton)
            .addComponent(clearButtonPanel)
            .getPanel();

        // 创建带边框的面板（无标题）
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(contentPanel, BorderLayout.CENTER);

        // 添加无标题的边框
        panel.setBorder(BorderFactory.createEtchedBorder());

        return panel;
    }

    private JPanel createReleaseLogProviderPanel() {
        // 创建 AI 单选框的提示标签
        JBLabel aiHintLabel = new JBLabel(ChangelogBundle.message("settings.release.log.generator.ai.hint"));
        aiHintLabel.setForeground(UIUtil.getLabelForeground());
        aiHintLabel.setFont(UIManager.getFont("Label.font").deriveFont(UIManager.getFont("Label.font").getSize() - 1f));
        JPanel aiHintPanel = new JPanel(new BorderLayout());
        aiHintPanel.setBorder(JBUI.Borders.emptyLeft(22)); // 缩进 22 像素，与单选框对齐
        aiHintPanel.add(aiHintLabel, BorderLayout.WEST);

        // 创建带缩进的 AI 提示词复选框面板
        JPanel aiPromptCheckBoxPanel = new JPanel(new BorderLayout());
        aiPromptCheckBoxPanel.setBorder(JBUI.Borders.emptyLeft(22)); // 缩进 22 像素
        aiPromptCheckBoxPanel.add(showAiReleaseLogPromptCheckBox, BorderLayout.WEST);

        // 创建带缩进的 AI 提示词面板容器
        JPanel aiPromptPanelWrapper = new JPanel(new BorderLayout());
        aiPromptPanelWrapper.setBorder(JBUI.Borders.emptyLeft(22)); // 缩进 22 像素
        aiPromptPanelWrapper.add(aiReleaseLogPromptPanel, BorderLayout.CENTER);

        // 创建 git-cliff 单选框的提示标签
        JBLabel gitCliffHintLabel = new JBLabel(ChangelogBundle.message("settings.release.log.generator.gitcliff.hint"));
        gitCliffHintLabel.setForeground(UIUtil.getLabelForeground());
        gitCliffHintLabel.setFont(UIManager.getFont("Label.font").deriveFont(UIManager.getFont("Label.font").getSize() - 1f));
        JPanel gitCliffHintPanel = new JPanel(new BorderLayout());
        gitCliffHintPanel.setBorder(JBUI.Borders.emptyLeft(22)); // 缩进 22 像素，与单选框对齐
        gitCliffHintPanel.add(gitCliffHintLabel, BorderLayout.WEST);

        // 创建带缩进的 git-cliff 配置复选框面板
        JPanel gitCliffConfigCheckBoxPanel = new JPanel(new BorderLayout());
        gitCliffConfigCheckBoxPanel.setBorder(JBUI.Borders.emptyLeft(22)); // 缩进 22 像素
        gitCliffConfigCheckBoxPanel.add(showGitCliffConfigCheckBox, BorderLayout.WEST);

        // 创建带缩进的 git-cliff 配置面板容器
        JPanel gitCliffConfigPanelWrapper = new JPanel(new BorderLayout());
        gitCliffConfigPanelWrapper.setBorder(JBUI.Borders.emptyLeft(22)); // 缩进 22 像素
        gitCliffConfigPanelWrapper.add(gitCliffConfigPanel, BorderLayout.CENTER);

        JPanel contentPanel = FormBuilder.createFormBuilder()
            .addComponent(releaseLogByAiRadioButton)
            // AI 提示信息
            .addComponent(aiHintPanel)
            // 显示 AI Release Log 提示词复选框（由 AI 单选框控制，带缩进）
            .addComponent(aiPromptCheckBoxPanel)
            .addComponent(aiPromptPanelWrapper)
            .addComponent(releaseLogByGitCliffRadioButton)
            // git-cliff 提示信息
            .addComponent(gitCliffHintPanel)
            // 显示 git-cliff 配置复选框（移到 provider 面板中，带缩进）
            .addComponent(gitCliffConfigCheckBoxPanel)
            .addComponent(gitCliffConfigPanelWrapper)
            // 添加下载进度面板
            .addComponent(gitCliffDownloadProgressPanel)
            .getPanel();

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(contentPanel, BorderLayout.CENTER);

        TitledBorder titledBorder = BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(),
            ChangelogBundle.message("settings.release.log.provider.title"));
        configureTitledBorder(titledBorder);
        panel.setBorder(titledBorder);

        return panel;
    }

    /**
     * 更新 git-cliff 单选框文本，添加版本号
     * <p>
     * 如果 git-cliff 已安装，则在单选框文本后添加版本号。
     */
    private void updateGitCliffRadioButtonText() {
        String baseText = ChangelogBundle.message("settings.release.log.generator.gitcliff");
        String version = GitCliffDownloadManager.getInstalledVersion();
        if (version != null && !version.isEmpty()) {
            releaseLogByGitCliffRadioButton.setText(baseText + " (" + version + ")");
        } else {
            releaseLogByGitCliffRadioButton.setText(baseText);
        }
    }

    /**
     * 更新 AI 单选框文本，添加当前选中的 AI 提供商名称和模型名称
     * <p>
     * 根据当前选中的 AI 提供商，动态更新 AI 单选按钮的显示文本。
     * 格式：AI (提供商名称:模型名称)
     */
    private void updateAiRadioButtonText() {
        String baseText = ChangelogBundle.message("settings.release.log.generator.ai");
        AIProviderConfig selectedProvider = aiProviderSelectionPanel != null
                                            ? aiProviderSelectionPanel.getSelectedProvider()
                                            : null;
        if (selectedProvider != null && selectedProvider.providerType != null) {
            String providerName = selectedProvider.providerType.getDisplayName();
            String modelName = selectedProvider.modelName != null && !selectedProvider.modelName.isEmpty()
                               ? selectedProvider.modelName : "";
            if (!modelName.isEmpty()) {
                releaseLogByAiRadioButton.setText(baseText + " (" + providerName + ":" + modelName + ")");
            } else {
                releaseLogByAiRadioButton.setText(baseText + " (" + providerName + ")");
            }
        } else {
            releaseLogByAiRadioButton.setText(baseText);
        }
    }

    /**
     * 设置 AI 提供商选择监听器
     * <p>
     * 直接使用 AIProviderSelectionPanel 的 providerComboBox 字段添加监听器，动态更新 AI 单选按钮文本。
     */
    private void setupAiProviderSelectionListener() {
        if (aiProviderSelectionPanel == null) {
            return;
        }

        // 直接使用 public 字段添加监听器
        JComboBox<AIProviderConfig> comboBox = aiProviderSelectionPanel.providerComboBox;
        if (comboBox != null) {
            comboBox.addItemListener(e -> {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    updateAiRadioButtonText();
                }
            });
            // 初始化时也更新一次
            SwingUtilities.invokeLater(this::updateAiRadioButtonText);
        }
    }

    /**
     * 处理 git-cliff 安装成功后的 UI 更新
     * <p>
     * 隐藏进度条，更新单选框文本显示版本号。
     */
    private void handleGitCliffInstallSuccess() {
        String finalVersion = GitCliffDownloadManager.getInstalledVersion();
        SwingUtilities.invokeLater(() -> {
            gitCliffDownloadProgressBar.setVisible(false);
            gitCliffDownloadStatusLabel.setVisible(false);
            gitCliffDownloadProgressPanel.setVisible(false);
            isDownloadingGitCliff = false;
            // 更新单选框文本，添加版本号
            if (finalVersion != null && !finalVersion.isEmpty()) {
                String baseText = ChangelogBundle.message("settings.release.log.generator.gitcliff");
                releaseLogByGitCliffRadioButton.setText(baseText + " (" + finalVersion + ")");
            } else {
                updateGitCliffRadioButtonText();
            }
        });
    }

    /**
     * 更新单选框文本
     * <p>
     * 根据设置中的 tag/hash 值动态更新单选框的描述文本。
     * 如果有值就显示，没有就不显示。
     *
     * @param settings 设置状态对象
     */
    private void updateRadioButtonTexts(SettingsState settings) {
        String baseTagText = ChangelogBundle.message("settings.gitcliff.use.tag");
        String tagValue = settings.lastUsedTag != null && !settings.lastUsedTag.isEmpty()
                          ? settings.lastUsedTag : null;
        if (tagValue != null) {
            useTagAsStartRadioButton.setText(baseTagText + " (" + tagValue + ")");
        } else {
            useTagAsStartRadioButton.setText(baseTagText);
        }

        String baseHashText = ChangelogBundle.message("settings.gitcliff.use.hash");
        String hashValue = settings.lastUsedHash != null && !settings.lastUsedHash.isEmpty()
                           ? settings.lastUsedHash : null;
        if (hashValue != null) {
            useHashAsStartRadioButton.setText(baseHashText + " (" + hashValue + ")");
        } else {
            useHashAsStartRadioButton.setText(baseHashText);
        }
    }

    /**
     * 清除 tag 和 hash
     * <p>
     * 清除设置中的 tag 和 hash 值，并更新单选框文本。
     */
    private void clearTagAndHash() {
        SettingsState settings = SettingsState.getInstance();
        settings.lastUsedTag = "";
        settings.lastUsedHash = "";
        updateRadioButtonTexts(settings);
    }

    private void updateGitCliffConfigAvailability(boolean enabled) {
        // 控制 git-cliff 相关配置的可用状态
        // 注意：use tag 和 use hash 不再受此方法控制，它们始终可用
        gitCliffConfigTextArea.setEnabled(enabled);
        showGitCliffConfigCheckBox.setEnabled(enabled);
        if (!enabled) {
            gitCliffConfigPanel.setVisible(false);
            showGitCliffConfigCheckBox.setSelected(false);
        }
        if (resetGitCliffConfigButton != null) {
            resetGitCliffConfigButton.setEnabled(enabled);
        }
    }

    private void updateAiReleaseLogPromptAvailability(boolean enabled) {
        // 控制 AI Release Log 提示词相关配置的可用状态
        aiReleaseLogPromptTextArea.setEnabled(enabled);
        showAiReleaseLogPromptCheckBox.setEnabled(enabled);
        if (!enabled) {
            aiReleaseLogPromptPanel.setVisible(false);
            showAiReleaseLogPromptCheckBox.setSelected(false);
        }
        if (resetAiReleaseLogPromptButton != null) {
            resetAiReleaseLogPromptButton.setEnabled(enabled);
        }
    }

    /**
     * 创建 Git-cliff 配置内容面板
     * <p>
     * 创建包含配置文本区域和重置按钮的面板。
     *
     * @return Git-cliff 配置内容面板
     */
    private JPanel createGitCliffConfigContentPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // 创建文本区域（参考提示词的实现）
        gitCliffConfigTextArea.setLineWrap(true);
        gitCliffConfigTextArea.setWrapStyleWord(true);

        // 创建滚动面板，并添加边框以在四周留出空间（参考提示词的实现）
        JBScrollPane scrollPane = new JBScrollPane(gitCliffConfigTextArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        // 添加边框，在四周留出10像素的空间
        scrollPane.setBorder(JBUI.Borders.empty(10));

        panel.add(scrollPane, BorderLayout.CENTER);

        // 创建重置按钮
        resetGitCliffConfigButton = new JButton(ChangelogBundle.message("settings.gitcliff.config.reset"));
        resetGitCliffConfigButton.addActionListener(e -> resetGitCliffConfig());
        resetGitCliffConfigButton.setEnabled(false); // 默认禁用
        panel.add(resetGitCliffConfigButton, BorderLayout.SOUTH);

        return panel;
    }

    /**
     * 重置 Git-cliff 配置为默认值
     * <p>
     * 将 Git-cliff 配置文本区域重置为默认配置。
     */
    private void resetGitCliffConfig() {
        gitCliffConfigTextArea.setText(SettingsState.getDefaultGitCliffConfig());
    }

    /**
     * 创建 AI Release Log 提示词内容面板
     * <p>
     * 创建包含提示词文本区域和重置按钮的面板。
     *
     * @return AI Release Log 提示词内容面板
     */
    private JPanel createAiReleaseLogPromptContentPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // 创建文本区域（参考提示词的实现）
        aiReleaseLogPromptTextArea.setLineWrap(true);
        aiReleaseLogPromptTextArea.setWrapStyleWord(true);

        // 创建滚动面板，并添加边框以在四周留出空间（参考提示词的实现）
        JBScrollPane scrollPane = new JBScrollPane(aiReleaseLogPromptTextArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        // 添加边框，在四周留出10像素的空间
        scrollPane.setBorder(JBUI.Borders.empty(10));

        panel.add(scrollPane, BorderLayout.CENTER);

        // 创建重置按钮
        resetAiReleaseLogPromptButton = new JButton(ChangelogBundle.message("settings.ai.release.log.prompt.reset"));
        resetAiReleaseLogPromptButton.addActionListener(e -> resetAiReleaseLogPrompt());
        resetAiReleaseLogPromptButton.setEnabled(false); // 默认禁用
        panel.add(resetAiReleaseLogPromptButton, BorderLayout.SOUTH);

        return panel;
    }

    /**
     * 重置 AI Release Log 提示词为默认值
     * <p>
     * 将 AI Release Log 提示词文本区域重置为默认提示词。
     */
    private void resetAiReleaseLogPrompt() {
        aiReleaseLogPromptTextArea.setText(SettingsState.getDefaultReleaseLogUserPrompt());
    }

    /**
     * 检查并下载 git-cliff
     * <p>
     * 如果 git-cliff 未安装，则先检查本地压缩包，如果存在则解压安装，
     * 如果解压安装失败或不存在压缩包才从网络下载。
     */
    private void checkAndDownloadGitCliff() {
        // 如果已安装，直接返回
        if (GitCliffDownloadManager.isInstalled()) {
            return;
        }

        // 如果正在下载，不重复触发
        if (isDownloadingGitCliff) {
            return;
        }

        // 显示下载进度面板
        gitCliffDownloadProgressBar.setVisible(true);
        gitCliffDownloadProgressBar.setIndeterminate(true);
        gitCliffDownloadProgressBar.setValue(0);
        gitCliffDownloadStatusLabel.setVisible(true);
        gitCliffDownloadStatusLabel.setText(ChangelogBundle.message("settings.gitcliff.download.starting"));
        gitCliffDownloadProgressPanel.setVisible(true);
        // 强制刷新 UI
        gitCliffDownloadProgressPanel.revalidate();
        gitCliffDownloadProgressPanel.repaint();

        isDownloadingGitCliff = true;

        // 在后台线程执行安装
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            ProgressIndicator indicator = new EmptyProgressIndicator();
            try {
                // 1. 先检查本地压缩包
                Path localPackage = GitCliffDownloadManager.findLocalPackage();
                if (localPackage != null) {
                    // 尝试从本地压缩包安装
                    SwingUtilities.invokeLater(() -> {
                        gitCliffDownloadStatusLabel.setText(ChangelogBundle.message("settings.gitcliff.download.installing.local"));
                    });
                    try {
                        GitCliffDownloadManager.installFromLocalPackage(localPackage, indicator);
                        // 本地安装成功
                        handleGitCliffInstallSuccess();
                        return; // 成功安装，直接返回
                    } catch (Exception ignored) {
                    }
                }

                // 2. 本地压缩包不存在或安装失败，从网络下载
                SwingUtilities.invokeLater(() -> {
                    gitCliffDownloadStatusLabel.setText(ChangelogBundle.message("settings.gitcliff.download.downloading"));
                });
                GitCliffDownloadManager.downloadAndInstall(
                    indicator,
                    (downloaded, total) -> {
                        // 更新进度条
                        SwingUtilities.invokeLater(() -> {
                            // 确保进度条和状态标签可见
                            if (!gitCliffDownloadProgressBar.isVisible()) {
                                gitCliffDownloadProgressBar.setVisible(true);
                            }
                            if (!gitCliffDownloadStatusLabel.isVisible()) {
                                gitCliffDownloadStatusLabel.setVisible(true);
                            }
                            if (!gitCliffDownloadProgressPanel.isVisible()) {
                                gitCliffDownloadProgressPanel.setVisible(true);
                            }

                            if (total > 0) {
                                int percent = (int) Math.min(100, Math.round(downloaded * 100.0 / total));
                                gitCliffDownloadProgressBar.setIndeterminate(false);
                                gitCliffDownloadProgressBar.setValue(percent);
                                gitCliffDownloadStatusLabel.setText(
                                    ChangelogBundle.message("settings.gitcliff.download.progress", percent));
                            } else {
                                gitCliffDownloadStatusLabel.setText(
                                    ChangelogBundle.message("settings.gitcliff.download.downloading"));
                            }
                            // 强制刷新
                            gitCliffDownloadProgressPanel.revalidate();
                            gitCliffDownloadProgressPanel.repaint();
                        });
                    }
                                                          );

                // 下载成功
                handleGitCliffInstallSuccess();
            } catch (Exception e) {
                // 下载失败
                SwingUtilities.invokeLater(() -> {
                    gitCliffDownloadProgressBar.setVisible(false);
                    gitCliffDownloadStatusLabel.setVisible(false);
                    gitCliffDownloadProgressPanel.setVisible(false);
                    isDownloadingGitCliff = false;
                    JOptionPane.showMessageDialog(
                        mainPanel,
                        ChangelogBundle.message("settings.gitcliff.download.failed", e.getMessage()),
                        ChangelogBundle.message("settings.gitcliff.download.title"),
                        JOptionPane.ERROR_MESSAGE);
                });
            }
        });
    }

    /**
     * 配置 TitledBorder 的字体和颜色
     * <p>
     * 显式设置字体和颜色，确保在 2025 版本中正常显示。
     * 使用 UIUtil 获取主题感知的文本颜色，自动适配浅色和深色主题。
     *
     * @param titledBorder 要配置的 TitledBorder
     */
    private void configureTitledBorder(@NotNull TitledBorder titledBorder) {
        titledBorder.setTitleFont(UIManager.getFont("Label.font"));
        Color titleColor = UIUtil.getLabelForeground();
        titledBorder.setTitleColor(titleColor);
    }

    /**
     * 创建提示词模板面板
     * <p>
     * 创建一个包含提示词模板 Tab 页的面板，并添加边框。
     *
     * @return 提示词模板面板
     */
    private JPanel createPromptTemplatesPanel() {
        JPanel contentPanel = FormBuilder.createFormBuilder()
            .addComponent(new JBLabel("  " + ChangelogBundle.message("settings.prompt.hint")))
            .addComponent(createPromptTabbedPane())
            .getPanel();

        // 创建带边框的面板
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(contentPanel, BorderLayout.CENTER);

        // 添加无标题的边框
        panel.setBorder(BorderFactory.createEtchedBorder());

        return panel;
    }

    /**
     * 创建变更日志设置面板
     * <p>
     * 包含显示提示词设置的复选框和提示词模板面板。
     *
     * @return 变更日志设置面板
     */
    private JPanel createChangelogSettingsPanel() {
        JPanel contentPanel = FormBuilder.createFormBuilder()
            .addComponent(showAdvancedSettingsCheckBox)
            .addComponent(advancedSettingsPanel)
            .getPanel();

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(contentPanel, BorderLayout.CENTER);

        TitledBorder titledBorder = BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(),
            ChangelogBundle.message("settings.changelog.settings.title"));
        configureTitledBorder(titledBorder);
        panel.setBorder(titledBorder);

        return panel;
    }

    /**
     * 创建提示词模板 Tab 页面板
     * <p>
     * 初始化一个包含多个提示配置选项卡的 JBTabbedPane，每个选项卡对应不同的提示类型。
     *
     * @return 包含提示配置选项卡的 JBTabbedPane 实例
     */
    private JBTabbedPane createPromptTabbedPane() {
        JBTabbedPane promptTabbedPane = new JBTabbedPane();
        // 设置 Tab 页的尺寸
        promptTabbedPane.setPreferredSize(new Dimension(600, 400));

        // 创建各个 Tab 页（已移除 Commit Message Template）
        promptTabbedPane.addTab(ChangelogBundle.message("settings.prompt.tab.system"), createPromptTab(systemPromptTextArea, "system"));
        promptTabbedPane.addTab(ChangelogBundle.message("settings.prompt.tab.changelog"), createPromptTab(changelogTemplateTextArea,
                                                                                                          "changelog"));
        promptTabbedPane.addTab(ChangelogBundle.message("settings.prompt.tab.daily.report"), createPromptTab(dailyReportTemplateTextArea,
                                                                                                             "daily.report"));
        promptTabbedPane.addTab(ChangelogBundle.message("settings.prompt.tab.weekly.report"),
                                createPromptTab(weeklyReportTemplateTextArea, "weekly.report"));

        return promptTabbedPane;
    }

    /**
     * 创建提示信息标签页面板
     * <p>
     * 根据给定的文本区域和提示类型，创建一个包含文本区域和重置按钮的标签页面板。
     *
     * @param textArea   文本区域组件
     * @param promptType 提示类型，用于加载对应的提示信息和资源
     * @return 包含文本区域和重置按钮的面板
     */
    private JPanel createPromptTab(JBTextArea textArea, String promptType) {
        JPanel tabPanel = new JPanel(new BorderLayout());

        // 创建文本区域
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setToolTipText(ChangelogBundle.message("settings.prompt." + promptType + ".tooltip"));

        // 添加文档监听器，根据内容自动调整大小
        textArea.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            /**
             * 处理文档事件, 调整文本区域大小
             * <p>
             * 当文档事件发生时, 调用 adjustTextAreaSize 方法调整文本区域的大小
             *
             * @param e 文档事件对象
             */
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                adjustTextAreaSize(textArea);
            }

            /**
             * 文档更新时触发的回调方法
             * <p>
             * 当文本域的内容发生变化时, 该方法会被调用, 并通过 {@link JTextArea} 方法自动调整文本域的尺寸, 以适应新的内容.
             *
             * @param e 文档事件, 包含了更新的具体信息
             */
            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                adjustTextAreaSize(textArea);
            }

            /**
             * 处理文档内容变更事件
             * <p>
             * 当文档内容发生变更时调用此方法, 用于调整文本区域的大小以适应内容变化.
             *
             * @param e 文档事件对象, 包含变更相关信息
             * @since 1.0
             */
            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                adjustTextAreaSize(textArea);
            }
        });

        // 创建滚动面板，并添加边框以在四周留出空间
        JBScrollPane scrollPane = new JBScrollPane(textArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        // 添加边框，在四周留出10像素的空间
        scrollPane.setBorder(JBUI.Borders.empty(10));

        tabPanel.add(scrollPane, BorderLayout.CENTER);

        // 创建重置按钮
        JButton resetButton = new JButton(ChangelogBundle.message("settings.prompt.reset"));
        resetButton.addActionListener(e -> resetPromptToDefault(promptType, textArea));
        tabPanel.add(resetButton, BorderLayout.SOUTH);

        // 初始化时根据内容调整大小
        SwingUtilities.invokeLater(() -> adjustTextAreaSize(textArea));

        return tabPanel;
    }

    /**
     * 根据文本内容自动调整文本区域的大小
     * <p>
     * 该方法会根据文本内容的行数自动调整文本区域的行数，但会设置最小和最大行数限制。
     * 最小行数：15行（初始大小）
     * 最大行数：50行（避免占用过多空间）
     *
     * @param textArea 要调整大小的文本区域
     */
    private void adjustTextAreaSize(JBTextArea textArea) {
        SwingUtilities.invokeLater(() -> {
            try {
                // 计算文本的行数
                int lineCount = textArea.getLineCount();

                // 设置最小和最大行数限制
                int minRows = 15;  // 最小行数
                int maxRows = 50;   // 最大行数

                // 计算实际需要的行数（至少显示所有内容，但不超过最大值）
                int rows = Math.max(minRows, Math.min(lineCount, maxRows));

                // 如果行数发生变化，更新文本区域的行数
                if (rows != textArea.getRows()) {
                    textArea.setRows(rows);
                    // 触发父容器重新布局
                    if (textArea.getParent() != null) {
                        textArea.getParent().revalidate();
                        textArea.getParent().repaint();
                    }
                }
            } catch (Exception e) {
                // 忽略异常，避免影响 UI
            }
        });
    }

    /**
     * 重置提示词到默认值
     * <p>
     * 根据提示类型，将文本区域重置为对应的默认值。
     *
     * @param promptType 提示类型
     * @param textArea   文本区域
     */
    private void resetPromptToDefault(String promptType, JBTextArea textArea) {
        switch (promptType) {
            case "system":
                textArea.setText(SettingsState.getDefaultChangelogSystemPrompt());
                break;
            case "changelog":
                textArea.setText(SettingsState.getDefaultChangelogUserPrompt());
                break;
            case "daily.report":
                textArea.setText(SettingsState.getDefaultDailyReportUserPrompt());
                break;
            case "weekly.report":
                textArea.setText(SettingsState.getDefaultWeeklyReportUserPrompt());
                break;
            case "commit.message":
                textArea.setText(SettingsState.getDefaultCommitMessageUserPrompt());
                break;
            case "commit.message.system":
                textArea.setText(SettingsState.getDefaultCommitMessageSystemPrompt());
                break;
        }
        adjustTextAreaSize(textArea);
    }

    /**
     * 释放资源
     * <p>
     * 移除注册的监听器，避免内存泄漏。
     */
    public void dispose() {
        if (aiProviderSelectionPanel != null) {
            aiProviderSelectionPanel.dispose();
        }
    }
}
