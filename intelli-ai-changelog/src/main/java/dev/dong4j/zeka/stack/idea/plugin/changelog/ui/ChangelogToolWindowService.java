package dev.dong4j.zeka.stack.idea.plugin.changelog.ui;

import com.intellij.icons.AllIcons;
import com.intellij.ide.scratch.ScratchFileService;
import com.intellij.ide.scratch.ScratchRootType;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.ex.ToolWindowEx;
import com.intellij.serviceContainer.NonInjectable;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Rectangle;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import dev.dong4j.zeka.stack.idea.plugin.changelog.PluginContents;
import dev.dong4j.zeka.stack.idea.plugin.changelog.util.ChangelogBundle;
import dev.dong4j.zeka.stack.idea.plugin.changelog.util.NotificationUtil;
import dev.dong4j.zeka.stack.idea.plugin.changelog.util.ToolWindowTitleUtil;
import icons.ChangelogIcons;

/**
 * Changelog 工具窗口服务类
 * <p> 该类提供了对变更日志工具窗口的操作和管理功能, 包括打开会话, 显示结果, 初始化历史内容, 保存历史记录和刷新历史列表等.
 * <p> 通过此服务, 可以方便地管理和查看项目中的变更日志.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.03
 * @since 1.0.0
 */
@Service(Service.Level.PROJECT)
public final class ChangelogToolWindowService {

    /** 历史记录内容页标题 */
    private static final String HISTORY_CONTENT_TITLE = "History";
    /** 历史记录文件标题前缀, 用于标识文件中的标题信息 */
    private static final String HISTORY_FILE_TITLE_PREFIX = "Title: ";
    /** 历史记录存储目录名称 */
    private static final String HISTORY_DIR_NAME = "IntelliAI Changelog";
    /** 历史记录文件的扩展名, 用于标识历史记录文件的格式 */
    private static final String HISTORY_FILE_EXTENSION = ".md";
    /** 删除图标 */
    private static final Icon DELETE_ICON = AllIcons.Actions.Close;
    /** 分类标签背景色 */
    private static final JBColor CATEGORY_BG = new JBColor(new Color(0xE6E6E6), new Color(0x3D3D3D));
    /** 分类标签前景色 */
    private static final JBColor CATEGORY_FG = new JBColor(new Color(0x4A4A4A), new Color(0xC8C8C8));
    /** Frontmatter 日期格式 */
    private static final DateTimeFormatter FRONT_MATTER_DATE_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss");

    /**
     * 根据 actionKey 获取标签图标
     *
     * @param actionKey 国际化 key
     * @return 图标
     */
    private static @NotNull Icon resolveCategoryIcon(@Nullable String actionKey) {
        if (actionKey == null) {
            return ChangelogIcons.CHANGELOG_16;
        }
        return switch (actionKey) {
            case "action.generate.changelog.gitlog" -> ChangelogIcons.LOGS;
            case "action.generate.changelog.diff" -> ChangelogIcons.DIFF;
            case "action.generate.daily.report.gitlog" -> ChangelogIcons.DAILY;
            case "action.generate.weekly.report.gitlog" -> ChangelogIcons.WEEKLY;
            case "action.generate.release.log" -> ChangelogIcons.RELEASE;
            default -> ChangelogIcons.CHANGELOG_16;
        };
    }

    /**
     * 根据 actionKey 获取标签文案
     *
     * @param actionKey 国际化 key
     * @param fallback  兜底文案
     * @return 标签文案
     */
    private static @NotNull String resolveCategoryText(@Nullable String actionKey, @NotNull String fallback) {
        if (actionKey == null) {
            return fallback;
        }
        try {
            return ChangelogBundle.message(actionKey);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    /**
     * 当前项目实例
     * <p> 用于获取项目的相关服务和上下文信息
     *
     * @see Project
     */
    private final Project project;

    /**
     * 存储 History 内容页
     * <p> 用于保存和管理变更日志工具窗口的历史内容页
     *
     * @see Content
     */
    private @Nullable Content historyContent;
    /**
     * 历史记录树模型
     */
    private @Nullable DefaultTreeModel historyTreeModel;
    /**
     * 历史记录树根节点
     */
    private @Nullable DefaultMutableTreeNode historyRootNode;
    /**
     * 历史记录树组件
     */
    private @Nullable Tree historyTree;
    /**
     * 历史记录统计文本
     * <p> 用于展示历史记录数量
     */
    private @Nullable JBLabel historyStatsLabel;
    /**
     * 历史记录过滤输入框
     */
    private @Nullable JBTextField historyFilterField;
    /**
     * 历史记录过滤条件
     */
    private @NotNull String historyFilterText = "";
    /**
     * 历史记录悬停索引
     */
    private int historyHoverRow = -1;

    /**
     * 存储每个输出会话的取消状态
     * <p> 键为输出会话的标题, 值为取消标志
     */
    private static final Map<String, AtomicBoolean> CANCELLATION_FLAGS = new ConcurrentHashMap<>();

    /**
     * 获取 ChangelogToolWindowService 的单例实例
     * <p> 通过指定的项目对象获取 ChangelogToolWindowService 的唯一实例.
     *
     * @param project 项目对象, 不能为 null
     * @return ChangelogToolWindowService 的单例实例, 不能为空
     */
    public static @NotNull ChangelogToolWindowService getInstance(@NotNull Project project) {
        return project.getService(ChangelogToolWindowService.class);
    }

    /**
     * 构造函数, 用于初始化变更日志工具窗口服务
     * <p> 该构造函数被标记为 @NonInjectable, 表示不应通过依赖注入的方式调用.
     *
     * @param project IDEA 项目实例, 用于获取项目的相关服务和上下文信息
     */
    @NonInjectable
    public ChangelogToolWindowService(@NotNull Project project) {
        this.project = project;
    }

    /**
     * 创建一个新的输出会话
     * <p> 该方法用于创建一个带有指定标题的工具窗口输出会话. 如果当前线程不是调度线程, 则在调度线程中创建并返回结果.
     *
     * @param title 输出会话的标题
     * @return 包含 JBTextArea 的 ChangelogOutputSession 对象
     */
    public @NotNull ChangelogOutputSession openSession(@NotNull String title) {
        return openSession(title, "", "", "");
    }

    /**
     * 创建一个新的输出会话
     *
     * @param title      输出标题
     * @param startPoint 起始点
     * @param endPoint   结束点
     * @return 输出会话
     */
    public @NotNull ChangelogOutputSession openSession(@NotNull String title,
                                                       @NotNull String startPoint,
                                                       @NotNull String endPoint) {
        return openSession(title, startPoint, endPoint, "");
    }

    /**
     * 创建一个新的输出会话
     *
     * @param title      输出标题
     * @param startPoint 起始点
     * @param endPoint   结束点
     * @param provider   使用的服务商信息
     * @return 输出会话
     */
    public @NotNull ChangelogOutputSession openSession(@NotNull String title,
                                                       @NotNull String startPoint,
                                                       @NotNull String endPoint,
                                                       @NotNull String provider) {
        if (ApplicationManager.getApplication().isDispatchThread()) {
            return createSession(title, startPoint, endPoint, provider);
        }
        AtomicReference<ChangelogOutputSession> ref = new AtomicReference<>();
        ApplicationManager.getApplication().invokeAndWait(() -> ref.set(createSession(title, startPoint, endPoint, provider)));
        return ref.get();
    }

    /**
     * 直接显示最终结果 (非流式)
     * <p> 该方法通过调用 openSession 创建一个输出会话, 并将指定内容一次性设置为会话的完整输出.
     *
     * @param title   输出标题, 用于标识此次输出会话
     * @param content 要显示的完整内容
     */
    public void showResult(@NotNull String title, @NotNull String content) {
        ChangelogOutputSession session = openSession(title);
        session.complete(content);
    }

    /**
     * 显示 History 页面
     */
    public void showHistoryContent() {
        ApplicationManager.getApplication().invokeLater(() -> {
            ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(PluginContents.PLUGIN_NAME);
            if (toolWindow == null) {
                NotificationUtil.showError(project, ChangelogBundle.message("toolwindow.unavailable"));
                return;
            }
            ensureHistoryContent(toolWindow);
            Content content = findContent(toolWindow, HISTORY_CONTENT_TITLE);
            if (content != null) {
                toolWindow.getContentManager().setSelectedContent(content);
            }
            if (toolWindow instanceof ToolWindowEx toolWindowEx) {
                toolWindowEx.activate(null, true, true);
            } else {
                toolWindow.activate(null, true, true);
            }
        });
    }

    /**
     * 初始化 History 内容页
     * <p> 确保 History 内容页存在并刷新内容. 如果当前线程是事件调度线程, 则直接调用; 否则通过 invokeAndWait 在事件调度线程中执行.
     *
     * @param toolWindow 工具窗口实例, 用于管理内容页
     */
    public void initHistoryContent(@NotNull ToolWindow toolWindow) {
        if (ApplicationManager.getApplication().isDispatchThread()) {
            ensureHistoryContent(toolWindow);
        } else {
            ApplicationManager.getApplication().invokeAndWait(() -> ensureHistoryContent(toolWindow));
        }
    }

    /**
     * 创建一个新的输出会话
     * <p> 该方法用于创建一个带有指定标题的工具窗口输出会话. 如果工具窗口不可用, 则显示错误通知并返回一个无效的输出会话.
     *
     * @param title 输出会话的标题
     * @return 包含 JBTextArea 的 ChangelogOutputSession 对象
     */
    private @NotNull ChangelogOutputSession createSession(@NotNull String title) {
        return createSession(title, "", "", "");
    }

    /**
     * 创建一个新的输出会话
     *
     * @param title      输出会话标题
     * @param startPoint 起始点
     * @param endPoint   结束点
     * @return 输出会话
     */
    private @NotNull ChangelogOutputSession createSession(@NotNull String title,
                                                          @NotNull String startPoint,
                                                          @NotNull String endPoint,
                                                          @NotNull String provider) {
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(PluginContents.PLUGIN_NAME);
        if (toolWindow == null) {
            NotificationUtil.showError(project, ChangelogBundle.message("toolwindow.unavailable"));
            return new ChangelogOutputSession(this, title, startPoint, endPoint, provider, null, null);
        }

        // 首次使用时，动态设置 toolwindow 的布局
        if (toolWindow.getContentManager().getContentCount() == 0) {
            setRightBottomDock(toolWindow);
        }
        ensureHistoryContent(toolWindow);

        JBTextArea textArea = new JBTextArea();
        // 允许用户在工具窗口中编辑内容
        textArea.setEditable(true);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setBackground(JBColor.background());
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, textArea.getFont().getSize()));
        textArea.setBorder(JBUI.Borders.empty(8));

        String fullTitle = buildTitle(title);
        // 为每个会话创建取消标志
        AtomicBoolean cancellationFlag = new AtomicBoolean(false);
        CANCELLATION_FLAGS.put(fullTitle, cancellationFlag);

        JPanel panel = new JPanel(new BorderLayout());
        // ActionToolbar 需要添加其组件实例到容器中
        panel.add(buildToolbar(textArea, fullTitle).getComponent(), BorderLayout.NORTH);
        panel.add(new JBScrollPane(textArea), BorderLayout.CENTER);

        // 创建一个ToolWindow 中的标签页
        Content content = ContentFactory.getInstance().createContent(panel, fullTitle, false);
        content.setCloseable(true);
        // 当内容关闭时，清理取消标志
        content.setDisposer(new Disposable() {
            /**
             * 释放资源并移除指定标题的取消标志
             * <p> 该方法在对象被释放时调用, 用于从全局取消标志集合中移除与当前实例相关联的标题
             *
             * @since 1.0
             */
            @Override
            public void dispose() {
                CANCELLATION_FLAGS.remove(fullTitle);
            }
        });
        toolWindow.getContentManager().addContent(content);
        toolWindow.getContentManager().setSelectedContent(content);

        if (toolWindow instanceof ToolWindowEx toolWindowEx) {
            toolWindowEx.activate(null, true, true);
        } else {
            toolWindow.activate(null, true, true);
        }

        return new ChangelogOutputSession(this, fullTitle, startPoint, endPoint, provider, textArea, cancellationFlag);
    }

    /**
     * 检查指定输出会话是否已被取消
     * <p> 通过会话标题查找对应的取消标志, 判断该会话是否已被请求停止.
     *
     * @param sessionTitle 会话标题, 用于查找对应的取消标志
     * @return 如果会话已被取消返回 true, 否则返回 false
     */
    public static boolean isCancelled(@NotNull String sessionTitle) {
        AtomicBoolean flag = CANCELLATION_FLAGS.get(sessionTitle);
        return flag != null && flag.get();
    }

    /**
     * 停止指定会话的输出
     * <p> 该方法用于标记指定标题的输出会话为已取消, 停止其后续输出操作.
     *
     * @param sessionTitle 输出会话的标题, 用于标识需要停止的会话
     */
    public static void stopOutput(@NotNull String sessionTitle) {
        AtomicBoolean flag = CANCELLATION_FLAGS.get(sessionTitle);
        if (flag != null) {
            flag.set(true);
        }
    }

    /**
     * 构建输出会话的工具栏
     * <p> 为给定的文本区域创建一个包含复制和停止操作的工具栏.
     *
     * @param textArea     文本区域对象, 用于显示输出内容
     * @param sessionTitle 输出会话的标题, 用于标识取消标志
     * @return 包含复制和停止操作的工具栏
     */
    private @NotNull ActionToolbar buildToolbar(@NotNull JBTextArea textArea, @NotNull String sessionTitle) {
        DefaultActionGroup group = new DefaultActionGroup();
        group.add(new CopyAllAction(textArea));
        group.add(new StopOutputAction(sessionTitle));
        ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar("ChangelogToolWindow", group, true);
        toolbar.setTargetComponent(textArea);
        return toolbar;
    }

    /**
     * 构建输出会话的标题
     * <p> 将传入的标题直接返回作为输出会话的标题
     *
     * @param title 输入的标题
     * @return 返回构建后的标题, 即传入的标题
     */
    private @NotNull String buildTitle(@NotNull String title) {
        return title;
    }

    /**
     * 设置工具窗口的右下角布局
     * <p> 尝试调用不同的版本方法以实现将右侧区域拆分为上下两块的效果. 优先使用带有 {@code Runnable} 参数的 {@code setSplitMode} 方法,
     * 如果失败, 则尝试调用不带参数的 {@code setSplitMode} 方法.
     *
     * @param toolWindow 工具窗口实例
     */
    private void setRightBottomDock(@NotNull ToolWindow toolWindow) {
        // 兼容不同版本的 API，优先使用 split mode 将右侧区域拆分为上下两块
        boolean applied = invokeBooleanMethod(toolWindow, "setSplitMode", true, Runnable.class);
        if (!applied) {
            invokeBooleanMethod(toolWindow, "setSplitMode", true);
        }
    }

    /**
     * 尝试调用指定的布尔方法并设置其参数
     * <p> 该方法通过反射机制调用工具窗口对象上的指定布尔方法, 并传递相应的参数. 如果方法调用成功, 则返回 true; 否则返回 false.
     *
     * @param toolWindow 工具窗口对象
     * @param methodName 要调用的方法名
     * @param value      布尔参数值
     * @param extraTypes 额外的参数类型数组
     * @return 如果方法调用成功则返回 true, 否则返回 false
     */
    private boolean invokeBooleanMethod(@NotNull ToolWindow toolWindow,
                                        @NotNull String methodName,
                                        boolean value,
                                        Class<?>... extraTypes) {
        try {
            Class<?>[] paramTypes = new Class<?>[1 + extraTypes.length];
            paramTypes[0] = boolean.class;
            System.arraycopy(extraTypes, 0, paramTypes, 1, extraTypes.length);
            Object[] params = new Object[1 + extraTypes.length];
            params[0] = value;
            for (int i = 0; i < extraTypes.length; i++) {
                params[i + 1] = null;
            }
            toolWindow.getClass().getMethod(methodName, paramTypes).invoke(toolWindow, params);
            return true;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    /**
     * 保存历史记录到 scratch 文件
     * <p> 根据给定的标题和内容创建或更新一个历史记录文件, 并刷新历史记录列表
     * <p> 如果内容为空, 则不会执行任何操作
     *
     * @param title      历史记录的标题
     * @param content    历史记录的内容
     * @param startPoint 起始点
     * @param endPoint   结束点
     * @param provider   使用的服务商信息
     *                <p>
     *                使用示例:
     *                <pre>{@code
     *                               saveHistory("RL:20260103120000",
     *                                          "Added a new feature to the application.",
     *                                          "v1.2.3",
     *                                          "HEAD",
     *                                          "Qianwen(qwen-turbo)");
     *                               }</pre>
     */
    public void saveHistory(@NotNull String title,
                            @NotNull String content,
                            @NotNull String startPoint,
                            @NotNull String endPoint,
                            @NotNull String provider) {
        if (content.isBlank()) {
            return;
        }
        ApplicationManager.getApplication().invokeLater(() -> {
            try {
                VirtualFile file = createHistoryScratchFile(title);
                if (file == null) {
                    return;
                }
                Document document = FileDocumentManager.getInstance().getDocument(file);
                if (document == null) {
                    return;
                }
                String fileContent = buildHistoryFileContent(title, content, startPoint, endPoint, provider);
                ApplicationManager.getApplication().runWriteAction(() -> document.setText(fileContent));
                FileDocumentManager.getInstance().saveDocument(document);
                refreshHistoryList();
            } catch (Exception ignored) {
                // 忽略写入异常，避免影响主流程
            }
        });
    }

    /**
     * 确保历史内容页面存在并刷新
     * <p> 检查工具窗口中是否存在指定标题的内容页. 如果存在但列表模型或列表组件为空, 则移除现有内容并重建历史内容页.
     * 如果不存在, 则直接重建历史内容页.
     * 最后刷新历史列表以确保数据是最新的.
     *
     * @param toolWindow 工具窗口实例
     */
    private void ensureHistoryContent(@NotNull ToolWindow toolWindow) {
        Content existing = findContent(toolWindow, HISTORY_CONTENT_TITLE);
        if (existing != null) {
            if (historyTreeModel == null || historyTree == null || historyRootNode == null) {
                toolWindow.getContentManager().removeContent(existing, false);
                rebuildHistoryContent(toolWindow);
                return;
            }
            historyContent = existing;
            refreshHistoryList();
            return;
        }

        rebuildHistoryContent(toolWindow);
    }

    /**
     * 重建 History 内容页与列表组件
     * <p> 此方法在工具窗口中重新创建 History 内容页及其列表组件. 如果工具窗口没有内容, 则先设置右下角布局.
     * <p> 随后初始化 History 列表模型和列表视图, 并为其添加鼠标点击和键盘按键事件监听器, 以便用户可以通过点击或按下 Enter 键来打开选中的历史记录.
     * <p> 最后, 将新的内容页添加到工具窗口的内容管理器中, 并刷新 History 列表以显示最新的历史记录.
     *
     * @param toolWindow 工具窗口实例
     */
    private void rebuildHistoryContent(@NotNull ToolWindow toolWindow) {
        if (toolWindow.getContentManager().getContentCount() == 0) {
            setRightBottomDock(toolWindow);
        }
        historyRootNode = new DefaultMutableTreeNode("root");
        historyTreeModel = new DefaultTreeModel(historyRootNode);
        historyTree = new Tree(historyTreeModel);
        historyTree.setRootVisible(false);
        historyTree.setShowsRootHandles(true);
        historyTree.setRowHeight(JBUI.scale(30));
        historyTree.setOpaque(true);
        historyTree.setBackground(UIUtil.getTreeBackground());
        historyTree.getEmptyText().setText("暂无历史记录");
        historyTree.setToggleClickCount(0);
        historyTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        historyTree.setCellRenderer(new HistoryTreeCellRenderer());
        historyTree.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if (historyTree == null) {
                    return;
                }
                int row = historyTree.getRowForLocation(e.getX(), e.getY());
                Rectangle bounds = row >= 0 ? historyTree.getRowBounds(row) : null;
                int nextRow = bounds != null && e.getY() >= bounds.y && e.getY() <= bounds.y + bounds.height
                              ? row
                              : -1;
                if (historyHoverRow != nextRow) {
                    historyHoverRow = nextRow;
                    historyTree.repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (historyHoverRow != -1) {
                    historyHoverRow = -1;
                    if (historyTree != null) {
                        historyTree.repaint();
                    }
                }
            }
        });
        historyTree.addMouseListener(new MouseAdapter() {
            /**
             * 处理鼠标点击事件
             * <p> 单击删除区域时删除记录, 其它区域单击打开记录
             *
             * @param e 鼠标事件对象, 不能为 null
             */
            @Override
            public void mouseClicked(MouseEvent e) {
                if (historyTree == null) {
                    return;
                }
                int row = historyTree.getClosestRowForLocation(e.getX(), e.getY());
                if (row < 0) {
                    return;
                }
                Rectangle bounds = historyTree.getRowBounds(row);
                if (bounds == null || e.getY() < bounds.y || e.getY() > bounds.y + bounds.height) {
                    return;
                }
                TreePath path = historyTree.getPathForRow(row);
                if (path == null) {
                    return;
                }
                historyTree.setSelectionPath(path);
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                Object userObject = node.getUserObject();
                if (isDeleteClick(e, row)) {
                    if (userObject instanceof HistoryItem item) {
                        deleteHistoryItem(item);
                    } else if (userObject instanceof HistoryGroup) {
                        deleteHistoryGroup(node);
                    }
                    return;
                }
                if (e.getClickCount() == 2) {
                    if (userObject instanceof HistoryGroup) {
                        if (historyTree.isExpanded(path)) {
                            historyTree.collapsePath(path);
                        } else {
                            historyTree.expandPath(path);
                        }
                        return;
                    }
                    if (userObject instanceof HistoryItem item) {
                        openHistoryItem(item);
                    }
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                showHistoryPopupIfNeeded(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                showHistoryPopupIfNeeded(e);
            }
        });
        historyTree.addKeyListener(new KeyAdapter() {
            /**
             * 处理键盘按键按下事件
             * <p> 回车键打开记录, Delete 键删除记录
             *
             * @param e 键盘事件对象, 包含按键信息
             */
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    HistoryItem selected = getSelectedHistoryItem();
                    if (selected != null) {
                        openHistoryItem(selected);
                    }
                    return;
                }
                if (e.getKeyCode() == KeyEvent.VK_DELETE) {
                    HistoryItem selected = getSelectedHistoryItem();
                    if (selected != null) {
                        deleteHistoryItem(selected);
                    }
                }
            }
        });

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(buildHistoryHeader(historyTree), BorderLayout.NORTH);
        panel.add(new JBScrollPane(historyTree), BorderLayout.CENTER);
        panel.add(buildHistoryFooter(), BorderLayout.SOUTH);

        historyContent = ContentFactory.getInstance().createContent(panel, HISTORY_CONTENT_TITLE, false);
        historyContent.setCloseable(false);
        toolWindow.getContentManager().addContent(historyContent, 0);
        refreshHistoryList();
    }

    /**
     * 构建 History 底部统计栏
     *
     * @return 底部面板
     */
    private @NotNull JComponent buildHistoryFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBorder(JBUI.Borders.empty(4, 8));
        historyStatsLabel = new JBLabel("共 0 条");
        historyStatsLabel.setForeground(JBColor.GRAY);
        footer.add(historyStatsLabel, BorderLayout.WEST);
        return footer;
    }

    /**
     * 查找指定标题的 content
     * <p> 遍历工具窗口的内容管理器, 查找与给定标题匹配的 content 对象.
     *
     * @param toolWindow 工具窗口实例
     * @param title      要查找的 content 标题
     * @return 如果找到匹配的 content 返回该对象, 否则返回 null
     */
    private @Nullable Content findContent(@NotNull ToolWindow toolWindow, @NotNull String title) {
        for (Content content : toolWindow.getContentManager().getContents()) {
            if (title.equals(content.getDisplayName())) {
                return content;
            }
        }
        return null;
    }

    /**
     * 构建历史记录头部
     * <p> 包含工具栏与过滤输入框
     *
     * @param tree 历史记录树组件, 用于设置工具栏的目标组件
     * @return 头部组件
     */
    private @NotNull JComponent buildHistoryHeader(@NotNull Tree tree) {
        DefaultActionGroup group = new DefaultActionGroup();
        group.add(new DeleteAllHistoryAction(tree));
        ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar("ChangelogHistory", group, true);
        toolbar.setTargetComponent(tree);
        JPanel header = new JPanel(new BorderLayout());
        header.add(toolbar.getComponent(), BorderLayout.WEST);
        header.add(buildHistoryFilterField(), BorderLayout.CENTER);
        header.setBorder(JBUI.Borders.empty(4));
        return header;
    }

    /**
     * 构建 History 过滤输入框
     *
     * @return 过滤输入框
     */
    private @NotNull JComponent buildHistoryFilterField() {
        historyFilterField = new JBTextField();
        historyFilterField.getEmptyText().setText("筛选历史记录");
        historyFilterField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateFilterText();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateFilterText();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateFilterText();
            }

            private void updateFilterText() {
                if (historyFilterField == null) {
                    return;
                }
                historyFilterText = historyFilterField.getText();
                refreshHistoryList();
            }
        });
        return historyFilterField;
    }

    /**
     * 判断是否点击了删除区域
     *
     * @param e     鼠标事件
     * @param row 列表行
     * @return 点击在删除区域则返回 true
     */
    private boolean isDeleteClick(@NotNull MouseEvent e, int row) {
        if (historyTree == null) {
            return false;
        }
        Rectangle bounds = historyTree.getRowBounds(row);
        if (bounds == null) {
            return false;
        }
        int iconWidth = DELETE_ICON.getIconWidth();
        int iconHeight = DELETE_ICON.getIconHeight();
        int xStart = bounds.x + JBUI.scale(4);
        int yStart = bounds.y + (bounds.height - iconHeight) / 2;
        boolean inY = e.getY() >= yStart && e.getY() <= yStart + iconHeight;
        boolean inX = e.getX() >= xStart && e.getX() <= xStart + iconWidth;
        return inX && inY;
    }

    /**
     * 删除指定的历史记录项
     *
     * @param item 历史记录项
     */
    private void deleteHistoryItem(@NotNull HistoryItem item) {
        ApplicationManager.getApplication().invokeLater(() -> {
            try {
                ApplicationManager.getApplication().runWriteAction(() -> {
                    try {
                        item.file.delete(this);
                    } catch (Exception ignored) {
                        // 忽略删除异常
                    }
                });
            } finally {
                refreshHistoryList();
            }
        });
    }

    /**
     * 删除指定分组下的所有历史记录
     *
     * @param groupNode 分组节点
     */
    private void deleteHistoryGroup(@NotNull DefaultMutableTreeNode groupNode) {
        ApplicationManager.getApplication().invokeLater(() -> {
            try {
                ApplicationManager.getApplication().runWriteAction(() -> {
                    for (int i = 0; i < groupNode.getChildCount(); i++) {
                        DefaultMutableTreeNode child = (DefaultMutableTreeNode) groupNode.getChildAt(i);
                        Object userObject = child.getUserObject();
                        if (userObject instanceof HistoryItem item) {
                            try {
                                item.file.delete(this);
                            } catch (Exception ignored) {
                                // 忽略删除异常
                            }
                        }
                    }
                });
            } finally {
                refreshHistoryList();
            }
        });
    }

    /**
     * 删除所有历史记录项
     * <p> 此方法会在确认对话框中询问用户是否删除所有历史记录. 如果用户选择确认, 则删除指定目录下的所有历史记录文件, 并刷新历史记录列表.
     * <p> 具体步骤如下:
     * 1. 显示确认对话框, 询问用户是否删除所有历史记录.
     * 2. 如果用户选择取消, 则不执行任何操作.
     * 3. 如果用户选择确认, 则在写入操作中删除历史记录目录下的所有符合条件的文件.
     * 4. 清除树形控件的选中状态, 并刷新历史记录列表以反映最新的状态.
     *
     * @param tree 树形控件对象, 用于显示和管理历史记录项
     */
    private void deleteAllHistoryItems(@NotNull Tree tree) {
        int result = Messages.showYesNoDialog(project,
                                              "确定要删除全部历史记录吗？",
                                              "删除全部历史记录",
                                              Messages.getWarningIcon());
        if (result != Messages.YES) {
            return;
        }
        ApplicationManager.getApplication().invokeLater(() -> {
            VirtualFile historyDir = getHistoryDir();
            if (historyDir == null) {
                return;
            }
            try {
                ApplicationManager.getApplication().runWriteAction(() -> {
                    for (VirtualFile file : historyDir.getChildren()) {
                        if (file.isDirectory() || !file.getName().endsWith(HISTORY_FILE_EXTENSION)) {
                            continue;
                        }
                        try {
                            file.delete(this);
                        } catch (Exception ignored) {
                            // 忽略删除异常
                        }
                    }
                });
            } finally {
                tree.clearSelection();
                refreshHistoryList();
            }
        });
    }

    /**
     * 打开选中的历史记录
     * <p> 从历史记录列表中获取选中的历史记录项, 并加载其内容, 然后在新的输出会话中显示该内容.
     *
     */
    private void openSelectedHistory() {
        HistoryItem selected = getSelectedHistoryItem();
        if (selected == null) {
            return;
        }
        openHistoryItem(selected);
    }

    /**
     * 获取选中的历史记录
     *
     * @return 历史记录项
     */
    private @Nullable HistoryItem getSelectedHistoryItem() {
        if (historyTree == null) {
            return null;
        }
        TreePath path = historyTree.getSelectionPath();
        if (path == null) {
            return null;
        }
        Object userObject = ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();
        if (userObject instanceof HistoryItem item) {
            return item;
        }
        return null;
    }

    /**
     * 打开指定历史记录
     *
     * @param item 历史记录项
     */
    private void openHistoryItem(@NotNull HistoryItem item) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            HistoryContent content = loadHistoryContent(item.file);
            if (content == null || content.body.isBlank()) {
                return;
            }
            ApplicationManager.getApplication().invokeLater(() -> {
                if (selectExistingContent(content.title)) {
                    return;
                }
                ChangelogOutputSession session = openSession(content.title);
                session.setText(content.body);
            });
        });
    }

    /**
     * 显示历史记录右键菜单
     *
     * @param e 鼠标事件
     */
    private void showHistoryPopupIfNeeded(@NotNull MouseEvent e) {
        if (!e.isPopupTrigger() || historyTree == null) {
            return;
        }
        int row = historyTree.getClosestRowForLocation(e.getX(), e.getY());
        if (row < 0) {
            return;
        }
        Rectangle bounds = historyTree.getRowBounds(row);
        if (bounds == null || e.getY() < bounds.y || e.getY() > bounds.y + bounds.height) {
            return;
        }
        TreePath path = historyTree.getPathForRow(row);
        if (path == null) {
            return;
        }
        Object userObject = ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();
        if (!(userObject instanceof HistoryItem item)) {
            return;
        }
        historyTree.setSelectionPath(path);
        JPopupMenu menu = new JPopupMenu();
        JMenuItem openItem = new JMenuItem("在编辑器中打开");
        openItem.addActionListener(actionEvent -> openHistoryFileInEditor(item));
        JMenuItem deleteItem = new JMenuItem("删除");
        deleteItem.addActionListener(actionEvent -> deleteHistoryItem(item));
        menu.add(openItem);
        menu.add(deleteItem);
        menu.show(historyTree, e.getX(), e.getY());
    }

    /**
     * 在编辑器中打开历史记录文件
     *
     * @param item 历史记录项
     */
    private void openHistoryFileInEditor(@NotNull HistoryItem item) {
        ApplicationManager.getApplication().invokeLater(() ->
                                                            new OpenFileDescriptor(project, item.file).navigate(true)
                                                       );
    }

    /**
     * 选择已存在的内容页
     *
     * @param title 内容标题
     * @return 如果找到并选中则返回 true
     */
    private boolean selectExistingContent(@NotNull String title) {
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(PluginContents.PLUGIN_NAME);
        if (toolWindow == null) {
            return false;
        }
        Content existing = findContent(toolWindow, title);
        if (existing == null) {
            return false;
        }
        toolWindow.getContentManager().setSelectedContent(existing);
        if (toolWindow instanceof ToolWindowEx toolWindowEx) {
            toolWindowEx.activate(null, true, true);
        } else {
            toolWindow.activate(null, true, true);
        }
        return true;
    }

    /**
     * 刷新历史记录列表
     * <p> 在后台线程中加载所有历史记录项, 并在 UI 线程中更新历史记录列表显示.
     *
     */
    private void refreshHistoryList() {
        if (historyTreeModel == null || historyRootNode == null || historyTree == null) {
            return;
        }
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            VirtualFile historyDir = getHistoryDir();
            if (historyDir != null) {
                VfsUtil.markDirtyAndRefresh(true, false, false, historyDir);
            }
            String filterText = historyFilterText;
            List<HistoryItem> items = loadHistoryItems(filterText);
            ApplicationManager.getApplication().invokeLater(() -> {
                if (historyTreeModel == null || historyRootNode == null || historyTree == null) {
                    return;
                }
                rebuildHistoryTree(items);
                updateHistoryStats(items);
            });
        });
    }

    /**
     * 重建历史记录树
     *
     * @param items 历史记录列表
     */
    private void rebuildHistoryTree(@NotNull List<HistoryItem> items) {
        if (historyRootNode == null || historyTreeModel == null || historyTree == null) {
            return;
        }
        historyRootNode.removeAllChildren();
        Map<String, DefaultMutableTreeNode> groupNodes = new LinkedHashMap<>();
        for (HistoryItem item : items) {
            String groupText = resolveCategoryText(item.actionKey, item.category);
            DefaultMutableTreeNode groupNode = groupNodes.get(groupText);
            if (groupNode == null) {
                HistoryGroup group = new HistoryGroup(groupText, resolveCategoryIcon(item.actionKey));
                groupNode = new DefaultMutableTreeNode(group);
                groupNodes.put(groupText, groupNode);
                historyRootNode.add(groupNode);
            }
            groupNode.add(new DefaultMutableTreeNode(item));
        }
        historyTreeModel.reload();
        for (int i = 0; i < historyTree.getRowCount(); i++) {
            historyTree.expandRow(i);
        }
    }

    /**
     * 更新历史记录统计信息
     * <p> 根据给定的历史记录项列表, 更新历史记录统计标签的内容, 包括总数, 分类信息和最近时间.
     *
     * @param items 历史记录项列表, 不能为 null
     *              <p>
     *              使用示例:
     *              <pre>{@code
     *              List<HistoryItem> items = getRecentHistoryItems();
     *              updateHistoryStats(items);
     *              }</pre>
     */
    private void updateHistoryStats(@NotNull List<HistoryItem> items) {
        if (historyStatsLabel == null) {
            return;
        }
        int count = items.size();
        String categoryText = buildCategoryStatsText(items);
        String latestText = buildLatestTimeText(items);
        StringBuilder builder = new StringBuilder();
        builder.append("共 ").append(count).append(" 条");
        if (!categoryText.isBlank()) {
            builder.append(" | 分类: ").append(categoryText);
        }
        if (!latestText.isBlank()) {
            builder.append(" | 最近: ").append(latestText);
        }
        historyStatsLabel.setText(builder.toString());
    }

    /**
     * 构建分类统计文案
     *
     * @param items 历史记录列表
     * @return 分类统计文本
     */
    private @NotNull String buildCategoryStatsText(@NotNull List<HistoryItem> items) {
        Map<String, Integer> counters = new LinkedHashMap<>();
        for (HistoryItem item : items) {
            String categoryText = resolveCategoryText(item.actionKey, item.category);
            counters.put(categoryText, counters.getOrDefault(categoryText, 0) + 1);
        }
        if (counters.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, Integer> entry : counters.entrySet()) {
            if (!builder.isEmpty()) {
                builder.append(", ");
            }
            builder.append(entry.getKey()).append(" ").append(entry.getValue());
        }
        return builder.toString();
    }

    /**
     * 构建最近时间文案
     *
     * @param items 历史记录列表
     * @return 最近时间文本
     */
    private @NotNull String buildLatestTimeText(@NotNull List<HistoryItem> items) {
        LocalDateTime latest = null;
        for (HistoryItem item : items) {
            LocalDateTime itemTime = item.resolveDateTime();
            if (latest == null || itemTime.isAfter(latest)) {
                latest = itemTime;
            }
        }
        if (latest == null) {
            return "";
        }
        return latest.format(FRONT_MATTER_DATE_FORMATTER);
    }

    /**
     * 加载所有历史记录项
     * <p> 从历史目录中读取所有符合格式的历史文件, 并解析为 HistoryItem 对象列表. 文件需以指定扩展名结尾且非目录.
     *
     * @return 历史记录项列表, 如果未找到历史目录或无有效文件则返回空列表
     */
    private @NotNull List<HistoryItem> loadHistoryItems(@NotNull String filterText) {
        return ReadAction.compute(() -> {
            List<HistoryItem> items = new ArrayList<>();
            VirtualFile historyDir = getHistoryDir();
            if (historyDir == null) {
                return items;
            }
            for (VirtualFile file : historyDir.getChildren()) {
                if (file.isDirectory() || !file.getName().endsWith(HISTORY_FILE_EXTENSION)) {
                    continue;
                }
                HistoryContent content = loadHistoryContent(file);
                if (content == null) {
                    continue;
                }
                String category = ToolWindowTitleUtil.extractCategory(content.title);
                String actionKey = ToolWindowTitleUtil.getActionKeyByAbbreviation(category);
                if (!matchesFilter(filterText, content, actionKey, category)) {
                    continue;
                }
                items.add(new HistoryItem(file, content.title, category, file.getTimeStamp(), content.dateText,
                                          actionKey, content.heading));
            }
            items.sort(Comparator.comparing(HistoryItem::category)
                           .thenComparing(HistoryItem::timeStamp, Comparator.reverseOrder()));
            return items;
        });
    }

    /**
     * 判断是否匹配过滤条件
     *
     * @param filterText 过滤文本
     * @param content    历史内容
     * @param actionKey  国际化 key
     * @param category   简称
     * @return 匹配则返回 true
     */
    private boolean matchesFilter(@NotNull String filterText,
                                  @NotNull HistoryContent content,
                                  @Nullable String actionKey,
                                  @NotNull String category) {
        if (filterText.isBlank()) {
            return true;
        }
        String query = filterText.toLowerCase();
        if (content.title.toLowerCase().contains(query)) {
            return true;
        }
        if (content.body.toLowerCase().contains(query)) {
            return true;
        }
        if (content.heading != null && content.heading.toLowerCase().contains(query)) {
            return true;
        }
        String categoryText = resolveCategoryText(actionKey, category);
        return categoryText.toLowerCase().contains(query);
    }

    /**
     * 加载历史记录文件内容
     * <p> 读取指定的虚拟文件并解析其内容, 提取标题和正文部分. 如果文件内容以标题前缀开头, 则解析标题和正文; 否则, 将文件名作为标题, 内容作为正文.
     *
     * @param file 历史记录文件, 不能为 null
     * @return 解析后的历史内容对象, 如果解析失败或文件无效则返回 null
     */
    private @Nullable HistoryContent loadHistoryContent(@NotNull VirtualFile file) {
        try {
            String text = ReadAction.compute(() -> {
                Document document = FileDocumentManager.getInstance().getDocument(file);
                return document != null ? document.getText() : VfsUtilCore.loadText(file);
            });
            FrontMatterData frontMatter = parseFrontMatter(text);
            String title = file.getNameWithoutExtension();
            String strippedText = frontMatter.body;
            String body = strippedText;
            if (strippedText.startsWith(HISTORY_FILE_TITLE_PREFIX)) {
                int lineEnd = strippedText.indexOf('\n');
                if (lineEnd > 0) {
                    title = strippedText.substring(HISTORY_FILE_TITLE_PREFIX.length(), lineEnd).trim();
                    body = strippedText.substring(lineEnd + 1);
                    if (body.startsWith("\n")) {
                        body = body.substring(1);
                    }
                } else {
                    title = strippedText.substring(HISTORY_FILE_TITLE_PREFIX.length()).trim();
                    body = "";
                }
            }
            String heading = extractFirstHeading(body);
            return new HistoryContent(title, body, frontMatter.dateText, heading);
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * 获取变更日志的历史目录
     * <p> 通过 ScratchFileService 获取根路径, 并在该路径下查找名为 "IntelliAI Changelog" 的子目录.
     * 如果根路径为空或无法找到对应的 VirtualFile, 则返回 null.
     *
     * @return 历史目录的 VirtualFile, 如果未找到或路径无效则返回 null
     */
    private @Nullable VirtualFile getHistoryDir() {
        String rootPath = ScratchFileService.getInstance().getRootPath(ScratchRootType.getInstance());
        if (rootPath.isBlank()) {
            return null;
        }
        VirtualFile root = LocalFileSystem.getInstance().findFileByPath(rootPath);
        if (root == null) {
            return null;
        }
        return VfsUtilCore.findRelativeFile(HISTORY_DIR_NAME, root);
    }

    /**
     * 创建 History scratch 文件
     * <p> 根据指定的标题生成安全文件名, 并在指定目录下创建或查找对应的 scratch 文件.
     *
     * @param title 标题, 用于构建文件名
     * @return 返回创建或找到的 VirtualFile 对象, 如果失败则返回 null
     */
    private @Nullable VirtualFile createHistoryScratchFile(@NotNull String title) {
        String fileName = buildHistoryFileName(title);
        String path = HISTORY_DIR_NAME + "/" + fileName;
        try {
            return ScratchFileService.getInstance()
                .findFile(ScratchRootType.getInstance(), path, ScratchFileService.Option.create_if_missing);
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * 构建历史记录文件的名称
     * <p>根据传入的标题生成符合规范的历史记录文件名, 仅包含安全化的标题部分.
     * <p>文件名格式为: 安全化标题. 扩展名
     * <p>安全化规则:
     * <ul>
     *   <li>将冒号 (:) 替换为连字符(-)</li>
     *   <li>将多个连续空白字符替换为单个下划线(_)</li>
     *   <li>移除所有非字母数字, 下划线, 点, 连字符的字符</li>
     * </ul>
     * <p>示例:
     * <pre>{@code
     * buildHistoryFileName("RL:20260103114605");
     * // 返回:RL-20260103114605.md
     * }</pre>
     *
     * @param title 历史记录的标题, 不能为空
     * @return 构建后的文件名, 格式为安全化标题. 扩展名
     */
    private @NotNull String buildHistoryFileName(@NotNull String title) {
        String safeTitle = title.replace(':', '-')
            .replaceAll("\\s+", "_")
            .replaceAll("[^a-zA-Z0-9._-]", "_");
        return safeTitle + HISTORY_FILE_EXTENSION;
    }

    /**
     * 构建 History 文件内容
     * <p> 将标题和内容按照指定格式拼接, 生成用于保存的文件内容. 标题前会添加固定前缀, 并在标题与内容之间插入换行符.
     *
     * @param title   标题, 用于标识该条历史记录
     * @param content 内容, 即要保存的具体文本信息
     * @return 拼接后的文件内容字符串
     */
    private @NotNull String buildHistoryFileContent(@NotNull String title, @NotNull String content) {
        return buildHistoryFileContent(title, content, "", "", "");
    }

    /**
     * 构建 History 文件内容
     *
     * @param title      标题
     * @param content    内容
     * @param startPoint 起始点
     * @param endPoint   结束点
     * @param provider   使用的服务商信息
     * @return 拼接后的文件内容字符串
     */
    private @NotNull String buildHistoryFileContent(@NotNull String title,
                                                    @NotNull String content,
                                                    @NotNull String startPoint,
                                                    @NotNull String endPoint,
                                                    @NotNull String provider) {
        String frontMatter = buildFrontMatter(title, startPoint, endPoint, provider);
        return frontMatter + "\n" + HISTORY_FILE_TITLE_PREFIX + title + "\n\n" + content;
    }

    /**
     * 构建 Frontmatter
     *
     * @param title      标题
     * @param startPoint 起始点
     * @param endPoint   结束点
     * @param provider   使用的服务商信息
     * @return Frontmatter 内容
     */
    private @NotNull String buildFrontMatter(@NotNull String title,
                                             @NotNull String startPoint,
                                             @NotNull String endPoint,
                                             @NotNull String provider) {
        String date = LocalDateTime.now().format(FRONT_MATTER_DATE_FORMATTER);
        String abbreviation = ToolWindowTitleUtil.extractCategory(title);
        String actionKey = ToolWindowTitleUtil.getActionKeyByAbbreviation(abbreviation);
        String type = actionKey == null ? abbreviation : ChangelogBundle.message(actionKey);
        return "---\n"
               + "date: " + date + "\n"
               + "type: " + type + "\n"
               + "provider: " + provider + "\n"
               + "point:\n"
               + "  start: " + startPoint + "\n"
               + "  end: " + endPoint + "\n"
               + "---\n";
    }

    /**
     * 解析 Frontmatter
     *
     * @param text 原始内容
     * @return Frontmatter 数据
     */
    private @NotNull FrontMatterData parseFrontMatter(@NotNull String text) {
        if (!text.startsWith("---\n")) {
            return new FrontMatterData(null, text);
        }
        int endIndex = text.indexOf("\n---\n");
        if (endIndex < 0) {
            return new FrontMatterData(null, text);
        }
        String header = text.substring(4, endIndex);
        String body = text.substring(endIndex + "\n---\n".length());
        String dateText = null;
        for (String line : header.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("date:")) {
                dateText = trimmed.substring("date:".length()).trim();
                break;
            }
        }
        return new FrontMatterData(dateText, body);
    }

    /**
     * 日志变更输出会话类
     * <p> 用于管理与变更日志工具窗口相关的输出会话, 包括文本内容的追加, 设置和完成操作.
     * <p> 该类封装了与日志输出相关的功能, 支持在 UI 线程中安全地更新文本区域内容, 并提供取消标志以控制输出流程.
     * <p> 主要用途是在插件或工具窗口中展示和记录变更日志内容, 支持将最终文本保存到历史记录中.
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.03
     * @since 1.0.0
     */
    public static final class ChangelogOutputSession {
        /** 会话所属的服务实例 */
        private final @NotNull ChangelogToolWindowService service;
        /**
         * 会话标题
         * <p> 用于标识当前输出会话的标题
         */
        private final @NotNull String sessionTitle;
        private final @NotNull String startPoint;
        private final @NotNull String endPoint;
        /** 使用的服务商信息 */
        private final @NotNull String provider;
        /** 文本区域组件, 用于显示输出内容 */
        private final @Nullable JBTextArea textArea;

        /** 取消标志, 用于停止流式输出 */
        private final @Nullable AtomicBoolean cancellationFlag;

        /**
         * 构造函数, 初始化 ChangelogOutputSession 对象
         * <p> 用于创建一个输出会话对象, 并设置关联的服务, 会话标题, 文本区域和取消标志
         *
         * @param service          会话所属的服务, 不能为空
         * @param sessionTitle     会话标题, 不能为空
         * @param startPoint       起始点
         * @param endPoint         结束点
         * @param provider         使用的服务商信息
         * @param textArea         关联的文本区域, 可以为 null
         * @param cancellationFlag 取消标志, 可以为 null
         */
        private ChangelogOutputSession(@NotNull ChangelogToolWindowService service,
                                       @NotNull String sessionTitle,
                                       @NotNull String startPoint,
                                       @NotNull String endPoint,
                                       @NotNull String provider,
                                       @Nullable JBTextArea textArea,
                                       @Nullable AtomicBoolean cancellationFlag) {
            this.service = service;
            this.sessionTitle = sessionTitle;
            this.startPoint = startPoint;
            this.endPoint = endPoint;
            this.provider = provider;
            this.textArea = textArea;
            this.cancellationFlag = cancellationFlag;
        }

        /**
         * 检查当前会话是否已被取消
         * <p> 通过检查取消标志的值, 判断输出操作是否应被中断.
         *
         * @return 如果取消标志存在且其值为 true, 则返回 true; 否则返回 false
         */
        public boolean isCancelled() {
            return cancellationFlag != null && cancellationFlag.get();
        }

        /**
         * 在文本区域的末尾追加指定的文本
         * <p> 如果文本区域为空或未初始化, 则不执行任何操作.
         *
         * @param text 要追加的文本
         */
        public void append(@NotNull String text) {
            if (textArea == null || text.isEmpty()) {
                return;
            }
            ApplicationManager.getApplication().invokeLater(() -> {
                textArea.append(text);
                textArea.setCaretPosition(textArea.getDocument().getLength());
            });
        }

        /**
         * 设置文本区域的内容
         * <p> 将指定的文本设置到文本区域中, 并将光标移动到文本末尾. 如果文本区域为空, 则不进行任何操作.
         *
         * @param text 要设置的文本, 不能为 null
         */
        public void setText(@NotNull String text) {
            if (textArea == null) {
                return;
            }
            ApplicationManager.getApplication().invokeLater(() -> {
                textArea.setText(text);
                textArea.setCaretPosition(textArea.getDocument().getLength());
            });
        }

        /**
         * 完成输出并保存历史记录
         * <p> 将指定的文本设置到文本区域中, 并将其保存到历史记录中.
         * <p> 此方法会在文本区域中显示完整的输出内容, 并调用服务的 saveHistory 方法来保存输出内容到历史记录.
         *
         * @param text 完整输出内容
         */
        public void complete(@NotNull String text) {
            setText(text);
            service.saveHistory(sessionTitle, text, startPoint, endPoint, provider);
        }
    }

    /**
     * 历史记录项记录类
     * <p> 用于表示用户操作或文件访问的历史记录条目, 包含文件信息, 标题, 分类和时间戳等属性
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.03
     * @since 1.0.0
     */
    private record HistoryItem(@NotNull VirtualFile file,
                               @NotNull String title,
                               @NotNull String category,
                               long timeStamp,
                               @Nullable String dateText,
                               @Nullable String actionKey,
                               @Nullable String heading) {
        /**
         * 获取解析后的时间
         *
         * @return 时间对象
         */
        private @NotNull LocalDateTime resolveDateTime() {
            if (dateText != null) {
                try {
                    return LocalDateTime.parse(dateText, FRONT_MATTER_DATE_FORMATTER);
                } catch (Exception ignored) {
                    // 忽略解析失败
                }
            }
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(timeStamp), ZoneId.systemDefault());
        }
    }

    /**
     * 历史内容记录类
     * <p> 用于封装历史记录中的标题和正文信息, 提供不可变的数据结构
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.03
     * @since 1.0.0
     */
    private record HistoryContent(@NotNull String title,
                                  @NotNull String body,
                                  @Nullable String dateText,
                                  @Nullable String heading) {
    }

    /**
     * Frontmatter 数据
     */
    private record FrontMatterData(@Nullable String dateText, @NotNull String body) {
    }

    /**
     * 提取首个一级或二级标题
     *
     * @param body Markdown 正文
     * @return 标题文本
     */
    private @Nullable String extractFirstHeading(@NotNull String body) {
        for (String line : body.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("# ")) {
                return trimmed.substring(2).trim();
            }
            if (trimmed.startsWith("## ")) {
                return trimmed.substring(3).trim();
            }
        }
        return null;
    }

    /**
     * 历史树分组节点
     */
    private record HistoryGroup(@NotNull String text, @NotNull Icon icon) {
    }

    /**
     * 历史树渲染器
     * <p> 展示分组节点与记录节点的不同样式
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.03
     * @since 1.0.0
     */
    private final class HistoryTreeCellRenderer extends JPanel implements TreeCellRenderer {
        private final JBLabel timestampLabel;
        private final JBLabel titleLabel;
        private final JBLabel deleteLabel;
        private final JBLabel groupLabel;
        private final JPanel contentPanel;

        private HistoryTreeCellRenderer() {
            super(new BorderLayout(JBUI.scale(2), 0));
            setBorder(JBUI.Borders.empty(2, 4));
            setOpaque(true);

            timestampLabel = new JBLabel();
            timestampLabel.setOpaque(true);
            timestampLabel.setBorder(JBUI.Borders.empty(2, 6));

            titleLabel = new JBLabel();
            titleLabel.setFont(titleLabel.getFont().deriveFont(Font.PLAIN));

            deleteLabel = new JBLabel(DELETE_ICON);
            deleteLabel.setBorder(JBUI.Borders.empty(0, 2));

            groupLabel = new JBLabel();
            groupLabel.setFont(groupLabel.getFont().deriveFont(Font.BOLD));

            contentPanel = new JPanel(new BorderLayout(JBUI.scale(6), 0));
            contentPanel.setOpaque(false);
            contentPanel.add(timestampLabel, BorderLayout.WEST);
            contentPanel.add(titleLabel, BorderLayout.CENTER);
        }

        @Override
        public Component getTreeCellRendererComponent(JTree tree,
                                                      Object value,
                                                      boolean selected,
                                                      boolean expanded,
                                                      boolean leaf,
                                                      int row,
                                                      boolean hasFocus) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
            Object userObject = node.getUserObject();
            removeAll();

            boolean hover = !selected && row == historyHoverRow;
            Color background = selected ? UIUtil.getTreeSelectionBackground(true)
                                        : (hover ? getHoverBackground(tree) : tree.getBackground());
            Color foreground = selected ? UIUtil.getTreeSelectionForeground(true) : tree.getForeground();
            setBackground(background);
            setPreferredSize(null);

            if (userObject instanceof HistoryGroup group) {
                groupLabel.setText(group.text);
                groupLabel.setIcon(group.icon);
                groupLabel.setForeground(foreground);
                add(deleteLabel, BorderLayout.WEST);
                add(groupLabel, BorderLayout.CENTER);
                return this;
            }
            if (userObject instanceof HistoryItem item) {
                String timestampText = formatTimestamp(item);
                timestampLabel.setText(timestampText);
                int maxHeadingWidth = calculateHeadingWidth(tree, timestampLabel);
                String headingText = buildHeadingTitle(item.heading, titleLabel.getFontMetrics(titleLabel.getFont()),
                                                       maxHeadingWidth);
                titleLabel.setText(headingText);
                titleLabel.setForeground(foreground);
                deleteLabel.setForeground(foreground);

                add(deleteLabel, BorderLayout.WEST);
                timestampLabel.setBackground(selected ? background : CATEGORY_BG);
                timestampLabel.setForeground(selected ? foreground : CATEGORY_FG);
                add(contentPanel, BorderLayout.CENTER);
                return this;
            }
            return this;
        }

        private Color getHoverBackground(@NotNull JTree tree) {
            return UIUtil.getTreeSelectionBackground(false);
        }

        private String formatTimestamp(@NotNull HistoryItem item) {
            if (item.dateText != null) {
                try {
                    LocalDateTime dateTime = LocalDateTime.parse(item.dateText, FRONT_MATTER_DATE_FORMATTER);
                    return dateTime.format(FRONT_MATTER_DATE_FORMATTER);
                } catch (Exception ignored) {
                    // 忽略解析失败，继续降级
                }
            }
            String raw = extractDisplayTitle(item.title);
            if (raw.length() == 14) {
                try {
                    LocalDateTime dateTime = LocalDateTime.parse(raw, DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
                    return dateTime.format(FRONT_MATTER_DATE_FORMATTER);
                } catch (Exception ignored) {
                    // 忽略解析失败
                }
            }
            return raw;
        }

        private int calculateHeadingWidth(@NotNull JTree tree, @NotNull JBLabel timestamp) {
            Rectangle visible = tree.getVisibleRect();
            if (visible.width <= 0) {
                return 0;
            }
            int deleteWidth = DELETE_ICON.getIconWidth() + JBUI.scale(6);
            int timestampWidth = timestamp.getPreferredSize().width;
            int gaps = JBUI.scale(12);
            return Math.max(0, visible.width - deleteWidth - timestampWidth - gaps);
        }

        private String buildHeadingTitle(@Nullable String heading, @NotNull FontMetrics metrics, int maxWidth) {
            if (heading == null || heading.isBlank()) {
                return "";
            }
            String trimmed = heading.trim();
            if (maxWidth <= 0) {
                return "";
            }
            String prefix = " - ";
            int available = maxWidth - metrics.stringWidth(prefix);
            if (available <= 0) {
                return "";
            }
            String fitted = fitToWidth(trimmed, metrics, available);
            return fitted.isEmpty() ? "" : prefix + fitted;
        }

        private String extractDisplayTitle(@NotNull String title) {
            int index = title.indexOf(':');
            if (index > 0 && index < title.length() - 1) {
                return title.substring(index + 1);
            }
            return title;
        }

        private String fitToWidth(@NotNull String text, @NotNull FontMetrics metrics, int maxWidth) {
            if (metrics.stringWidth(text) <= maxWidth) {
                return text;
            }
            String ellipsis = "...";
            int ellipsisWidth = metrics.stringWidth(ellipsis);
            int available = maxWidth - ellipsisWidth;
            if (available <= 0) {
                return "";
            }
            int end = text.length();
            while (end > 0 && metrics.stringWidth(text.substring(0, end)) > available) {
                end--;
            }
            return end <= 0 ? "" : text.substring(0, end) + ellipsis;
        }

    }

    /**
     * 停止输出操作类
     * <p> 用于在指定会话中停止输出操作, 通常用于控制终端或日志工具窗口中的输出行为.
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.03
     * @since 1.0.0
     */
    private static final class StopOutputAction extends AnAction {
        /** 会话标题, 用于标识要停止的输出会话 */
        private final String sessionTitle;

        /**
         * 初始化停止输出动作
         * <p> 用于创建一个 StopOutputAction 实例, 并设置其名称, 描述和图标.
         *
         * @param sessionTitle 要停止的输出会话标题, 不能为空
         */
        private StopOutputAction(@NotNull String sessionTitle) {
            super(ChangelogBundle.message("toolwindow.stop.text"),
                  ChangelogBundle.message("toolwindow.stop.text"),
                  AllIcons.Actions.Suspend);
            this.sessionTitle = sessionTitle;
        }

        /**
         * 执行停止输出会话的操作
         * <p> 当用户触发该动作时, 会调用 stopOutput 方法以停止指定标题的输出会话流式输出.
         *
         * @param e AnActionEvent 对象, 包含动作事件信息
         */
        @Override
        public void actionPerformed(@NotNull com.intellij.openapi.actionSystem.AnActionEvent e) {
            stopOutput(sessionTitle);
        }
    }

    /**
     * 删除全部历史记录操作类
     * <p>该内部类用于实现删除所有历史记录项的功能, 通常作为 IntelliJ 平台插件中的一个动作 (Action) 使用.
     * <p>该类继承自 AnAction, 并在 actionPerformed 方法中触发二次确认后批量删除历史文件, 完成后刷新历史记录列表.
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.03
     * @since 1.0.0
     */
    private final class DeleteAllHistoryAction extends AnAction {
        /**
         * 历史记录树组件
         */
        private final Tree tree;

        /**
         * 构造函数, 初始化删除全部历史记录动作
         * <p> 设置动作名称, 描述和图标, 并将传入的树组件赋值给成员变量
         *
         * @param tree 历史记录树组件, 不能为 null
         */
        private DeleteAllHistoryAction(@NotNull Tree tree) {
            super("Delete All",
                  "Delete All",
                  AllIcons.Actions.GC);
            this.tree = tree;
        }

        /**
         * 处理删除全部历史记录的动作
         * <p> 执行二次确认后批量删除历史目录下的记录文件, 并刷新历史记录列表.
         * <p> 此方法在事件发生时被调用, 确保在 UI 线程中安全地执行文件删除操作.
         *
         * @param e 动作事件对象, 包含触发事件的上下文信息, 不能为 null
         */
        @Override
        public void actionPerformed(@NotNull com.intellij.openapi.actionSystem.AnActionEvent e) {
            deleteAllHistoryItems(tree);
        }
    }

    /**
     * 复制全部操作类
     * <p> 用于在工具窗口中执行复制全部文本的操作, 从指定的文本区域获取内容并设置到系统剪贴板
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.03
     * @since 1.0.0
     */
    private static final class CopyAllAction extends AnAction {
        /**
         * 文本区域组件, 用于显示和编辑文本内容
         *
         * @see JBTextArea
         */
        private final JBTextArea textArea;

        /**
         * 构造函数, 初始化 CopyAllAction 对象
         * <p> 该构造函数用于创建一个 CopyAllAction 实例, 并设置其名称和图标.
         *
         * @param textArea 要复制文本的 JBTextArea 组件, 不能为空
         */
        private CopyAllAction(@NotNull JBTextArea textArea) {
            super(ChangelogBundle.message("toolwindow.copy.text"),
                  ChangelogBundle.message("toolwindow.copy.text"),
                  AllIcons.Actions.Copy);
            this.textArea = textArea;
        }

        /**
         * 复制文本区域中的所有文本到剪贴板
         * <p> 获取文本区域中的文本, 并将其复制到系统剪贴板中. 如果文本为空, 则不执行任何操作.
         *
         * @param e AnActionEvent 对象, 包含动作事件信息
         */
        @Override
        public void actionPerformed(@NotNull com.intellij.openapi.actionSystem.AnActionEvent e) {
            String text = textArea.getText();
            if (text == null || text.isEmpty()) {
                return;
            }
            com.intellij.openapi.ide.CopyPasteManager.getInstance()
                .setContents(new StringSelection(text));
        }
    }
}
