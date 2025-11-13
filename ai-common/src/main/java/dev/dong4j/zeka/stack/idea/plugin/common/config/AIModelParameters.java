package dev.dong4j.zeka.stack.idea.plugin.common.config;

/**
 * AI 模型参数配置。
 */
public class AIModelParameters {
    /** 温度参数, 用于控制生成结果的随机性, 值范围通常在 0.0 到 1.0 之间 */
    public double temperature = 0.1;
    /**
     * 最大令牌数量
     * <p>
     * 表示系统或接口允许处理的最大令牌数, 用于控制资源使用或请求规模
     */
    public int maxTokens = 1000;
    /** topP 参数, 用于控制生成文本时的采样概率, 取值范围为 0.0 到 1.0 */
    public double topP = 0.9;
    /**
     * 用于指定取前 K 个元素的数值
     * <p>
     * 该值通常用于排序或筛选操作中, 表示需要保留的最高优先级元素数量
     */
    public int topK = 50;
    /** 用于调整模型生成内容时的注意力分布, 值越大表示对某些内容的抑制越强 */
    public double presencePenalty = 0.0;

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
        parameters.maxTokens = this.maxTokens;
        parameters.topP = this.topP;
        parameters.topK = this.topK;
        parameters.presencePenalty = this.presencePenalty;
        return parameters;
    }
}
