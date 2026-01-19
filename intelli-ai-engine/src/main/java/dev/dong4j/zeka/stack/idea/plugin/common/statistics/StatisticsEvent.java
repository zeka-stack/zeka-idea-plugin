package dev.dong4j.zeka.stack.idea.plugin.common.statistics;

import lombok.Getter;
import lombok.Setter;

/**
 * 统计事件数据类
 * <p> 用于封装和传递统计相关的事件数据, 包括插件标识, 事件类型, 提供者, 模型,Token 数量, 创建时间, 项目名称, 结果状态, 延迟毫秒数, 输入输出 Token 数量及用户行为等信息.
 * 该类支持多种构造函数以适应不同场景的数据初始化, 并提供将当前对象转换为 {@code StatisticsRecord} 记录对象的方法.
 * 同时支持通过字符串编码设置用户行为, 并提供字符串表示形式以便调试和日志输出.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.19
 * @since 1.0.0
 */
@Getter
public class StatisticsEvent {

    /** 插件标识, 用于区分不同统计来源的事件 */
    @Setter
    private StatisticsPluginId pluginId;
    /** 事件类型, 用于标识统计事件的具体类别 */
    @Setter
    private StatisticsEventType eventType;
    /** 服务提供方标识, 如 API 服务提供商名称或平台标识 */
    @Setter
    private String provider;
    /** 模型名称, 用于标识所使用的 AI 模型, 如 gpt-4,claude-3 等 */
    @Setter
    private String model;
    /** 请求使用的令牌总数, 用于统计和计费 */
    @Setter
    private long tokenCount;
    /** 创建时间戳, 单位为毫秒, 记录事件生成的精确时间 */
    @Setter
    private long createdAt;
    /** 请求结果状态, 用于标识统计事件的处理结果, 如成功, 失败或超时 */
    @Setter
    private String resultStatus;
    /** 请求延迟时间 (毫秒) */
    @Setter
    private long latencyMs;
    /** 输入的令牌数量, 单位为个 */
    @Setter
    private long inputToken;
    /** 输出的令牌数量, 用于统计模型生成内容的长度 */
    @Setter
    private long outputToken;
    /** 项目名称, 用于标识统计事件所属的项目 */
    private String projectName;
    /** 用户操作行为, 用于记录用户在统计事件中的具体操作类型, 如点击, 提交, 取消等 */
    private StatisticsUserAction userAction;

    /**
     * 默认构造函数, 初始化创建时间戳为当前时间
     * <p> 该构造函数用于创建 StatisticsEvent 实例时自动设置 {@code createdAt} 字段为当前系统时间戳 (毫秒级)
     */
    public StatisticsEvent() {
        this.createdAt = System.currentTimeMillis();
    }

    /**
     * 初始化统计事件对象, 设置基本属性并自动记录创建时间
     * <p> 该构造函数用于创建一个统计事件实例, 包含插件 ID, 事件类型, 提供者, 模型, 令牌数量和项目名称, 并自动设置当前时间戳作为创建时间
     *
     * @param pluginId    插件标识
     * @param eventType   事件类型
     * @param provider    提供者名称
     * @param model       模型名称
     * @param tokenCount  令牌总数
     * @param projectName 项目名称
     */
    public StatisticsEvent(StatisticsPluginId pluginId, StatisticsEventType eventType,
                           String provider, String model, long tokenCount, String projectName) {
        this.pluginId = pluginId;
        this.eventType = eventType;
        this.provider = provider;
        this.model = model;
        this.tokenCount = tokenCount;
        this.projectName = projectName;
        this.createdAt = System.currentTimeMillis();
    }

    /**
     * 初始化统计事件对象, 设置所有事件相关属性
     * <p> 该构造函数用于创建包含完整统计信息的事件对象, 包括插件 ID, 事件类型, 提供者, 模型, 令牌数量, 项目名称, 结果状态, 延迟时间, 输入输出令牌数及用户操作行为
     *
     * @param pluginId     插件标识
     * @param eventType    事件类型
     * @param provider     提供者名称
     * @param model        模型名称
     * @param tokenCount   总令牌数量
     * @param projectName  项目名称
     * @param resultStatus 结果状态
     * @param latencyMs    延迟时间 (毫秒)
     * @param inputToken   输入令牌数量
     * @param outputToken  输出令牌数量
     * @param userAction   用户操作行为对象
     */
    public StatisticsEvent(StatisticsPluginId pluginId,
                           StatisticsEventType eventType,
                           String provider,
                           String model,
                           long tokenCount,
                           String projectName,
                           String resultStatus,
                           long latencyMs,
                           long inputToken,
                           long outputToken,
                           StatisticsUserAction userAction) {
        this.pluginId = pluginId;
        this.eventType = eventType;
        this.provider = provider;
        this.model = model;
        this.tokenCount = tokenCount;
        this.projectName = projectName;
        this.resultStatus = resultStatus;
        this.latencyMs = latencyMs;
        this.inputToken = inputToken;
        this.outputToken = outputToken;
        this.userAction = userAction;
        this.createdAt = System.currentTimeMillis();
    }

    /**
     * 将当前统计事件转换为统计记录对象
     * <p> 根据当前对象的属性值创建并返回一个 {@link StatisticsRecord} 实例, 包含插件 ID, 事件类型, 提供者, 模型, 令牌数量, 创建时间, 项目名称, 结果状态, 延迟毫秒数, 输入令牌数, 输出令牌数和用户操作信息.
     *
     * @return 新创建的 {@link StatisticsRecord} 实例, 包含当前事件的所有统计信息
     */
    public StatisticsRecord toRecord() {
        return new StatisticsRecord(
            pluginId.getCode(),
            eventType.getCode(),
            provider,
            model,
            tokenCount,
            createdAt,
            projectName,
            resultStatus,
            latencyMs,
            inputToken,
            outputToken,
            userAction
        );
    }

    /**
     * 设置用户操作行为对象
     * <p> 将指定的用户操作行为对象赋值给当前事件的 userAction 字段
     *
     * @param userAction 用户操作行为对象, 不能为空
     */
    public void setUserAction(StatisticsUserAction userAction) {
        this.userAction = userAction;
    }

    /**
     * 设置用户操作行为, 通过字符串代码转换为枚举类型
     * <p> 将传入的用户操作字符串代码转换为 {@link StatisticsUserAction} 枚举对象并赋值给当前实例的 userAction 字段
     *
     * @param userAction 用户操作代码字符串, 例如 "CLICK","SWIPE" 等, 必须是 {@link StatisticsUserAction} 支持的有效代码
     */
    public void setUserAction(String userAction) {
        this.userAction = StatisticsUserAction.fromCode(userAction);
    }

    /**
     * 获取用户操作的代码标识
     * <p> 根据当前用户操作对象获取其对应的代码标识, 若用户操作对象为 null, 则返回空字符串
     *
     * @return 用户操作代码标识, 若用户操作对象为 null 则返回空字符串
     */
    public String getUserActionCode() {
        return userAction == null ? "" : userAction.getCode();
    }

    /**
     * 生成当前 StatisticsEvent 对象的字符串表示形式
     * <p> 返回包含所有字段值的格式化字符串, 用于调试和日志输出
     * <pre>{@code
     * StatisticsEvent{pluginId=PluginId.A, eventType=EVENT_TYPE_1, provider='openai', model='gpt-4', tokenCount=123, projectName='my-project', createdAt=1700000000000, resultStatus='SUCCESS', latencyMs=200, inputToken=50, outputToken=73, userAction='USER_ACTION_1'}
     * }</pre>
     *
     * @return 包含所有字段值的字符串表示
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
               ", createdAt=" + createdAt +
               ", resultStatus='" + resultStatus + '\'' +
               ", latencyMs=" + latencyMs +
               ", inputToken=" + inputToken +
               ", outputToken=" + outputToken +
               ", userAction='" + getUserActionCode() + '\'' +
               '}';
    }
}
