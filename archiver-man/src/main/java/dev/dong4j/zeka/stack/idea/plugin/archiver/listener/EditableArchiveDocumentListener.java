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
 * Save/Auto-save hook for editable archive files.
 */
public final class EditableArchiveDocumentListener implements FileDocumentManagerListener {

    @Override
    public void beforeDocumentSaving(@NotNull Document document) {
        processDocument(document);
    }

    @Override
    public void beforeAllDocumentsSaving() {
        FileDocumentManager manager = FileDocumentManager.getInstance();
        for (Document document : manager.getUnsavedDocuments()) {
            processDocument(document);
        }
    }

    private void processDocument(@NotNull Document document) {
        VirtualFile file = FileDocumentManager.getInstance().getFile(document);
        if (!(file instanceof EditableArchiveVirtualFile editableFile)) {
            return;
        }
        Project project = editableFile.getProject();
        if (project.isDisposed()) {
            return;
        }
        EditableArchiveService.getInstance().scheduleSave(project, editableFile, document.getText());
    }
}

