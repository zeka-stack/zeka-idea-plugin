package dev.dong4j.zeka.stack.idea.plugin.statistics;

import java.io.Serial;
import java.io.Serializable;

import lombok.Data;

/**
 * 使用统计模型
 * <p>
 * 用于记录和统计系统的使用情况，包含模板名称、时间戳、版本信息、IDE信息及插件版本等字段，适用于监控和分析系统使用数据。
 *
 * @author dong4j
 * @version 1.0.0
 * @date 2025.10.25
 * @since 1.0.0
 */
@Data
public class UsageModel implements Serializable {
    /** 序列化版本号，用于确保类的兼容性 */
    @Serial
    private static final long serialVersionUID = 6226996001429878903L;
    /** 模板名称 */
    private String templateName;
    /** 时间戳，表示请求或操作的时间 */
    private Long timestamp;
    /** 版本号 */
    private String version;
    /** IDE 名称 */
    private String ide;
    /** IDE 版本号 */
    private String ideVersion;
    /** 插件版本号 */
    private String pluginVersion;
}
