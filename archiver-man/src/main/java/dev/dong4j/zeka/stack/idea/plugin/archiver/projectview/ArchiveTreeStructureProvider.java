package dev.dong4j.zeka.stack.idea.plugin.archiver.projectview;

import com.intellij.ide.projectView.TreeStructureProvider;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFileSystemItem;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 使 ZIP/JAR 在 Project View 中可展开的结构提供者
 *
 * @author dong4j
 * @since 0.2.0
 */
@SuppressWarnings("D")
public final class ArchiveTreeStructureProvider implements TreeStructureProvider {

    private static final String ZIP = "zip";
    private static final String JAR = "jar";

    @Override
    public @NotNull Collection<AbstractTreeNode<?>> modify(@NotNull AbstractTreeNode<?> parent,
                                                           @NotNull Collection<AbstractTreeNode<?>> children,
                                                           ViewSettings settings) {
        Project project = parent.getProject();
        if (project == null) {
            return children;
        }

        Object value = parent.getValue();
        if (!(value instanceof PsiElement element)) {
            return children;
        }

        VirtualFile virtualFile = null;
        if (value instanceof PsiFileSystemItem item) {
            virtualFile = item.getVirtualFile();
        } else if (element.getContainingFile() != null) {
            virtualFile = element.getContainingFile().getVirtualFile();
        }

        if (virtualFile == null || !isArchive(virtualFile)) {
            return children;
        }

        VirtualFile archiveRoot = JarFileSystem.getInstance().getJarRootForLocalFile(virtualFile);
        if (archiveRoot == null) {
            return children;
        }

        List<AbstractTreeNode<?>> archiveNodes = new ArrayList<>();
        VirtualFile[] archiveChildren = archiveRoot.getChildren();
        if (archiveChildren != null) {
            for (VirtualFile child : archiveChildren) {
                if (child != null && child.isValid()) {
                    archiveNodes.add(new ArchiveEntryTreeNode(project, child, settings));
                }
            }
        }
        return archiveNodes.isEmpty() ? children : archiveNodes;
    }

    private boolean isArchive(@NotNull VirtualFile file) {
        String extension = file.getExtension();
        if (extension == null) {
            return false;
        }
        String lower = extension.toLowerCase();
        return ZIP.equals(lower) || JAR.equals(lower);
    }
}

