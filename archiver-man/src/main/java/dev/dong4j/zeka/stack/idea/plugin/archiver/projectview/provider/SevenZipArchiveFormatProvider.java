package dev.dong4j.zeka.stack.idea.plugin.archiver.projectview.provider;

import com.intellij.openapi.vfs.VirtualFile;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

import dev.dong4j.zeka.stack.idea.plugin.archiver.core.ArchiveExtractionService;

/**
 * 处理 7z 归档。
 */
public final class SevenZipArchiveFormatProvider implements ArchiveFormatProvider {

    private static final Set<String> EXTENSIONS = Set.of("7z");
    private final ArchiveExtractionService extractionService = ArchiveExtractionService.getInstance();

    @Override
    public boolean supports(@NotNull VirtualFile file) {
        String ext = file.getExtension();
        return ext != null && EXTENSIONS.contains(ext.toLowerCase());
    }

    @Override
    public @Nullable VirtualFile getArchiveRoot(@NotNull VirtualFile localArchiveFile) {
        return extractionService.extractSevenZipArchive(localArchiveFile);
    }
}

