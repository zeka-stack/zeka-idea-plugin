package dev.dong4j.zeka.stack.idea.plugin.common.config;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * 思考能力三探针探测结果
 * <p>
 * 字段为 public 以支持 PersistentStateComponent XML 序列化.
 * 在测试连接成功后写入, 并随可用服务商配置持久化.
 *
 * @author dong4j
 * @version 1.0.0
 * @since 1.0.0
 */
public class ThinkingProbeResult {

    /** 汇总能力画像 */
    public ThinkingCapability capability = ThinkingCapability.UNKNOWN;

    public boolean omitOk;
    public boolean trueOk;
    public boolean falseOk;

    public boolean omitHasThinking;
    public boolean trueHasThinking;
    public boolean falseHasThinking;

    /** 探针失败时的简短错误 (成功时为 null) */
    public String omitError;
    public String trueError;
    public String falseError;

    /** 探测完成时间戳 (epoch millis) */
    public long probedAt;

    /** 给人看的多行摘要 */
    public String summary;

    /**
     * 深拷贝
     *
     * @return 副本
     */
    @NotNull
    public ThinkingProbeResult copy() {
        ThinkingProbeResult copy = new ThinkingProbeResult();
        copy.capability = this.capability != null ? this.capability : ThinkingCapability.UNKNOWN;
        copy.omitOk = this.omitOk;
        copy.trueOk = this.trueOk;
        copy.falseOk = this.falseOk;
        copy.omitHasThinking = this.omitHasThinking;
        copy.trueHasThinking = this.trueHasThinking;
        copy.falseHasThinking = this.falseHasThinking;
        copy.omitError = this.omitError;
        copy.trueError = this.trueError;
        copy.falseError = this.falseError;
        copy.probedAt = this.probedAt;
        copy.summary = this.summary;
        return copy;
    }

    /**
     * 内容是否与另一结果等价 (用于 isModified)
     *
     * @param other 另一结果, 可为 null
     * @return 等价返回 true
     */
    public boolean contentEquals(@Nullable ThinkingProbeResult other) {
        if (other == null) {
            return false;
        }
        return capability == other.capability
               && omitOk == other.omitOk
               && trueOk == other.trueOk
               && falseOk == other.falseOk
               && omitHasThinking == other.omitHasThinking
               && trueHasThinking == other.trueHasThinking
               && falseHasThinking == other.falseHasThinking
               && Objects.equals(omitError, other.omitError)
               && Objects.equals(trueError, other.trueError)
               && Objects.equals(falseError, other.falseError)
               && probedAt == other.probedAt
               && Objects.equals(summary, other.summary);
    }

    /**
     * 格式化为结果弹窗 / 日志可读文本
     *
     * @return 多行摘要
     */
    @NotNull
    public String formatForDisplay() {
        if (summary != null && !summary.isBlank()) {
            return summary;
        }
        ThinkingCapability cap = capability != null ? capability : ThinkingCapability.UNKNOWN;
        return "结论: " + cap.displayLabel();
    }
}
