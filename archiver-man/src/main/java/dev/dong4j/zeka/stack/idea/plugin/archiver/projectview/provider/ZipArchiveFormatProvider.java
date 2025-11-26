package dev.dong4j.zeka.stack.idea.plugin.archiver.projectview.provider;

import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.VirtualFile;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Zip/Jar 格式 provider。
 *
 * @author dong4j
 * @since 0.3.0
 */
final class ZipArchiveFormatProvider implements ArchiveFormatProvider {

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of("zip", "jar");

    @Override
    public boolean supports(@NotNull VirtualFile file) {
        String extension = file.getExtension();
        if (extension == null) {
            return false;
        }
        return SUPPORTED_EXTENSIONS.contains(extension.toLowerCase());
    }

    @Override
    public @Nullable VirtualFile getArchiveRoot(@NotNull VirtualFile localArchiveFile) {
        return JarFileSystem.getInstance().getJarRootForLocalFile(localArchiveFile);
    }
}

