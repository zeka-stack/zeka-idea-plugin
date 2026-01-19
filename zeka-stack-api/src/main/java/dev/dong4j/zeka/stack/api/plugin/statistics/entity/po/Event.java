package dev.dong4j.zeka.stack.api.plugin.statistics.entity.po;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serial;
import java.util.Date;

import dev.dong4j.zeka.starter.mybatis.base.BasePO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import static dev.dong4j.zeka.starter.mybatis.base.AuditTime.CREATE_TIME;

/**
 * <p> 统计事件表 实体类  </p>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.19 11:55
 * @since 1.0.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("event")
public class Event extends BasePO<Long, Event> {

    /** serialVersionUID */
    @Serial
    private static final long serialVersionUID = 1L;
    /** 设备 ID-表字段 */
    public static final String DEVICE_ID = "device_id";
    /** 客户端上报时间戳(毫秒)-表字段 */
    public static final String CLIENT_TIMESTAMP = "client_timestamp";
    /** 项目名称-表字段 */
    public static final String PROJECT_NAME = "project_name";
    /** 插件标识-表字段 */
    public static final String PLUGIN_ID = "plugin_id";
    /** 事件类型-表字段 */
    public static final String EVENT_TYPE = "event_type";
    /** AI 服务商-表字段 */
    public static final String PROVIDER = "provider";
    /** 模型名称-表字段 */
    public static final String MODEL = "model";
    /** 总 token 数-表字段 */
    public static final String TOKEN_COUNT = "token_count";
    /** 结果状态-表字段 */
    public static final String RESULT_STATUS = "result_status";
    /** 耗时(毫秒)-表字段 */
    public static final String LATENCY_MS = "latency_ms";
    /** 输入 token 数-表字段 */
    public static final String INPUT_TOKEN = "input_token";
    /** 输出 token 数-表字段 */
    public static final String OUTPUT_TOKEN = "output_token";
    /** 触发入口-表字段 */
    public static final String USER_ACTION = "user_action";
    /** 服务端接收时间-表字段 */
    public static final String RECEIVED_TIME = "received_time";

    /** 设备 ID */
    @TableField("`device_id`")
    private String deviceId;
    /** 客户端上报时间戳(毫秒) */
    @TableField("`client_timestamp`")
    private Long clientTimestamp;
    /** 项目名称 */
    @TableField("`project_name`")
    private String projectName;
    /** 插件标识 */
    @TableField("`plugin_id`")
    private String pluginId;
    /** 事件类型 */
    @TableField("`event_type`")
    private String eventType;
    /** AI 服务商 */
    @TableField("`provider`")
    private String provider;
    /** 模型名称 */
    @TableField("`model`")
    private String model;
    /** 总 token 数 */
    @TableField("`token_count`")
    private Long tokenCount;
    /** 结果状态 */
    @TableField("`result_status`")
    private String resultStatus;
    /** 耗时(毫秒) */
    @TableField("`latency_ms`")
    private Long latencyMs;
    /** 输入 token 数 */
    @TableField("`input_token`")
    private Long inputToken;
    /** 输出 token 数 */
    @TableField("`output_token`")
    private Long outputToken;
    /** 触发入口 */
    @TableField("`user_action`")
    private String userAction;
    /** 服务端接收时间 */
    @TableField("`received_time`")
    private Date receivedTime;
    /**
     * 创建时间, 插入时自动填充
     * <p> 用于记录数据创建的精确时间戳 </p>
     *
     * @see FieldFill
     * @see CREATE_TIME
     */
    @TableField(value = CREATE_TIME, fill = FieldFill.INSERT)
    private Date createTime;
}
