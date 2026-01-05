package dev.dong4j.zeka.stack.idea.plugin.common.statistics;

/**
 * <p>Description : 统计事件.</p>
 *
 * @author dong4j
 * @version 1.4.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2025.01.05
 */
public class StatisticsEvent {

    /** 插件 ID */
    private StatisticsPluginId pluginId;
    /** 事件类型 */
    private StatisticsEventType eventType;
    /** AI 服务商 */
    private String provider;
    /** 模型名称 */
    private String model;
    /** 消耗的 token 数 */
    private long tokenCount;
    /** 项目名称 */
    private String projectName;

    /**
     * 默认构造函数, 用于创建 StatisticsEvent 实例
     * <p> 该构造函数初始化一个空的 StatisticsEvent 对象, 所有字段均为默认值
     */
    public StatisticsEvent() {
    }

    /**
     * 构造统计事件对象
     * <p> 用于初始化一个统计事件, 包含插件 ID, 事件类型,AI 服务商, 模型名称, 消耗的 token 数, 项目名称等信息
     *
     * @param pluginId    插件 ID, 不能为空
     * @param eventType   事件类型, 不能为空
     * @param provider    AI 服务商, 不能为空
     * @param model       模型名称, 不能为空
     * @param tokenCount  消耗的 token 数, 必须为非负数
     * @param projectName 项目名称, 不能为空
     */
    public StatisticsEvent(StatisticsPluginId pluginId, StatisticsEventType eventType,
                           String provider, String model, long tokenCount, String projectName) {
        this.pluginId = pluginId;
        this.eventType = eventType;
        this.provider = provider;
        this.model = model;
        this.tokenCount = tokenCount;
        this.projectName = projectName;
    }

    /**
     * 转换为 StatisticsRecord
     *
     * @return 返回转换后的 StatisticsRecord 对象
     */
    public StatisticsRecord toRecord() {
        return new StatisticsRecord(
            pluginId.getCode(),
            eventType.getCode(),
            provider,
            model,
            tokenCount,
            System.currentTimeMillis(),
            projectName
        );
    }

    /**
     * 获取插件 ID
     * <p> 返回当前统计事件的插件 ID
     *
     * @return 插件 ID
     */
    public StatisticsPluginId getPluginId() {
        return pluginId;
    }

    /**
     * 设置插件 ID
     * <p> 用于设置统计事件所属的插件标识符
     *
     * @param pluginId 插件 ID, 不能为 null
     */
    public void setPluginId(StatisticsPluginId pluginId) {
        this.pluginId = pluginId;
    }

    /**
     * 获取事件类型
     * <p> 返回当前统计事件的类型, 该类型定义了事件的分类信息.
     *
     * @return 事件类型
     */
    public StatisticsEventType getEventType() {
        return eventType;
    }

    /**
     * 设置事件类型
     *
     * @param eventType 事件类型, 表示当前统计事件的类别
     */
    public void setEventType(StatisticsEventType eventType) {
        this.eventType = eventType;
    }

    /**
     * 获取 AI 服务商
     *
     * @return AI 服务商名称
     */
    public String getProvider() {
        return provider;
    }

    /**
     * 设置 AI 服务商信息
     * <p> 此方法用于更新当前统计事件对象中的 AI 服务商信息
     *
     * @param provider AI 服务商名称
     */
    public void setProvider(String provider) {
        this.provider = provider;
    }

    /**
     * 获取模型名称
     * <p> 返回当前统计事件关联的模型名称.
     *
     * @return 模型名称, 如果未设置则返回 null
     */
    public String getModel() {
        return model;
    }

    /**
     * 设置模型名称
     * <p> 用于设置当前统计事件所关联的模型名称.
     *
     * @param model 模型名称, 可以为 null
     */
    public void setModel(String model) {
        this.model = model;
    }

    /**
     * 获取消耗的 token 数
     * <p> 返回当前统计事件中消耗的 token 数
     *
     * @return 消耗的 token 数
     */
    public long getTokenCount() {
        return tokenCount;
    }

    /**
     * 设置消耗的 token 数
     * <p> 用于设置统计事件中消耗的 token 数量
     *
     * @param tokenCount 消耗的 token 数, 必须为非负数
     */
    public void setTokenCount(long tokenCount) {
        this.tokenCount = tokenCount;
    }

    /**
     * 获取项目名称
     *
     * @return 项目名称
     */
    public String getProjectName() {
        return projectName;
    }

    /**
     * 设置项目名称
     *
     * @param projectName 项目名称, 用于标识事件所属的项目
     */
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    /**
     * 返回当前统计事件对象的字符串表示形式
     * <p> 该方法重写了 Object 类的 toString 方法, 提供了一个包含统计事件详细信息的字符串表示
     *
     * @return 包含统计事件信息的字符串
     */
    @Override
    public String toString() {
        return "StatisticsEvent{" +
               "pluginId=" + pluginId +
               ", eventType=" + eventType +
               ", provider='" + provider + '\'' +
               ", model='" + model + '\'' +
               ", tokenCount=" + tokenCount +
               ", projectName='" + projectName + '\'' +
               '}';
    }
}
