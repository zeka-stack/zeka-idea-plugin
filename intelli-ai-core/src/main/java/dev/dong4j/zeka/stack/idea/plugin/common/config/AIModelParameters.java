package dev.dong4j.zeka.stack.idea.plugin.common.config;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * AI 模型参数配置类
 * <p>
 * 用于配置 AI 模型的各种参数, 包括温度, 最大令牌数,topP,topK 和存在惩罚等参数
 * 提供参数复制功能, 支持创建参数对象的副本
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email "mailto:zeka.stack@gmail.com"
 * @date 2025.11.30
 * @since 1.0.0
 */
public class AIModelParameters {
    /** 温度参数, 用于控制生成结果的随机性, 值范围通常在 0.0 到 1.0 之间, 或 "auto" 表示使用模型默认值 */
    public String temperature = "auto";
    /**
     * 最大令牌数量
     * <p>
     * 表示系统或接口允许处理的最大令牌数, 用于控制资源使用或请求规模, 或 "auto" 表示使用模型默认值
     */
    public String maxTokens = "auto";
    /** topP 参数, 用于控制生成文本时的采样概率, 取值范围为 0.0 到 1.0, 或 "auto" 表示使用模型默认值 */
    public String topP = "auto";
    /**
     * 用于指定取前 K 个元素的数值
     * <p>
     * 该值通常用于排序或筛选操作中, 表示需要保留的最高优先级元素数量, 或 "auto" 表示使用模型默认值
     */
    public String topK = "auto";
    /** 用于调整模型生成内容时的注意力分布, 值越大表示对某些内容的抑制越强, 或 "auto" 表示使用模型默认值 */
    public String presencePenalty = "auto";

    /**
     * 创建并返回当前 {@link AIModelParameters} 对象的拷贝.
     * <p>
     * 该方法会生成一个新的 {@code AIModelParameters} 实例, 并将当前实例的
     * {@code temperature},{@code maxTokens},{@code topP},{@code topK},{@code presencePenalty}
     * 等属性复制到新实例中. 返回的对象与原对象互不影响, 修改新对象不会影响原对象.
     *
     * @return 当前对象的拷贝
     */
    public AIModelParameters copy() {
        AIModelParameters parameters = new AIModelParameters();
        parameters.temperature = this.temperature;
        // 在复制时也进行迁移，确保复制的配置格式正确
        parameters.maxTokens = migrateMaxTokens(this.maxTokens);
        parameters.topP = this.topP;
        parameters.topK = this.topK;
        parameters.presencePenalty = this.presencePenalty;
        return parameters;
    }

    /**
     * 将老配置中的 maxTokens 值转换为新格式（K 单位，纯数字）
     * <p>
     * 输入框已经提示单位是 K，用户只能输入数字，所以返回值应该是纯数字字符串。
     * <p>
     * 转换规则：
     * <ul>
     *   <li>如果 maxTokens 是 "auto"，返回 "auto"</li>
     *   <li>如果 maxTokens 是纯数字且 >= 1000，说明是老配置（实际 token 数），需要除以 1000 转换为 K 单位</li>
     *   <li>如果 maxTokens 是纯数字且 < 1000，可能是 K 单位的值，直接返回</li>
     * </ul>
     *
     * @param maxTokens 原始 maxTokens 值（老配置只有纯数字）
     * @return 转换后的 maxTokens 值（纯数字字符串或 "auto"）
     */
    @NotNull
    public static String migrateMaxTokens(@Nullable String maxTokens) {
        if (maxTokens == null || maxTokens.trim().isEmpty() || "auto".equalsIgnoreCase(maxTokens.trim())) {
            return "auto";
        }

        String trimmed = maxTokens.trim();
        
        // 尝试解析为数字（老配置只有纯数字）
        try {
            int value = Integer.parseInt(trimmed);
            // 如果 >= 1000，说明是老配置（实际 token 数），转换为 K 单位（除以 1000）
            if (value >= 1000) {
                double valueInK = value / 1000.0;
                // 如果是整数，返回整数部分
                if (valueInK == (int) valueInK) {
                    return String.valueOf((int) valueInK);
                } else {
                    // 保留一位小数
                    return String.format("%.1f", valueInK);
                }
            } else {
                // 如果 < 1000，可能是 K 单位的值，直接返回（保持原值）
                return trimmed;
            }
        } catch (NumberFormatException e) {
            // 如果不是数字，返回 "auto"
            return "auto";
        }
    }
}
