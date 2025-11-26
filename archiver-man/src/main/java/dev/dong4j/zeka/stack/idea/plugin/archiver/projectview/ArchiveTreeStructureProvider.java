package dev.dong4j.zeka.stack.idea.plugin.archiver.projectview;

import com.intellij.ide.projectView.TreeStructureProvider;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFileSystemItem;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;

import dev.dong4j.zeka.stack.idea.plugin.archiver.core.NestedArchiveCacheService;
import dev.dong4j.zeka.stack.idea.plugin.archiver.projectview.provider.ArchiveFormatProvider;
import dev.dong4j.zeka.stack.idea.plugin.archiver.projectview.provider.ArchiveFormatRegistry;
import dev.dong4j.zeka.stack.idea.plugin.archiver.util.ArchiverFeatureToggles;

/**
 * Project View 中的归档展开结构提供者。
 *
 * @author dong4j
 * @since 0.2.0
 */
public final class ArchiveTreeStructureProvider implements TreeStructureProvider {

    private static final Logger LOG = Logger.getInstance(ArchiveTreeStructureProvider.class);
    private final ArchiveFormatRegistry registry = ArchiveFormatRegistry.getInstance();
    private final NestedArchiveCacheService cacheService = NestedArchiveCacheService.getInstance();

    @Override
    public @NotNull Collection<AbstractTreeNode<?>> modify(@NotNull AbstractTreeNode<?> parent,
                                                           @NotNull Collection<AbstractTreeNode<?>> children,
                                                           ViewSettings settings) {
        Collection<AbstractTreeNode<?>> originalSnapshot = new java.util.ArrayList<>(children);
        if (!ArchiverFeatureToggles.ENABLE_ARCHIVE_BROWSER) {
            return originalSnapshot;
        }
        Project project = parent.getProject();
        if (project == null) {
            return originalSnapshot;
        }

        Object value = parent.getValue();
        if (!(value instanceof PsiElement element)) {
            return originalSnapshot;
        }

        VirtualFile virtualFile = null;
        if (value instanceof PsiFileSystemItem item) {
            virtualFile = item.getVirtualFile();
        } else if (element.getContainingFile() != null) {
            virtualFile = element.getContainingFile().getVirtualFile();
        }

        if (virtualFile == null || !virtualFile.isValid()) {
            return originalSnapshot;
        }

        ArchiveFormatProvider provider = registry.findProvider(virtualFile);
        if (provider == null) {
            return originalSnapshot;
        }

        VirtualFile localFile = cacheService.toLocalIfNeeded(virtualFile);
        if (localFile == null) {
            return children;
        }

        try {
            VirtualFile archiveRoot = provider.getArchiveRoot(localFile);
            if (archiveRoot == null) {
                return originalSnapshot;
            }
            Collection<? extends AbstractTreeNode<?>> nodes = provider.buildNodes(project, archiveRoot, settings);
            if (nodes.isEmpty()) {
                return originalSnapshot;
            }
            return new java.util.ArrayList<>(nodes);
        } catch (Exception ex) {
            LOG.warn("Failed to build project view nodes for archive: " + localFile.getPath(), ex);
            return originalSnapshot;
        }
    }
}

