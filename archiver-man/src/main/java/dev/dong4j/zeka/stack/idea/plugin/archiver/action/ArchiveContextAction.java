package dev.dong4j.zeka.stack.idea.plugin.archiver.action;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.ArchiveFileSystem;
import com.intellij.psi.PsiFile;

import org.jetbrains.annotations.NotNull;

import dev.dong4j.zeka.stack.idea.plugin.archiver.core.EditableArchiveException;
import dev.dong4j.zeka.stack.idea.plugin.archiver.core.EditableArchiveService;
import dev.dong4j.zeka.stack.idea.plugin.archiver.core.EditableArchiveVirtualFile;
import dev.dong4j.zeka.stack.idea.plugin.archiver.icons.ArchiverIcons;
import dev.dong4j.zeka.stack.idea.plugin.archiver.util.ArchiverBundle;
import dev.dong4j.zeka.stack.idea.plugin.archiver.util.NotificationUtil;

/**
 * 压缩条目上下文 Action - 右键菜单触发
 * <p>
 * 为 Archiver Man 提供统一入口，在启用完全内联编辑之前用于
 * 预览 ZIP 条目信息并展示通知。
 *
 * @author dong4j
 * @since 0.1.0
 */
public class ArchiveContextAction extends AnAction {

    public ArchiveContextAction() {
        super(
            ArchiverBundle.message("action.archive.context.title"),
            ArchiverBundle.message("action.archive.context.description"),
            ArchiverIcons.ARCHIVER_16
             );
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        VirtualFile virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE);

        if (project == null) {
            NotificationUtil.showError(project, ArchiverBundle.message("error.no.project"));
            return;
        }

        if (virtualFile == null && psiFile != null) {
            virtualFile = psiFile.getVirtualFile();
        }

        if (virtualFile == null) {
            NotificationUtil.showError(project, ArchiverBundle.message("error.no.file"));
            return;
        }

        EditableArchiveService service = EditableArchiveService.getInstance();

        if (!(virtualFile.getFileSystem() instanceof ArchiveFileSystem) || virtualFile.isDirectory()) {
            NotificationUtil.showWarning(project, ArchiverBundle.message("error.archive.unsupported"));
            return;
        }

        try {
            EditableArchiveVirtualFile editableFile = service.createEditableCopy(project, virtualFile);
            FileEditorManager.getInstance(project).openFile(editableFile, true);
            NotificationUtil.showInfo(project, ArchiverBundle.message("success.archive.open", editableFile.getName()));
        } catch (EditableArchiveException ex) {
            NotificationUtil.showError(project, ex.getMessage());
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        VirtualFile virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE);
        if (virtualFile == null && psiFile != null) {
            virtualFile = psiFile.getVirtualFile();
        }
        boolean enabled = project != null
                          && virtualFile != null
                          && virtualFile.getFileSystem() instanceof ArchiveFileSystem
                          && !virtualFile.isDirectory();
        e.getPresentation().setEnabledAndVisible(enabled);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}

