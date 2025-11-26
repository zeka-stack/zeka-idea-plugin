package dev.dong4j.zeka.stack.idea.plugin.archiver.projectview.provider;

import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

import dev.dong4j.zeka.stack.idea.plugin.archiver.projectview.ArchiveEntryTreeNode;

/**
 * 定义归档格式在 Project View 中的展示能力。
 * <p>
 * 后续扩展新格式（如 Tar、7z）仅需实现该接口。
 *
 * @author dong4j
 * @since 0.3.0
 */
public interface ArchiveFormatProvider {

    /**
     * 当前 provider 是否支持该本地文件。
     *
     * @param file 本地磁盘上的归档文件
     * @return true 表示支持
     */
    boolean supports(@NotNull VirtualFile file);

    /**
     * 获取归档根节点（通常是 {@link com.intellij.openapi.vfs.newvfs.ArchiveFileSystem} 的根目录）。
     *
     * @param localArchiveFile 本地归档文件
     * @return 归档根节点，获取失败时返回 null
     */
    @Nullable
    VirtualFile getArchiveRoot(@NotNull VirtualFile localArchiveFile);

    /**
     * 构建归档在 Project View 中的展示节点。
     *
     * @param project      项目
     * @param archiveRoot  归档根目录
     * @param viewSettings 视图设置
     * @return 节点集合
     */
    default @NotNull Collection<? extends AbstractTreeNode<?>> buildNodes(@NotNull Project project,
                                                                          @NotNull VirtualFile archiveRoot,
                                                                          @NotNull ViewSettings viewSettings) {
        VirtualFile[] children = archiveRoot.getChildren();
        if (children == null || children.length == 0) {
            return List.of();
        }
        return java.util.Arrays.stream(children)
            .filter(child -> child != null && child.isValid())
            .map(child -> new ArchiveEntryTreeNode(project, child, viewSettings))
            .collect(java.util.stream.Collectors.toList());
    }
}

