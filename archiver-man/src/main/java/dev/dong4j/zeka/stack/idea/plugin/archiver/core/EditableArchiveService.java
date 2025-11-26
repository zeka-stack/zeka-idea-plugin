package dev.dong4j.zeka.stack.idea.plugin.archiver.core;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.ArchiveFileSystem;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.util.concurrency.AppExecutorUtil;

import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.zip.CRC32;

import dev.dong4j.zeka.stack.idea.plugin.archiver.util.ArchiverBundle;
import dev.dong4j.zeka.stack.idea.plugin.archiver.util.ArchiverFeatureToggles;
import dev.dong4j.zeka.stack.idea.plugin.archiver.util.NotificationUtil;

/**
 * 归档条目编辑服务。
 */
@Service(Service.Level.APP)
public final class EditableArchiveService {
    private static final Logger LOG = Logger.getInstance(EditableArchiveService.class);
    private static final DateTimeFormatter BACKUP_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final Map<Path, Object> archiveLocks = new ConcurrentHashMap<>();
    private final Map<Path, PendingBatch> pendingBatches = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = AppExecutorUtil.getAppScheduledExecutorService();
    private final ArchiveExtractionService extractionService = ArchiveExtractionService.getInstance();

    public static EditableArchiveService getInstance() {
        return ApplicationManager.getApplication().getService(EditableArchiveService.class);
    }

    @NotNull
    public EditableArchiveVirtualFile createEditableCopy(@NotNull Project project,
                                                         @NotNull VirtualFile archiveEntry) throws EditableArchiveException {
        if (archiveEntry.isDirectory()) {
            throw new EditableArchiveException(ArchiverBundle.message("error.archive.unsupported"));
        }
        ArchiveSource source = resolveSource(archiveEntry)
            .orElseThrow(() -> new EditableArchiveException(ArchiverBundle.message("error.archive.resolve.failed")));

        byte[] content = readEntryBytes(source);
        long maxBytes = ArchiverFeatureToggles.maxEditableBytes();
        if (content.length > maxBytes) {
            throw new EditableArchiveException(ArchiverBundle.message("error.archive.too.large", maxBytes / (1024 * 1024)));
        }

        Charset charset = source.charset();
        FileType fileType = source.viewFile().getFileType();
        long crc = computeCrc(content);
        EditableArchiveEntry entry = buildEntryMetadata(source, charset, crc);

        return new EditableArchiveVirtualFile(project,
                                              archiveEntry.getName(),
                                              fileType,
                                              entry,
                                              charset,
                                              new String(content, charset),
                                              source.viewFile());
    }

    public void scheduleSave(@NotNull Project project,
                             @NotNull EditableArchiveVirtualFile editableFile,
                             @NotNull String content) {
        PendingSave save = new PendingSave(project, editableFile, content);
        int delay = ArchiverFeatureToggles.batchSaveDelayMillis();
        Path archive = editableFile.getEntry().archivePath();
        if (delay <= 0) {
            flushBatch(archive, List.of(save));
            return;
        }
        PendingBatch batch = pendingBatches.computeIfAbsent(archive, key -> new PendingBatch());
        synchronized (batch) {
            batch.saves.add(save);
            if (batch.future == null) {
                batch.future = scheduler.schedule(() -> {
                    List<PendingSave> pending;
                    synchronized (batch) {
                        pending = new ArrayList<>(batch.saves);
                        batch.saves.clear();
                        batch.future = null;
                    }
                    pendingBatches.remove(archive, batch);
                    flushBatch(archive, pending);
                }, delay, TimeUnit.MILLISECONDS);
            }
        }
    }

    /**
     * 立即保存所有待处理的更改到指定归档
     *
     * @param archivePath 归档路径
     */
    public void flushPendingSaves(@NotNull Path archivePath) {
        PendingBatch batch = pendingBatches.get(archivePath);
        if (batch != null) {
            synchronized (batch) {
                if (batch.future != null) {
                    // 取消计划的批量保存
                    batch.future.cancel(false);
                    batch.future = null;
                }
                if (!batch.saves.isEmpty()) {
                    List<PendingSave> pending = new ArrayList<>(batch.saves);
                    batch.saves.clear();
                    pendingBatches.remove(archivePath, batch);
                    flushBatch(archivePath, pending);
                }
            }
        }
    }

    @SuppressWarnings("D")
    private void flushBatch(@NotNull Path archivePath, @NotNull List<PendingSave> saves) {
        for (PendingSave save : saves) {
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                // 在执行线程中再次检查项目是否已关闭
                if (save.project().isDisposed()) {
                    return;
                }
                try {
                    writeEntry(save.project(), save.file(), save.content(), null);
                } catch (EditableArchiveException ex) {
                    StatusBar statusBar = WindowManager.getInstance().getStatusBar(save.project());
                    if (statusBar != null) {
                        statusBar.setInfo(ex.getMessage());
                    }
                    NotificationUtil.showError(save.project(), ex.getMessage());
                } catch (Exception ex) {
                    // 捕获所有其他异常，避免静默失败
                    StatusBar statusBar = WindowManager.getInstance().getStatusBar(save.project());
                    if (statusBar != null) {
                        statusBar.setInfo("Failed to save archive: " + ex.getMessage());
                    }
                    NotificationUtil.showError(save.project(), "Failed to save archive: " + ex.getMessage());
                    LOG.error("Unexpected error during archive save", ex);
                }
            });
        }
    }

    void writeEntry(@NotNull Project project,
                    @NotNull EditableArchiveVirtualFile editableFile,
                    @NotNull String content,
                    @Nullable ProgressIndicator indicator) throws EditableArchiveException {
        if (indicator != null) {
            indicator.setIndeterminate(true);
            indicator.setText(ArchiverBundle.message("notification.archive.save.started", editableFile.getName()));
        }

        EditableArchiveEntry entry = editableFile.getEntry();
        Path archivePath = entry.archivePath();
        byte[] bytes = content.getBytes(entry.charset());
        if (bytes.length > ArchiverFeatureToggles.maxEditableBytes()) {
            throw new EditableArchiveException(ArchiverBundle.message("error.archive.too.large",
                                                                      ArchiverFeatureToggles.maxEditableBytes() / (1024 * 1024)));
        }
        Object lock = archiveLocks.computeIfAbsent(archivePath, path -> new Object());

        synchronized (lock) {
            long currentTimestamp = getArchiveTimestamp(archivePath);
            if (currentTimestamp != entry.archiveTimestamp()) {
                throw new EditableArchiveException(ArchiverBundle.message("error.archive.conflict"));
            }

            Path backupPath = null;
            if (ArchiverFeatureToggles.isAutoBackupEnabled()) {
                backupPath = createBackup(archivePath);
            }
            try {
                writeContentToArchive(entry, bytes);
            } catch (IOException ioException) {
                if (backupPath != null) {
                    restoreFromBackup(archivePath, backupPath);
                }
                throw new EditableArchiveException(ArchiverBundle.message("error.archive.save.failed", ioException.getMessage()),
                                                   ioException);
            }

            long newTimestamp = getArchiveTimestamp(archivePath);
            long newCrc = computeCrc(bytes);
            editableFile.updateEntry(entry.withUpdatedState(newTimestamp, newCrc));

            refreshArchive(archivePath);
            refreshSourceFile(editableFile);
            StatusBar statusBar = WindowManager.getInstance().getStatusBar(project);
            if (statusBar != null) {
                statusBar.setInfo(ArchiverBundle.message("status.archive.save.success", entry.entryPath()));
            }
        }
    }

    private void writeContentToArchive(@NotNull EditableArchiveEntry entry, byte[] bytes) throws IOException, EditableArchiveException {
        switch (entry.format()) {
            case ZIP -> writeZipEntry(entry, bytes);
            case TAR -> repackTar(entry, bytes, false);
            case TAR_GZ -> repackTar(entry, bytes, true);
            case GZIP -> repackGzip(entry, bytes);
            case SEVEN_Z -> repackSevenZip(entry, bytes);
        }
    }

    private void writeZipEntry(@NotNull EditableArchiveEntry entry, byte[] bytes) throws IOException {
        try (FileSystem zipFs = newZipFileSystem(entry.archivePath())) {
            Path pathInsideZip = zipFs.getPath("/" + entry.entryPath());
            Path parent = pathInsideZip.getParent();
            if (parent != null && Files.notExists(parent)) {
                Files.createDirectories(parent);
            }
            Files.write(pathInsideZip, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }
    }

    private void repackTar(@NotNull EditableArchiveEntry entry, byte[] bytes, boolean gzip) throws IOException, EditableArchiveException {
        ensureExtracted(entry);
        Files.createDirectories(entry.extractedFile().getParent());
        Files.write(entry.extractedFile(), bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        Path temp = Files.createTempFile("archiver-man", ".tar");
        try (OutputStream fileStream = Files.newOutputStream(temp);
             OutputStream wrappedStream = gzip ? new GzipCompressorOutputStream(fileStream) : fileStream;
             TarArchiveOutputStream tarOut = new TarArchiveOutputStream(wrappedStream)) {
            tarOut.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
            Files.walk(entry.extractedRoot())
                .filter(path -> Files.isRegularFile(path) && !isInternalFile(path))
                .forEach(path -> {
                    String relative = entry.extractedRoot().relativize(path).toString().replace('\\', '/');
                    TarArchiveEntry tarEntry = new TarArchiveEntry(relative);
                    tarEntry.setSize(safeSize(path));
                    try (InputStream in = Files.newInputStream(path)) {
                        tarOut.putArchiveEntry(tarEntry);
                        in.transferTo(tarOut);
                        tarOut.closeArchiveEntry();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
        }
        moveTempToArchive(temp, entry.archivePath());
    }

    private void repackGzip(@NotNull EditableArchiveEntry entry, byte[] bytes) throws IOException, EditableArchiveException {
        ensureExtracted(entry);
        Files.createDirectories(entry.extractedFile().getParent());
        Files.write(entry.extractedFile(), bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        Path temp = Files.createTempFile("archiver-man", ".gz");
        try (OutputStream out = Files.newOutputStream(temp);
             GzipCompressorOutputStream gzip = new GzipCompressorOutputStream(out);
             InputStream in = Files.newInputStream(entry.extractedFile())) {
            in.transferTo(gzip);
        }
        moveTempToArchive(temp, entry.archivePath());
    }

    private void repackSevenZip(@NotNull EditableArchiveEntry entry, byte[] bytes) throws IOException, EditableArchiveException {
        ensureExtracted(entry);
        Files.createDirectories(entry.extractedFile().getParent());
        Files.write(entry.extractedFile(), bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        Path temp = Files.createTempFile("archiver-man", ".7z");
        try (SevenZOutputFile sevenZ = new SevenZOutputFile(temp.toFile())) {
            Files.walk(entry.extractedRoot())
                .filter(path -> Files.isRegularFile(path) && !isInternalFile(path))
                .forEach(path -> {
                    String relative = entry.extractedRoot().relativize(path).toString().replace('\\', '/');
                    SevenZArchiveEntry archiveEntry = sevenZ.createArchiveEntry(path.toFile(), relative);
                    try (InputStream in = Files.newInputStream(path)) {
                        sevenZ.putArchiveEntry(archiveEntry);
                        byte[] buffer = new byte[8192];
                        int read;
                        while ((read = in.read(buffer)) != -1) {
                            sevenZ.write(buffer, 0, read);
                        }
                        sevenZ.closeArchiveEntry();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
        }
        moveTempToArchive(temp, entry.archivePath());
    }

    private void moveTempToArchive(@NotNull Path temp, @NotNull Path archivePath) throws IOException {
        Files.move(temp, archivePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    private void ensureExtracted(@NotNull EditableArchiveEntry entry) throws EditableArchiveException {
        if (entry.extractedFile() == null || entry.extractedRoot() == null) {
            throw new EditableArchiveException(ArchiverBundle.message("error.archive.extracted.missing"));
        }
    }

    private boolean isInternalFile(@NotNull Path path) {
        return ArchiveExtractionService.MARKER_FILE_NAME.equals(path.getFileName().toString());
    }

    private long safeSize(@NotNull Path path) {
        try {
            return Files.size(path);
        } catch (IOException e) {
            return 0L;
        }
    }

    private FileSystem newZipFileSystem(@NotNull Path archivePath) throws IOException {
        try {
            return FileSystems.newFileSystem(archivePath, (ClassLoader) null);
        } catch (FileSystemAlreadyExistsException ignored) {
            try (FileSystem existing = FileSystems.getFileSystem(archivePath.toUri())) {
                existing.close();
            } catch (Exception ignoredClose) {
                // ignore
            }
            return FileSystems.newFileSystem(archivePath, (ClassLoader) null);
        }
    }

    private void refreshArchive(@NotNull Path archivePath) {
        LocalFileSystem.getInstance().refreshAndFindFileByNioFile(archivePath);
    }

    private void refreshSourceFile(@NotNull EditableArchiveVirtualFile file) {
        VirtualFile source = file.getSourceFile();
        if (source != null && source.isValid()) {
            source.refresh(true, false);
        }
    }

    private long getArchiveTimestamp(@NotNull Path archivePath) throws EditableArchiveException {
        try {
            return Files.getLastModifiedTime(archivePath).toMillis();
        } catch (IOException e) {
            throw new EditableArchiveException(ArchiverBundle.message("error.archive.resolve.failed"), e);
        }
    }

    private Path createBackup(@NotNull Path archivePath) throws EditableArchiveException {
        String timestamp = BACKUP_TIME_FORMATTER.format(LocalDateTime.now());
        Path backup = archivePath.resolveSibling(archivePath.getFileName() + "." + timestamp + ".bak");
        try {
            Files.copy(archivePath, backup, StandardCopyOption.REPLACE_EXISTING);
            return backup;
        } catch (IOException e) {
            throw new EditableArchiveException(ArchiverBundle.message("error.archive.backup.failed", e.getMessage()), e);
        }
    }

    private void restoreFromBackup(@NotNull Path archivePath, @NotNull Path backup) {
        if (!Files.exists(backup)) {
            return;
        }
        try {
            Files.copy(backup, archivePath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ignored) {
        }
    }

    private EditableArchiveEntry buildEntryMetadata(@NotNull ArchiveSource source,
                                                    @NotNull Charset charset,
                                                    long crc) throws EditableArchiveException {
        long timestamp = getArchiveTimestamp(source.archivePath());
        return new EditableArchiveEntry(source.archivePath(),
                                        source.entryPath(),
                                        charset,
                                        timestamp,
                                        crc,
                                        source.format(),
                                        source.extractedRoot(),
                                        source.extractedFile());
    }

    private Optional<ArchiveSource> resolveSource(@NotNull VirtualFile archiveEntry) {
        if (archiveEntry.getFileSystem() instanceof ArchiveFileSystem) {
            String url = archiveEntry.getUrl();
            int jarSeparatorIndex = url.indexOf(JarFileSystem.JAR_SEPARATOR);
            if (jarSeparatorIndex < 0) {
                return Optional.empty();
            }
            String archiveUrl = url.substring(0, jarSeparatorIndex);
            String entryPath = url.substring(jarSeparatorIndex + JarFileSystem.JAR_SEPARATOR.length());
            String archivePathStr = VfsUtilCore.urlToPath(archiveUrl);
            if (archivePathStr == null || archivePathStr.isEmpty()) {
                return Optional.empty();
            }
            Charset charset = Optional.ofNullable(archiveEntry.getCharset()).orElse(StandardCharsets.UTF_8);
            return Optional.of(new ArchiveSource(Path.of(archivePathStr), entryPath, ArchiveFormat.ZIP, charset, archiveEntry, null, null));
        }
        return extractionService.findDescriptor(archiveEntry)
            .map(descriptor -> {
                Charset charset = Optional.ofNullable(archiveEntry.getCharset()).orElse(StandardCharsets.UTF_8);
                return new ArchiveSource(descriptor.archivePath(),
                                         descriptor.entryPath(),
                                         descriptor.format(),
                                         charset,
                                         archiveEntry,
                                         descriptor.rootDir(),
                                         descriptor.extractedFile());
            });
    }

    private byte[] readEntryBytes(@NotNull ArchiveSource source) throws EditableArchiveException {
        try {
            if (source.viewFile().getFileSystem() instanceof ArchiveFileSystem) {
                return source.viewFile().contentsToByteArray();
            }
            return Files.readAllBytes(source.extractedFile());
        } catch (IOException e) {
            throw new EditableArchiveException(ArchiverBundle.message("error.archive.open.failed", e.getMessage()), e);
        }
    }

    private long computeCrc(byte[] data) {
        CRC32 crc32 = new CRC32();
        crc32.update(data);
        return crc32.getValue();
    }

    private record PendingSave(Project project, EditableArchiveVirtualFile file, String content) {
    }

    private static final class PendingBatch {
        private final List<PendingSave> saves = new ArrayList<>();
        private java.util.concurrent.ScheduledFuture<?> future;
    }

    private record ArchiveSource(Path archivePath,
                                 String entryPath,
                                 ArchiveFormat format,
                                 Charset charset,
                                 VirtualFile viewFile,
                                 Path extractedRoot,
                                 Path extractedFile) {
    }
}

