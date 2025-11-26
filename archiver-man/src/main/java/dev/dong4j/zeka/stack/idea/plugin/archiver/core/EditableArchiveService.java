package dev.dong4j.zeka.stack.idea.plugin.archiver.core;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import dev.dong4j.zeka.stack.idea.plugin.archiver.util.ArchiverBundle;
import dev.dong4j.zeka.stack.idea.plugin.archiver.util.NotificationUtil;

/**
 * 压缩条目编辑服务
 * <p>
 * 负责将只读的压缩条目转换为可编辑的 {@link EditableArchiveVirtualFile}，
 * 并在保存时将内容写回原始 ZIP/JAR。
 *
 * @author dong4j
 * @since 0.2.0
 */
@Service(Service.Level.APP)
public final class EditableArchiveService {
    private static final long MAX_EDITABLE_BYTES = 2 * 1024 * 1024; // 2MB
    private static final DateTimeFormatter BACKUP_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final Map<Path, Object> archiveLocks = new ConcurrentHashMap<>();

    public static EditableArchiveService getInstance() {
        return ApplicationManager.getApplication().getService(EditableArchiveService.class);
    }

    /**
     * 创建可编辑副本
     *
     * @param project      当前项目
     * @param archiveEntry 压缩条目
     * @return 可编辑虚拟文件
     * @throws EditableArchiveException 当条目不符合编辑要求时抛出异常
     */
    @NotNull
    public EditableArchiveVirtualFile createEditableCopy(@NotNull Project project,
                                                         @NotNull VirtualFile archiveEntry) throws EditableArchiveException {
        if (archiveEntry.isDirectory() || !(archiveEntry.getFileSystem() instanceof ArchiveFileSystem)) {
            throw new EditableArchiveException(ArchiverBundle.message("error.archive.unsupported"));
        }

        ArchiveCoordinates coordinates = resolveCoordinates(archiveEntry)
            .orElseThrow(() -> new EditableArchiveException(ArchiverBundle.message("error.archive.resolve.failed")));

        byte[] content = readEntryBytes(archiveEntry);
        if (content.length > MAX_EDITABLE_BYTES) {
            throw new EditableArchiveException(ArchiverBundle.message("error.archive.too.large", MAX_EDITABLE_BYTES / (1024 * 1024)));
        }

        Charset charset = Optional.ofNullable(archiveEntry.getCharset()).orElse(StandardCharsets.UTF_8);
        FileType fileType = archiveEntry.getFileType();

        EditableArchiveEntry entry = buildEntryMetadata(coordinates.archivePath(), coordinates.entryPath(), charset);
        EditableArchiveVirtualFile editableFile =
            new EditableArchiveVirtualFile(project, archiveEntry.getName(), fileType, entry, charset, new String(content, charset));
        editableFile.setOriginalFile(archiveEntry);
        return editableFile;
    }

    /**
     * 触发保存任务
     *
     * @param project      项目
     * @param editableFile 可编辑文件
     * @param content      最新内容
     */
    public void scheduleSave(@NotNull Project project,
                             @NotNull EditableArchiveVirtualFile editableFile,
                             @NotNull String content) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                writeEntry(project, editableFile, content, null);
            } catch (EditableArchiveException ex) {
                StatusBar statusBar = WindowManager.getInstance().getStatusBar(project);
                if (statusBar != null) {
                    statusBar.setInfo(ex.getMessage());
                }
                NotificationUtil.showError(project, ex.getMessage());
            }
        });
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
        Object lock = archiveLocks.computeIfAbsent(archivePath, path -> new Object());

        synchronized (lock) {
            long currentTimestamp = getArchiveTimestamp(archivePath);
            if (currentTimestamp != entry.archiveTimestamp()) {
                throw new EditableArchiveException(ArchiverBundle.message("error.archive.conflict"));
            }

            Path backupPath = createBackup(archivePath);
            try {
                writeContentToArchive(entry, content);
            } catch (IOException ioException) {
                restoreFromBackup(archivePath, backupPath);
                throw new EditableArchiveException(ArchiverBundle.message("error.archive.save.failed", ioException.getMessage()),
                                                   ioException);
            }

            long newTimestamp = getArchiveTimestamp(archivePath);
            long newCrc = computeCrc(content.getBytes(entry.charset()));
            editableFile.updateEntry(entry.withUpdatedState(newTimestamp, newCrc));

            refreshArchive(archivePath);
            StatusBar statusBar = WindowManager.getInstance().getStatusBar(project);
            if (statusBar != null) {
                statusBar.setInfo(ArchiverBundle.message("status.archive.save.success", entry.entryPath()));
            }
        }
    }

    private void writeContentToArchive(@NotNull EditableArchiveEntry entry, @NotNull String content) throws IOException {
        byte[] bytes = content.getBytes(entry.charset());
        try (FileSystem zipFs = newZipFileSystem(entry.archivePath())) {
            Path pathInsideZip = zipFs.getPath("/" + entry.entryPath());
            Path parent = pathInsideZip.getParent();
            if (parent != null && Files.notExists(parent)) {
                Files.createDirectories(parent);
            }
            Files.write(pathInsideZip, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }
    }

    private FileSystem newZipFileSystem(@NotNull Path archivePath) throws IOException {
        try {
            return FileSystems.newFileSystem(archivePath, (ClassLoader) null);
        } catch (FileSystemAlreadyExistsException ignored) {
            // 关闭遗留的文件系统后重新打开
            try (FileSystem existing = FileSystems.getFileSystem(archivePath.toUri())) {
                existing.close();
            } catch (Exception ignoredClose) {
                // 忽略关闭异常，继续打开新的文件系统
            }
            return FileSystems.newFileSystem(archivePath, (ClassLoader) null);
        }
    }

    private void refreshArchive(@NotNull Path archivePath) {
        LocalFileSystem.getInstance()
            .refreshAndFindFileByNioFile(archivePath);
    }

    private long getArchiveTimestamp(@NotNull Path archivePath) throws EditableArchiveException {
        try {
            return Files.getLastModifiedTime(archivePath).toMillis();
        } catch (IOException e) {
            throw new EditableArchiveException(ArchiverBundle.message("error.archive.resolve.failed"), e);
        }
    }

    private Path createBackup(@NotNull Path archivePath) throws EditableArchiveException {
        String timestamp = BACKUP_TIME_FORMATTER.format(java.time.LocalDateTime.now());
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
            // 如果备份恢复失败，交由用户手动处理
        }
    }

    private EditableArchiveEntry buildEntryMetadata(@NotNull Path archivePath,
                                                    @NotNull String entryPath,
                                                    @NotNull Charset charset) throws EditableArchiveException {
        try (ZipFile zipFile = new ZipFile(archivePath.toFile())) {
            ZipEntry zipEntry = zipFile.getEntry(entryPath);
            if (zipEntry == null) {
                throw new EditableArchiveException(ArchiverBundle.message("error.archive.entry.missing", entryPath));
            }
            long timestamp = getArchiveTimestamp(archivePath);
            long crc = zipEntry.getCrc();
            return new EditableArchiveEntry(archivePath, entryPath, charset, timestamp, crc);
        } catch (IOException e) {
            throw new EditableArchiveException(ArchiverBundle.message("error.archive.open.failed", e.getMessage()), e);
        }
    }

    private Optional<ArchiveCoordinates> resolveCoordinates(@NotNull VirtualFile archiveEntry) {
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
        return Optional.of(new ArchiveCoordinates(Path.of(archivePathStr), entryPath));
    }

    private byte[] readEntryBytes(@NotNull VirtualFile file) throws EditableArchiveException {
        try {
            return file.contentsToByteArray();
        } catch (IOException e) {
            throw new EditableArchiveException(ArchiverBundle.message("error.archive.open.failed", e.getMessage()), e);
        }
    }

    private long computeCrc(byte[] data) {
        CRC32 crc32 = new CRC32();
        crc32.update(data);
        return crc32.getValue();
    }

    private record ArchiveCoordinates(Path archivePath, String entryPath) {
    }
}

