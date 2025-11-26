package dev.dong4j.zeka.stack.idea.plugin.archiver.action;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.ArchiveFileSystem;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

import dev.dong4j.zeka.stack.idea.plugin.archiver.core.ArchiveExtractionService;
import dev.dong4j.zeka.stack.idea.plugin.archiver.core.EditableArchiveService;
import dev.dong4j.zeka.stack.idea.plugin.archiver.core.EditableArchiveVirtualFile;
import dev.dong4j.zeka.stack.idea.plugin.archiver.icons.ArchiverIcons;
import dev.dong4j.zeka.stack.idea.plugin.archiver.util.ArchiverBundle;
import dev.dong4j.zeka.stack.idea.plugin.archiver.util.ArchiverFeatureToggles;
import dev.dong4j.zeka.stack.idea.plugin.archiver.util.NotificationUtil;

/**
 * 强制保存归档内容的 Action - 立即写回所有待处理的更改
 * <p>
 * 当用户感觉保存未生效时，可以使用此操作强制刷新所有待处理的归档更改。
 *
 * @author dong4j
 * @since 0.5.1
 */
public class ForceSaveArchiveAction extends AnAction {

    public ForceSaveArchiveAction() {
        super(
            ArchiverBundle.message("action.archive.force.save.title"),
            ArchiverBundle.message("action.archive.force.save.description"),
            ArchiverIcons.ARCHIVER_16
             );
    }

    @SuppressWarnings("D")
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        VirtualFile virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE);

        if (project == null) {
            NotificationUtil.showError(project, ArchiverBundle.message("error.no.project"));
            return;
        }

        if (virtualFile == null) {
            NotificationUtil.showError(project, ArchiverBundle.message("error.no.file"));
            return;
        }

        if (!ArchiverFeatureToggles.isEditableModeEnabled()) {
            NotificationUtil.showWarning(project, ArchiverBundle.message("error.editable.disabled"));
            return;
        }

        // 检查是否是可编辑的归档文件或其内部条目
        boolean isArchiveRelated = virtualFile.getFileSystem() instanceof ArchiveFileSystem
                                   || ArchiveExtractionService.getInstance().findDescriptor(virtualFile).isPresent()
                                   || virtualFile instanceof EditableArchiveVirtualFile;

        if (!isArchiveRelated) {
            NotificationUtil.showWarning(project, ArchiverBundle.message("error.archive.unsupported"));
            return;
        }

        try {
            EditableArchiveService service = EditableArchiveService.getInstance();

            // 如果虚拟文件是可编辑的归档虚拟文件，获取其归档路径并刷新
            if (virtualFile instanceof EditableArchiveVirtualFile editableFile) {
                Path archivePath = editableFile.getEntry().archivePath();
                service.flushPendingSaves(archivePath);
                NotificationUtil.showInfo(project, ArchiverBundle.message("success.archive.force.save", virtualFile.getName()));
            } else {
                // 对于其他情况，尝试获取其归档路径
                var descriptorOpt = ArchiveExtractionService.getInstance().findDescriptor(virtualFile);
                if (descriptorOpt.isPresent()) {
                    service.flushPendingSaves(descriptorOpt.get().archivePath());
                    NotificationUtil.showInfo(project, ArchiverBundle.message("success.archive.force.save.all"));
                } else {
                    // 如果是 ZIP 文件，尝试获取其路径
                    String url = virtualFile.getUrl();
                    int jarSeparatorIndex = url.indexOf("!/");
                    if (jarSeparatorIndex > 0) {
                        String archiveUrl = url.substring(0, jarSeparatorIndex);
                        String archivePathStr = com.intellij.openapi.vfs.VfsUtilCore.urlToPath(archiveUrl);
                        if (archivePathStr != null && !archivePathStr.isEmpty()) {
                            service.flushPendingSaves(Path.of(archivePathStr));
                            NotificationUtil.showInfo(project, ArchiverBundle.message("success.archive.force.save.all"));
                        }
                    }
                }
            }
        } catch (Exception ex) {
            NotificationUtil.showError(project, ArchiverBundle.message("error.archive.force.save.failed", ex.getMessage()));
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        VirtualFile virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE);
        boolean isArchiveRelated = project != null
                                   && ArchiverFeatureToggles.isEditableModeEnabled()
                                   && virtualFile != null
                                   && (virtualFile.getFileSystem() instanceof ArchiveFileSystem
                                       || ArchiveExtractionService.getInstance().findDescriptor(virtualFile).isPresent()
                                       || virtualFile instanceof EditableArchiveVirtualFile);
        e.getPresentation().setEnabledAndVisible(isArchiveRelated);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}