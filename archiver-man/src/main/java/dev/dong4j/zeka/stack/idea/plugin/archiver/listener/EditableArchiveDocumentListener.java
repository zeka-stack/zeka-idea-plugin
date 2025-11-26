package dev.dong4j.zeka.stack.idea.plugin.archiver.listener;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileDocumentManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

import org.jetbrains.annotations.NotNull;

import dev.dong4j.zeka.stack.idea.plugin.archiver.core.EditableArchiveService;
import dev.dong4j.zeka.stack.idea.plugin.archiver.core.EditableArchiveVirtualFile;

/**
 * 文档保存监听器
 * <p>
 * 拦截 {@link EditableArchiveVirtualFile} 的保存动作，将内容写回原始压缩包。
 *
 * @author dong4j
 * @since 0.2.0
 */
public final class EditableArchiveDocumentListener implements FileDocumentManagerListener {

    @Override
    public void beforeDocumentSaving(@NotNull Document document) {
        VirtualFile file = FileDocumentManager.getInstance().getFile(document);
        if (!(file instanceof EditableArchiveVirtualFile editableFile)) {
            return;
        }

        Project project = editableFile.getProject();
        if (project.isDisposed()) {
            return;
        }

        EditableArchiveService service = EditableArchiveService.getInstance();
        String content = document.getText();
        service.scheduleSave(project, editableFile, content);
    }
}

