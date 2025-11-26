package dev.dong4j.zeka.stack.idea.plugin.archiver.core;

import org.jetbrains.annotations.NotNull;

import java.nio.charset.Charset;
import java.nio.file.Path;

/**
 * 可编辑压缩条目元数据
 * <p>
 * 保存了压缩包真实路径、条目路径、字符集以及最近一次写入时的校验信息，
 * 便于在写回时做冲突检测与状态更新。
 *
 * @author dong4j
 * @version 0.2.0
 * @since 0.2.0
 */
public record EditableArchiveEntry(Path archivePath, String entryPath, Charset charset, long archiveTimestamp, long crc) {
    public EditableArchiveEntry(@NotNull Path archivePath,
                                @NotNull String entryPath,
                                @NotNull Charset charset,
                                long archiveTimestamp,
                                long crc) {
        this.archivePath = archivePath;
        this.entryPath = entryPath;
        this.charset = charset;
        this.archiveTimestamp = archiveTimestamp;
        this.crc = crc;
    }

    @Override
    @NotNull
    public Path archivePath() {
        return archivePath;
    }

    @Override
    @NotNull
    public String entryPath() {
        return entryPath;
    }

    @Override
    @NotNull
    public Charset charset() {
        return charset;
    }

    /**
     * 创建一个带最新 CRC 与时间戳的拷贝，便于保存成功后刷新状态。
     *
     * @param newTimestamp 压缩包文件最新修改时间
     * @param newCrc       条目最新 CRC
     * @return 新的条目元数据
     */
    @NotNull
    public EditableArchiveEntry withUpdatedState(long newTimestamp, long newCrc) {
        return new EditableArchiveEntry(archivePath, entryPath, charset, newTimestamp, newCrc);
    }
}

