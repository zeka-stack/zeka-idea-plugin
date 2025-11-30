package dev.dong4j.zeka.stack.idea.plugin.workflow.ui;

import com.intellij.ide.scratch.ScratchFileService;
import com.intellij.ide.scratch.ScratchRootType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.util.ui.JBUI;

import org.intellij.plugins.markdown.ui.preview.MarkdownHtmlPanel;
import org.intellij.plugins.markdown.ui.preview.MarkdownHtmlPanelProvider;
import org.intellij.plugins.markdown.ui.preview.html.MarkdownUtil;
import org.jetbrains.annotations.NotNull;

import java.awt.BorderLayout;
import java.io.IOException;

import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * 工作流结果展示工具窗口（使用 MarkdownHtmlPanel 渲染）
 * <p>
 * 使用 IntelliJ IDEA 内置的 MarkdownHtmlPanel 来显示工作流说明：
 * <ul>
 *   <li>直接使用 MarkdownHtmlPanel 渲染，不依赖 FileEditor</li>
 *   <li>自动适配主题样式</li>
 *   <li>支持 Markdown 设置页面的配置</li>
 *   <li>支持 Mermaid 图表渲染（如果安装了 Mermaid 插件）</li>
 * </ul>
 * <p>
 * 文件持久化到 Scratches and Consoles 的"扩展/IntelliAI Tracer"目录。
 * <p>
 * 项目级别服务，每个项目独立的工具窗口实例。
 *
 * @author dong4j
 * @version 1.0.0
 */
@Service(Service.Level.PROJECT)
public final class WorkflowResultToolWindow {

    /** 工具窗口 ID */
    public static final String TOOL_WINDOW_ID = "IntelliAI Tracer";

    /** 主面板 */
    private JPanel mainPanel;

    /** 项目实例 */
    private final Project project;

    /** 可刷新的 Markdown 预览面板 */
    private RefreshableMarkdownPanel previewPanel;

    /** 当前打开的文件 */
    private VirtualFile virtualFile;

    /** Document 对象 */
    private Document document;

    /**
     * 构造函数
     *
     * @param project 项目实例
     */
    public WorkflowResultToolWindow(@NotNull Project project) {
        this.project = project;
    }

    /**
     * 初始化工具窗口内容
     * <p>
     * 注意：此方法在 ToolWindowFactory 中调用，可能不在 EDT 线程中。
     *
     * @return 主面板组件
     */
    @NotNull
    public JComponent getContent() {
        if (mainPanel == null) {
            mainPanel = new JPanel(new BorderLayout());
            mainPanel.setBorder(JBUI.Borders.empty());
        }
        return mainPanel;
    }

    /**
     * 创建 scratch 文件并写入初始内容（阶段1：元数据）
     * <p>
     * 在调用 AI 之前调用，写入元数据（代码链接、文件链接、JSON 数据）。
     * 文件会持久化到 Scratches and Consoles 的"扩展/IntelliAI Tracer"目录。
     *
     * @param metadata Markdown 元数据内容
     * @param fileName 文件名（基于方法签名）
     * @return Scratch 文件的 VirtualFile
     */
    @NotNull
    public VirtualFile createScratchFileWithMetadata(@NotNull String metadata, @NotNull String fileName) {
        // 确保在 EDT 线程中执行
        if (!ApplicationManager.getApplication().isDispatchThread()) {
            VirtualFile[] result = new VirtualFile[1];
            ApplicationManager.getApplication().invokeAndWait(() -> {
                result[0] = doCreateScratchFileWithMetadata(metadata, fileName);
            });
            return result[0];
        }
        return doCreateScratchFileWithMetadata(metadata, fileName);
    }

    /**
     * 实际创建 scratch 文件（必须在 EDT 线程中调用）
     */
    @NotNull
    private VirtualFile doCreateScratchFileWithMetadata(@NotNull String metadata, @NotNull String fileName) {
        try {
            // 使用 ScratchFileService 创建 scratch 文件
            ScratchRootType rootType = ScratchRootType.getInstance();

            VirtualFile scratchFile = ScratchFileService.getInstance().findFile(rootType,
                                                                                fileName,
                                                                                ScratchFileService.Option.create_if_missing);
            if (scratchFile == null) {
                throw new IOException("无法创建或访问 Scratch 文件: " + fileName);
            }

            // 保存文件引用
            virtualFile = scratchFile;

            // 获取 Document（必须先获取，否则无法写入）
            document = FileDocumentManager.getInstance().getDocument(virtualFile);
            if (document == null) {
                throw new IOException("无法获取 Document: " + fileName);
            }

            // 在 WriteAction 中使用 Document API 写入内容
            // 只使用 Document API，避免直接写入文件，防止 File Cache Conflict
            ApplicationManager.getApplication().runWriteAction(() -> {
                document.setText(metadata);
            });

            // 初始化预览面板
            initPreviewPanel(metadata);

            // 显示工具窗口
            showToolWindow();

            return virtualFile;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create scratch file: " + e.getMessage(), e);
        }
    }

    /**
     * 初始化预览面板
     *
     * @param initialContent 初始内容
     */
    private void initPreviewPanel(@NotNull String initialContent) {
        if (previewPanel != null) {
            // 如果已经存在，更新内容
            previewPanel.updateMarkdown(initialContent);
            return;
        }

        try {
            // 确保主面板已初始化
            if (mainPanel == null) {
                getContent();
            }

            // 创建可刷新的 Markdown 预览面板
            previewPanel = new RefreshableMarkdownPanel(project, virtualFile, initialContent);

            // 添加到主面板
            mainPanel.removeAll();
            mainPanel.add(previewPanel, BorderLayout.CENTER);
            mainPanel.revalidate();
            mainPanel.repaint();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize preview panel: " + e.getMessage(), e);
        }
    }

    /**
     * 追加 AI 响应结果到文件（阶段2：AI 结果）
     * <p>
     * 在调用 AI 并获取响应后调用，追加 AI 结果到文件，使用 `---` 分隔。
     * <p>
     * 注意：只使用 Document API 更新内容，避免直接写入文件，防止 File Cache Conflict。
     *
     * @param aiResult AI 生成的 Markdown 结果
     */
    public void appendAIResult(@NotNull String aiResult) {
        // 确保在 EDT 线程中执行
        ApplicationManager.getApplication().invokeLater(() -> {
            if (document == null && virtualFile != null) {
                document = FileDocumentManager.getInstance().getDocument(virtualFile);
            }
            if (document != null) {
                ApplicationManager.getApplication().runWriteAction(() -> {
                    String separator = "\n\n---\n\n";
                    String currentText = document.getText();
                    String newText = currentText + separator + aiResult;

                    // 只使用 Document API 更新内容，不要直接写入文件
                    // Document 会自动同步到 VirtualFile，避免 File Cache Conflict
                    document.setText(newText);
                });

                // 更新预览面板
                if (previewPanel != null) {
                    String newContent = document.getText();
                    previewPanel.updateMarkdown(newContent);
                }
            }
        });
    }

    /**
     * 显示工具窗口
     */
    private void showToolWindow() {
        ApplicationManager.getApplication().invokeLater(() -> {
            ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
            ToolWindow toolWindow = toolWindowManager.getToolWindow(TOOL_WINDOW_ID);
            if (toolWindow != null) {
                toolWindow.show(null);
            }
        });
    }

    /**
     * 获取实例（静态方法）
     *
     * @param project 项目实例
     * @return 工具窗口实例
     */
    @NotNull
    public static WorkflowResultToolWindow getInstance(@NotNull Project project) {
        return project.getService(WorkflowResultToolWindow.class);
    }

    // --------------------------
    // 内部类：可刷新 Markdown Panel
    // --------------------------

    /**
     * 可刷新的 Markdown 预览面板
     * <p>
     * 使用 MarkdownHtmlPanel 直接渲染 Markdown 内容，不依赖 FileEditor。
     */
    private static class RefreshableMarkdownPanel extends JPanel {
        private final Project project;
        private final MarkdownHtmlPanel htmlPanel;
        private final VirtualFile markdownFile;
        private String currentContent;

        /**
         * 构造函数
         *
         * @param project 项目实例
         * @param markdownFile Markdown 文件
         * @param initialContent 初始内容
         */
        @SuppressWarnings("UnstableApiUsage")
        public RefreshableMarkdownPanel(@NotNull Project project,
                                        @NotNull VirtualFile markdownFile,
                                        @NotNull String initialContent) {
            super(new BorderLayout());
            this.project = project;
            this.markdownFile = markdownFile;
            this.currentContent = initialContent;

            // 创建 MarkdownHtmlPanel
            // 使用 getProviders() 获取第一个可用的 provider
            // 获取第一个可用 Provider
            MarkdownHtmlPanelProvider provider = MarkdownHtmlPanelProvider.getAvailableProviders().get(0);

            // 注意：createHtmlPanel() 是实验性 API，可能在未来版本中改变
            // 目前没有稳定的替代方案，如果未来 API 变更，需要相应更新
            // 创建 HTML 面板（会自动从 VirtualFile 读取内容）
            this.htmlPanel = provider.createHtmlPanel(project, markdownFile);

            add(this.htmlPanel.getComponent(), BorderLayout.CENTER);

            // 初次渲染：将 Markdown 转换为 HTML
            String htmlContent = convertMarkdownToHtml(project, initialContent);
            this.htmlPanel.setHtml(htmlContent, 0, null);
        }


        /**
         * 更新 Markdown 内容并刷新显示
         *
         * @param newContent 新的 Markdown 内容
         */
        public void updateMarkdown(@NotNull String newContent) {
            ApplicationManager.getApplication().invokeLater(() -> {
                try {
                    // 将 Markdown 转换为 HTML 后刷新显示
                    String htmlContent = convertMarkdownToHtml(project, newContent);
                    htmlPanel.setHtml(htmlContent, 0, null);
                    currentContent = newContent;
                } catch (Exception e) {
                    // 如果转换失败，尝试直接使用原始内容
                    try {
                        htmlPanel.setHtml(newContent, 0, null);
                        currentContent = newContent;
                    } catch (Exception ex) {
                        // 忽略错误
                    }
                }
            });
        }


        /**
         * 获取 Markdown 文件
         *
         * @return Markdown 文件
         */
        @NotNull
        public VirtualFile getMarkdownFile() {
            return markdownFile;
        }

        /**
         * 释放资源
         */
        public void dispose() {
            if (htmlPanel != null) {
                htmlPanel.dispose();
            }
        }

        /**
         * 将 Markdown 转换为 HTML
         *
         * @param project 项目实例
         * @param markdown Markdown 内容
         * @return HTML 内容
         */
        @NotNull
        private String convertMarkdownToHtml(@NotNull Project project, @NotNull String markdown) {
            try {
                // 使用 IntelliJ IDEA 内置的 Markdown 转 HTML 转换器
                // MarkdownUtil.generateMarkdownHtml 需要 VirtualFile, text, project
                return MarkdownUtil.INSTANCE.generateMarkdownHtml(markdownFile, markdown, project);
            } catch (Exception e) {
                // 如果转换失败，返回原始 Markdown（作为 HTML 文本显示）
                return "<pre>" + markdown.replace("<", "&lt;").replace(">", "&gt;") + "</pre>";
            }
        }
    }
}
