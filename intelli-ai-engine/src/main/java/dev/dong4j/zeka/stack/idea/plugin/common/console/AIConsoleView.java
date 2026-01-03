package dev.dong4j.zeka.stack.idea.plugin.common.console;

import com.intellij.execution.filters.HyperlinkInfo;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataSink;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.UiDataProvider;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import com.intellij.ui.content.ContentManagerEvent;
import com.intellij.ui.content.ContentManagerListener;
import com.intellij.util.ui.JBUI;

import org.jetbrains.annotations.NotNull;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import dev.dong4j.zeka.stack.idea.plugin.common.EngineContents;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIConsoleLogger;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderSettings;
import dev.dong4j.zeka.stack.idea.plugin.common.util.AICommonBundle;
import lombok.Getter;


/**
 * AI 控制台视图服务类
 * <p> 用于管理和显示 AI 控制台界面, 支持在 IntelliJ IDEA 的 Problems 工具窗口中集成控制台视图.
 * <p> 该类实现了 Disposable 和 AIConsoleLogger 接口, 提供了初始化, 刷新, 显示控制台等功能.
 * <p> 通过 Problems 工具窗口的标签页展示控制台日志信息, 并支持占位面板和控制台面板之间的切换.
 * <p> 主要功能包括:
 * <ul>
 * <li> 初始化控制台视图 </li>
 * <li> 确保控制台标签页可见 </li>
 * <li> 刷新面板显示状态 </li>
 * <li> 创建和管理控制台面板及占位面板 </li>
 * </ul>
 * <p> 使用示例:
 * <pre>{@code
 * AIConsoleView aiConsoleView = AIConsoleView.getInstance(project);
 * aiConsoleView.initConsole();
 * aiConsoleView.ensureTabVisible();
 * }</pre>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.03
 * @since 1.0.0
 */
@Service(Service.Level.PROJECT)
public final class AIConsoleView implements Disposable, AIConsoleLogger {

    /**
     * Problems 工具窗口 ID(新旧兼容)
     * <p> 用于标识 Problems 工具窗口, 支持新旧版本的兼容性.
     */
    public static final String PROBLEMS_TOOL_WINDOW_ID = "Problems";

    /**
     * Problems 工具窗口旧 ID
     * 用于兼容旧版本 IntelliJ IDEA 的工具窗口标识符
     */
    public static final String PROBLEMS_TOOL_WINDOW_LEGACY_ID = ToolWindowId.PROBLEMS_VIEW;

    /**
     * 控制台 Tab 名称
     * 该名称用于在 Problems 工具窗口中标识控制台标签页
     */
    public static final String CONSOLE_TAB_NAME = EngineContents.PLUGIN_NAME;

    /** 面板切换标识, 用于表示占位面板的卡片名称 */
    private static final String CARD_PLACEHOLDER = "placeholder";
    /**
     * 控制台 Tab 名称
     * 用于标识控制台标签页在 Problems 工具窗口中的显示名称
     */
    private static final String CARD_CONSOLE = "console";

    /** 时间格式:yyyy.MM.dd HH:mm:ss */
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");

    /** 获取 Console 视图实例 */
    @Getter
    private ConsoleView consoleView;

    /**
     * 项目实例
     * <p>
     * 用于访问当前 IntelliJ IDEA 项目环境的项目对象.
     */
    private final Project project;

    /**
     * 根面板 (占位 / 控制台切换)
     * <p>
     * 该字段用于存储根面板, 支持在占位和控制台之间进行切换.
     *
     * @see #ensureRootPanel()
     */
    private JComponent rootPanel;

    /**
     * 根面板布局
     * <p>
     * 该字段用于定义根面板的布局类型, 使用 CardLayout 实现面板之间的切换.
     */
    private CardLayout rootLayout;

    /**
     * Console 面板 (含左侧工具栏)
     * <p>
     * 该字段用于存储包含左侧工具栏的控制台面板组件.
     */
    private JComponent consolePanel;

    /**
     * 占位面板
     * <p>
     * 该面板用于在控制台未启用详细日志记录时显示占位信息.
     */
    private JComponent placeholderPanel;

    /**
     * 控制台内容 Tab
     * <p>
     * 该字段用于存储控制台内容的标签页对象, 通常与 Problems 工具窗口中的控制台标签页相关联.
     * 在插件初始化过程中, 通过 {@link #ensureProblemsToolWindowTab(ToolWindow)} 方法创建并添加该内容标签页.
     * 该字段在后续操作中用于判断控制台标签页是否被选中, 或刷新面板显示状态.
     */
    private Content consoleContent;

    /** Console 面板是否已挂载 */
    private boolean consolePanelAdded = false;

    /** 是否已注册内容监听 */
    private boolean contentListenerRegistered = false;

    /** 是否已显示欢迎信息 */
    private boolean welcomeMessageShow = false;

    /** 是否自动滚动到末尾 */
    @Getter
    private boolean autoScrollToEndEnabled = false;

    /** 用于缓存日志流数据的字符串构建器 */
    private final StringBuilder streamBuffer = new StringBuilder();

    /**
     * 构造函数, 初始化 AI 控制台视图服务
     *
     * @param project 项目实例, 用于访问当前 IntelliJ IDEA 项目环境
     */
    public AIConsoleView(Project project) {
        this.project = project;
    }

    /**
     * 初始化 Console
     *
     * @return 控制台视图实例
     */
    public ConsoleView initConsole() {
        ensureConsolePanel();
        ensureTabVisible();
        showConsolePanel();
        return consoleView;
    }

    /**
     * 确保控制台标签可见
     * <p>
     * 该方法确保在 Problems 工具窗口中显示控制台标签页. 如果工具窗口存在, 则会确保标签页已创建并可见.
     * 此方法通过 IntelliJ IDEA 的 UI 线程调度执行, 确保 UI 操作的安全性.
     * <p>
     * 使用示例:
     * <pre>{@code
     * aiConsoleView.ensureTabVisible();
     * }</pre>
     *
     * @see #ensureProblemsToolWindowTab(ToolWindow)
     * @see #refreshPanelBySettings()
     */
    public void ensureTabVisible() {
        ApplicationManager.getApplication().invokeLater(() -> {
            ensureRootPanel();
            ToolWindow toolWindow = findProblemsToolWindow();
            if (toolWindow != null) {
                ensureProblemsToolWindowTab(toolWindow);
            }
            syncConsoleTogglesFromSettings();
            refreshPanelBySettings();
        });
    }

    /**
     * 显示工具窗口
     * <p>
     * 该方法用于确保控制台面板已正确挂载, 并在找到 Problems 工具窗口后显示控制台标签页.
     * 如果工具窗口不可见, 则调用 show 方法使其可见.
     */
    private void showToolWindow() {
        ApplicationManager.getApplication().invokeLater(() -> {
            ToolWindow toolWindow = findProblemsToolWindow();
            if (toolWindow != null) {
                ensureProblemsToolWindowTab(toolWindow);
                showConsolePanel();
                selectConsoleTab(toolWindow);
                if (!toolWindow.isVisible()) {
                    toolWindow.show(null);
                }
            }
        });
    }

    /**
     * 确保在 Problems 工具窗口中创建控制台标签页
     * <p>
     * 该方法用于检查当前工具窗口是否已包含控制台内容面板, 若不存在则创建并添加.
     * 创建的内容面板包含根面板 (占位 / 控制台切换), 并注册内容监听器以跟踪状态变化.
     *
     * @param toolWindow 目标 Problems 工具窗口实例
     */
    private void ensureProblemsToolWindowTab(@NotNull ToolWindow toolWindow) {
        ensureRootPanel();
        ContentManager contentManager = toolWindow.getContentManager();
        if (consoleContent != null && contentManager.getIndexOfContent(consoleContent) >= 0) {
            return;
        }
        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(rootPanel, CONSOLE_TAB_NAME, false);
        content.setCloseable(true);
        content.setDisposer(() -> consoleContent = null);
        contentManager.addContent(content);
        consoleContent = content;
        registerContentListener(contentManager);
    }

    /**
     * 确保根面板已初始化
     * <p> 如果根面板尚未创建, 则初始化根面板及其布局, 并添加占位面板.
     *
     */
    private void ensureRootPanel() {
        if (rootPanel != null) {
            return;
        }
        rootLayout = new CardLayout();
        rootPanel = new ConsoleRootPanel(project, rootLayout);
        placeholderPanel = buildPlaceholderPanel();
        rootPanel.add(placeholderPanel, CARD_PLACEHOLDER);
        rootLayout.show(rootPanel, CARD_PLACEHOLDER);
    }

    /**
     * 确保控制台面板已创建并添加到根面板中
     * <p>
     * 如果控制台视图 (ConsoleView) 尚未初始化, 则创建一个新的控制台实例和对应的 UI 面板.
     * 之后确保根面板 (rootPanel) 已经存在, 并将控制台面板添加到根面板的布局中, 使用 CARD_CONSOLE 标识卡位切换.
     */
    private void ensureConsolePanel() {
        if (consoleView == null) {
            consoleView = TextConsoleBuilderFactory.getInstance()
                .createBuilder(project)
                .getConsole();
            consolePanel = buildConsolePanel(consoleView);
        }
        ensureRootPanel();
        if (!consolePanelAdded) {
            rootPanel.add(consolePanel, CARD_CONSOLE);
            consolePanelAdded = true;
        }
    }

    /**
     * 显示控制台面板
     * <p> 确保控制台面板已初始化, 并将其设置为当前显示的面板.
     */
    private void showConsolePanel() {
        ensureConsolePanel();
        rootLayout.show(rootPanel, CARD_CONSOLE);
        if (AIProviderSettings.getInstance().verboseLogging) {
            printWelcomeIfNeeded();
        }
    }

    /**
     * 根据设置刷新面板显示
     * <p> 根据 verboseLogging 设置决定显示控制台面板还是占位面板.
     *
     */
    public void refreshPanelBySettings() {
        ensureRootPanel();
        AIProviderSettings settings = AIProviderSettings.getInstance();
        applyWordWrapSetting(settings.autoWordWrap);
        autoScrollToEndEnabled = settings.autoScrollToEnd;
        if (settings.verboseLogging) {
            showConsolePanel();
        } else {
            rootLayout.show(rootPanel, CARD_PLACEHOLDER);
        }
    }

    /**
     * 构建控制台面板
     * <p> 创建包含工具栏和控制台视图的面板组件, 用于显示日志信息和操作按钮.
     *
     * @param console 控制台视图实例, 不能为空
     * @return 构建好的控制台面板组件
     */
    private JComponent buildConsolePanel(@NotNull ConsoleView console) {
        DefaultActionGroup actionGroup = new DefaultActionGroup();
        actionGroup.add(new OpenConsoleSettingsAction());
        actionGroup.addSeparator();
        actionGroup.add(new RefreshLoggingAction());
        actionGroup.add(new StopLoggingAction());
        actionGroup.add(new ToggleWordWrapAction());
        actionGroup.add(new ScrollToEndAction());
        actionGroup.add(new ClearConsoleAction());

        ActionToolbar toolbar = ActionManager.getInstance()
            .createActionToolbar("IntelliAIEngineConsoleToolbar", actionGroup, false);
        toolbar.setTargetComponent(console.getComponent());

        JComponent panel = new ConsolePanel(project);
        panel.add(toolbar.getComponent(), BorderLayout.WEST);
        panel.add(console.getComponent(), BorderLayout.CENTER);
        panel.setPreferredSize(console.getComponent().getPreferredSize());
        return panel;
    }

    /**
     * 构建占位面板
     * <p> 用于在控制台未启用时显示欢迎信息和操作按钮, 包含标题, 描述, 启用按钮, 链接以及插件列表.
     *
     * @return 占位面板组件
     */
    private JComponent buildPlaceholderPanel() {
        JBPanel<JBPanel<?>> panel = new JBPanel<>(new GridBagLayout());
        JBPanel<JBPanel<?>> content = new JBPanel<>();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(JBUI.Borders.empty(24));

        JBLabel titleLabel = new JBLabel(
            AICommonBundle.message("console.placeholder.title"),
            IconLoader.getIcon("/META-INF/pluginIcon.svg", AIConsoleView.class),
            JBLabel.LEFT
        );
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        titleLabel.setHorizontalAlignment(JBLabel.CENTER);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 18f));
        titleLabel.setIconTextGap(10);

        JLabel descriptionLabel = new JBLabel(AICommonBundle.message("console.placeholder.description"));
        descriptionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton enableButton = new JButton(AICommonBundle.message("console.placeholder.enable.button"));
        enableButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        enableButton.addActionListener(event -> enableVerboseLoggingAndShowConsole());

        JLabel moreLink = createLinkLabel(AICommonBundle.message("console.placeholder.learn.more"),
                                          "https://ideaplugin.dong4j.site/engine/landing.html");
        JLabel whatsNewLink = createLinkLabel(AICommonBundle.message("console.placeholder.whats.new"),
                                              "https://ideaplugin.dong4j.site/whatsnew");
        JLabel separator = new JBLabel("|");

        JPanel actionRow = new JBPanel<>(new FlowLayout(FlowLayout.CENTER, 12, 0));
        actionRow.setOpaque(false);
        actionRow.add(enableButton);
        actionRow.add(moreLink);
        actionRow.add(separator);
        actionRow.add(whatsNewLink);

        JPanel pluginsBlock = buildPluginsBlock();

        content.add(titleLabel);
        content.add(Box.createVerticalStrut(8));
        content.add(descriptionLabel);
        content.add(Box.createVerticalStrut(12));
        content.add(actionRow);
        content.add(Box.createVerticalStrut(12));
        content.add(pluginsBlock);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 1;
        constraints.weighty = 1;
        constraints.anchor = GridBagConstraints.CENTER;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = JBUI.emptyInsets();
        panel.add(content, constraints);
        return panel;
    }

    /**
     * 构建插件块面板
     * <p> 创建一个包含插件标题和插件列表的面板, 每个插件都有一个链接指向其详细页面.
     *
     * @return 包含插件信息的面板
     * @since 1.0.0
     */
    private JPanel buildPluginsBlock() {
        JLabel pluginsTitle = new JBLabel(AICommonBundle.message("console.placeholder.plugins.title"));
        pluginsTitle.setHorizontalAlignment(JLabel.LEFT);
        pluginsTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel listPanel = new JBPanel<>();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setOpaque(false);
        listPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        listPanel.add(buildPluginItem("IntelliAI Javadoc", "https://ideaplugin.dong4j.site/javadoc/landing.html"));
        listPanel.add(Box.createVerticalStrut(6));
        listPanel.add(buildPluginItem("IntelliAI Changelog", "https://ideaplugin.dong4j.site/changelog/landing.html"));
        listPanel.add(Box.createVerticalStrut(6));
        listPanel.add(buildPluginItem("IntelliAI Tracer", "https://ideaplugin.dong4j.site/tracer/landing.html"));
        listPanel.add(Box.createVerticalStrut(6));
        listPanel.add(buildPluginItem("IntelliAI Swagger", "https://ideaplugin.dong4j.site/swagger/landing.html"));

        JPanel block = new JBPanel<>();
        block.setLayout(new BoxLayout(block, BoxLayout.Y_AXIS));
        block.setOpaque(false);
        block.setAlignmentX(Component.LEFT_ALIGNMENT);
        block.add(pluginsTitle);
        block.add(Box.createVerticalStrut(6));
        block.add(listPanel);

        JPanel container = new JBPanel<>(new FlowLayout(FlowLayout.CENTER, 0, 0));
        container.setOpaque(false);
        container.add(block);
        return container;
    }

    /**
     * 构建插件信息项(带项目符号和超链接)
     * <p>用于在控制台占位面板中显示插件名称和链接, 通常用于展示 IntelliAI Engine 支持的插件列表.
     * <p>每个插件项包含一个项目符号 (•) 和一个可点击的超链接, 点击后会打开对应插件的网页.
     * <p>使用示例:
     * <pre>{@code
     * JPanel pluginItem = buildPluginItem("IntelliAI Javadoc", "https://ideaplugin.dong4j.site/javadoc/landing.html");
     * }</pre>
     *
     * @param name 插件名称, 不能为空
     * @param url  插件的超链接地址, 不能为空
     * @return 包含项目符号和超链接的面板, 用于在 UI 中展示插件信息
     */
    private JPanel buildPluginItem(@NotNull String name, @NotNull String url) {
        JLabel bullet = new JBLabel("•");
        JLabel link = createLinkLabel(name, url);
        JPanel row = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, 6, 0));
        row.setOpaque(false);
        row.add(bullet);
        row.add(link);
        return row;
    }

    /**
     * 创建一个带有超链接功能的 JLabel
     * <p> 设置文本颜色为蓝色, 并在鼠标点击时打开指定的 URL
     *
     * @param text 显示的文本, 不能为空
     * @param url  超链接指向的 URL, 不能为空
     * @return 返回配置好的 JLabel 实例
     */
    private JLabel createLinkLabel(@NotNull String text, @NotNull String url) {
        JBLabel label = new JBLabel(text);
        label.setForeground(JBColor.namedColor("Link.activeForeground", new JBColor(0x589DF6, 0x589DF6)));
        label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        label.addMouseListener(new java.awt.event.MouseAdapter() {
            /**
             * 处理鼠标单击事件, 打开指定的 URL.
             * <p> 当用户点击组件时, 调用 {@link BrowserUtil#browse(String)} 方法在默认浏览器中打开预设的 URL.
             *
             * @param e 鼠标事件对象, 包含触发事件的相关信息.
             */
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                BrowserUtil.browse(url);
            }
        });
        return label;
    }

    /**
     * 启用详细日志记录并显示控制台
     * <p> 将详细日志记录设置为启用状态, 保存设置, 并显示控制台面板. 同时, 如果需要, 打印欢迎信息.
     *
     */
    void enableVerboseLoggingAndShowConsole() {
        AIProviderSettings settings = AIProviderSettings.getInstance();
        settings.verboseLogging = true;
        ApplicationManager.getApplication().saveSettings();
        showConsolePanel();
        printWelcomeIfNeeded();
    }

    /**
     * 注册内容选择监听器
     * <p> 用于监听控制台内容的选择变化事件, 当当前选中的内容是本插件的控制台面板时,
     * 会调用 refreshPanelBySettings 方法刷新面板状态.
     *
     * @param contentManager 内容管理器实例, 用于添加监听器
     */
    private void registerContentListener(@NotNull ContentManager contentManager) {
        if (contentListenerRegistered) {
            return;
        }
        contentManager.addContentManagerListener(new ContentManagerListener() {
            /**
             * 当内容管理器的选择发生变化时触发此方法
             * <p> 检查事件中的内容是否为控制台内容且已被选中, 如果是则刷新面板设置
             *
             * @param event 内容管理器事件, 表示选择变化的信息, 不能为 null
             */
            @Override
            public void selectionChanged(@NotNull ContentManagerEvent event) {
                if (event.getContent() == consoleContent && event.getContent().isSelected()) {
                    refreshPanelBySettings();
                }
            }
        });
        contentListenerRegistered = true;
    }

    /**
     * 查找 Problems 工具窗口
     * <p> 尝试使用新 ID 查找 Problems 工具窗口, 如果不存在则使用旧 ID 查找.
     *
     * @return Problems 工具窗口实例, 如果未找到则返回 null
     */
    private ToolWindow findProblemsToolWindow() {
        ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
        ToolWindow toolWindow = toolWindowManager.getToolWindow(PROBLEMS_TOOL_WINDOW_ID);
        if (toolWindow != null) {
            return toolWindow;
        }
        return toolWindowManager.getToolWindow(PROBLEMS_TOOL_WINDOW_LEGACY_ID);
    }

    /**
     * 选择控制台标签页
     * <p> 在指定的工具窗口中查找并选中控制台内容标签页, 如果该标签页存在的话.
     *
     * @param toolWindow 工具窗口实例
     */
    private void selectConsoleTab(@NotNull ToolWindow toolWindow) {
        if (consoleContent == null) {
            return;
        }
        ContentManager contentManager = toolWindow.getContentManager();
        if (contentManager.getIndexOfContent(consoleContent) >= 0) {
            contentManager.setSelectedContent(consoleContent);
        }
    }

    /**
     * 判断控制台标签页是否被选中
     * <p> 检查 Problems 工具窗口中的当前选中内容是否为本插件的控制台面板.
     *
     * @return 如果控制台标签页被选中则返回 true, 否则返回 false
     */
    public boolean isConsoleTabSelected() {
        ToolWindow toolWindow = findProblemsToolWindow();
        if (toolWindow == null) {
            return false;
        }
        ContentManager contentManager = toolWindow.getContentManager();
        Content selected = contentManager.getSelectedContent();
        if (selected == null) {
            return false;
        }
        if (consoleContent != null && selected == consoleContent) {
            return true;
        }
        return rootPanel != null && selected.getComponent() == rootPanel;
    }

    /**
     * 获取 AI 控制台视图的单例实例
     *
     * @param project 项目实例, 用于访问当前 IntelliJ IDEA 项目环境
     * @return 返回 AI 控制台视图的实例
     */
    @NotNull
    public static AIConsoleView getInstance(@NotNull Project project) {
        return project.getService(AIConsoleView.class);
    }

    /**
     * 输出普通信息 (带时间戳)
     * <p>
     * 仅在 verboseLogging 启用时输出.
     *
     * @param message 消息内容
     */
    @Override
    public void printWithTimestamp(String message) {
        String timestamp = "[" + TIME_FORMAT.format(new Date()) + "] ";
        printInternal(timestamp + message + "\n", ConsoleViewContentType.NORMAL_OUTPUT);
    }

    /**
     * 输出普通信息 (不带时间戳)
     * <p>
     * 仅在 verboseLogging 启用时输出.
     *
     * @param message 消息内容
     */
    @Override
    public void print(String message) {
        printInternal(message + "\n", ConsoleViewContentType.NORMAL_OUTPUT);
    }

    /**
     * 输出成功信息(绿色, 不带时间戳)
     * <p>
     * 仅在启用了详细日志记录 (verboseLogging) 时输出.
     *
     * @param message 要输出的消息内容
     */
    @Override
    public void printSuccess(String message) {
        printInternal(message + "\n", ConsoleViewContentType.LOG_INFO_OUTPUT);
    }

    /**
     * 输出警告信息 (黄色, 不带时间戳)
     * <p>
     * 仅在 verboseLogging 启用时输出.
     *
     * @param message 要输出的警告消息内容
     */
    @Override
    public void printWarning(String message) {
        printInternal(message + "\n", ConsoleViewContentType.LOG_WARNING_OUTPUT);
    }

    /**
     * 输出错误信息 (红色, 不带时间戳)
     * <p>
     * 仅在 verboseLogging 启用时输出.
     *
     * @param message 错误消息内容
     */
    @Override
    public void printError(String message) {
        printInternal(message + "\n", ConsoleViewContentType.ERROR_OUTPUT);
    }

    /**
     * 内部输出方法 (实际执行输出操作)
     *
     * @param message     消息内容
     * @param contentType 内容类型
     */
    private void printInternal(String message, @NotNull ConsoleViewContentType contentType) {
        if (message == null || message.trim().isEmpty()) {
            return;
        }
        ApplicationManager.getApplication().invokeLater(() -> {
            ConsoleView console = ensureConsoleView();
            if (console != null) {
                // 首次输出日志时，先显示欢迎信息
                if (!welcomeMessageShow) {
                    printWelcomeMessage();
                    welcomeMessageShow = true;
                }
                console.print(message, contentType);
                scrollToEndIfNeeded();
                showToolWindow();
            }
        });
    }

    /**
     * 输出欢迎信息 (不受 verboseLogging 控制)
     * <p>
     * 用于在控制台初始化时显示欢迎信息和使用说明.
     *
     * @param message 消息内容, 不能为空或空白字符串
     */
    private void printWelcome(String message) {
        if (message == null || message.trim().isEmpty()) {
            return;
        }
        ApplicationManager.getApplication().invokeLater(() -> {
            ConsoleView console = ensureConsoleView();
            if (console != null) {
                console.print(message, ConsoleViewContentType.NORMAL_OUTPUT);
                // 注意：printWelcome 不调用 showToolWindow()，由调用者控制
            }
        });
    }

    /**
     * 输出欢迎信息和使用说明 (不受 verboseLogging 控制)
     * <p>
     * 在首次有日志输出时自动显示 IntelliAI Engine 的欢迎信息, 使用方式和提示.
     * 该方法会在首次调用时显示欢迎信息, 后续调用不会重复显示.
     */
    @SuppressWarnings("DuplicatedCode")
    private void printWelcomeMessage() {
        int width = getConsoleWidthChars();
        int innerWidth = clamp(width - 2, 40, 120);
        int lineWidth = clamp(width, 40, 120);

        String title = AICommonBundle.message("console.welcome.title");
        printWelcome(boxTop(innerWidth));
        printWelcome(boxLine(center(title, innerWidth)));
        printWelcome(boxBottom(innerWidth));
        printWelcome("");
        printWelcome("  " + AICommonBundle.message("console.welcome.description") + "\n");
        printWelcome("");
        printWelcome("  " + AICommonBundle.message("console.welcome.tips.title") + "\n");
        printWelcome("  " + AICommonBundle.message("console.welcome.tips.verbose.logging") + "\n");
        printWelcome("  " + AICommonBundle.message("console.welcome.tips.code.location") + "\n");
        printWelcome(repeat("─", lineWidth) + "\n");
    }

    /**
     * 输出可点击的超链接 (跳转到代码位置)
     * <p>
     * 仅在 verboseLogging 启用时输出.
     *
     * @param message     消息内容
     * @param virtualFile 目标文件
     * @param line        目标行号 (从 0 开始)
     */
    @Override
    public void printHyperlink(String message, @NotNull VirtualFile virtualFile, int line) {
        if (message == null || message.trim().isEmpty()) {
            return;
        }
        ApplicationManager.getApplication().invokeLater(() -> {
            ConsoleView console = ensureConsoleView();
            if (console != null) {
                // 创建超链接信息
                HyperlinkInfo hyperlinkInfo = project1 -> {
                    // 打开文件并跳转到指定行
                    new OpenFileDescriptor(project, virtualFile, line, 0).navigate(true);
                };

                // 输出带超链接的消息
                console.printHyperlink(message, hyperlinkInfo);
                console.print("\n", ConsoleViewContentType.NORMAL_OUTPUT);
                scrollToEndIfNeeded();
                showToolWindow();
            }
        });
    }

    /**
     * 输出带时间戳的可点击超链接 (跳转到代码位置)
     * <p>
     * 仅在 verboseLogging 启用时输出.
     *
     * @param message     消息内容
     * @param virtualFile 目标文件
     * @param line        目标行号 (从 0 开始)
     */
    @Override
    public void printHyperlinkWithTimestamp(String message, @NotNull VirtualFile virtualFile, int line) {
        if (message == null || message.trim().isEmpty()) {
            return;
        }
        ApplicationManager.getApplication().invokeLater(() -> {
            ConsoleView console = ensureConsoleView();
            if (console != null) {
                // 输出时间戳
                String timestamp = "[" + TIME_FORMAT.format(new Date()) + "] ";
                console.print(timestamp, ConsoleViewContentType.NORMAL_OUTPUT);

                // 创建超链接信息
                HyperlinkInfo hyperlinkInfo = project1 -> {
                    // 打开文件并跳转到指定行
                    new OpenFileDescriptor(project, virtualFile, line, 0).navigate(true);
                };

                // 输出带超链接的消息
                console.printHyperlink(message, hyperlinkInfo);
                console.print("\n", ConsoleViewContentType.NORMAL_OUTPUT);
                scrollToEndIfNeeded();
                showToolWindow();
            }
        });
    }

    /**
     * 输出流数据到控制台
     * <p> 将传入的字符串块追加到内部缓冲区, 并在 UI 线程中异步输出到控制台.
     * 如果是首次输出, 会显示欢迎信息.
     *
     * @param chunk 要输出的字符串块, 不能为 null 且不能为空
     */
    @Override
    public void printStream(@NotNull String chunk) {
        if (chunk.isEmpty()) {
            return;
        }
        streamBuffer.append(chunk);
        ApplicationManager.getApplication().invokeLater(() -> {
            ConsoleView console = ensureConsoleView();
            if (console != null) {
                if (!welcomeMessageShow) {
                    printWelcomeMessage();
                    welcomeMessageShow = true;
                }
                console.print(chunk, ConsoleViewContentType.NORMAL_OUTPUT);
                scrollToEndIfNeeded();
                showToolWindow();
            }
        });
    }

    /**
     * 完成流数据输出, 清空缓冲区并显示控制台
     * <p> 该方法用于完成流数据的输出操作, 会清空内部的流缓冲区, 并确保控制台面板可见.
     * 如果尚未显示欢迎信息, 则会显示欢迎信息.
     *
     */
    @Override
    public void completeStream() {
        streamBuffer.setLength(0);
        ApplicationManager.getApplication().invokeLater(() -> {
            ConsoleView console = ensureConsoleView();
            if (console != null) {
                if (!welcomeMessageShow) {
                    printWelcomeMessage();
                    welcomeMessageShow = true;
                }
                console.print("\n", ConsoleViewContentType.NORMAL_OUTPUT);
                scrollToEndIfNeeded();
                showToolWindow();
            }
        });
    }

    /**
     * 输出纯文本流内容
     * <p>
     * 将指定的文本块直接输出到控制台, 不添加任何时间戳或特殊格式.
     * 该方法通常用于输出非结构化或原始数据流, 如 AI 模型的实时响应.
     * <p>
     * 使用示例:
     * <pre>{@code
     * printStreamPlain("模型正在处理请求...");
     * printStreamPlain("进度: 50%");
     * printStreamPlain("处理完成, 结果已返回.");
     * }</pre>
     *
     * @param chunk 要输出的文本块, 不能为空字符串
     */
    @Override
    public void printStreamPlain(@NotNull String chunk) {
        if (chunk.isEmpty()) {
            return;
        }
        ApplicationManager.getApplication().invokeLater(() -> {
            ConsoleView console = ensureConsoleView();
            if (console != null) {
                console.print(chunk, ConsoleViewContentType.NORMAL_OUTPUT);
                scrollToEndIfNeeded();
                showToolWindow();
            }
        });
    }

    /**
     * 完成流式输出 (纯文本模式)
     * <p> 清空内部缓冲区, 并在控制台中输出换行符, 表示一次流式输出结束.
     * <p> 该方法通常用于在日志或消息流结束后进行清理和格式化处理.
     *
     * @see #printStream(String)
     * @see #streamBuffer
     */
    @Override
    public void completeStreamPlain() {
        streamBuffer.setLength(0);
        ApplicationManager.getApplication().invokeLater(() -> {
            ConsoleView console = ensureConsoleView();
            if (console != null) {
                console.print("\n", ConsoleViewContentType.NORMAL_OUTPUT);
                scrollToEndIfNeeded();
                showToolWindow();
            }
        });
    }

    /**
     * 设置是否自动滚动到控制台末尾
     * <p> 该方法用于启用或禁用控制台在输出新内容时自动滚动到末尾的功能.
     * 修改的设置会立即应用并保存到全局配置中.
     *
     * @param enabled 如果为 true, 则启用自动滚动; 否则禁用
     */
    public void setAutoScrollToEndEnabled(boolean enabled) {
        autoScrollToEndEnabled = enabled;
        AIProviderSettings settings = AIProviderSettings.getInstance();
        settings.autoScrollToEnd = enabled;
        ApplicationManager.getApplication().saveSettings();
    }

    /**
     * 滚动到控制台末尾
     * <p> 尝试通过控制台视图的 API 滚动到末尾. 如果直接方法不可用, 则回退到编辑器的滚动模型.
     * <p> 此方法通常用于确保用户看到最新的日志输出.
     * <p> 使用示例:
     * <pre>{@code
     * aiConsoleView.scrollToEnd();
     * }</pre>
     *
     * @see ConsoleView#scrollToEnd()
     */
    public void scrollToEnd() {
        ConsoleView console = getConsoleView();
        if (console == null) {
            return;
        }
        try {
            console.getClass().getMethod("scrollToEnd").invoke(console);
            return;
        } catch (Exception ignored) {
            // fallback to editor scrolling model
        }
        try {
            Object editorObj = console.getClass().getMethod("getEditor").invoke(console);
            if (editorObj != null) {
                Object scrollingModel = editorObj.getClass().getMethod("getScrollingModel").invoke(editorObj);
                if (scrollingModel != null) {
                    scrollingModel.getClass().getMethod("scrollToEnd").invoke(scrollingModel);
                }
            }
        } catch (Exception ignored) {
            // ignore
        }
    }

    /**
     * 设置控制台自动换行功能的启用状态
     * <p> 该方法用于更新控制台的自动换行设置, 并持久化保存到配置中.
     *
     * @param enabled true 表示启用自动换行,false 表示禁用自动换行
     */
    public void setWordWrapEnabled(boolean enabled) {
        applyWordWrapSetting(enabled);
        AIProviderSettings settings = AIProviderSettings.getInstance();
        settings.autoWordWrap = enabled;
        ApplicationManager.getApplication().saveSettings();
    }

    /**
     * 检查控制台编辑器是否启用了软换行
     * <p> 通过获取当前控制台编辑器实例, 检查其编辑器设置中是否启用了软换行功能.
     * 软换行允许长行在可视区域内自动换行, 而不会影响实际文本内容.
     *
     * @return 如果控制台编辑器存在且启用了软换行, 则返回 true, 否则返回 false
     */
    public boolean isWordWrapEnabled() {
        Editor editor = getConsoleEditor();
        return editor != null && editor.getSettings().isUseSoftWraps();
    }

    /**
     * 应用软换行设置
     * <p> 根据给定的布尔值设置控制台编辑器的软换行功能.
     * <p> 如果编辑器实例存在, 则启用或禁用软换行功能.
     *
     * @param enabled 是否启用软换行
     */
    private void applyWordWrapSetting(boolean enabled) {
        Editor editor = getConsoleEditor();
        if (editor != null) {
            editor.getSettings().setUseSoftWraps(enabled);
        }
    }

    /**
     * 获取控制台编辑器实例
     * <p> 通过控制台视图获取对应的编辑器实例. 如果控制台视图不存在或无法获取编辑器, 则返回 null.
     *
     * @return 控制台编辑器实例, 如果无法获取则返回 null
     */
    private Editor getConsoleEditor() {
        ConsoleView console = getConsoleView();
        if (console == null) {
            return null;
        }
        try {
            Object editorObj = console.getClass().getMethod("getEditor").invoke(console);
            if (editorObj instanceof Editor) {
                return (Editor) editorObj;
            }
        } catch (Exception ignored) {
            // ignore
        }
        return null;
    }

    /**
     * 同步控制台设置与界面状态
     * <p> 根据配置文件中的设置更新自动滚动到末尾和自动换行的选项, 确保 UI 状态与配置一致.
     *
     */
    private void syncConsoleTogglesFromSettings() {
        AIProviderSettings settings = AIProviderSettings.getInstance();
        autoScrollToEndEnabled = settings.autoScrollToEnd;
        applyWordWrapSetting(settings.autoWordWrap);
    }

    /**
     * 根据配置自动滚动到控制台末尾
     * <p> 如果自动滚动到末尾功能已启用, 则调用 {@link #scrollToEnd()} 方法滚动到控制台的最后一条消息.
     *
     * @since 1.0.0
     */
    private void scrollToEndIfNeeded() {
        if (autoScrollToEndEnabled) {
            scrollToEnd();
        }
    }

    /**
     * 确保控制台视图已初始化
     * <p> 如果控制台视图尚未创建, 则调用 {@code ensureConsolePanel()} 方法进行初始化.
     *
     * @return 已初始化的 ConsoleView 实例
     */
    private ConsoleView ensureConsoleView() {
        if (consoleView == null) {
            ensureConsolePanel();
        }
        return consoleView;
    }

    private int getConsoleWidthChars() {
        Editor editor = getConsoleEditor();
        if (editor == null) {
            return 80;
        }
        int width = editor.getContentComponent().getWidth();
        if (width <= 0) {
            return 80;
        }
        int charWidth = editor.getContentComponent()
            .getFontMetrics(editor.getContentComponent().getFont())
            .charWidth('W');
        if (charWidth <= 0) {
            return 80;
        }
        return Math.max(40, width / charWidth);
    }

    private String boxTop(int innerWidth) {
        return "╔" + repeat("═", innerWidth) + "╗\n";
    }

    private String boxBottom(int innerWidth) {
        return "╚" + repeat("═", innerWidth) + "╝\n";
    }

    private String boxLine(@NotNull String content) {
        return "║" + content + "║\n";
    }

    private String center(@NotNull String text, int width) {
        if (text.length() >= width) {
            return text.substring(0, width);
        }
        int padding = width - text.length();
        int left = padding / 2;
        int right = padding - left;
        return repeat(" ", left) + text + repeat(" ", right);
    }

    private String repeat(@NotNull String value, int count) {
        if (count <= 0) {
            return "";
        }
        StringBuilder builder = new StringBuilder(count * value.length());
        for (int i = 0; i < count; i++) {
            builder.append(value);
        }
        return builder.toString();
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * 在需要时输出欢迎信息
     * <p>
     * 如果欢迎信息尚未显示, 则调用 {@code printWelcomeMessage} 输出欢迎信息和使用说明.
     * 欢迎信息包括 IntelliAI Engine 的标题, 描述, 使用提示等, 仅在首次调用时显示.
     *
     * @see #printWelcomeMessage()
     */
    private void printWelcomeIfNeeded() {
        if (!welcomeMessageShow) {
            printWelcomeMessage();
            welcomeMessageShow = true;
        }
    }

    /**
     * 清除控制台内容
     * <p> 清空控制台缓冲区, 并重置欢迎信息标志.
     * <p> 如果控制台实例存在, 则清除控制台内容并根据需要重新打印欢迎信息.
     *
     * @since 1.0.0
     */
    public void clearConsole() {
        streamBuffer.setLength(0);
        welcomeMessageShow = false;
        ConsoleView console = getConsoleView();
        if (console != null) {
            console.clear();
            printWelcomeIfNeeded();
        }
    }

    /**
     * 释放资源
     * <p>
     * 由 IntelliJ Platform 在项目关闭时自动调用.
     * 清理 Console 视图资源, 避免内存泄漏.
     *
     * @see Disposable
     */
    @Override
    public void dispose() {
        if (consoleView != null) {
            consoleView.dispose();
            consoleView = null;
        }
        consolePanel = null;
        rootPanel = null;
        rootLayout = null;
        placeholderPanel = null;
        consoleContent = null;
        consolePanelAdded = false;
        contentListenerRegistered = false;
    }

    /**
     * 控制台面板类
     * <p> 用于在 IDE 中展示控制台输出内容, 并提供与项目相关的 UI 数据支持
     * <p> 该类继承自 JBPanel 并实现了 UiDataProvider 接口, 主要用于在 UI 界面中展示控制台信息
     * <p> 通过设置项目引用, 为 UI 数据提供者接口提供数据支持, 实现与项目上下文的绑定
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.03
     * @since 1.0.0
     */
    private static class ConsolePanel extends JBPanel<ConsolePanel> implements UiDataProvider {

        /**
         * 当前项目实例
         *
         * @see Project
         */
        private final Project project;

        /**
         * 构造函数, 初始化控制台面板
         *
         * @param project 项目对象, 不能为 null
         */
        private ConsolePanel(@NotNull Project project) {
            super(new BorderLayout());
            this.project = project;
        }

        /**
         * 提供项目数据快照
         * <p> 将当前项目的引用设置到数据接收器中, 以便在工具窗口或其他组件中使用
         *
         * @param sink 数据接收器, 用于接收项目数据
         */
        @Override
        public void uiDataSnapshot(@NotNull DataSink sink) {
            sink.set(CommonDataKeys.PROJECT, project);
        }
    }

    /**
     * 控制台根面板类
     * <p>作为控制台界面的容器面板, 支持通过 {@link CardLayout} 管理多个子面板, 并实现 UI 数据提供接口以向 IDE 注入上下文数据.
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.03
     * @since 1.0.0
     */
    private static class ConsoleRootPanel extends JBPanel<ConsoleRootPanel> implements UiDataProvider {

        /** 当前项目的引用, 用于提供上下文信息 */
        private final Project project;

        /**
         * 构造函数, 初始化控制台根面板
         * <p> 使用给定的项目和布局来初始化控制台根面板
         *
         * @param project 项目实例, 不能为 null
         * @param layout  布局实例, 不能为 null
         */
        private ConsoleRootPanel(@NotNull Project project, @NotNull CardLayout layout) {
            super(layout);
            this.project = project;
        }

        /**
         * 向数据接收器提供当前面板的上下文数据
         * <p> 该方法用于将当前面板所持有的项目 (Project) 对象注入到指定的数据接收器中, 以供其他组件使用
         *
         * @param sink 数据接收器, 用于接收和存储上下文数据
         */
        @Override
        public void uiDataSnapshot(@NotNull DataSink sink) {
            sink.set(CommonDataKeys.PROJECT, project);
        }
    }
}
