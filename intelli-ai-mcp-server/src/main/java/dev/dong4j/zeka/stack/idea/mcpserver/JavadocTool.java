package dev.dong4j.zeka.stack.idea.mcpserver;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIChatRequest;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIConsoleLogger;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIServiceException;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.AIServiceProvider;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIProviderType;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIServiceFactory;
import dev.dong4j.zeka.stack.idea.plugin.common.javadoc.JavadocDefaults;
import dev.dong4j.zeka.stack.idea.plugin.common.javadoc.util.AiCodePreprocessor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * generate_javadoc 工具处理器
 * <p>
 * 接收外部 agent 传入的源码文本, 按 elementKind 选择 core 的默认模板,
 * 经 core AI 执行链生成 Javadoc 注释, 再由 {@link AiCodePreprocessor} 做结果后处理,
 * 返回带注释的完整代码文本, 由调用方(agent)自行写回文件.
 * <p>
 * 工具入参:
 * <ul>
 *   <li>sourceCode(必填): 待生成注释的源码文本</li>
 *   <li>elementKind(可选): class / method / field / test, 默认 method,
 *       决定使用哪个默认 prompt 模板</li>
 *   <li>responseLanguage(可选): 注释语言, 默认 中文</li>
 * </ul>
 *
 * @author dong4j
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
public final class JavadocTool {

    /** 供应商配置与密钥(启动时装配) */
    private final ServerConfig serverConfig;

    /**
     * 初始化工具处理器
     *
     * @param serverConfig 服务器配置
     */
    public JavadocTool(@NotNull ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    /**
     * 执行 Javadoc 生成
     *
     * @param args 工具入参(来自 MCP 客户端)
     * @return 带注释的代码文本
     * @throws AIServiceException AI 调用失败时抛出
     */
    public @NotNull String execute(@NotNull Map<String, Object> args) throws AIServiceException {
        String sourceCode = requireString(args, "sourceCode");
        String elementKind = optionalString(args, "elementKind", "method").toLowerCase();
        String responseLanguage = optionalString(args, "responseLanguage", "中文");

        String template = switch (elementKind) {
            case "class" -> JavadocDefaults.getDefaultClassPromptTemplate();
            case "field" -> JavadocDefaults.getDefaultFieldPromptTemplate();
            case "test" -> JavadocDefaults.getDefaultTestPromptTemplate();
            default -> JavadocDefaults.getDefaultMethodPromptTemplate();
        };

        // 模板中的 ${language} 占位符替换为响应语言, 与插件宿主行为一致
        String templateFilled = template.replace("${language}", responseLanguage);
        String systemPrompt = JavadocDefaults.getDefaultSystemPromptTemplate()
            .replace("${language}", responseLanguage);
        String userPrompt = templateFilled + "\n\n待生成注释的源码:\n" + sourceCode;

        String result = callAi(systemPrompt, userPrompt);
        // 与插件生成链路一致: 剥离 markdown 围栏/多余说明等 AI 输出噪声
        return AiCodePreprocessor.preprocess(result);
    }

    /**
     * 调用 core AI 执行链
     *
     * @param systemPrompt 系统提示词
     * @param userPrompt   用户提示词
     * @return AI 生成内容
     * @throws AIServiceException 调用失败时抛出
     */
    private @NotNull String callAi(@NotNull String systemPrompt, @NotNull String userPrompt) throws AIServiceException {
        AIProviderConfig config = serverConfig.providerConfig();
        AIServiceProvider provider = AIServiceFactory.createProvider(new StderrLogger(), config);
        return provider.generateContent(new AIChatRequest(systemPrompt, userPrompt),
                                        serverConfig.apiKey(), null);
    }

    /**
     * 读取必填字符串参数
     *
     * @param args 参数表
     * @param name 参数名
     * @return 参数值
     * @throws IllegalArgumentException 缺失时抛出(MCP 返回 isError 结果)
     */
    static @NotNull String requireString(@NotNull Map<String, Object> args, @NotNull String name) {
        String v = optionalString(args, name, null);
        if (v == null || v.isBlank()) {
            throw new IllegalArgumentException("缺少必填参数: " + name);
        }
        return v;
    }

    /**
     * 读取可选字符串参数
     *
     * @param args         参数表
     * @param name         参数名
     * @param defaultValue 默认值
     * @return 参数值或默认值
     */
    static @NotNull String optionalString(@NotNull Map<String, Object> args,
                                          @NotNull String name,
                                          @Nullable String defaultValue) {
        Object v = args.get(name);
        return v instanceof String s && !s.isBlank() ? s : defaultValue;
    }

    /**
     * stderr 静默日志器
     * <p> MCP stdio 模式下 stdout 是 JSON-RPC 协议通道, 执行链日志必须走 stderr,
     * 否则会污染协议流导致客户端解析失败.
     */
    static class StderrLogger implements AIConsoleLogger {
        /**
         * 输出普通日志
         *
         * @param message 消息
         */
        @Override
        public void print(@NotNull String message) {
            log.debug("{}", message);
        }

        /**
         * 输出带时间戳的日志
         *
         * @param message 消息
         */
        @Override
        public void printWithTimestamp(@NotNull String message) {
            log.debug("{}", message);
        }

        /**
         * 输出成功日志
         *
         * @param message 消息
         */
        @Override
        public void printSuccess(@NotNull String message) {
            log.debug("{}", message);
        }

        /**
         * 输出警告日志
         *
         * @param message 消息
         */
        @Override
        public void printWarning(@NotNull String message) {
            log.warn("{}", message);
        }

        /**
         * 输出错误日志
         *
         * @param message 消息
         */
        @Override
        public void printError(@NotNull String message) {
            log.error("{}", message);
        }

        /**
         * 输出超链接日志(MCP 模式降级为普通日志)
         *
         * @param message  消息
         * @param filePath 文件路径
         * @param line     行号
         */
        @Override
        public void printHyperlink(@NotNull String message, @NotNull String filePath, int line) {
            log.debug("{}", message);
        }

        /**
         * 输出带时间戳的超链接日志(MCP 模式降级为普通日志)
         *
         * @param message  消息
         * @param filePath 文件路径
         * @param line     行号
         */
        @Override
        public void printHyperlinkWithTimestamp(@NotNull String message, @NotNull String filePath, int line) {
            log.debug("{}", message);
        }

        /**
         * 流式输出(MCP 模式丢弃)
         *
         * @param chunk 内容块
         */
        @Override
        public void printStream(@NotNull String chunk) {
        }

        /**
         * 完成流式输出
         */
        @Override
        public void completeStream() {
        }

        /**
         * 流式输出(不换行, MCP 模式丢弃)
         *
         * @param chunk 内容块
         */
        @Override
        public void printStreamPlain(@NotNull String chunk) {
        }

        /**
         * 完成流式输出(不换行)
         */
        @Override
        public void completeStreamPlain() {
        }
    }
}
