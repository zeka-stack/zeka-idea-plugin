package dev.dong4j.zeka.stack.idea.plugin.archiver.projectview;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.PlatformIcons;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import dev.dong4j.zeka.stack.idea.plugin.archiver.util.ArchiverBundle;
import dev.dong4j.zeka.stack.idea.plugin.archiver.util.ArchiverFeatureToggles;

/**
 * Project View 中的压缩条目节点
 *
 * @author dong4j
 * @since 0.2.0
 */
public final class ArchiveEntryTreeNode extends AbstractTreeNode<VirtualFile> {
    private final ViewSettings viewSettings;

    public ArchiveEntryTreeNode(@NotNull Project project, @NotNull VirtualFile virtualFile, @NotNull ViewSettings viewSettings) {
        super(project, virtualFile);
        this.viewSettings = viewSettings;
    }

    @Override
    public @NotNull Collection<? extends AbstractTreeNode<?>> getChildren() {
        VirtualFile value = getValue();
        if (value == null || !value.isValid() || !value.isDirectory()) {
            return java.util.Collections.emptyList();
        }

        List<AbstractTreeNode<?>> nodes = new ArrayList<>();
        VirtualFile[] children = value.getChildren();
        if (children != null) {
            for (VirtualFile child : children) {
                if (child != null && child.isValid()) {
                    nodes.add(new ArchiveEntryTreeNode(getProject(), child, viewSettings));
                }
            }
        }
        return nodes;
    }

    @Override
    protected void update(@NotNull PresentationData presentation) {
        VirtualFile value = getValue();
        if (value == null || !value.isValid()) {
            return;
        }
        presentation.setPresentableText(value.getName());
        presentation.setIcon(value.isDirectory() ? PlatformIcons.FOLDER_ICON : resolveIcon(value));
        if (ArchiverFeatureToggles.showEditableBadge()) {
            String badge = ArchiverFeatureToggles.isEditableModeEnabled()
                           ? ArchiverBundle.message("label.archive.editable")
                           : ArchiverBundle.message("label.archive.readonly");
            presentation.setLocationString(badge);
        }
    }

    @Nullable
    private javax.swing.Icon resolveIcon(@NotNull VirtualFile file) {
        FileType type = FileTypeManager.getInstance().getFileTypeByFileName(file.getName());
        return type.getIcon();
    }

    @Override
    public boolean canNavigate() {
        VirtualFile value = getValue();
        return value != null && value.isValid() && !value.isDirectory();
    }

    @Override
    public void navigate(boolean requestFocus) {
        VirtualFile value = getValue();
        if (value == null || !value.isValid() || value.isDirectory()) {
            return;
        }
        new OpenFileDescriptor(getProject(), value).navigate(requestFocus);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ArchiveEntryTreeNode node = (ArchiveEntryTreeNode) obj;
        VirtualFile value = getValue();
        return value != null && value.equals(node.getValue());
    }

    @Override
    public int hashCode() {
        VirtualFile value = getValue();
        return value != null ? value.hashCode() : 0;
    }
}

