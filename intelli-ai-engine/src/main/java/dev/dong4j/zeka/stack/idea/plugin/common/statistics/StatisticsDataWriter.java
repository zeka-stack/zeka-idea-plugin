package dev.dong4j.zeka.stack.idea.plugin.common.statistics;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import lombok.extern.slf4j.Slf4j;
/**
 * 统计数据写入器.
 *
 * @author dong4j
 * @version 1.4.0
 * @email mailto:dong4j@gmail.com
 * @date 2025 年 01 月 05 日
 */
@Slf4j
public class StatisticsDataWriter {

    /** 文件魔数, 用于标识文件类型 */
    private static final byte[] MAGIC = "INTELLAI".getBytes(java.nio.charset.StandardCharsets.UTF_8);
    /** 版本号 */
    private static final int VERSION = 1;
    /**
     * 文件头大小
     * <p>
     * 该常量表示统计数据文件的文件头大小, 单位为字节.
     */
    private static final int HEADER_SIZE = 20;
    /** 日期格式 */
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    static {
        DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    /** 统计数据存储目录 */
    private final File statisticsDir;

    /**
     * 构造函数
     *
     * @param statisticsDir 统计数据目录
     */
    public StatisticsDataWriter(File statisticsDir) {
        this.statisticsDir = statisticsDir;
        if (!statisticsDir.exists()) {
            statisticsDir.mkdirs();
        }
    }

    /**
     * 获取今日数据文件
     *
     * @return 今日日期命名的数据文件
     */
    public File getTodayDataFile() {
        String dateStr = DATE_FORMAT.format(new Date());
        return new File(statisticsDir, dateStr + ".data");
    }

    /**
     * 根据日期获取数据文件
     *
     * @param date 日期字符串, 格式为 yyyy-MM-dd
     * @return 数据文件对象
     */
    public File getDataFile(String date) {
        return new File(statisticsDir, date + ".data");
    }

    /**
     * 创建新文件 (如果不存在)
     *
     * @param file 要创建的文件对象
     * @throws IOException 如果发生 I/O 错误
     */
    public void createFileIfNotExists(File file) throws IOException {
        if (!file.exists()) {
            file.createNewFile();
            // 写入文件头
            try (RandomAccessFile raf = new RandomAccessFile(file, "rws")) {
                raf.write(MAGIC);
                raf.writeInt(VERSION);
                raf.writeInt(0); // recordCount = 0
                raf.writeInt(0); // checksum = 0
            }
        }
    }

    /**
     * 追加写入单条记录
     *
     * @param record 记录对象
     * @throws IOException 如果文件读写过程中发生 I/O 错误
     */
    public void writeRecord(StatisticsRecord record) throws IOException {
        File file = getTodayDataFile();
        log.debug("Writing statistics record to file: {}, record: {}", file.getAbsolutePath(), record);
        createFileIfNotExists(file);

        try (DataOutputStream dos = new DataOutputStream(
            new BufferedOutputStream(
                Files.newOutputStream(file.toPath(), StandardOpenOption.APPEND)))) {

            // 写入加密后的字符串
            dos.writeUTF(EncryptUtils.encryptToHex(record.getPluginId()));
            dos.writeUTF(EncryptUtils.encryptToHex(record.getEventType()));
            dos.writeUTF(EncryptUtils.encryptToHex(record.getProvider()));
            dos.writeUTF(EncryptUtils.encryptToHex(record.getModel()));
            dos.writeLong(record.getTokenCount());
            dos.writeLong(record.getCreatedAt());
            dos.writeUTF(EncryptUtils.encryptToHex(record.getProjectName()));
            dos.writeUTF(EncryptUtils.encryptToHex(record.getResultStatus()));
            dos.writeLong(record.getLatencyMs());
            dos.writeLong(record.getInputToken());
            dos.writeLong(record.getOutputToken());
            dos.writeUTF(EncryptUtils.encryptToHex(record.getUserActionCode()));
        }

        // 更新 recordCount 和 checksum
        updateHeader(file, 1);
    }

    /**
     * 批量写入记录
     *
     * @param records 记录列表
     * @throws IOException IO 异常
     */
    public void writeRecords(java.util.List<StatisticsRecord> records) throws IOException {
        if (records == null || records.isEmpty()) {
            return;
        }
        File file = getTodayDataFile();
        log.debug("Writing {} statistics records to file: {}", records.size(), file.getAbsolutePath());
        createFileIfNotExists(file);

        try (DataOutputStream dos = new DataOutputStream(
            new BufferedOutputStream(
                Files.newOutputStream(file.toPath(), StandardOpenOption.APPEND)))) {

            for (StatisticsRecord record : records) {
                dos.writeUTF(EncryptUtils.encryptToHex(record.getPluginId()));
                dos.writeUTF(EncryptUtils.encryptToHex(record.getEventType()));
                dos.writeUTF(EncryptUtils.encryptToHex(record.getProvider()));
                dos.writeUTF(EncryptUtils.encryptToHex(record.getModel()));
                dos.writeLong(record.getTokenCount());
                dos.writeLong(record.getCreatedAt());
                dos.writeUTF(EncryptUtils.encryptToHex(record.getProjectName()));
                dos.writeUTF(EncryptUtils.encryptToHex(record.getResultStatus()));
                dos.writeLong(record.getLatencyMs());
                dos.writeLong(record.getInputToken());
                dos.writeLong(record.getOutputToken());
                dos.writeUTF(EncryptUtils.encryptToHex(record.getUserActionCode()));
            }
        }

        // 更新 recordCount 和 checksum
        updateHeader(file, records.size());
    }

    /**
     * 更新文件头信息, 包括记录数量和校验和
     *
     * @param file      文件对象
     * @param increment 要增加的记录数量 (非负数)
     * @throws IOException IO 异常
     */
    private void updateHeader(File file, int increment) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(file, "rws")) {
            // 跳过 magic (8) + version (4) = 12 字节
            raf.seek(12);

            // 读取旧的 recordCount
            int oldRecordCount = raf.readInt();

            int recordCount = oldRecordCount + Math.max(increment, 0);

            raf.seek(12);
            raf.writeInt(recordCount);

            // 计算并写入 CRC32
            int checksum = Crc32Utils.calculateFile(file.toPath());
            raf.writeInt(checksum);
        }
    }

    /**
     * 获取文件中的记录数
     *
     * @param file 文件
     * @return 记录数
     * @throws IOException IO 异常
     */
    public int getRecordCount(File file) throws IOException {
        if (!file.exists()) {
            return 0;
        }
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            raf.seek(12);
            return raf.readInt();
        }
    }
}
