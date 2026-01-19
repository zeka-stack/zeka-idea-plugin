package dev.dong4j.zeka.stack.idea.plugin.common.statistics;

import lombok.Getter;
import lombok.Setter;

/**
 * 统计记录数据类
 * <p>用于封装和传递统计相关的数据记录, 包括插件 ID, 事件类型, 提供者, 模型,Token 数量, 创建时间, 项目名称, 结果状态, 延迟毫秒数, 输入输出 Token 数量以及用户操作行为等字段.
 * 该类支持通过 Lombok 的 {@code @Getter} 和 {@code @Setter} 注解自动生成 getter/setter 方法, 便于在业务系统中快速访问和修改统计数据.
 * <p>适用于日志统计, 性能监控, 用户行为分析等场景, 可作为数据传输对象 (DTO) 在服务层与数据层之间传递.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.19
 * @since 1.0.0
 */
@Getter
@Setter
public class StatisticsRecord {

    /** 插件标识符, 用于区分不同插件的统计记录 */
    private String pluginId;
    /** 事件类型, 用于标识统计记录对应的事件类别 */
    private String eventType;
    /** 服务提供者标识, 用于区分不同服务提供商 */
    private String provider;
    /** 模型标识, 用于区分使用的不同模型名称 */
    private String model;
    /** 记录的总令牌数量 */
    private long tokenCount;
    /** 创建时间戳, 单位为毫秒 */
    private long createdAt;
    /** 项目名称, 用于标识统计记录所属的项目 */
    private String projectName;
    /** 请求处理结果状态, 如成功, 失败, 超时等, 用于标识统计记录的最终处理结果 <a href="https://example.com">https://example.com</a> */
    private String resultStatus;
    /** 请求延迟时间 (毫秒) */
    private long latencyMs;
    /** 输入的 Token 数量 */
    private long inputToken;
    /** 输出的令牌数量 */
    private long outputToken;
    /** 用户操作行为信息, 用于记录用户在系统中的具体操作类型, 如点击, 提交, 删除等,<a href="https://example.com">https://example.com</a> */
    private StatisticsUserAction userAction;

    /**
     * 默认构造函数, 用于创建 StatisticsRecord 实例
     * <p> 该构造函数不接受任何参数, 初始化一个空的 StatisticsRecord 对象
     */
    public StatisticsRecord() {
    }

    /**
     * 初始化统计记录对象的构造函数
     * <p> 用于创建一个包含插件 ID, 事件类型, 提供者, 模型, 令牌计数, 创建时间及项目名称的统计记录实例
     *
     * @param pluginId    插件唯一标识
     * @param eventType   事件类型标识
     * @param provider    提供者名称
     * @param model       模型名称
     * @param tokenCount  令牌总数
     * @param createdAt   创建时间戳 (毫秒)
     * @param projectName 项目名称
     */
    public StatisticsRecord(String pluginId, String eventType, String provider, String model,
                            long tokenCount, long createdAt, String projectName) {
        this.pluginId = pluginId;
        this.eventType = eventType;
        this.provider = provider;
        this.model = model;
        this.tokenCount = tokenCount;
        this.createdAt = createdAt;
        this.projectName = projectName;
    }

    /**
     * 初始化统计记录对象的完整构造函数
     * <p> 用于创建包含所有字段的统计记录实例, 适用于需要完整数据填充的场景
     *
     * @param pluginId     插件 ID
     * @param eventType    事件类型
     * @param provider     提供商
     * @param model        模型名称
     * @param tokenCount   总 token 数量
     * @param createdAt    创建时间戳
     * @param projectName  项目名称
     * @param resultStatus 结果状态
     * @param latencyMs    延迟毫秒数
     * @param inputToken   输入 token 数量
     * @param outputToken  输出 token 数量
     * @param userAction   用户操作信息
     */
    public StatisticsRecord(String pluginId, String eventType, String provider, String model,
                            long tokenCount, long createdAt, String projectName,
                            String resultStatus, long latencyMs, long inputToken, long outputToken,
                            StatisticsUserAction userAction) {
        this.pluginId = pluginId;
        this.eventType = eventType;
        this.provider = provider;
        this.model = model;
        this.tokenCount = tokenCount;
        this.createdAt = createdAt;
        this.projectName = projectName;
        this.resultStatus = resultStatus;
        this.latencyMs = latencyMs;
        this.inputToken = inputToken;
        this.outputToken = outputToken;
        this.userAction = userAction;
    }

    /**
     * 获取用户操作代码
     * <p> 如果用户操作对象为 null, 则返回空字符串; 否则调用用户操作对象的 getCode 方法获取操作代码
     *
     * @return 用户操作代码, 若用户操作对象为 null 则返回空字符串
     */
    public String getUserActionCode() {
        return userAction == null ? "" : userAction.getCode();
    }

    /**
     * 生成当前 StatisticsRecord 对象的字符串表示形式
     * <p> 返回包含所有字段值的格式化字符串, 用于调试或日志输出
     * <p> 输出格式示例:StatisticsRecord{pluginId='xxx', eventType='yyy', ...}
     *
     * @return 包含所有字段值的字符串表示
     */
    @Override
    public String toString() {
        return "StatisticsRecord{" +
               "pluginId='" + pluginId + '\'' +
               ", eventType='" + eventType + '\'' +
               ", provider='" + provider + '\'' +
               ", model='" + model + '\'' +
               ", tokenCount=" + tokenCount +
               ", createdAt=" + createdAt +
               ", projectName='" + projectName + '\'' +
               ", resultStatus='" + resultStatus + '\'' +
               ", latencyMs=" + latencyMs +
               ", inputToken=" + inputToken +
               ", outputToken=" + outputToken +
               ", userAction='" + getUserActionCode() + '\'' +
               '}';
    }
}
