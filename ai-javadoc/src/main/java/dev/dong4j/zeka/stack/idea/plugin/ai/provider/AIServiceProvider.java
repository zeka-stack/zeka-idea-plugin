package dev.dong4j.zeka.stack.idea.plugin.ai.provider;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import dev.dong4j.zeka.stack.idea.plugin.ai.AIServiceException;
import dev.dong4j.zeka.stack.idea.plugin.ai.ValidationResult;
import dev.dong4j.zeka.stack.idea.plugin.task.DocumentationTask;

/**
 * AI 服务提供商接口
 *
 * <p>定义了所有 AI 服务提供商必须实现的基本功能。
 * 这个接口抽象了不同 AI 服务的具体实现细节，使得系统可以方便地切换和扩展不同的 AI 服务。
 * 通过统一的接口设计，插件可以支持多种 AI 服务提供商，提高系统的灵活性和可扩展性。
 *
 * <p>接口设计原则：
 * <ul>
 *   <li>单一职责：每个方法只负责一个特定的功能</li>
 *   <li>开闭原则：对扩展开放，对修改关闭</li>
 *   <li>依赖倒置：高层模块不依赖低层模块，都依赖抽象</li>
 * </ul>
 *
 * <p>支持的提供商类型：
 * <ul>
 *   <li>通义千问（QianWen）</li>
 *   <li>Ollama（本地模型）</li>
 *   <li>OpenAI（可选）</li>
 *   <li>其他兼容 OpenAI API 的服务</li>
 * </ul>
 *
 * <p>使用示例：
 * <pre>
 * AIServiceProvider provider = AIServiceFactory.createProvider(settings);
 * String documentation = provider.generateDocumentation(code, DocumentationTask.TaskType.METHOD, "java");
 * </pre>
 *
 * @author dong4j
 * @version 1.0.0
 * @since 1.0.0
 */
public interface AIServiceProvider {

    /**
     * 生成文档注释
     *
     * <p>根据提供的代码片段和文档类型，调用 AI 服务生成相应的文档注释。
     * 这是接口的核心方法，负责与 AI 服务进行交互，获取生成的文档内容。
     *
     * <p>处理流程：
     * <ol>
     *   <li>构建 Prompt：根据文档类型选择相应的 Prompt 模板</li>
     *   <li>调用 AI 服务：发送请求到 AI 服务提供商</li>
     *   <li>处理响应：解析 AI 服务返回的结果</li>
     *   <li>返回结果：返回格式化的文档注释</li>
     * </ol>
     *
     * <p>异常处理：
     * 当 AI 服务调用失败时，会抛出 AIServiceException 异常，
     * 上层代码可以根据异常类型采取相应的处理措施。
     *
     * @param code     代码片段，包含需要生成文档的代码元素
     * @param type     文档类型，决定使用的 Prompt 模板
     * @param language 编程语言（如 "java", "kotlin"），用于语言特定的处理
     * @return 生成的文档注释，如果失败返回空字符串
     * @throws AIServiceException 当 AI 服务调用失败时抛出，包含详细的错误信息
     * @see AIServiceException
     * @see DocumentationTask.TaskType
     */
    @NotNull
    String generateDocumentation(@NotNull String code,
                                 @NotNull DocumentationTask.TaskType type,
                                 @NotNull String language) throws AIServiceException;

    /**
     * 验证配置是否正确
     *
     * <p>检查 API Key、Base URL、模型名称等配置是否有效。
     * 通过发送测试请求到 AI 服务来验证配置的正确性。
     * 这个方法在用户保存配置时调用，确保配置的有效性。
     *
     * <p>验证内容包括：
     * <ul>
     *   <li>必要配置项是否为空</li>
     *   <li>API Key 是否有效（如果需要）</li>
     *   <li>Base URL 是否可访问</li>
     *   <li>模型是否可用</li>
     * </ul>
     *
     * <p>验证策略：
     * <ul>
     *   <li>发送简单的测试请求</li>
     *   <li>检查响应状态和内容</li>
     *   <li>根据结果返回验证状态和详细信息</li>
     * </ul>
     *
     * <p>返回结果：
     * <ul>
     *   <li>成功：ValidationResult.success("连接成功")</li>
     *   <li>失败：ValidationResult.failure("错误消息", "详细信息")</li>
     * </ul>
     *
     * @return 验证结果，包含成功/失败状态和详细信息
     * @see ValidationResult
     */
    @NotNull
    ValidationResult validateConfiguration();

    /**
     * 获取提供商标识符
     *
     * <p>返回唯一标识此提供商的字符串，用于配置存储和识别。
     * 标识符在整个系统中必须是唯一的，用于区分不同的 AI 服务提供商。
     *
     * <p>标识符规范：
     * <ul>
     *   <li>使用小写字母和下划线</li>
     *   <li>简洁明了，易于理解</li>
     *   <li>与提供商名称保持一致</li>
     * </ul>
     *
     * @return 提供商标识符，如 QIANWEN, OLLAMA, CUSTOM
     */
    @NotNull
    String getProviderId();

    /**
     * 获取提供商显示名称
     *
     * <p>返回用于UI显示的友好名称，供用户在设置界面中选择。
     * 显示名称应该是本地化的，支持多语言环境。
     *
     * <p>命名规范：
     * <ul>
     *   <li>使用中文或英文，根据语言环境切换</li>
     *   <li>包含提供商的官方名称</li>
     *   <li>可以包含额外信息（如本地模型标识）</li>
     * </ul>
     *
     * @return 提供商显示名称，如 "通义千问", "Ollama (本地)"
     */
    @NotNull
    String getProviderName();

    /**
     * 获取支持的模型列表
     *
     * <p>返回此提供商支持的所有模型名称列表。
     * 列表包含推荐的常用模型，用户也可以输入其他支持的模型名称。
     *
     * <p>列表特点：
     * <ul>
     *   <li>包含推荐的常用模型</li>
     *   <li>模型名称应该是完整的、可直接使用的</li>
     *   <li>列表可能随时间更新，反映提供商的最新支持情况</li>
     * </ul>
     *
     * @return 模型名称列表，包含此提供商支持的模型
     */
    @NotNull
    List<String> getSupportedModels();

    /**
     * 获取默认模型
     *
     * <p>返回此提供商推荐的默认模型。
     * 默认模型应该是性能和效果平衡的最佳选择，
     * 适合大多数用户的使用场景。
     *
     * <p>选择标准：
     * <ul>
     *   <li>性能稳定</li>
     *   <li>效果良好</li>
     *   <li>成本合理</li>
     *   <li>广泛支持</li>
     * </ul>
     *
     * @return 默认模型名称，推荐给用户使用的模型
     */
    @NotNull
    String getDefaultModel();

    /**
     * 获取默认 Base URL
     *
     * <p>返回此提供商的默认 API 地址。
     * Base URL 是调用 AI 服务 API 的基础地址，
     * 通常由提供商官方提供。
     *
     * <p>URL 格式要求：
     * <ul>
     *   <li>完整的 HTTP/HTTPS 地址</li>
     *   <li>不包含具体的 API 路径</li>
     *   <li>以斜杠结尾（便于路径拼接）</li>
     * </ul>
     *
     * @return 默认 Base URL，用于构建完整的 API 请求地址
     */
    @NotNull
    String getDefaultBaseUrl();

    /**
     * 获取可用的模型列表
     *
     * <p>通过调用提供商的 API 接口获取当前可用的模型列表。
     * 这个方法会实际调用远程服务，获取最新的模型信息。
     * 与 getSupportedModels() 不同，这个方法返回的是实时数据。
     *
     * <p>使用场景：
     * <ul>
     *   <li>用户配置 Base URL 和 API Key 后，动态获取可用模型</li>
     *   <li>验证模型名称是否有效</li>
     *   <li>提供最新的模型选择列表</li>
     * </ul>
     *
     * <p>实现要求：
     * <ul>
     *   <li>必须处理网络异常和超时</li>
     *   <li>必须处理 API 认证失败</li>
     *   <li>必须处理响应格式错误</li>
     *   <li>失败时应该返回空列表而不是抛出异常</li>
     * </ul>
     *
     * <p>不同提供商的 API 接口：
     * <ul>
     *   <li>Ollama: GET {baseUrl}/models</li>
     *   <li>OpenAI: GET {baseUrl}/models</li>
     *   <li>通义千问: GET {baseUrl}/models</li>
     * </ul>
     *
     * @return 可用模型名称列表，如果获取失败返回空列表
     * @see #getSupportedModels()
     */
    @NotNull
    List<String> getAvailableModels();

    /**
     * 是否需要 API Key
     *
     * <p>判断此提供商是否需要 API Key 进行身份验证。
     * 某些本地服务（如 Ollama）不需要 API Key，
     * 而云服务通常需要 API Key 来控制访问权限。
     *
     * <p>使用场景：
     * <ul>
     *   <li>配置验证时检查必要字段</li>
     *   <li>UI 显示时控制 API Key 输入框的可见性</li>
     *   <li>请求构建时决定是否添加认证头</li>
     * </ul>
     *
     * @return 如果需要 API Key 返回 true，否则返回 false
     */
    boolean requiresApiKey();
}

