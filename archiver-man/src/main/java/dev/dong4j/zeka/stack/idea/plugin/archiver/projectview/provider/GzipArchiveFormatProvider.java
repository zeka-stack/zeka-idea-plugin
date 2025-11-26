package dev.dong4j.zeka.stack.idea.plugin.archiver.projectview.provider;

import com.intellij.openapi.vfs.VirtualFile;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Set;

import dev.dong4j.zeka.stack.idea.plugin.archiver.core.ArchiveExtractionService;

/**
 * 处理 .gz /.tgz/.tar.gz 归档。
 */
public final class GzipArchiveFormatProvider implements ArchiveFormatProvider {

    private static final Set<String> GZIP_EXTENSIONS = Set.of("gz", "tgz");
    private final ArchiveExtractionService extractionService = ArchiveExtractionService.getInstance();

    @Override
    public boolean supports(@NotNull VirtualFile file) {
        String ext = file.getExtension();
        if (ext == null) {
            return false;
        }
        ext = ext.toLowerCase(Locale.ENGLISH);
        if (GZIP_EXTENSIONS.contains(ext)) {
            return true;
        }
        // 特殊判定 .tar.gz
        return file.getName().toLowerCase(Locale.ENGLISH).endsWith(".tar.gz");
    }

    @Override
    public @Nullable VirtualFile getArchiveRoot(@NotNull VirtualFile localArchiveFile) {
        if (isTarGz(localArchiveFile)) {
            return extractionService.extractTarGzArchive(localArchiveFile);
        }
        return extractionService.extractPlainGzip(localArchiveFile);
    }

    private boolean isTarGz(@NotNull VirtualFile file) {
        String name = file.getName().toLowerCase(Locale.ENGLISH);
        return name.endsWith(".tar.gz") || name.endsWith(".tgz");
    }
}

