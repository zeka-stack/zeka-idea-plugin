package dev.dong4j.zeka.stack.idea.javadoc.git;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.EditorKind;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.ui.OnePixelSplitter;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.TreeSpeedSearch;
import com.intellij.ui.components.ActionLink;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import com.intellij.ui.content.MessageView;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ui.JBUI;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.BorderLayout;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.swing.JPanel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import dev.dong4j.zeka.stack.idea.javadoc.task.DocumentationTask;
import dev.dong4j.zeka.stack.idea.javadoc.util.JavadocBundle;

/**
 * 提供对提交日志中缺少 Javadoc 的检测和修复功能的面板组件.
 * <p> 该类实现了 Disposable 接口, 用于管理资源释放. 它通过树形结构展示项目中缺少 Javadoc 的文件和任务,
 * 并提供生成所有缺失 Javadoc 的功能. 面板支持在编辑器中显示具体的文件位置, 并根据选择的任务动态更新编辑器视图.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.20
 * @since 1.0.0
 */
public final class CommitJavadocDetailsPanel implements Disposable {
    /** 工具窗口的唯一标识符, 用于在 IDE 中定位和管理该面板 */
    private static final String TOOL_WINDOW_ID = "Messages";
    /** 用于在项目中唯一标识 CommitJavadocDetailsPanel 实例的键值, 用于存储和检索面板实例. */
    private static final Key<CommitJavadocDetailsPanel> PANEL_KEY =
        Key.create("dev.dong4j.zeka.stack.idea.javadoc.commit.details.panel");

    /** 当前项目实例, 用于访问项目级资源和服务 */
    private final Project project;
    /** 根面板, 整个详情面板的顶层容器组件 */
    private final JPanel rootPanel;
    /**
     * 编辑器面板容器
     * <p> 用于显示当前选中文件的代码编辑器组件, 提供编辑器的布局和管理功能.
     *
     * @see JPanel
     */
    private final JPanel editorHolder;
    /**
     * 显示 Javadoc 详情树
     * <p>
     * 用于在界面中展示文档任务的树形结构, 支持文件和任务的层级显示.
     *
     * @see DefaultTreeModel
     */
    private final Tree tree;
    /** 树形结构数据模型, 用于管理 Javadoc 任务在树形视图中的节点数据与层级关系 */
    private final DefaultTreeModel treeModel;
    /** 摘要标签, 用于显示检测结果摘要信息 */
    private final JBLabel summaryLabel;
    /** 生成所有缺失 Javadoc 的 Action 链接 */
    private final ActionLink generateAllLink;

    /** 当前文本编辑器实例 */
    private Editor currentEditor;
    /** 当前在编辑器中显示的虚拟文件 */
    private VirtualFile currentFile;
    /** DocumentationTask 列表, 用于管理缺失文档注释的任务 */
    private List<DocumentationTask> currentTasks = new ArrayList<>();

    /**
     * 构造一个 CommitJavadocDetailsPanel 实例, 初始化面板的核心 UI 组件和事件处理.
     *
     * <p> 该构造函数主要完成以下工作:
     *
     * <ul>
     *   <li> 创建并配置树模型, 树视图以及自定义渲染器;</li>
     *   <li> 初始化摘要标签, 生成按钮及其行为逻辑;</li>
     *   <li> 构造左侧文件树与右侧编辑器分隔面板;</li>
     *   <li> 为树节点添加选择监听器, 以便在用户选中任务或文件时在编辑器中显示相应源码位置.</li>
     * </ul>
     *
     * @param project 当前项目实例, 用于获取项目相关资源与配置
     */
    private CommitJavadocDetailsPanel(@NotNull Project project) {
        this.project = project;

        DefaultMutableTreeNode root = new DefaultMutableTreeNode(new NodeData(NodeType.ROOT, "ROOT"));
        this.treeModel = new DefaultTreeModel(root);
        this.tree = new Tree(treeModel);
        this.tree.setRootVisible(false);
        this.tree.setShowsRootHandles(true);
        this.tree.setCellRenderer(new JavadocTreeCellRenderer());
        new TreeSpeedSearch(tree, path -> {
            Object userObject = ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();
            if (userObject instanceof NodeData nodeData) {
                return nodeData.displayName;
            }
            return "";
        });

        this.summaryLabel = new JBLabel();
        this.generateAllLink = new ActionLink();
        this.generateAllLink.setText(JavadocBundle.message("commit.check.javadoc.details.generate.all"));
        this.generateAllLink.setToolTipText(JavadocBundle.message("commit.check.javadoc.fix.action"));
        this.generateAllLink.setEnabled(false);
        this.generateAllLink.addActionListener(event -> {
            if (currentTasks.isEmpty()) {
                return;
            }
            new CommitJavadocGenerator(project).generateForTasks(currentTasks);
        });

        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(JBUI.Borders.empty(6, 8));
        header.add(summaryLabel, BorderLayout.WEST);
        header.add(generateAllLink, BorderLayout.EAST);

        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.add(ScrollPaneFactory.createScrollPane(tree), BorderLayout.CENTER);

        this.editorHolder = new JPanel(new BorderLayout());
        this.editorHolder.setBorder(JBUI.Borders.empty(4));
        this.editorHolder.add(new JBLabel(JavadocBundle.message("commit.check.javadoc.details.editor.placeholder")),
                              BorderLayout.CENTER);

        OnePixelSplitter splitPane = new OnePixelSplitter(false, 0.28f);
        splitPane.setFirstComponent(leftPanel);
        splitPane.setSecondComponent(editorHolder);

        this.rootPanel = new JPanel(new BorderLayout());
        this.rootPanel.add(header, BorderLayout.NORTH);
        this.rootPanel.add(splitPane, BorderLayout.CENTER);

        this.tree.addTreeSelectionListener(event -> {
            TreePath path = event.getPath();
            if (path == null) {
                return;
            }
            Object node = path.getLastPathComponent();
            if (!(node instanceof DefaultMutableTreeNode mutableTreeNode)) {
                return;
            }
            Object userObject = mutableTreeNode.getUserObject();
            if (!(userObject instanceof NodeData nodeData)) {
                return;
            }
            if (nodeData.location != null) {
                showInEditor(nodeData.location);
            } else if (nodeData.locations != null && !nodeData.locations.isEmpty()) {
                showInEditor(nodeData.locations.get(0));
            }
        });
    }

    /**
     * 显示缺失 Javadoc 的详情面板
     *
     * <p> 此方法会在 IDE 的 Messages 工具窗口中展示提交时检测到的缺失 Javadoc 任务, 并提供
     * UI 供用户查看任务列表与对应文件. 若面板已存在, 则复用现有实例; 否则创建新实例并
     * 添加至工具窗口. 随后会更新任务列表, 刷新树视图并自动将该工具窗口激活.
     *
     * @param project 当前项目实例
     * @param tasks   包含缺失 Javadoc 的代码元素的任务列表
     */
    public static void show(@NotNull Project project, @NotNull List<DocumentationTask> tasks) {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (project.isDisposed() || DumbService.isDumb(project)) {
                return;
            }

            MessageView messageView = MessageView.getInstance(project);
            ContentManager manager = messageView.getContentManager();
            String title = JavadocBundle.message("commit.check.javadoc.details.title");
            Content target = null;
            for (Content content : manager.getContents()) {
                if (title.equals(content.getDisplayName())) {
                    target = content;
                    break;
                }
            }

            CommitJavadocDetailsPanel panel;
            if (target == null) {
                panel = new CommitJavadocDetailsPanel(project);
                target = ContentFactory.getInstance().createContent(panel.getComponent(), title, false);
                target.putUserData(PANEL_KEY, panel);
                target.setDisposer(panel);
                manager.addContent(target);
            } else {
                panel = target.getUserData(PANEL_KEY);
                if (panel == null) {
                    panel = new CommitJavadocDetailsPanel(project);
                    target.setComponent(panel.getComponent());
                    target.putUserData(PANEL_KEY, panel);
                    target.setDisposer(panel);
                }
            }

            panel.updateTasks(tasks);
            manager.setSelectedContent(target);

            ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(TOOL_WINDOW_ID);
            if (toolWindow != null) {
                toolWindow.activate(null, true);
            }
        });
    }

    /**
     * 更新任务列表并刷新界面显示
     * <p> 此方法会根据传入的任务列表更新当前任务集合, 并重建任务树视图. 同时, 更新摘要信息和生成按钮的状态.
     *
     * @param tasks 包含文档任务的列表
     */
    private void updateTasks(@NotNull List<DocumentationTask> tasks) {
        currentTasks = List.copyOf(tasks);
        List<TaskLocation> locations = buildLocations(tasks);
        rebuildTree(locations);
        CommitJavadocChecker.DetectionSummary summary = CommitJavadocChecker.buildDetectionSummary(tasks);
        summaryLabel.setText(JavadocBundle.message("commit.check.javadoc.details.summary", summary.summary()));
        generateAllLink.setEnabled(!tasks.isEmpty());

        if (!locations.isEmpty()) {
            DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
            if (root.getChildCount() > 0) {
                DefaultMutableTreeNode firstChild = (DefaultMutableTreeNode) root.getChildAt(0);
                tree.setSelectionPath(new TreePath(firstChild.getPath()));
            }
        }
    }

    /**
     * 重构树结构以显示任务位置信息
     * <p> 根据给定的任务位置列表构建文件系统树结构, 并更新树模型.
     * 此方法会将任务列表按文件进行分组, 根据项目的相对路径对文件进行排序, 并对每个文件内的任务按行号进行排序.
     * 最后刷新树形视图并展开根节点.
     *
     * @param locations 任务位置列表, 包含每个任务所在的文件及其行号
     */
    private void rebuildTree(@NotNull List<TaskLocation> locations) {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
        root.removeAllChildren();

        Map<VirtualFile, List<TaskLocation>> tasksByFile = new HashMap<>();
        for (TaskLocation location : locations) {
            tasksByFile.computeIfAbsent(location.file, key -> new ArrayList<>()).add(location);
        }

        VirtualFile baseDir = project.getBaseDir();
        List<Map.Entry<VirtualFile, List<TaskLocation>>> entries = new ArrayList<>(tasksByFile.entrySet());
        entries.sort((left, right) -> {
            String leftPath = getRelativePath(baseDir, left.getKey());
            String rightPath = getRelativePath(baseDir, right.getKey());
            return leftPath.compareToIgnoreCase(rightPath);
        });

        for (Map.Entry<VirtualFile, List<TaskLocation>> entry : entries) {
            VirtualFile file = entry.getKey();
            List<TaskLocation> fileLocations = entry.getValue();
            fileLocations.sort((left, right) -> Integer.compare(left.line, right.line));
            String relativePath = getRelativePath(baseDir, file);
            NodeData fileData = new NodeData(NodeType.FILE, relativePath);
            fileData.file = file;
            fileData.locations = fileLocations;
            DefaultMutableTreeNode fileNode = new DefaultMutableTreeNode(fileData);
            root.add(fileNode);

            for (TaskLocation location : fileLocations) {
                NodeData taskData = new NodeData(NodeType.TASK, location.displayName);
                taskData.file = file;
                taskData.location = location;
                DefaultMutableTreeNode taskNode = new DefaultMutableTreeNode(taskData);
                fileNode.add(taskNode);
            }
        }

        treeModel.reload();
        tree.expandRow(0);
    }

    /**
     * 获取相对于基础目录的相对路径
     * <p> 如果基础目录为 null, 则直接返回文件的完整路径; 否则使用 {@code VfsUtilCore.getRelativePath} 计算相对路径, 若计算失败则返回文件完整路径.
     *
     * @param baseDir 基础目录, 可为空
     * @param file    目标文件, 不能为空
     * @return 相对路径, 若无法计算则返回文件的完整路径
     */
    private String getRelativePath(@Nullable VirtualFile baseDir, @NotNull VirtualFile file) {
        if (baseDir == null) {
            return file.getPath();
        }
        String relative = VfsUtilCore.getRelativePath(file, baseDir, '/');
        return relative != null ? relative : file.getPath();
    }

    /**
     * 根据文档任务列表构建位置信息列表
     * <p> 遍历传入的文档任务列表, 为每个任务获取其对应的源码文件, 行号及显示名称, 并封装为 TaskLocation 对象, 最终返回包含所有任务位置信息的列表.
     * 该方法在只读操作上下文中执行, 确保线程安全.
     *
     * @param tasks 文档任务列表, 不能为空
     * @return 包含所有任务位置信息的列表, 每个元素代表一个任务在源码中的位置和显示名称
     */
    private List<TaskLocation> buildLocations(@NotNull List<DocumentationTask> tasks) {
        return ApplicationManager.getApplication().runReadAction((Computable<List<TaskLocation>>) () -> {
            List<TaskLocation> locations = new ArrayList<>();
            for (DocumentationTask task : tasks) {
                PsiElement element = task.getElement();
                if (!element.isValid()) {
                    continue;
                }
                PsiFile psiFile = element.getContainingFile();
                if (psiFile == null || psiFile.getVirtualFile() == null) {
                    continue;
                }
                VirtualFile file = psiFile.getVirtualFile();
                Document document = PsiDocumentManager.getInstance(project).getDocument(psiFile);
                int line = 0;
                if (document != null) {
                    line = Math.max(0, document.getLineNumber(element.getTextOffset()));
                }
                String displayName = buildTaskDisplayName(task, line);
                locations.add(new TaskLocation(task, file, line, displayName));
            }
            return locations;
        });
    }

    /**
     * 构建任务的显示名称
     * <p> 根据给定的文档任务和行号, 生成一个包含元素名称和行号信息的字符串, 用于在界面上展示任务位置.</p>
     *
     * @param task 文档任务对象, 包含需要展示的元素信息
     * @param line 行号, 表示任务在文件中的具体位置 (从 0 开始)
     * @return 包含元素名称和行号信息的字符串, 格式为 "元素名称: 行号"
     */
    private String buildTaskDisplayName(@NotNull DocumentationTask task, int line) {
        String lineInfo = line >= 0 ? " :" + (line + 1) : "";
        return task.getElementName() + lineInfo;
    }

    /**
     * 在编辑器中显示指定位置的代码
     * <p> 根据给定的 TaskLocation 定位到文件并滚动至对应行, 使该位置在编辑器中可见.
     *
     * @param location 包含文件和行号信息的位置对象
     */
    private void showInEditor(@NotNull TaskLocation location) {
        VirtualFile file = location.file;
        if (!file.isValid()) {
            VirtualFile refreshed = LocalFileSystem.getInstance().findFileByPath(file.getPath());
            if (refreshed == null || !refreshed.isValid()) {
                return;
            }
            file = refreshed;
        }

        if (!Objects.equals(currentFile, file)) {
            replaceEditor(file);
        }
        if (currentEditor == null) {
            return;
        }
        Editor editor = currentEditor;
        Document document = editor.getDocument();
        int line = Math.min(Math.max(0, location.line), Math.max(0, document.getLineCount() - 1));
        int offset = document.getLineStartOffset(line);
        editor.getCaretModel().moveToOffset(offset);
        editor.getScrollingModel().scrollToCaret(ScrollType.MAKE_VISIBLE);
    }

    /**
     * 替换当前编辑器为指定文件的编辑器
     * <p> 该方法用于在界面中切换显示的代码文件, 释放旧的编辑器资源, 并加载新的文件内容到编辑区域.
     *
     * @param file 要加载的文件对象, 表示需要展示的源码文件
     */
    private void replaceEditor(@NotNull VirtualFile file) {
        releaseEditor();
        Document document = FileDocumentManager.getInstance().getDocument(file);
        if (document == null) {
            showEditorPlaceholder();
            return;
        }
        Editor editor = createEditor(document, file);
        currentEditor = editor;
        currentFile = file;
        editorHolder.removeAll();
        editorHolder.add(editor.getComponent(), BorderLayout.CENTER);
        editorHolder.revalidate();
        editorHolder.repaint();
    }

    /**
     * 获取组件面板
     * <p> 返回当前实例的根面板组件, 该组件包含了工具窗口中的文件树和编辑器视图.
     *
     * @return 根面板组件
     */
    public @NotNull JPanel getComponent() {
        return rootPanel;
    }

    /**
     * 释放当前面板所持有的资源
     * <p> 若当前编辑器实例不为 null, 则调用 {@link EditorFactory#releaseEditor} 释放其占用的所有资源, 随后将编辑器实例与文件对象置为 null, 以便垃圾回收.
     *
     * @see Disposable#dispose()
     */
    @Override
    public void dispose() {
        releaseEditor();
    }

    /**
     * 释放当前编辑器所占用的资源
     * <p> 若当前编辑器实例不为 null, 则调用 {@link EditorFactory#releaseEditor} 释放其占用的所有资源, 随后将编辑器实例与文件对象置为 null, 以便垃圾回收.
     *
     * @see Disposable#dispose()
     */
    private void releaseEditor() {
        if (currentEditor != null) {
            EditorFactory.getInstance().releaseEditor(currentEditor);
            currentEditor = null;
            currentFile = null;
        }
    }

    /**
     * 显示编辑器占位符界面
     * <p> 当编辑器中无有效文件内容时, 显示提示标签以告知用户当前区域为空. 此方法用于在切换文件或加载失败时, 保持界面布局完整并提供友好提示.</p>
     *
     * @see JBLabel
     * @see BorderLayout
     * @see JPanel#revalidate()* @see JPanel#repaint()
     */
    private void showEditorPlaceholder() {
        editorHolder.removeAll();
        editorHolder.add(new JBLabel(JavadocBundle.message("commit.check.javadoc.details.editor.placeholder")),
                         BorderLayout.CENTER);
        editorHolder.revalidate();
        editorHolder.repaint();
    }

    /**
     * 创建一个编辑器实例用于显示指定文件的代码内容
     * <p> 优先尝试通过反射调用包含 {@code EditorKind} 参数的 {@code createEditor} 方法, 若失败则回退到无参版本. 该方法用于在界面中加载并显示指定文件的源码内容.
     *
     * @param document 与文件关联的文档对象, 不能为空
     * @param file     要加载的虚拟文件对象, 不能为空
     * @return 创建的编辑器实例, 用于在 UI 中显示文件内容
     */
    private Editor createEditor(@NotNull Document document, @NotNull VirtualFile file) {
        EditorFactory factory = EditorFactory.getInstance();
        try {
            Method method = EditorFactory.class.getMethod("createEditor",
                                                          Document.class,
                                                          Project.class,
                                                          VirtualFile.class,
                                                          boolean.class,
                                                          EditorKind.class);
            return (Editor) method.invoke(factory, document, project, file, false, EditorKind.MAIN_EDITOR);
        } catch (ReflectiveOperationException ignored) {
            return factory.createEditor(document, project, file, false);
        }
    }

    /**
     * 节点类型枚举
     * <p> 用于标识 Javadoc 详情面板中树形结构的各种节点类型, 支持根节点, 目录节点, 文件节点和任务节点四种类型
     * <p> 该枚举在 CommitJavadocDetailsPanel 的树形视图中使用, 帮助区分不同层级和用途的节点, 以便正确渲染和交互
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.20
     * @since x.x.x
     */
    private enum NodeType {
        /** 根节点类型, 表示树结构的顶层节点 */
        ROOT,
        /** 目录节点类型 */
        DIR,
        /** 文件类型的节点 */
        FILE,
        /** 任务节点类型 */
        TASK
    }

    /**
     * 节点数据封装类
     * <p> 用于在 Javadoc 任务树结构中表示不同层级的节点信息, 包括节点类型, 显示名称, 关联文件及任务位置等元数据. 该类作为树形结构中每个节点的数据载体, 支持文件, 目录, 任务等不同类型的节点渲染与定位.</p>
     * <p> 该类为不可变数据结构, 通过构造函数初始化核心属性, 支持通过类型区分节点用途, 并可关联具体任务或文件位置, 用于在 UI 中展示缺失 Javadoc 的代码元素及其在源码中的位置.</p>
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.20
     * @since 1.0.0
     */
    private static final class NodeData {
        /** 节点类型, 用于标识节点的类别或用途 */
        private final NodeType type;
        /** 显示名称 */
        private final String displayName;
        /** 文件对象, 表示当前节点关联的虚拟文件 */
        private VirtualFile file;
        /** 任务位置信息 */
        private TaskLocation location;
        /**
         * 任务位置列表, 用于存储多个任务在代码中的定位信息
         * <p> 通常用于代码导航, 调试或任务追踪场景
         *
         * @see TaskLocation
         */
        private List<TaskLocation> locations;

        /**
         * 构造一个 NodeData 实例
         * <p> 初始化节点数据, 设置节点类型和显示名称
         *
         * @param type        节点类型, 不能为空
         * @param displayName 节点的显示名称, 不能为空
         */
        private NodeData(@NotNull NodeType type, @NotNull String displayName) {
            this.type = type;
            this.displayName = displayName;
        }
    }

    /**
     * 任务位置记录类
     * <p> 用于封装缺失 Javadoc 的文档任务在源码中的具体位置信息, 包括任务对象, 文件, 行号和显示名称 </p>
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.20
     * @since 1.0.0
     */
    private record TaskLocation(DocumentationTask task, VirtualFile file, int line, String displayName) {
        /**
         * 创建一个任务位置实例
         * <p> 该构造方法用于初始化 TaskLocation 对象, 记录任务对应的文件位置信息 </p>
         *
         * @param task        文档任务对象, 不能为 null
         * @param file        虚拟文件对象, 不能为 null
         * @param line        文件中的行号
         * @param displayName 显示名称, 不能为 null
         */
        private TaskLocation(@NotNull DocumentationTask task,
                             @NotNull VirtualFile file,
                             int line,
                             @NotNull String displayName) {
            this.task = task;
            this.file = file;
            this.line = line;
            this.displayName = displayName;
        }
    }

    /**
     * Javadoc 树形组件渲染器
     * <p>用于自定义 Javadoc 详情树中各个节点的显示样式, 根据节点类型 (目录, 文件或任务) 设置对应的图标, 以提升界面的可读性与交互体验
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.20
     * @since 1.0.0
     */
    private static final class JavadocTreeCellRenderer extends DefaultTreeCellRenderer {
        /**
         * 重写树节点渲染器组件, 用于自定义树形结构中每个节点的显示样式
         * <p>在渲染树节点时, 根据节点数据对象 (NodeData) 设置显示文本和图标, 支持文件夹, 文件, 任务等不同类型的节点渲染</p>
         *
         * @param tree     树组件实例
         * @param value    当前节点的数据对象
         * @param selected 是否被选中
         * @param expanded 是否展开
         * @param leaf     是否为叶子节点
         * @param row      当前行号
         * @param hasFocus 是否获得焦点
         * @return 渲染后的组件实例
         */
        @Override
        public java.awt.Component getTreeCellRendererComponent(javax.swing.JTree tree,
                                                               Object value,
                                                               boolean selected,
                                                               boolean expanded,
                                                               boolean leaf,
                                                               int row,
                                                               boolean hasFocus) {
            java.awt.Component component = super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
            if (value instanceof DefaultMutableTreeNode node) {
                Object userObject = node.getUserObject();
                if (userObject instanceof NodeData nodeData) {
                    setText(nodeData.displayName);
                    setIcon(resolveIcon(nodeData));
                }
            }
            return component;
        }

        /**
         * 根据节点数据解析并返回对应的图标
         * <p> 根据传入的 NodeData 对象类型和相关属性, 返回适合显示的图标. 支持目录, 文件以及任务类型.</p>
         *
         * @param data 节点数据对象, 包含类型和其他元信息
         * @return 返回与节点类型匹配的图标对象
         * @since 1.0
         */
        private javax.swing.Icon resolveIcon(@NotNull NodeData data) {
            if (data.type == NodeType.DIR) {
                return AllIcons.Nodes.Folder;
            }
            if (data.type == NodeType.FILE && data.file != null) {
                return data.file.getFileType().getIcon();
            }
            if (data.type == NodeType.TASK && data.location != null) {
                DocumentationTask.TaskType type = data.location.task.getType();
                return switch (type) {
                    case CLASS -> AllIcons.Nodes.Class;
                    case INTERFACE -> AllIcons.Nodes.Interface;
                    case ENUM -> AllIcons.Nodes.Enum;
                    case FIELD -> AllIcons.Nodes.Field;
                    case METHOD, TEST_METHOD -> AllIcons.Nodes.Method;
                };
            }
            return AllIcons.Nodes.Folder;
        }
    }
}
