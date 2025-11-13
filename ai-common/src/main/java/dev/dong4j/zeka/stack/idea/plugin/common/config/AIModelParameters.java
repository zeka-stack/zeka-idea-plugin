package dev.dong4j.zeka.stack.idea.plugin.common.config;

/**
 * AI 模型参数配置。
 */
public class AIModelParameters {
    public double temperature = 0.1;
    public int maxTokens = 1000;
    public double topP = 0.9;
    public int topK = 50;
    public double presencePenalty = 0.0;

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
