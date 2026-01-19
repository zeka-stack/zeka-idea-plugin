package dev.dong4j.zeka.stack.idea.plugin.common.statistics;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 统计数据文件解析测试类
 * <p> 用于验证统计数据文件的解析流程, 包括文件存在性检查, 文件头校验, 校验和验证, 记录数量统计及完整记录读取.
 * 该测试类模拟真实数据文件处理场景, 确保解析器在不同输入条件下行为正确.
 * <p> 示例使用路径:<pre>{@code /Users/dong4j/.zeka-stack/plugin/engine/statistics/2026-01-19.data}</pre>
 * <p> 测试流程包括:
 * <ul>
 *   <li> 验证文件是否存在 </li>
 *   <li> 验证文件头格式是否合法 </li>
 *   <li> 验证文件校验和是否正确 </li>
 *   <li> 输出文件基本信息与记录统计 </li>
 *   <li> 逐条打印解析出的统计记录 </li>
 * </ul>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.19
 * @since 1.0.0
 */
class StatisticsDataFileParserTest {

    /**
     * 测试解析统计数据文件功能
     * <p>
     * 测试场景: 当未提供数据文件路径时, 应抛出异常提示用户必须指定路径
     * 预期结果: 若路径有效且文件存在, 则验证文件头和校验和, 读取并打印所有记录
     * <p>
     * 注意: 运行此测试前需通过 -DstatsFile=/path/to/file.data 或设置 STATS_FILE 环境变量指定数据文件路径
     * <p>
     * 相关类:{@link StatisticsDataReader}
     * <p>
     * 输出示例:
     * <ul>
     *   <li> 文件路径:{file absolute path}</li>
     *   <li> 文件头是否有效:{true/false}</li>
     *   <li> 校验和是否正确:{true/false}</li>
     *   <li> 记录总数 (含头):{count}</li>
     *   <li> 解析出的记录数量:{count}</li>
     *   <li> 逐条打印记录内容 </li>
     * </ul>
     * <p>
     * 参考文档:<a href="https://example.com/stats-file-format"> 数据文件格式说明 </a>
     */
    @Test
    void parseDataFile() throws Exception {
        String path = "/Users/dong4j/.zeka-stack/plugin/engine/statistics/2026-01-19.data";

        File dataFile = new File(path);
        assertTrue(dataFile.exists(), "Data file not found: " + dataFile.getAbsolutePath());

        StatisticsDataReader reader = new StatisticsDataReader(dataFile.getParentFile());
        boolean validHeader = reader.validateFile(dataFile);
        boolean checksumOk = reader.verifyChecksum(dataFile);

        System.out.println("File: " + dataFile.getAbsolutePath());
        System.out.println("Header valid: " + validHeader);
        System.out.println("Checksum ok: " + checksumOk);
        System.out.println("Record count (header): " + reader.getRecordCount(dataFile));

        List<StatisticsRecord> records = reader.readAllRecords(dataFile);
        System.out.println("Records parsed: " + records.size());
        for (int i = 0; i < records.size(); i++) {
            System.out.println("[" + i + "] " + records.get(i));
        }
    }
}
