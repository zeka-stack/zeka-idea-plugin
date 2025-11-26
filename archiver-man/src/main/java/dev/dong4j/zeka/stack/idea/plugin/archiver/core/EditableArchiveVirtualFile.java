package dev.dong4j.zeka.stack.idea.plugin.archiver.core;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightVirtualFile;

import org.jetbrains.annotations.NotNull;

import java.nio.charset.Charset;

/**
 * 内存中的可编辑压缩条目
 * <p>
 * 继承 {@link LightVirtualFile}，用来承载 ZIP/JAR 条目的临时编辑副本，
 * 同时保留关联的 {@link EditableArchiveEntry} 元数据，便于写回原压缩包。
 *
 * @author dong4j
 * @version 0.2.0
 * @since 0.2.0
 */
public final class EditableArchiveVirtualFile extends LightVirtualFile {
    private final Project project;
    private EditableArchiveEntry entry;
    private final VirtualFile sourceFile;

    public EditableArchiveVirtualFile(@NotNull Project project,
                                      @NotNull String name,
                                      @NotNull FileType fileType,
                                      @NotNull EditableArchiveEntry entry,
                                      @NotNull Charset charset,
                                      @NotNull CharSequence content,
                                      @NotNull VirtualFile sourceFile) {
        super(name, fileType, content);
        this.project = project;
        this.entry = entry;
        this.sourceFile = sourceFile;
        setWritable(true);
        setFileType(fileType);
        setCharset(charset);
    }

    @NotNull
    public Project getProject() {
        return project;
    }

    @NotNull
    public EditableArchiveEntry getEntry() {
        return entry;
    }

    public void updateEntry(@NotNull EditableArchiveEntry newEntry) {
        this.entry = newEntry;
    }

    @NotNull
    public VirtualFile getSourceFile() {
        return sourceFile;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public boolean isWritable() {
        return true;
    }
}

