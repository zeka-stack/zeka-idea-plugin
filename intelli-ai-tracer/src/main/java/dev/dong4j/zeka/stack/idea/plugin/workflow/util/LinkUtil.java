package dev.dong4j.zeka.stack.idea.plugin.workflow.util;

import com.intellij.ide.scratch.ScratchFileService;
import com.intellij.ide.scratch.ScratchRootType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;

import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 链接处理工具类
 * <p>
 * 提供统一的链接处理功能，支持：
 * <ul>
 *   <li>java:// 协议：跳转到代码文件指定位置</li>
 *   <li>scratch:// 协议：打开临时文件</li>
 * </ul>
 *
 * @author dong4j
 * @version 1.0.0
 */
public final class LinkUtil {

    /** java:// 链接格式：java://文件路径:行号:列号 */
    private static final Pattern FILE_LINK_PATTERN = Pattern.compile("java://(.+?):(\\d+):(\\d+)");

    /** java:// 链接格式（只有行号）：java://文件路径:行号 */
    private static final Pattern FILE_LINK_PATTERN_SIMPLE = Pattern.compile("java://(.+?):(\\d+)");

    /** scratch:// 链接格式：scratch://文件名 */
    private static final Pattern SCRATCH_LINK_PATTERN = Pattern.compile("scratch://(.+)");

    private LinkUtil() {
        // 工具类，禁止实例化
    }

    /**
     * 处理链接点击事件
     *
     * @param project 项目实例
     * @param link    链接地址
     * @return 是否成功处理链接
     */
    public static boolean handleLink(@NotNull Project project, @NotNull String link) {
        // 确保在 EDT 中执行
        ApplicationManager.getApplication().invokeLater(() -> {
            try {
                // 处理 java:// 链接
                if (link.startsWith("java://")) {
                    handleFileLink(project, link);
                } else if (link.startsWith("scratch://")) {
                    // 处理 scratch:// 链接
                    handleScratchLink(project, link);
                }
            } catch (Exception e) {
                // 忽略错误，避免影响其他功能
            }
        });

        // 立即返回 true，表示已接受处理（异步执行）
        return true;
    }

    /**
     * 处理文件链接（java:// 协议）
     *
     * @param project 项目实例
     * @param link    文件链接
     */
    private static void handleFileLink(@NotNull Project project, @NotNull String link) {
        // 尝试完整格式：java://文件路径:行号:列号
        Matcher matcher = FILE_LINK_PATTERN.matcher(link);
        if (matcher.matches()) {
            String filePath = matcher.group(1);
            int line = Integer.parseInt(matcher.group(2)) - 1; // 转换为 0 基索引
            int column = Integer.parseInt(matcher.group(3)) - 1; // 转换为 0 基索引

            openFile(project, filePath, line, column);
            return;
        }

        // 尝试简单格式：java://文件路径:行号
        matcher = FILE_LINK_PATTERN_SIMPLE.matcher(link);
        if (matcher.matches()) {
            String filePath = matcher.group(1);
            int line = Integer.parseInt(matcher.group(2)) - 1; // 转换为 0 基索引

            openFile(project, filePath, line, 0);
        }
    }

    /**
     * 处理临时文件链接（scratch:// 协议）
     *
     * @param project 项目实例
     * @param link    临时文件链接
     */
    private static void handleScratchLink(@NotNull Project project, @NotNull String link) {
        Matcher matcher = SCRATCH_LINK_PATTERN.matcher(link);
        if (!matcher.matches()) {
            return;
        }

        String fileName = matcher.group(1);

        try {
            // 查找 scratch 文件
            VirtualFile scratchFile = ScratchFileService.getInstance()
                .findFile(ScratchRootType.getInstance(), fileName, ScratchFileService.Option.existing_only);

            if (scratchFile != null) {
                new OpenFileDescriptor(project, scratchFile).navigate(true);
            }
        } catch (Exception e) {
            // 忽略异常
        }
    }

    /**
     * 打开文件并跳转到指定位置
     *
     * @param project  项目实例
     * @param filePath 文件路径
     * @param line     行号（0 基索引）
     * @param column   列号（0 基索引）
     */
    private static void openFile(@NotNull Project project, @NotNull String filePath, int line, int column) {
        VirtualFile file = LocalFileSystem.getInstance().findFileByPath(filePath);
        if (file != null) {
            OpenFileDescriptor descriptor = new OpenFileDescriptor(project, file, line, column);
            descriptor.navigate(true);
        }
    }

    /**
     * 生成文件链接
     *
     * @param filePath 文件路径
     * @param line     行号（1 基索引）
     * @param column   列号（1 基索引）
     * @return 文件链接
     */
    @NotNull
    public static String createFileLink(@NotNull String filePath, int line, int column) {
        return String.format("java://%s:%d:%d", filePath, line, column);
    }

    /**
     * 生成临时文件链接
     *
     * @param fileName 文件名
     * @return 临时文件链接
     */
    @NotNull
    public static String createScratchLink(@NotNull String fileName) {
        return String.format("scratch://%s", fileName);
    }
}
