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
import java.util.Set;
import java.util.stream.Collectors;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIProviderType;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIModelParameters;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderSettings;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIRuntimeSettings;
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

    /**
     * 默认 AI 提供商类型
     * <p>
     * 插件使用的默认供应商，从全局可用供应商列表中选取。
     * 全局供应商配置在 Settings → Tools → AI Common 中管理。
     *
     * @see AIProviderSettings
     */
    public AIProviderConfig providerConfig;

    /**
     * 模型参数（可选）
     * <p>
     * 如果为 null，则使用全局默认值。
     */
    public AIModelParameters modelParameters = new AIModelParameters();

    /**
     * 运行时设置（可选）
     * <p>
     * 如果为 null，则使用全局默认值。
     */
    public AIRuntimeSettings runtimeSettings = new AIRuntimeSettings();

    /**
     * 性能模式开关
     */
    public boolean performanceMode = false;

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


    // ==================== JavaDoc 标签配置 ====================

    /**
     * 自定义 JavaDoc 标签列表
     *
     * <p>用户可以在设置页面配置自定义的 JavaDoc 标签。
     * 这些标签会被自动注册到 JavadocDeclarationInspection 中，
     * 使得 IntelliJ IDEA 不会将这些标签标记为未知标签。
     *
     * <p>标签格式：
     * <ul>
     *   <li>标签名称不包含 @ 符号</li>
     *   <li>标签名称不区分大小写</li>
     *   <li>标签名称不能包含逗号、空格等特殊字符</li>
     * </ul>
     *
     * <p>默认值: ["date", "email"]
     * <p>首次加载配置时，如果列表为空，会自动添加默认的 "date" 和 "email" 标签。
     *
     * <p>示例：
     * <pre>
     * customJavaDocTags = ["date", "email", "custom"]
     * </pre>
     *
     * @see dev.dong4j.zeka.stack.idea.plugin.component.CustomJavaDocTagRegistrar
     * @since 1.3.4
     */
    public List<String> customJavaDocTags = new ArrayList<>();

    /**
     * 是否显示自定义 JavaDoc 标签配置面板
     *
     * <p>控制设置页面中自定义 JavaDoc 标签配置表格的显示/隐藏。
     * 用户可以通过复选框控制是否显示标签管理表格，减少设置页面长度。
     *
     * <p>默认值: false（默认隐藏，减少页面长度）
     *
     * @since 1.4.0
     */
    public boolean showCustomJavaDocTags = false;

    /**
     * 是否显示高级设置
     *
     * <p>控制设置页面中高级设置区域的显示/隐藏。
     * 高级设置包括模型参数设置和 Prompt 模板配置。
     * 用户可以通过复选框控制是否显示高级设置，减少设置页面长度。
     *
     * <p>默认值: false（默认隐藏，减少页面长度）
     *
     * @since 1.4.0
     */
    public boolean showAdvancedSettings = false;

    /**
     * 是否在中英文之间添加空格
     *
     * <p>控制是否在格式化 JavaDoc 时，在中文字符和英文字符/数字之间自动添加空格。
     * 例如："这是一个User类" 会格式化为 "这是一个 User 类"。
     *
     * <p>默认值: true（默认启用，提升可读性）
     *
     * @see dev.dong4j.zeka.stack.idea.plugin.util.JavaDocFormatter
     * @since 1.4.0
     */
    public boolean addSpaceBetweenChineseAndEnglish = true;

    /**
     * 是否将中文标点符号转换为英文标点符号
     *
     * <p>控制是否在格式化 JavaDoc 时，将中文标点符号替换为对应的英文标点符号。
     * 例如："这是一个类，用于处理数据。" 会格式化为 "这是一个类, 用于处理数据."。
     *
     * <p>默认值: true（默认启用，符合 JavaDoc 规范）
     *
     * @see dev.dong4j.zeka.stack.idea.plugin.util.JavaDocFormatter
     * @since 1.4.0
     */
    public boolean replaceChinesePunctuation = true;

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

        // 初始化默认的自定义 JavaDoc 标签（仅在配置为空时）
        if (customJavaDocTags == null || customJavaDocTags.isEmpty()) {
            customJavaDocTags = new ArrayList<>();
            customJavaDocTags.add("date");
            customJavaDocTags.add("email");
        }
    }

    // ==================== 辅助方法 ====================

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
        return providerConfig != null && providerConfig.providerType.requiresApiKey();
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
     * 获取自定义 JavaDoc 标签列表（去重、去空、转小写）
     *
     * <p>对标签列表进行规范化处理：
     * <ul>
     *   <li>去除空字符串和 null 值</li>
     *   <li>去除重复标签</li>
     *   <li>转换为小写（标签不区分大小写）</li>
     *   <li>去除前后空格</li>
     * </ul>
     *
     * @return 规范化后的标签列表
     */
    @NotNull
    public List<String> getNormalizedCustomJavaDocTags() {
        if (customJavaDocTags == null) {
            return new ArrayList<>();
        }

        return customJavaDocTags.stream()
            .filter(tag -> tag != null && !tag.trim().isEmpty())
            .map(String::trim)
            .map(String::toLowerCase)
            .distinct()
            .sorted()
            .collect(Collectors.toList());
    }

    /**
     * 验证标签名称是否有效
     *
     * <p>标签名称规则：
     * <ul>
     *   <li>不能为空</li>
     *   <li>只能包含字母、数字、下划线、连字符</li>
     *   <li>不能包含空格、逗号等特殊字符</li>
     * </ul>
     *
     * @param tagName 标签名称
     * @return 如果标签名称有效返回 true
     */
    public static boolean isValidTagName(@Nullable String tagName) {
        if (tagName == null || tagName.trim().isEmpty()) {
            return false;
        }

        // 标签名称只能包含字母、数字、下划线、连字符
        return tagName.matches("^[a-zA-Z0-9_-]+$");
    }

}

