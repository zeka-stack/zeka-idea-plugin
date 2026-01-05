package dev.dong4j.zeka.stack.idea.plugin.common.statistics;

/**
 * <p>Description : 统计数据记录.</p>
 *
 * @author dong4j
 * @version 1.4.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2025.01.05
 */
public class StatisticsRecord {

    /** 插件 ID */
    private String pluginId;
    /** 事件类型 */
    private String eventType;
    /** AI 服务商 */
    private String provider;
    /** 模型名称 */
    private String model;
    /** 消耗的 token 数 */
    private long tokenCount;
    /** 创建时间戳 */
    private long createdAt;
    /** 项目名称 */
    private String projectName;

    /**
     * 默认构造函数
     * <p> 初始化一个空的 StatisticsRecord 实例, 所有字段初始值为默认值.
     *
     */
    public StatisticsRecord() {
    }

    /**
     * 构造函数, 用于初始化统计数据记录对象
     * <p> 创建一个新的 StatisticsRecord 实例, 并设置插件 ID, 事件类型,AI 服务商, 模型名称, 消耗的 token 数, 创建时间戳和项目名称
     *
     * @param pluginId    插件 ID
     * @param eventType   事件类型
     * @param provider    AI 服务商
     * @param model       模型名称
     * @param tokenCount  消耗的 token 数量
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
     * 获取插件 ID
     * <p> 返回当前统计数据记录的插件 ID
     *
     * @return 插件 ID
     */
    public String getPluginId() {
        return pluginId;
    }

    /**
     * 设置插件 ID
     *
     * @param pluginId 插件的唯一标识符
     */
    public void setPluginId(String pluginId) {
        this.pluginId = pluginId;
    }

    /**
     * 获取事件类型
     * <p> 返回当前统计数据记录所关联的事件类型
     *
     * @return 事件类型, 可能为 null
     */
    public String getEventType() {
        return eventType;
    }

    /**
     * 设置事件类型
     * <p> 用于设置当前统计数据记录的事件类型 </p>
     *
     * @param eventType 事件类型, 不能为 null
     */
    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    /**
     * 获取 AI 服务商信息
     * <p> 返回当前统计数据记录中存储的 AI 服务商名称.</p>
     *
     * @return AI 服务商名称, 如果未设置则返回 null
     */
    public String getProvider() {
        return provider;
    }

    /**
     * 设置 AI 服务商
     * <p> 用于记录当前统计记录所使用的 AI 服务提供商名称
     *
     * @param provider AI 服务商名称, 不能为 null 或空字符串
     */
    public void setProvider(String provider) {
        this.provider = provider;
    }

    /**
     * 获取模型名称
     * <p> 返回当前统计记录中所使用的 AI 模型名称
     *
     * @return 模型名称, 可能为 null 或空字符串
     */
    public String getModel() {
        return model;
    }

    /**
     * 设置模型名称
     * <p> 用于记录当前统计数据所使用的 AI 模型名称
     *
     * @param model 模型名称, 不能为 null 或空字符串
     */
    public void setModel(String model) {
        this.model = model;
    }

    /**
     * 获取消耗的 token 数
     * <p> 返回统计数据记录中消耗的 token 数量.
     *
     * @return 消耗的 token 数
     */
    public long getTokenCount() {
        return tokenCount;
    }

    /**
     * 设置消耗的 token 数量
     *
     * @param tokenCount 消耗的 token 数, 必须为非负数
     */
    public void setTokenCount(long tokenCount) {
        this.tokenCount = tokenCount;
    }

    /**
     * 获取记录的创建时间戳
     * <p> 返回该统计数据记录的创建时间戳, 单位为毫秒.
     *
     * @return 记录的创建时间戳
     */
    public long getCreatedAt() {
        return createdAt;
    }

    /**
     * 设置创建时间戳
     * <p> 此方法用于设置统计数据记录的创建时间戳
     *
     * @param createdAt 创建时间戳
     */
    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * 获取项目名称
     * <p> 返回当前统计数据记录所属的项目名称 </p>
     *
     * @return 项目名称, 可能为 null
     */
    public String getProjectName() {
        return projectName;
    }

    /**
     * 设置项目名称
     * <p> 用于设置统计数据记录所属的项目名称.
     *
     * @param projectName 项目名称, 可以为 null
     */
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    /**
     * 返回此统计数据记录的字符串表示形式
     * <p> 该方法重写了 {@link Object#toString()}, 用于生成包含所有字段信息的可读字符串.
     *
     * @return 字符串格式的 StatisticsRecord 对象内容
     * @since 1.4.0
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
               '}';
    }
}
