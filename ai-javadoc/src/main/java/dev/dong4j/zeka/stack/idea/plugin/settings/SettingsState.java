package dev.dong4j.zeka.stack.idea.plugin.settings;

import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.CredentialAttributesKt;
import com.intellij.credentialStore.Credentials;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.xmlb.XmlSerializerUtil;

import org.apache.commons.codec.digest.DigestUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import dev.dong4j.zeka.stack.idea.plugin.ai.AIProviderType;
import dev.dong4j.zeka.stack.idea.plugin.ai.provider.AICompatibleProvider;
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
    storages = @Storage("zeka.stack.ai.javadoc.xml")
)
public class SettingsState implements PersistentStateComponent<SettingsState> {

    // ==================== PasswordSafe 相关常量 ====================

    /** PasswordSafe 服务名称 */
    private static final String PASSWORD_SAFE_SERVICE_NAME = "AI Javadoc";

    /** PasswordSafe 存储键名前缀 */
    private static final String PASSWORD_SAFE_KEY_PREFIX = "AI_JAVADOC_API_KEY_";

    // ==================== AI 提供商配置 ====================

    /**
     * AI 服务提供商类型
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
     * @see AIProviderType
     */
    public AIProviderType providerType = AIProviderType.QIANWEN;

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
     */
    public Set<String> supportedLanguages = new HashSet<>() {{
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
     * 是否覆盖已有注释
     *
     * <p>控制是否覆盖已经存在的文档注释。
     * - false（默认）：跳过已有注释的元素，只对没有注释的元素生成文档
     * - true：覆盖已有注释，无论是否已有注释都会生成新的文档
     *
     * <p>默认值: false（跳过已有注释）
     *
     * @see TaskCollector#shouldGenerateForElement(PsiElement)
     */
    public boolean overrideExisting = false;

    /**
     * 是否启用代码压缩以减少 token 使用量
     *
     * <p>控制是否为代码元素进行压缩处理以减少传递给 AI 的 token 数量。
     *
     * <p>对于类级别的代码，压缩包括：
     * <ul>
     *   <li>删除多余的空行和空白字符</li>
     *   <li>删除单行注释（// 注释）</li>
     *   <li>保留 JavaDoc 注释（/** 注释）</li>
     *   <li>如果超过最大行数限制，进行截取</li>
     * </ul>
     *
     * <p>对于方法和字段级别的代码，压缩包括：
     * <ul>
     *   <li>删除所有注释（Javadoc、块注释、单行注释）</li>
     *   <li>删除多余空格和空行</li>
     *   <li>缩进压缩到最小层级（每层 1 个空格）</li>
     * </ul>
     *
     * <p>注意：压缩后的代码会保持层级关系，但可能会影响 AI 对代码的理解。
     * 建议在需要减少 token 消耗时开启。
     *
     * <p>默认值: false
     *
     * @see TaskCollector#optimizeClassCode(String)
     * @see dev.dong4j.zeka.stack.idea.plugin.util.AiCodePreprocessor
     */
    public boolean enableCodeCompression = false;

    /**
     * 类代码最大行数限制
     *
     * <p>当启用代码压缩处理类代码时，如果代码行数超过此限制，将进行截取。
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
     * <p>用途：用于性能模式中的并行处理，同一服务商可以有多个不同配置
     *
     * <p>默认值: 空集合
     */
    public List<ProviderConfig> availableProviders = new LinkedList<>();

    /**
     * 默认服务提供商配置映射
     *
     * <p>存储每个服务商类型的默认配置，Key 为服务商类型，Value 为对应的配置。
     *
     * <p>用途：
     * <ul>
     *   <li>切换服务商时加载对应的默认配置</li>
     *   <li>保持每个服务商类型的配置独立</li>
     *   <li>避免切换服务商时丢失 API Key</li>
     *   <li>区别于 availableProviders（可有多个相同服务商）</li>
     * </ul>
     *
     * <p>设计说明：
     * <ul>
     *   <li>每个服务商类型只有一个默认配置</li>
     *   <li>UUID 在创建时生成并保持不变</li>
     *   <li>API Key 通过 PasswordSafe 存储，使用 UUID 关联</li>
     * </ul>
     *
     * <p>默认值: 空 Map
     */
    public Map<AIProviderType, ProviderConfig> defaultProviders = new HashMap<>();

    /**
     * 服务提供商配置信息
     */
    public static class ProviderConfig {
        /** 唯一标识符，用于关联 PasswordSafe 中的 API 密钥 */
        public String md5;
        /** 提供商标识符 */
        public AIProviderType providerType;
        /** 模型名称 */
        public String modelName;
        /** 基础请求地址 */
        public String baseUrl;
        /** 配置是否已验证的标志 */
        public boolean configurationVerified = true;
        /** 最近一次验证的时间戳，单位为毫秒 */
        public long lastVerifiedTime;

        public ProviderConfig() {}

        /**
         * 复制构造函数，创建 ProviderConfig 的深拷贝
         * <p>
         * 用于避免对象引用共享导致的意外修改
         *
         * @param source 源配置对象
         */
        public ProviderConfig(ProviderConfig source) {
            this.md5 = source.md5;
            this.providerType = source.providerType;
            this.modelName = source.modelName;
            this.baseUrl = source.baseUrl;
            this.configurationVerified = source.configurationVerified;
            this.lastVerifiedTime = source.lastVerifiedTime;
        }

        /**
         * 构造一个 ProviderConfig 对象（指定 UUID）
         * <p>
         * 初始化 ProviderConfig 实例，允许指定 UUID（用于复用已有的 UUID）
         * API 密钥将通过 PasswordSafe 单独存储
         *
         * @param apiKey       API 密钥，如果为 null 则自动生成
         * @param providerType 提供者ID
         * @param modelName    模型名称
         * @param baseUrl      基础URL
         */
        public ProviderConfig(@Nullable String apiKey,
                              AIProviderType providerType,
                              String modelName,
                              String baseUrl,
                              boolean configurationVerified) {
            this.providerType = providerType;
            this.modelName = modelName;
            this.baseUrl = baseUrl;
            this.md5 = buildMd5(apiKey);
            this.configurationVerified = configurationVerified;
            this.lastVerifiedTime = System.currentTimeMillis();
        }

        public String buildMd5(String apiKey) {
            return DigestUtils.md5Hex(String.format("%s|%s|%s|%s", providerType, baseUrl, modelName, apiKey));
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
            6. 不要添加不存在的参数,返回值和异常的注释标签
            7. 可以使用 @since, @Deprecated 等标签
            
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
            你是一个专业的 Java 开发工程师，专门负责为 Java 代码生成高质量的 JavaDoc 注释,
            精通 Java 编程语言和 JavaDoc 规范，能够准确理解代码逻辑并生成清晰、准确的中文注释。
            现在的任务是分析用户提供的代码片段，并生成符合 JavaDoc 标准的注释。
            请始终使用中文编写注释，确保注释内容准确、简洁、易懂。
            
            重要要求：
            - 只返回 JavaDoc 注释，不要返回代码本身;
            - 不要使用任何 markdown 代码块标记（如 ```java）;
            - 确保注释格式符合 JavaDoc 标准;
            - 注释内容要准确描述代码的功能和用途;
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
    // public boolean isValid() {
    //     // 检查必需字段
    //     if (providerType == null) {
    //         return false;
    //     }
    //
    //     // 从 defaultProviders 获取当前服务商的配置
    //     ProviderConfig defaultConfig = getDefaultProviderConfig(providerType);
    //
    //     if (defaultConfig.modelName == null || defaultConfig.modelName.trim().isEmpty()) {
    //         return false;
    //     }
    //
    //     if (defaultConfig.baseUrl == null || defaultConfig.baseUrl.trim().isEmpty()) {
    //         return false;
    //     }
    //
    //     // 检查是否需要 API Key
    //     if (!requiresApiKey()) {
    //         return true;
    //     }
    //
    //     String apiKey = getDefaultApiKey();
    //     return apiKey != null && !apiKey.trim().isEmpty();
    // }

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
     * 获取可用的提供商配置列表
     *
     * @return 已验证的提供商配置列表
     */
    @NotNull
    public List<ProviderConfig> getAvailableProviders() {
        return availableProviders.stream()
            .filter(config -> config.configurationVerified)
            .toList();
    }

    /**
     * 获取指定服务商类型的默认配置
     * <p>
     * 如果指定的服务商类型不存在默认配置，则自动创建一个新的配置并初始化为该服务商的默认值
     *
     * @param providerType 服务商类型
     * @return 默认配置，永不为 null
     */
    @NotNull
    public ProviderConfig getDefaultProviderConfig(@NotNull AIProviderType providerType) {
        return defaultProviders.computeIfAbsent(providerType, type -> new ProviderConfig(
            "".trim(),
            type,
            type.getDefaultModel(),
            type.getDefaultBaseUrl(),
            false
        ));
    }

    /**
     * 更新指定服务商类型的默认配置
     * <p>
     * 将给定的配置保存为指定服务商类型的默认配置，如果已存在则覆盖
     *
     * @param providerType 服务商类型
     * @param config       配置信息
     */
    public void updateDefaultProviderConfig(@NotNull AIProviderType providerType,
                                            @NotNull ProviderConfig config) {
        defaultProviders.put(providerType, config);
    }

    /**
     * 将当前配置重置为默认值
     * <p>
     * 该方法会将所有配置参数恢复到初始默认状态，包括AI提供者、模型名称、基础URL、API密钥等，
     * 以及生成配置、重试设置、温度参数、最大令牌数等。
     */
    public void resetToDefaults() {
        providerType = AIProviderType.QIANWEN;

        // 重置 defaultProviders 中当前服务商的配置
        ProviderConfig defaultConfig = getDefaultProviderConfig(AIProviderType.QIANWEN);
        defaultConfig.modelName = AIProviderType.QIANWEN.getDefaultModel();
        defaultConfig.baseUrl = AIProviderType.QIANWEN.getDefaultBaseUrl();
        defaultConfig.configurationVerified = false;
        updateDefaultProviderConfig(AIProviderType.QIANWEN, defaultConfig);

        supportedLanguages = new HashSet<>();
        supportedLanguages.add("java");

        generateForClass = true;
        generateForMethod = true;
        generateForField = true;
        overrideExisting = false;
        enableCodeCompression = false;
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

    // ==================== PasswordSafe API Key 管理方法 ====================

    /**
     * 创建 CredentialAttributes
     * <p>
     * 为指定的键名创建凭证属性对象，用于 PasswordSafe 存储和读取
     *
     * @param key 存储键名
     * @return CredentialAttributes 对象
     */
    @NotNull
    private static CredentialAttributes createCredentialAttributes(@NotNull String key) {
        return new CredentialAttributes(
            CredentialAttributesKt.generateServiceName(PASSWORD_SAFE_SERVICE_NAME, key)
        );
    }

    /**
     * 获取默认服务商的 API Key
     * <p>
     * 从 PasswordSafe 中读取默认服务商的 API 密钥
     *
     * @return API Key，如果不存在则返回 null
     */
    // @Nullable
    // public String getDefaultApiKey1() {
    //     Credentials credentials = PasswordSafe.getInstance().get(
    //         createCredentialAttributes(PASSWORD_SAFE_KEY_DEFAULT)
    //                                                             );
    //     return credentials != null ? credentials.getPasswordAsString() : null;
    // }
    //
    // /**
    //  * 设置默认服务商的 API Key
    //  * <p>
    //  * 将默认服务商的 API 密钥存储到 PasswordSafe 中
    //  *
    //  * @param apiKey API 密钥，如果为 null 或空字符串则删除已存储的密钥
    //  */
    // public void setDefaultApiKey1(@Nullable String apiKey) {
    //     if (apiKey == null || apiKey.trim().isEmpty()) {
    //         // 如果 apiKey 为空，删除已存储的密钥
    //         PasswordSafe.getInstance().set(
    //             createCredentialAttributes(PASSWORD_SAFE_KEY_DEFAULT),
    //             null
    //                                       );
    //     } else {
    //         // 存储 API 密钥
    //         PasswordSafe.getInstance().set(
    //             createCredentialAttributes(PASSWORD_SAFE_KEY_DEFAULT),
    //             new Credentials("default", apiKey)
    //                                       );
    //     }
    // }

    /**
     * 获取指定 ProviderConfig 的 API Key
     * <p>
     * 从 PasswordSafe 中读取指定提供商配置的 API 密钥
     *
     * @param uuid 提供商配置的 UUID
     * @return API Key，如果不存在则返回 null
     */
    @Nullable
    public static String getApiKey(@Nullable String uuid) {
        if (uuid == null || uuid.trim().isEmpty()) {
            return null;
        }

        Credentials credentials = PasswordSafe.getInstance().get(
            createCredentialAttributes(PASSWORD_SAFE_KEY_PREFIX + uuid)
                                                                );
        return credentials != null ? credentials.getPasswordAsString() : null;
    }

    /**
     * 设置指定 ProviderConfig 的 API Key
     * <p>
     * 将指定提供商配置的 API 密钥存储到 PasswordSafe 中
     *
     * @param uuid   提供商配置的 UUID
     * @param apiKey API 密钥，如果为 null 或空字符串则删除已存储的密钥
     */
    public static void setApiKey(@Nullable String uuid, @Nullable String apiKey) {
        if (uuid == null || uuid.trim().isEmpty()) {
            return;
        }

        if (apiKey == null || apiKey.trim().isEmpty()) {
            // 如果 apiKey 为空，删除已存储的密钥
            PasswordSafe.getInstance().set(
                createCredentialAttributes(PASSWORD_SAFE_KEY_PREFIX + uuid),
                null
                                          );
        } else {
            // 存储 API 密钥
            PasswordSafe.getInstance().set(
                createCredentialAttributes(PASSWORD_SAFE_KEY_PREFIX + uuid),
                new Credentials(uuid, apiKey)
                                          );
        }
    }

    /**
     * 删除指定 ProviderConfig 的 API Key
     * <p>
     * 从 PasswordSafe 中删除指定提供商配置的 API 密钥
     *
     * @param uuid 提供商配置的 UUID
     */
    public static void deleteApiKey(@Nullable String uuid) {
        if (uuid == null || uuid.trim().isEmpty()) {
            return;
        }

        PasswordSafe.getInstance().set(
            createCredentialAttributes(PASSWORD_SAFE_KEY_PREFIX + uuid),
            null
                                      );
    }

    /**
     * 删除默认服务商的 API Key
     * <p>
     * 从 PasswordSafe 中删除默认服务商的 API 密钥
     */
    // public void deleteDefaultApiKey() {
    //     PasswordSafe.getInstance().set(
    //         createCredentialAttributes(PASSWORD_SAFE_KEY_DEFAULT),
    //         null
    //                                   );
    // }
}

