package dev.dong4j.zeka.stack.idea.plugin.archiver.core;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 嵌套归档缓存服务。
 * <p>
 * 当 Project View 中出现 {@code outer.zip!/inner.jar} 等多层嵌套时，将条目复制到临时目录后
 * 再交给对应的 {@link com.intellij.openapi.vfs.newvfs.ArchiveFileSystem} 处理。
 *
 * @author dong4j
 * @since 0.3.0
 */
@Service(Service.Level.APP)
public final class NestedArchiveCacheService {

    private static final Logger LOG = Logger.getInstance(NestedArchiveCacheService.class);
    private static final String CACHE_DIR_NAME = "archiver-man";
    private static final String NESTED_DIR_NAME = "nested";

    private final LocalFileSystem localFileSystem = LocalFileSystem.getInstance();
    private final Path cacheRoot;
    private final Map<String, Object> locks = new ConcurrentHashMap<>();

    public NestedArchiveCacheService() {
        this.cacheRoot = Path.of(FileUtil.getTempDirectory(), CACHE_DIR_NAME, NESTED_DIR_NAME);
        try {
            Files.createDirectories(cacheRoot);
        } catch (IOException e) {
            LOG.debug("Failed to create cache directory: " + cacheRoot, e);
        }
    }

    public static NestedArchiveCacheService getInstance() {
        return ApplicationManager.getApplication().getService(NestedArchiveCacheService.class);
    }

    /**
     * 如果是嵌套归档文件，将其复制到临时目录并返回新的本地 {@link VirtualFile}。
     * 非嵌套文件返回原引用。
     *
     * @param file 可能处于嵌套中的虚拟文件
     * @return 本地虚拟文件，可直接交给 provider 处理；复制失败返回 {@code null}
     */
    public @Nullable VirtualFile toLocalIfNeeded(@NotNull VirtualFile file) {
        if (!isNestedArchive(file)) {
            return file;
        }
        if (file.isDirectory()) {
            return null;
        }
        try {
            Path cached = ensureCached(file);
            return localFileSystem.refreshAndFindFileByNioFile(cached);
        } catch (IOException ex) {
            LOG.debug("Failed to cache nested archive: " + file.getPath(), ex);
            return null;
        }
    }

    /**
     * 判断 VirtualFile 是否位于嵌套归档中。
     */
    public boolean isNestedArchive(@NotNull VirtualFile file) {
        return file.getPath().contains(JarFileSystem.JAR_SEPARATOR);
    }

    private Path ensureCached(@NotNull VirtualFile file) throws IOException {
        String cacheKey = buildCacheKey(file);
        Path targetDir = cacheRoot.resolve(cacheKey);
        Path targetFile = targetDir.resolve(file.getName());
        Object lock = locks.computeIfAbsent(cacheKey, key -> new Object());
        synchronized (lock) {
            if (Files.notExists(targetFile)) {
                Files.createDirectories(targetDir);
                try (InputStream inputStream = file.getInputStream()) {
                    Files.copy(inputStream, targetFile);
                }
            }
        }
        return targetFile;
    }

    private String buildCacheKey(@NotNull VirtualFile file) {
        String source = file.getPath() + "|" + file.getTimeStamp() + "|" + file.getLength();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(source.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(source.hashCode());
        }
    }
}

