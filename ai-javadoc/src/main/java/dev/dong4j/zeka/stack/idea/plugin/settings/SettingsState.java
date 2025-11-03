package dev.dong4j.zeka.stack.idea.plugin.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.xmlb.XmlSerializerUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import dev.dong4j.zeka.stack.idea.plugin.ai.AIProviderType;
import dev.dong4j.zeka.stack.idea.plugin.ai.AIServiceFactory;
import dev.dong4j.zeka.stack.idea.plugin.ai.provider.AICompatibleProvider;
import dev.dong4j.zeka.stack.idea.plugin.ai.provider.AIServiceProvider;
import dev.dong4j.zeka.stack.idea.plugin.task.DocumentationTask;
import dev.dong4j.zeka.stack.idea.plugin.task.TaskCollector;

/**
 * 插件配置状态
 *
 * <p>使用 IntelliJ Platform 的持久化组件机制保存和加载配置。
 * 配置将保存在 IDE 的配置目录中的 JavaDocAI.xml 文件中。
 * 作为插件的核心配置管理类，负责所有用户设置的存储和访问。
 *
 * <p>配置包括：
 * <ul>
 *   <li>AI 服务提供商设置</li>
 *   <li>模型配置</li>
 *   <li>语言支持</li>
 *   <li>高级选项</li>
 *   <li>Prompt 模板配置</li>
 * </ul>
 *
 * <p>设计模式：
 * <ul>
 *   <li>单例模式：通过 getInstance() 获取全局唯一实例</li>
 *   <li>持久化模式：实现 PersistentStateComponent 接口</li>
 *   <li>配置分组：按功能将配置项分组管理</li>
 * </ul>
 *
 * @author dong4j
 * @version 1.0.0
 * @see PersistentStateComponent
 * @see State
 * @see Storage
 * @since 1.0.0
 */
@State(
    name = "JavaDocAISettings",
    storages = @Storage("JavaDocAI.xml")
)
public class SettingsState implements PersistentStateComponent<SettingsState> {

    // ==================== AI 提供商配置 ====================

    /**
     * AI 服务提供商 ID
     *
     * <p>标识当前使用的 AI 服务提供商。
     * 决定使用哪个 AIServiceProvider 实现。
     *
     * <p>支持的值:
     * <ul>
     *   <li>QIANWEN: 通义千问服务</li>
     *   <li>OLLAMA: Ollama 本地服务</li>
     *   <li>CUSTOM: 自定义服务（兼容 OpenAI API）</li>
     * </ul>
     *
     * <p>默认值: QIANWEN
     *
     * @see AIServiceFactory#createProvider(SettingsState)
     * @see AIProviderType
     */
    public String aiProvider = AIProviderType.QIANWEN.getProviderId();

    /**
     * 模型名称
     *
     * <p>指定使用的 AI 模型名称。
     * 不同的提供商支持不同的模型。
     *
     * <p>默认值: "qwen3-8b" (通义千问8B模型)
     *
     * @see AIServiceProvider#getSupportedModels()
     */
    public String modelName = "qwen3-8b";

    /**
     * API Base URL
     *
     * <p>AI 服务的 API 基础地址。
     * 不同提供商有不同的默认地址。
     * 系统会自动处理末尾的斜杠，确保URL格式正确。
     *
     * <p>默认值: "<a href="https://dashscope.aliyuncs.com/compatible-mode/v1">...</a>" (通义千问)
     *
     * @see AIServiceProvider#getDefaultBaseUrl()
     * @see #normalizeBaseUrl(String)
     */
    public String baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1";

    /**
     * API Key
     *
     * <p>访问 AI 服务所需的 API 密钥。
     * 某些本地服务（如 Ollama）不需要此密钥。
     *
     * <p>安全考虑:
     * <ul>
     *   <li>敏感信息，应谨慎处理</li>
     *   <li>建议使用 PasswordSafe 存储（未来优化）</li>
     *   <li>在日志中应脱敏显示</li>
     * </ul>
     *
     * <p>默认值: "" (空字符串)
     *
     * @see AIServiceProvider#requiresApiKey()
     */
    public String apiKey = "";

    /**
     * 配置是否已通过测试验证
     *
     * <p>标记当前配置是否已通过连接测试。
     * 只有通过测试的配置才能被用于生成文档。
     *
     * <p>验证要求:
     * <ul>
     *   <li>API Key 有效（如果需要）</li>
     *   <li>Base URL 可访问</li>
     *   <li>模型可用</li>
     *   <li>能够成功调用 AI 服务</li>
     * </ul>
     *
     * <p>状态管理:
     * <ul>
     *   <li>测试成功时设置为 true</li>
     *   <li>修改关键配置时重置为 false</li>
     *   <li>初始状态为 false</li>
     * </ul>
     *
     * <p>默认值: false
     *
     * @see #isValid()
     */
    public boolean configurationVerified = false;

    // ==================== 功能配置 ====================

    /**
     * 支持的编程语言
     *
     * <p>插件支持的编程语言集合。
     * 目前只支持 Java，未来可扩展到 Kotlin, Python 等。
     *
     * <p>设计考虑:
     * <ul>
     *   <li>使用 Set 避免重复</li>
     *   <li>默认包含 "java"</li>
     *   <li>支持动态添加新语言</li>
     * </ul>
     *
     * <p>默认值: {"java"}
     *
     * @see #isLanguageSupported(String)
     */
    public Set<String> supportedLanguages = new HashSet<String>() {{
        add("java");
    }};

    /**
     * 是否为类生成文档
     *
     * <p>控制是否为类元素生成 JavaDoc 文档。
     * 用户可在设置界面中切换。
     *
     * <p>默认值: true
     */
    public boolean generateForClass = true;

    /**
     * 是否为方法生成文档
     *
     * <p>控制是否为方法元素生成 JavaDoc 文档。
     * 包括普通方法和测试方法。
     *
     * <p>默认值: true
     *
     * @see TaskCollector#collectFromFile(PsiFile)
     */
    public boolean generateForMethod = true;

    /**
     * 是否为字段生成文档
     *
     * <p>控制是否为字段（成员变量）元素生成 JavaDoc 文档。
     * 默认关闭，因为字段通常较简单。
     *
     * <p>默认值: true
     *
     * @see TaskCollector#collectFromFile(PsiFile)
     */
    public boolean generateForField = true;

    /**
     * 是否跳过已有文档的元素
     *
     * <p>控制是否跳过已经包含 JavaDoc 注释的代码元素。
     * 避免重复生成和覆盖用户自定义注释。
     *
     * <p>默认值: true
     *
     * @see TaskCollector#shouldGenerateForElement(PsiElement)
     */
    public boolean skipExisting = true;

    /**
     * 是否优化类代码以减少 token 消耗
     *
     * <p>控制是否为类级别的代码进行优化以减少传递给 AI 的 token 数量。
     * 优化包括：删除多余空行、删除单行注释、保留 JavaDoc 注释等。
     *
     * <p>默认值: true
     *
     * @see TaskCollector#optimizeClassCode(String)
     */
    public boolean optimizeClassCode = true;

    /**
     * 类代码最大行数限制
     *
     * <p>当优化类代码时，如果代码行数超过此限制，将进行截取。
     * 这有助于控制传递给 AI 的 token 数量，避免超长代码导致的性能问题。
     *
     * <p>默认值: 1000
     *
     * @see TaskCollector#optimizeClassCode(String)
     */
    public int maxClassCodeLines = 1000;

    // ==================== 高级配置 ====================

    /**
     * 最大重试次数
     *
     * <p>AI 服务调用失败时的最大重试次数。
     * 用于处理网络波动或服务临时不可用。
     *
     * <p>默认值: 2
     *
     * @see AICompatibleProvider#generateDocumentation(String, DocumentationTask.TaskType, String)
     */
    public int maxRetries = 2;

    /**
     * 请求超时时间（毫秒）
     *
     * <p>AI 服务请求的超时时间。
     * 避免长时间等待影响用户体验。
     *
     * <p>默认值: 10000 (10 秒)
     */
    public int timeout = 10000;

    /**
     * 基础等待时间（毫秒）
     *
     * <p>重试机制中的基础等待时间。
     * 实际等待时间 = waitDuration * 2^(attempt-1)
     *
     * <p>默认值: 5000 (5 秒)
     *
     * @see AICompatibleProvider#generateDocumentation(String, DocumentationTask.TaskType, String)
     */
    public long waitDuration = 5000;

    /**
     * 温度参数
     *
     * <p>控制 AI 生成结果的随机性。
     * 范围 0.0-1.0，较低的值产生更确定的结果。
     * 对于文档生成，建议使用较低值保证一致性。
     *
     * <p>默认值: 0.1 (越低越稳定；注释生成主要是语义重述，不需要太多创造力。)
     */
    public double temperature = 0.1;

    /**
     * 最大 Token 数量
     *
     * <p>AI 服务生成响应的最大 token 数量。
     * 控制生成内容的长度和成本。
     *
     * <p>默认值: 1000
     *
     * @see <a href="https://help.aliyun.com/zh/dashscope/developer-reference/max_tokens">Max Tokens 说明</a>
     */
    public int maxTokens = 1000;

    /**
     * Top-p 参数
     *
     * <p>控制 AI 生成结果的多样性。
     * 范围 0.0-1.0，较低的值产生更确定的结果。
     * 与 temperature 配合使用，控制生成内容的随机性。
     *
     * <p>默认值: 0.9 (保留高概率词，但允许少量变体（比如不同描述方式）。
     */
    public double topP = 0.9;

    /**
     * Top-k 参数
     *
     * <p>限制 AI 在生成下一个 token 时考虑的候选词数量。
     * 范围 1-100，较低的值产生更确定的结果。
     * 与 temperature 和 top-p 配合使用，控制生成内容的随机性。
     *
     * <p>默认值: 50 (平衡创意与质量)
     */
    public int topK = 50;

    /**
     * Presence Penalty 参数
     *
     * <p>控制 AI 避免重复生成相同内容的倾向。
     * 范围 -2.0 到 2.0，正值减少重复，负值增加重复。
     * 对于文档生成，建议使用正值避免重复描述。
     *
     * <p>默认值: 0.0 (不需要惩罚重复，因为注释模板往往有固定格式。
     */
    public double presencePenalty = 0.0;

    /**
     * 可用的服务提供商列表
     *
     * <p>存储所有已配置且通过验证的AI服务提供商信息。
     * 每个提供商包含其配置信息和验证状态。
     *
     * <p>默认值: 空列表
     */
    public List<ProviderConfig> availableProviders = new ArrayList<>();

    /**
     * 服务提供商配置信息
     */
    public static class ProviderConfig {
        /** 提供商标识符 */
        public String providerId;
        /** 模型名称 */
        public String modelName;
        /** 基础请求地址 */
        public String baseUrl;
        /** API 密钥，用于身份验证和接口调用 */
        public String apiKey;
        /** 配置是否已验证的标志 */
        public boolean configurationVerified;
        /** 最近一次验证的时间戳，单位为毫秒 */
        public long lastVerifiedTime;

        /**
         * 默认构造函数
         * <p>
         * 初始化 ProviderConfig 实例的默认构造函数
         */
        public ProviderConfig() {}

        /**
         * 构造一个 ProviderConfig 对象
         * <p>
         * 初始化 ProviderConfig 实例，设置提供者ID、模型名称、基础URL、API密钥、配置验证状态，并记录最后一次验证时间
         *
         * @param providerId            提供者ID
         * @param modelName             模型名称
         * @param baseUrl               基础URL
         * @param apiKey                API密钥
         * @param configurationVerified 配置是否已验证
         */
        public ProviderConfig(String providerId, String modelName, String baseUrl, String apiKey, boolean configurationVerified) {
            this.providerId = providerId;
            this.modelName = modelName;
            this.baseUrl = baseUrl;
            this.apiKey = apiKey;
            this.configurationVerified = configurationVerified;
            this.lastVerifiedTime = System.currentTimeMillis();
        }

        /**
         * 判断当前对象与指定对象是否相等
         * <p>
         * 该方法重写 Object 类的 equals 方法，用于比较两个 ProviderConfig 对象是否相等。
         * 比较依据为 providerId、baseUrl 和 apiKey 字段是否相等。
         *
         * @param obj 要比较的对象
         * @return 如果对象相等则返回 true，否则返回 false
         */
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            ProviderConfig that = (ProviderConfig) obj;
            return Objects.equals(providerId, that.providerId) &&
                   Objects.equals(baseUrl, that.baseUrl) &&
                   Objects.equals(apiKey, that.apiKey);
        }

        /**
         * 生成对象的哈希码
         * <p>
         * 根据 providerId、baseUrl 和 apiKey 三个字段生成哈希码，用于对象的唯一标识。
         *
         * @return 对象的哈希码值
         */
        @Override
        public int hashCode() {
            return Objects.hash(providerId, baseUrl, apiKey);
        }
    }

    /**
     * 是否启用性能模式
     *
     * <p>性能模式下，当任务数量大于5个时，会使用多个可用的服务提供商进行并行处理。
     * 这可以显著提高大量文件处理的速度。
     *
     * <p>默认值: false
     */
    public boolean performanceMode = false;

    /**
     * 是否显示性能模式统计信息
     *
     * <p>控制性能模式完成后是否显示统计信息对话框。
     * 对话框包含各提供商的处理结果统计。
     *
     * <p>默认值: false
     */
    public boolean showProviderStatistics = false;

    /**
     * 是否启用详细日志
     *
     * <p>控制是否输出详细的调试日志。
     * 用于问题排查和开发调试。
     *
     * <p>默认值: false
     *
     * @see AICompatibleProvider#generateDocumentation(String, DocumentationTask.TaskType, String)
     */
    public boolean verboseLogging = false;

    // ==================== Prompt 配置 ====================

    /**
     * 类的 Prompt 模板
     *
     * <p>为类元素生成文档时使用的 Prompt 模板。
     * 使用 %s 作为代码占位符。
     * 用户可在设置界面自定义。
     *
     * <p>默认值: getDefaultClassPromptTemplate()
     *
     * @see #getDefaultClassPromptTemplate()
     */
    public String classPromptTemplate = getDefaultClassPromptTemplate();

    /**
     * 方法的 Prompt 模板
     *
     * <p>为方法元素生成文档时使用的 Prompt 模板。
     * 使用 %s 作为代码占位符。
     * 用户可在设置界面自定义。
     *
     * <p>默认值: getDefaultMethodPromptTemplate()
     *
     * @see #getDefaultMethodPromptTemplate()
     */
    public String methodPromptTemplate = getDefaultMethodPromptTemplate();

    /**
     * 字段的 Prompt 模板
     *
     * <p>为字段元素生成文档时使用的 Prompt 模板。
     * 使用 %s 作为代码占位符。
     * 用户可在设置界面自定义。
     *
     * <p>默认值: getDefaultFieldPromptTemplate()
     *
     * @see #getDefaultFieldPromptTemplate()
     */
    public String fieldPromptTemplate = getDefaultFieldPromptTemplate();

    /**
     * 测试方法的 Prompt 模板
     *
     * <p>为测试方法生成文档时使用的 Prompt 模板。
     * 使用 %s 作为代码占位符。
     * 用户可在设置界面自定义。
     *
     * <p>默认值: getDefaultTestPromptTemplate()
     *
     * @see #getDefaultTestPromptTemplate()
     */
    public String testPromptTemplate = getDefaultTestPromptTemplate();

    /**
     * 系统提示词模板
     *
     * <p>用于设定 AI 角色和行为准则的系统提示词。
     * 这个提示词会作为 system 消息发送给 AI 服务，
     * 用于建立 AI 的基本角色和响应风格。
     *
     * <p>系统提示词的作用：
     * <ul>
     *   <li>设定 AI 的专业角色（Java 开发工程师）</li>
     *   <li>建立响应格式要求（中文 JavaDoc）</li>
     *   <li>定义输出规范（只返回注释，不返回代码）</li>
     *   <li>确保一致性和专业性</li>
     * </ul>
     *
     * <p>默认值: getDefaultSystemPromptTemplate()
     *
     * @see #getDefaultSystemPromptTemplate()
     */
    public String systemPromptTemplate = getDefaultSystemPromptTemplate();

    /**
     * 获取默认的类 Prompt 模板
     *
     * <p>返回为类元素生成文档的默认 Prompt 模板。
     * 包含详细的格式要求和示例。
     *
     * <p>模板特点:
     * <ul>
     *   <li>要求使用中文编写</li>
     *   <li>包含完整的 JavaDoc 格式</li>
     *   <li>提供具体示例</li>
     *   <li>使用 %s 作为代码占位符</li>
     * </ul>
     *
     * @return 默认的类 Prompt 模板
     */
    @NotNull
    public static String getDefaultClassPromptTemplate() {
        return """
            请为以下类/接口/枚举生成类级别的 JavaDoc 注释（中文）。
            
            # 重要说明
            - 下面的代码可能已经包含旧的 JavaDoc 注释，请忽略或改进它
            - 只返回类/接口/枚举级别的 JavaDoc 注释，不要返回方法、字段等其他元素的注释
            - 不要返回代码本身，只返回注释
            - 不要使用任何 markdown 代码块标记（如 ```java）
            
            # 格式要求
            1. 必须包含完整的 JavaDoc 格式，包括开始标记 /** 和结束标记 */
            2. 使用中文编写注释内容
            3. 注释要准确描述类/接口/枚举的职责、主要功能和使用场景
            4. 如果是工具类，需要说明主要提供的功能
            5. 如果是接口，需要说明接口的用途和实现要求
            6. 如果是枚举，需要说明枚举的用途和各个值的含义
            7. 如果有特殊的设计模式，需要说明
            8. 可以使用 @author、@version、@since 等标签
            9. 添加 @date 标签，如果已有相关时间标签, 需要格式化为 yyyy.mm.dd
            
            # 示例
            输入代码：
            public class UserService {
                public User findById(int id) { ... }
                public void save(User user) { ... }
            }
            
            输出注释：
            /**
             * 用户服务类
             * <p>
             * 提供用户相关的业务逻辑处理，包括用户的查询、创建、更新和删除等操作
             *
             * @author dong4j
             * @date 2025.10.24
             * @version 1.0.0
             * @since 1.0.0
             */
            
            待处理的代码片段:
            
            %s
            
            """;
    }

    /**
     * 获取默认的方法 Prompt 模板
     *
     * <p>返回为方法元素生成文档的默认 Prompt 模板。
     * 强调参数、返回值和异常的描述。
     *
     * <p>模板特点:
     * <ul>
     *   <li>要求使用中文编写</li>
     *   <li>强调 @param、@return、@throws 标签</li>
     *   <li>提供具体示例</li>
     *   <li>使用 %s 作为代码占位符</li>
     * </ul>
     *
     * @return 默认的方法 Prompt 模板
     */
    @NotNull
    public static String getDefaultMethodPromptTemplate() {
        return """
            请为以下方法生成 JavaDoc 注释（中文）。
            
            # 重要说明
            - 下面的代码可能已经包含旧的 JavaDoc 注释，请忽略或改进它
            - 只返回方法级别的 JavaDoc 注释，不要返回类、字段等其他元素的注释
            - 不要返回代码本身，只返回注释
            - 不要使用任何 markdown 代码块标记（如 ```java）
            
            # 格式要求
            1. 必须包含完整的 JavaDoc 格式，包括开始标记 /** 和结束标记 */
            2. 使用中文编写注释内容
            3. 注释要准确描述方法的功能、参数、返回值
            4. 如果有参数, 必须包含 @param 标签
            5. 如果有返回值, 必须包含 @return 标签
            6. 如果有异常抛出，使用 @throws 标签
            7. 可以使用 @since 等标签
            
            # 示例
            输入代码：
            public String getUserName(int userId) throws UserNotFoundException {
                return userService.findById(userId).getName();
            }
            
            输出注释：
            /**
             * 根据用户ID获取用户名称
             * <p>
             * 通过用户ID查找用户并返回用户名称
             *
             * @param userId 用户ID
             * @return 用户名称
             * @throws UserNotFoundException 当用户不存在时抛出
             */
            
            待处理的代码片段:
            
            %s
            
            """;
    }

    /**
     * 获取默认的字段 Prompt 模板
     *
     * <p>返回为字段元素生成文档的默认 Prompt 模板。
     * 特别处理单行和多行格式。
     *
     * <p>模板特点:
     * <ul>
     *   <li>要求使用中文编写</li>
     *   <li>区分简单和复杂字段</li>
     *   <li>提供多种格式示例</li>
     *   <li>使用 %s 作为代码占位符</li>
     * </ul>
     *
     * @return 默认的字段 Prompt 模板
     */
    @NotNull
    public static String getDefaultFieldPromptTemplate() {
        return """
            请为以下字段生成 JavaDoc 注释（中文）。
            
            # 重要说明
            - 下面的代码可能已经包含旧的 JavaDoc 注释，请忽略或改进它
            - 只返回字段级别的 JavaDoc 注释，不要返回类、方法等其他元素的注释
            - 不要返回代码本身，只返回注释
            - 不要使用任何 markdown 代码块标记（如 ```java）
            
            # 格式要求
            1. 必须返回完整的 JavaDoc 格式，包括开始标记 /** 和结束标记 */
            2. 使用中文编写注释内容
            3. 注释要准确描述字段的用途和含义
            4. **格式规则（重要）**：
               - 如果字段说明简单（不超过 80 个字符，没有 @tag 标签），必须使用单行格式：/** 字段说明 */
               - 如果字段说明复杂（包含多个信息点、有 @tag 标签、或超过 80 个字符），使用多行格式
            
            # 示例
            示例1 - 简单字段：
            输入：private String username;
            输出：/** 用户名 */
            
            示例2 - 带旧注释的字段：
            输入：/** 旧注释 */ private String tokenValue;
            输出：/** AccessToken 值 */
            
            示例3 - 复杂字段：
            输入：private UserConfig config;
            输出：
            /**
             * 用户配置信息
             * <p>
             * 包含用户偏好设置、主题配置等
             *
             * @see UserConfig
             */
            
            待处理的代码片段:
            
            %s
            
            """;
    }

    /**
     * 获取默认的测试方法 Prompt 模板
     *
     * <p>返回为测试方法生成文档的默认 Prompt 模板。
     * 关注测试目标、场景和预期结果。
     *
     * <p>模板特点:
     * <ul>
     *   <li>要求使用中文编写</li>
     *   <li>强调测试场景描述</li>
     *   <li>提供具体示例</li>
     *   <li>使用 %s 作为代码占位符</li>
     * </ul>
     *
     * @return 默认的测试方法 Prompt 模板
     */
    @NotNull
    public static String getDefaultTestPromptTemplate() {
        return """
            请为以下测试方法生成 JavaDoc 注释（中文）。
            
            # 重要说明
            - 下面的代码可能已经包含旧的 JavaDoc 注释，请忽略或改进它
            - 只返回测试方法级别的 JavaDoc 注释，不要返回类、字段等其他元素的注释
            - 不要返回代码本身，只返回注释
            - 不要使用任何 markdown 代码块标记（如 ```java）
            
            # 格式要求
            1. 必须包含完整的 JavaDoc 格式，包括开始标记 /** 和结束标记 */
            2. 使用中文编写注释内容
            3. 注释应描述：测试目标、测试场景、预期结果
            4. 如果代码中有 @link 引用，请在注释中使用 {@link ClassName#methodName} 格式
            5. 如果运行单元测试需要特殊的场景, 尽量添加上说明
            
            # 示例
            输入代码：
            @Test
            public void testGetUserName_whenUserExists_shouldReturnName() {
                User user = new User(1, "John");
                when(userService.findById(1)).thenReturn(user);
                assertEquals("John", service.getUserName(1));
            }
            
            输出注释：
            /**
             * 测试获取用户名称功能
             * <p>
             * 测试场景：当用户存在时
             * 预期结果：应返回正确的用户名称
             */
            
            待处理的代码片段:
            
            %s
            
            """;
    }

    /**
     * 获取默认的系统提示词模板
     *
     * <p>返回用于设定 AI 角色和行为准则的默认系统提示词。
     * 这个提示词会作为 system 消息发送给 AI 服务，
     * 用于建立 AI 的基本角色和响应风格。
     *
     * <p>模板特点:
     * <ul>
     *   <li>设定 AI 的专业角色（Java 开发工程师）</li>
     *   <li>建立响应格式要求（中文 JavaDoc）</li>
     *   <li>定义输出规范（只返回注释，不返回代码）</li>
     *   <li>确保一致性和专业性</li>
     * </ul>
     *
     * @return 默认的系统提示词模板
     */
    @NotNull
    public static String getDefaultSystemPromptTemplate() {
        return """
            你是一个专业的 Java 开发工程师，专门负责为 Java 代码生成高质量的 JavaDoc 注释。
            
            你精通 Java 编程语言和 JavaDoc 规范，能够准确理解代码逻辑并生成清晰、准确的中文注释。
            
            你的任务是分析用户提供的代码片段，并生成符合 JavaDoc 标准的注释，包括类、方法、字段等元素的文档。
            
            请始终使用中文编写注释，确保注释内容准确、简洁、易懂。
            
            重要要求：
            - 只返回 JavaDoc 注释，不要返回代码本身
            - 不要使用任何 markdown 代码块标记（如 ```java）
            - 确保注释格式符合 JavaDoc 标准
            - 注释内容要准确描述代码的功能和用途
            """;
    }

    // ==================== 持久化方法 ====================

    /**
     * 获取全局配置实例
     *
     * <p>返回插件配置的全局单例实例。
     * 使用 IntelliJ Platform 的服务机制获取。
     *
     * <p>使用示例:
     * <pre>
     * SettingsState settings = SettingsState.getInstance();
     * String provider = settings.aiProvider;
     * </pre>
     *
     * @return 配置实例
     * @see ApplicationManager#getApplication()
     */
    @NotNull
    public static SettingsState getInstance() {
        return ApplicationManager.getApplication().getService(SettingsState.class);
    }

    /**
     * 获取当前设置状态
     * <p>
     * 返回当前对象作为设置状态，用于状态管理或数据传递
     *
     * @return 当前设置状态，可能为 null
     */
    @Nullable
    @Override
    public SettingsState getState() {
        return this;
    }

    /**
     * 加载状态信息到当前对象
     * <p>
     * 通过复制传入的 SettingsState 对象的状态数据到当前对象中
     *
     * @param state 要加载的状态对象
     */
    @Override
    public void loadState(@NotNull SettingsState state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    // ==================== 辅助方法 ====================

    /**
     * 验证配置是否有效
     *
     * <p>检查必需配置项是否完整且有效。
     * 用于保存配置前的验证。
     *
     * <p>验证内容:
     * <ul>
     *   <li>AI 提供商 ID 不为空</li>
     *   <li>模型名称不为空</li>
     *   <li>Base URL 不为空</li>
     *   <li>如需要 API Key 则不为空</li>
     * </ul>
     *
     * @return 如果配置有效返回 true
     * @see #requiresApiKey()
     */
    public boolean isValid() {
        // 检查必需字段
        if (aiProvider == null || aiProvider.trim().isEmpty()) {
            return false;
        }

        if (modelName == null || modelName.trim().isEmpty()) {
            return false;
        }

        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            return false;
        }

        // 检查是否需要 API Key
        return !requiresApiKey() || (apiKey != null && !apiKey.trim().isEmpty());
    }

    /**
     * 当前配置是否需要 API Key
     *
     * <p>根据当前选择的 AI 提供商判断是否需要 API Key。
     * 使用枚举类型进行判断，避免字符串比较的错误。
     *
     * <p>判断逻辑:
     * <ul>
     *   <li>OLLAMA 返回 false</li>
     *   <li>其他提供商返回 true</li>
     * </ul>
     *
     * @return 如果需要 API Key 返回 true
     * @see AIProviderType#requiresApiKey()
     */
    public boolean requiresApiKey() {
        AIProviderType providerType = AIProviderType.fromProviderId(aiProvider);
        return providerType != null && providerType.requiresApiKey();
    }

    /**
     * 检查是否支持指定语言
     *
     * <p>检查插件是否支持指定的编程语言。
     * 不区分大小写。
     *
     * <p>检查逻辑:
     * <ul>
     *   <li>将语言标识符转为小写</li>
     *   <li>检查是否在 supportedLanguages 集合中</li>
     * </ul>
     *
     * @param language 语言标识符（如 "java", "kotlin"）
     * @return 如果支持返回 true
     */
    public boolean isLanguageSupported(String language) {
        return supportedLanguages != null && supportedLanguages.contains(language.toLowerCase());
    }

    /**
     * 添加或更新提供商配置
     *
     * @param providerConfig 提供商配置
     */
    public void addOrUpdateProvider(@NotNull ProviderConfig providerConfig) {
        // 移除相同的配置（基于providerId, baseUrl, apiKey）
        availableProviders.removeIf(config ->
                                        Objects.equals(config.providerId, providerConfig.providerId) &&
                                        Objects.equals(config.baseUrl, providerConfig.baseUrl) &&
                                        Objects.equals(config.apiKey, providerConfig.apiKey)
                                   );

        // 添加新配置
        availableProviders.add(providerConfig);
    }

    /**
     * 获取可用的提供商配置列表
     *
     * @return 已验证的提供商配置列表
     */
    @NotNull
    public List<ProviderConfig> getAvailableProviders() {
        return availableProviders.stream()
            .filter(config -> config.configurationVerified)
            .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 检查是否有多个可用的提供商
     *
     * @return 如果有多个提供商返回true
     */
    public boolean hasMultipleProviders() {
        return getAvailableProviders().size() > 1;
    }

    /**
     * 将当前配置重置为默认值
     * <p>
     * 该方法会将所有配置参数恢复到初始默认状态，包括AI提供者、模型名称、基础URL、API密钥等，
     * 以及生成配置、重试设置、温度参数、最大令牌数等。
     */
    public void resetToDefaults() {
        aiProvider = AIProviderType.QIANWEN.getProviderId();
        modelName = AIProviderType.QIANWEN.getDefaultModel();
        baseUrl = AIProviderType.QIANWEN.getDefaultBaseUrl();
        apiKey = "";
        configurationVerified = false;

        supportedLanguages = new HashSet<>();
        supportedLanguages.add("java");

        generateForClass = true;
        generateForMethod = true;
        generateForField = true;
        skipExisting = true;
        optimizeClassCode = true;
        maxClassCodeLines = 1000;

        maxRetries = 2;
        timeout = 10000;
        waitDuration = 5000;
        temperature = 0.1;
        maxTokens = 1000;
        topP = 0.9;
        topK = 50;
        presencePenalty = 0.0;
        performanceMode = false;
        verboseLogging = false;

        classPromptTemplate = getDefaultClassPromptTemplate();
        methodPromptTemplate = getDefaultMethodPromptTemplate();
        fieldPromptTemplate = getDefaultFieldPromptTemplate();
        testPromptTemplate = getDefaultTestPromptTemplate();
    }

    /**
     * 创建配置的副本
     *
     * <p>创建当前配置的深拷贝副本。
     * 用于配置比较或临时修改。
     *
     * <p>实现方式:
     * <ul>
     *   <li>使用 XmlSerializerUtil.copyBean 进行深拷贝</li>
     *   <li>创建新的 SettingsState 实例</li>
     * </ul>
     *
     * @return 配置副本
     * @see XmlSerializerUtil#copyBean(Object, Object)
     */
    @NotNull
    public SettingsState copy() {
        SettingsState copy = new SettingsState();
        XmlSerializerUtil.copyBean(this, copy);
        return copy;
    }

    /**
     * 标准化 Base URL
     *
     * <p>确保 Base URL 格式正确，移除末尾的斜杠。
     * 这样可以避免在拼接 API 路径时出现双斜杠的问题。
     *
     * <p>处理规则：
     * <ul>
     *   <li>移除末尾的单个或多个斜杠</li>
     *   <li>保留协议部分（http:// 或 https://）</li>
     *   <li>处理空字符串和 null 值</li>
     * </ul>
     *
     * <p>示例：
     * <ul>
     *   <li>"<a href="https://api.openai.com/v1/">...</a>" → "https://api.openai.com/v1"</li>
     *   <li>"http://localhost:11434/v1///" → "http://localhost:11434/v1"</li>
     *   <li>"https://api.example.com" → "https://api.example.com"</li>
     * </ul>
     *
     * @param baseUrl 原始 Base URL
     * @return 标准化后的 Base URL
     */
    @NotNull
    public static String normalizeBaseUrl(@NotNull String baseUrl) {
        if (baseUrl.trim().isEmpty()) {
            return "";
        }

        String normalized = baseUrl.trim();

        // 移除末尾的斜杠
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        return normalized;
    }

    /**
     * 设置 Base URL（自动标准化）
     *
     * <p>设置 Base URL 时自动进行标准化处理，
     * 确保 URL 格式正确。
     *
     * @param baseUrl 要设置的 Base URL
     */
    public void setBaseUrl(@NotNull String baseUrl) {
        this.baseUrl = normalizeBaseUrl(baseUrl);
    }
}

