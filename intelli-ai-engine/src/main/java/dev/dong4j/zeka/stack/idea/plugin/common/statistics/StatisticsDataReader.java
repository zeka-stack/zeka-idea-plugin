package dev.dong4j.zeka.stack.idea.plugin.common.statistics;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * 统计数据读取器.
 *
 * @author dong4j
 * @version 1.4.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2025.01.05
 */
public class StatisticsDataReader {

    /** 文件魔数, 用于标识文件格式 */
    private static final byte[] MAGIC = "INTELLAI".getBytes(java.nio.charset.StandardCharsets.UTF_8);
    /** 版本号 */
    private static final int VERSION = 1;
    /** 文件头大小, 表示文件头占用的字节数 */
    private static final int HEADER_SIZE = 20;

    /** 统计数据目录, 用于读取和存储统计信息文件 */
    private final File statisticsDir;

    /**
     * 构造函数
     *
     * @param statisticsDir 统计数据目录
     */
    public StatisticsDataReader(File statisticsDir) {
        this.statisticsDir = statisticsDir;
    }

    /**
     * 验证文件头
     *
     * @param file 文件
     * @return 是否有效
     */
    public boolean validateFile(File file) {
        if (!file.exists() || !file.isFile()) {
            return false;
        }

        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            // 验证 magic
            byte[] fileMagic = new byte[8];
            raf.read(fileMagic);
            if (!java.util.Arrays.equals(MAGIC, fileMagic)) {
                return false;
            }

            // 验证版本号
            int fileVersion = raf.readInt();
            return fileVersion == VERSION;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * 读取文件中的所有记录
     *
     * @param file 文件
     * @return 记录列表
     * @throws IOException IO 异常
     */
    public List<StatisticsRecord> readAllRecords(File file) throws IOException {
        List<StatisticsRecord> records = new ArrayList<>();

        if (!file.exists()) {
            return records;
        }

        try (DataInputStream dis = new DataInputStream(
            new BufferedInputStream(Files.newInputStream(file.toPath())))) {

            // 跳过文件头
            byte[] header = new byte[HEADER_SIZE];
            dis.readFully(header);

            while (dis.available() > 0) {
                StatisticsRecord record = readRecord(dis);
                if (record != null) {
                    records.add(record);
                }
            }
        }

        return records;
    }

    /**
     * 读取单条记录
     *
     * @param dis 数据输入流
     * @return 记录对象, 若到达文件末尾则返回 null
     * @throws IOException 如果读取过程中发生 I/O 异常
     */
    private StatisticsRecord readRecord(DataInputStream dis) throws IOException {
        try {
            String pluginId = EncryptUtils.decryptFromHex(dis.readUTF());
            String eventType = EncryptUtils.decryptFromHex(dis.readUTF());
            String provider = EncryptUtils.decryptFromHex(dis.readUTF());
            String model = EncryptUtils.decryptFromHex(dis.readUTF());
            long tokenCount = dis.readLong();
            long createdAt = dis.readLong();
            String projectName = EncryptUtils.decryptFromHex(dis.readUTF());

            return new StatisticsRecord(pluginId, eventType, provider, model, tokenCount, createdAt, projectName);
        } catch (EOFException e) {
            return null;
        }
    }

    /**
     * 从指定位置开始读取记录
     *
     * @param file          文件
     * @param startPosition 开始位置
     * @param count         读取数量
     * @return 记录列表
     * @throws IOException IO 异常
     */
    public List<StatisticsRecord> readRecordsFromPosition(File file, long startPosition, int count) throws IOException {
        List<StatisticsRecord> records = new ArrayList<>();

        if (!file.exists()) {
            return records;
        }

        long safeStart = Math.max(startPosition, HEADER_SIZE);
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            raf.seek(safeStart);
            try (DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(raf.getFD())))) {
                for (int i = 0; i < count && dis.available() > 0; i++) {
                    StatisticsRecord record = readRecord(dis);
                    if (record != null) {
                        records.add(record);
                    }
                }
            }
        }

        return records;
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
            // 跳过 magic (8) + version (4) = 12 字节
            raf.seek(12);
            return raf.readInt();
        }
    }

    /**
     * 验证文件 CRC32
     *
     * @param file 文件
     * @return 是否有效
     * @throws IOException IO 异常
     */
    public boolean verifyChecksum(File file) throws IOException {
        if (!file.exists()) {
            return true;
        }

        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            // 跳过 magic (8) + version (4) + recordCount (4) = 16 字节
            raf.seek(16);
            int storedChecksum = raf.readInt();

            int calculatedChecksum = Crc32Utils.calculateFile(file.toPath());
            return storedChecksum == calculatedChecksum;
        }
    }

    /**
     * 获取所有数据文件的日期列表.
     * <p>
     * 该方法遍历统计数据目录, 查找所有以 ".data" 结尾的文件, 并提取文件名中的日期部分, 组成日期列表.
     *
     * @return 日期列表
     */
    public List<String> getAllDataFileDates() {
        List<String> dates = new ArrayList<>();
        if (!statisticsDir.exists()) {
            return dates;
        }

        File[] files = statisticsDir.listFiles((dir, name) -> name.endsWith(".data"));
        if (files != null) {
            for (File file : files) {
                String name = file.getName();
                String date = name.substring(0, name.length() - 5); // 去掉 .data 后缀
                dates.add(date);
            }
        }
        return dates;
    }
}
