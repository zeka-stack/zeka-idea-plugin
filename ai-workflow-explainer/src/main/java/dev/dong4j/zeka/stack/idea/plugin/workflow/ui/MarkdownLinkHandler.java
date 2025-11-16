package dev.dong4j.zeka.stack.idea.plugin.workflow.ui;

import com.intellij.ide.scratch.ScratchFileService;
import com.intellij.ide.scratch.ScratchRootType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;

import org.intellij.plugins.markdown.extensions.MarkdownBrowserPreviewExtension;
import org.intellij.plugins.markdown.ui.preview.MarkdownHtmlPanel;
import org.intellij.plugins.markdown.ui.preview.ResourceProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Markdown 链接处理器
 * <p>
 * 处理 Markdown 预览中的链接点击事件，支持：
 * <ul>
 *   <li>file:// 协议：跳转到代码文件指定位置</li>
 *   <li>scratch:// 协议：打开临时文件</li>
 * </ul>
 *
 * @author dong4j
 * @version 1.0.0
 */
public class MarkdownLinkHandler implements MarkdownBrowserPreviewExtension, ResourceProvider {

    private static final String OPEN_LINK_EVENT_NAME = "openLink";

    /** file:// 链接格式：file://文件路径:行号:列号 */
    private static final Pattern FILE_LINK_PATTERN = Pattern.compile("java://(.+?):(\\d+):(\\d+)");

    /** file:// 链接格式（只有行号）：file://文件路径:行号 */
    private static final Pattern FILE_LINK_PATTERN_SIMPLE = Pattern.compile("java://(.+?):(\\d+)");

    /** scratch:// 链接格式：scratch://文件名 */
    private static final Pattern SCRATCH_LINK_PATTERN = Pattern.compile("scratch://(.+)");

    private final MarkdownHtmlPanel panel;
    private final Project project;

    /**
     * 构造函数
     *
     * @param panel   Markdown HTML 面板
     * @param project 项目实例
     */
    public MarkdownLinkHandler(@NotNull MarkdownHtmlPanel panel, @NotNull Project project) {
        this.panel = panel;
        this.project = project;

        // 订阅链接点击事件
        // 使用公开的 API 访问 browserPipe
        var browserPipe = panel.getBrowserPipe();
        if (browserPipe != null) {
            browserPipe.subscribe(OPEN_LINK_EVENT_NAME, this::handleLink);
            Disposer.register(this, () -> {
                browserPipe.removeSubscription(OPEN_LINK_EVENT_NAME, this::handleLink);
            });
        }
    }

    /**
     * 处理链接点击事件
     *
     * @param link 链接地址
     */
    private void handleLink(@NotNull String link) {
        ApplicationManager.getApplication().invokeLater(() -> {
            try {
                // 处理 file:// 链接
                if (link.startsWith("java://")) {
                    handleFileLink(link);
                    return;
                }

                // 处理 scratch:// 链接
                if (link.startsWith("scratch://")) {
                    handleScratchLink(link);
                }
            } catch (Exception e) {
                // 忽略错误，避免影响其他功能
            }
        });
    }

    /**
     * 处理 file:// 链接
     *
     * @param link 链接地址（格式：file://文件路径:行号:列号 或 file://文件路径:行号）
     */
    private void handleFileLink(@NotNull String link) {
        // 尝试匹配完整格式（包含列号）
        Matcher matcher = FILE_LINK_PATTERN.matcher(link);
        if (matcher.matches()) {
            String filePath = matcher.group(1);
            int line = Integer.parseInt(matcher.group(2)) - 1; // 转换为 0-based
            int column = Integer.parseInt(matcher.group(3)) - 1; // 转换为 0-based

            VirtualFile virtualFile = LocalFileSystem.getInstance().findFileByPath(filePath);
            if (virtualFile != null) {
                OpenFileDescriptor descriptor = new OpenFileDescriptor(project, virtualFile, line, column);
                descriptor.navigate(true);
            }
            return;
        }

        // 尝试匹配简单格式（只有行号）
        matcher = FILE_LINK_PATTERN_SIMPLE.matcher(link);
        if (matcher.matches()) {
            String filePath = matcher.group(1);
            int line = Integer.parseInt(matcher.group(2)) - 1; // 转换为 0-based

            VirtualFile virtualFile = LocalFileSystem.getInstance().findFileByPath(filePath);
            if (virtualFile != null) {
                OpenFileDescriptor descriptor = new OpenFileDescriptor(project, virtualFile, line, 0);
                descriptor.navigate(true);
            }
        }
    }

    /**
     * 处理 scratch:// 链接
     *
     * @param link 链接地址（格式：scratch://文件名）
     */
    private void handleScratchLink(@NotNull String link) {
        Matcher matcher = SCRATCH_LINK_PATTERN.matcher(link);
        if (matcher.matches()) {
            String fileName = matcher.group(1);

            // 查找 scratch 文件
            ScratchRootType rootType = ScratchRootType.getInstance();
            try {
                VirtualFile scratchFile = ScratchFileService.getInstance().findFile(rootType,
                                                                                    fileName,
                                                                                    ScratchFileService.Option.existing_only);
                if (scratchFile != null) {
                    // 打开文件
                    com.intellij.openapi.fileEditor.FileEditorManager.getInstance(project)
                        .openFile(scratchFile, true);
                }
            } catch (IOException ignored) {
            }

        }
    }

    @Override
    public void dispose() {
        // 清理资源
    }

    @Override
    public boolean canProvide(@NotNull String resourceName) {
        return false;
    }

    @Override
    @Nullable
    public Resource loadResource(@NotNull String resourceName) {
        return null;
    }

    /**
     * Provider 类，用于创建 MarkdownLinkHandler 实例
     */
    public static class Provider implements MarkdownBrowserPreviewExtension.Provider {
        @Override
        @Nullable
        public MarkdownBrowserPreviewExtension createBrowserExtension(@NotNull MarkdownHtmlPanel panel) {
            // 使用公开的 API 获取 Project
            Project project = panel.getProject();
            if (project == null) {
                return null;
            }

            return new MarkdownLinkHandler(panel, project);
        }
    }
}

