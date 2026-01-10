package dev.dong4j.zeka.stack.idea.plugin.archiver.core;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;

import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * 负责将不受 IntelliJ 默认 ArchiveFileSystem 支持的归档文件解压到临时目录，供 Project View 展示。
 *
 * @author dong4j
 * @since 0.3.0
 */
@Slf4j
@Service(Service.Level.APP)
public final class ArchiveExtractionService {

    private static final String EXTRACT_DIR_NAME = "archiver-man";
    private static final String EXTRACT_SUB_DIR = "extracted";
    static final String MARKER_FILE_NAME = ".completed";

    private final LocalFileSystem localFileSystem = LocalFileSystem.getInstance();
    private final Path extractionRoot;
    private final Map<String, Object> locks = new ConcurrentHashMap<>();
    private final Map<String, ExtractedEntryDescriptor> entryIndex = new ConcurrentHashMap<>();

    public ArchiveExtractionService() {
        this.extractionRoot = Path.of(FileUtil.getTempDirectory(), EXTRACT_DIR_NAME, EXTRACT_SUB_DIR);
        try {
            Files.createDirectories(extractionRoot);
        } catch (IOException e) {
            log.debug("Failed to create extraction root: " + extractionRoot, e);
        }
    }

    public static ArchiveExtractionService getInstance() {
        return ApplicationManager.getApplication().getService(ArchiveExtractionService.class);
    }

    public @Nullable VirtualFile extractTarArchive(@NotNull VirtualFile file) {
        return extract(file, ArchiveFormat.TAR, this::extractTarStream);
    }

    public @Nullable VirtualFile extractTarGzArchive(@NotNull VirtualFile file) {
        return extract(file, ArchiveFormat.TAR_GZ, this::extractTarFromGzipStream);
    }

    public @Nullable VirtualFile extractPlainGzip(@NotNull VirtualFile file) {
        return extract(file, ArchiveFormat.GZIP, this::extractSingleGzipFile);
    }

    public @Nullable VirtualFile extractSevenZipArchive(@NotNull VirtualFile file) {
        return extract(file, ArchiveFormat.SEVEN_Z, this::extractSevenZipEntries);
    }

    public Optional<ExtractedEntryDescriptor> findDescriptor(@NotNull VirtualFile file) {
        File ioFile = VfsUtilCore.virtualToIoFile(file);
        String key = normalizeKey(ioFile.toPath());
        return Optional.ofNullable(entryIndex.get(key));
    }

    private @Nullable VirtualFile extract(@NotNull VirtualFile file,
                                          @NotNull ArchiveFormat format,
                                          @NotNull ArchiveExtractor extractor) {
        try {
            Path targetDir = ensureExtracted(file, format, extractor);
            Path archivePath = VfsUtilCore.virtualToIoFile(file).toPath();
            indexExtractedEntries(targetDir, archivePath, format);
            return localFileSystem.refreshAndFindFileByNioFile(targetDir);
        } catch (IOException ex) {
            log.debug("Failed to extract archive: " + file.getPath(), ex);
            return null;
        }
    }

    private Path ensureExtracted(@NotNull VirtualFile file,
                                 @NotNull ArchiveFormat format,
                                 @NotNull ArchiveExtractor extractor) throws IOException {
        String suffix = format.name().toLowerCase();
        String cacheKey = buildCacheKey(file, suffix);
        Path targetDir = extractionRoot.resolve(cacheKey);
        Path marker = targetDir.resolve(MARKER_FILE_NAME);
        Object lock = locks.computeIfAbsent(cacheKey, key -> new Object());
        synchronized (lock) {
            if (Files.exists(marker) && isRootIndexed(targetDir)) {
                return targetDir;
            }
            FileUtil.delete(targetDir.toFile());
            Files.createDirectories(targetDir);
            try {
                extractor.extract(file, targetDir);
                Files.createFile(marker);
            } catch (IOException e) {
                FileUtil.delete(targetDir.toFile());
                throw e;
            }
        }
        return targetDir;
    }

    private boolean isRootIndexed(@NotNull Path root) {
        return entryIndex.values().stream().anyMatch(descriptor -> descriptor.rootDir().equals(root));
    }

    private void extractTarStream(@NotNull VirtualFile file, @NotNull Path targetDir) throws IOException {
        try (InputStream inputStream = file.getInputStream()) {
            extractTarFromStream(inputStream, targetDir);
        }
    }

    private void extractTarFromGzipStream(@NotNull VirtualFile file, @NotNull Path targetDir) throws IOException {
        try (InputStream inputStream = file.getInputStream();
             GzipCompressorInputStream gzipStream = new GzipCompressorInputStream(inputStream)) {
            extractTarFromStream(gzipStream, targetDir);
        }
    }

    private void extractSingleGzipFile(@NotNull VirtualFile file, @NotNull Path targetDir) throws IOException {
        String fileName = stripGzipExtension(file.getName());
        Path outFile = resolveSafe(targetDir, fileName);
        Files.createDirectories(outFile.getParent());
        try (InputStream inputStream = file.getInputStream();
             GzipCompressorInputStream gzipStream = new GzipCompressorInputStream(inputStream);
             OutputStream outputStream = Files.newOutputStream(outFile)) {
            gzipStream.transferTo(outputStream);
        }
    }

    private void extractSevenZipEntries(@NotNull VirtualFile file, @NotNull Path targetDir) throws IOException {
        File localFile = VfsUtilCore.virtualToIoFile(file);
        try (SevenZFile sevenZFile = new SevenZFile(localFile)) {
            SevenZArchiveEntry entry;
            byte[] buffer = new byte[8192];
            while ((entry = sevenZFile.getNextEntry()) != null) {
                Path outPath = resolveSafe(targetDir, entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(outPath);
                    continue;
                }
                Files.createDirectories(outPath.getParent());
                try (OutputStream outputStream = Files.newOutputStream(outPath)) {
                    int read;
                    while ((read = sevenZFile.read(buffer)) > 0) {
                        outputStream.write(buffer, 0, read);
                    }
                }
            }
        }
    }

    private void extractTarFromStream(@NotNull InputStream stream, @NotNull Path targetDir) throws IOException {
        try (TarArchiveInputStream tarInputStream = new TarArchiveInputStream(stream)) {
            TarArchiveEntry entry;
            while ((entry = tarInputStream.getNextTarEntry()) != null) {
                Path outPath = resolveSafe(targetDir, entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(outPath);
                } else {
                    Files.createDirectories(outPath.getParent());
                    try (OutputStream outputStream = Files.newOutputStream(outPath)) {
                        tarInputStream.transferTo(outputStream);
                    }
                }
            }
        }
    }

    private Path resolveSafe(@NotNull Path root, @NotNull String entryName) throws IOException {
        Path resolved = root.resolve(entryName).normalize();
        if (!resolved.startsWith(root)) {
            throw new IOException("Entry outside target dir: " + entryName);
        }
        return resolved;
    }

    private String stripGzipExtension(@NotNull String name) {
        if (name.endsWith(".gz")) {
            return name.substring(0, name.length() - 3);
        }
        return name;
    }

    private void indexExtractedEntries(@NotNull Path root,
                                       @NotNull Path archivePath,
                                       @NotNull ArchiveFormat format) throws IOException {
        entryIndex.entrySet().removeIf(entry -> entry.getValue().rootDir().equals(root));
        try (Stream<Path> stream = Files.walk(root)) {
            stream.filter(path -> Files.isRegularFile(path) && !isInternal(path))
                .forEach(path -> registerEntry(root, archivePath, format, path));
        }
    }

    private void registerEntry(@NotNull Path root,
                               @NotNull Path archivePath,
                               @NotNull ArchiveFormat format,
                               @NotNull Path file) {
        String relativePath = root.relativize(file).toString().replace('\\', '/');
        String key = normalizeKey(file);
        entryIndex.put(key, new ExtractedEntryDescriptor(archivePath, relativePath, format, root, file));
    }

    private boolean isInternal(@NotNull Path path) {
        return MARKER_FILE_NAME.equals(path.getFileName().toString());
    }

    private String normalizeKey(@NotNull Path path) {
        return FileUtil.toSystemIndependentName(path.toAbsolutePath().toString());
    }

    private String buildCacheKey(@NotNull VirtualFile file, @NotNull String suffix) {
        String source = file.getPath() + "|" + file.getTimeStamp() + "|" + file.getLength() + "|" + suffix;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(source.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(source.hashCode());
        }
    }

    @FunctionalInterface
    private interface ArchiveExtractor {
        void extract(@NotNull VirtualFile file, @NotNull Path targetDir) throws IOException;
    }

    public record ExtractedEntryDescriptor(Path archivePath,
                                           String entryPath,
                                           ArchiveFormat format,
                                           Path rootDir,
                                           Path extractedFile) {
    }
}

