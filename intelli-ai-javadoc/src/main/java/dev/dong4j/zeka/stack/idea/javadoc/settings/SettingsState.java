package dev.dong4j.zeka.stack.idea.javadoc.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.messages.Topic;
import com.intellij.util.xmlb.XmlSerializerUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import dev.dong4j.zeka.stack.idea.javadoc.PluginContents;
import dev.dong4j.zeka.stack.idea.javadoc.component.CustomJavadocTagRegistrar;
import dev.dong4j.zeka.stack.idea.javadoc.component.JavadocFileTemplatesHandler;
import dev.dong4j.zeka.stack.idea.javadoc.task.GenerationContext;
import dev.dong4j.zeka.stack.idea.javadoc.task.TaskCollector;
import dev.dong4j.zeka.stack.idea.javadoc.task.TaskExecutor;
import dev.dong4j.zeka.stack.idea.javadoc.util.AiCodePreprocessor;
import dev.dong4j.zeka.stack.idea.javadoc.util.JavadocSingleLineFormatter;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIProviderType;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderSettings;
import dev.dong4j.zeka.stack.idea.plugin.kit.MessageFormatter;

/**
 * Javadoc AI 设置状态类
 * <p>
 * 该类用于管理 Javadoc AI 插件的各项配置参数, 包括 AI 提供商配置, 支持的语言类型,
 * 生成选项, 自定义标签配置, 提示模板等. 实现了持久化状态组件接口, 支持配置的保存和加载.
 * 通过单例模式提供全局访问入口, 用于控制 Javadoc 生成的行为和外观设置.
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email mailto:zeka.stack@gmail.com
 * @date 2025.11.30
 * @since 1.0.0
 */
@State(
    name = "JavaDocAISettings",
    storages = @Storage("zeka.stack.intelliai.javadoc.xml")
)
public class SettingsState implements PersistentStateComponent<SettingsState> {

    /**
     * 默认 AI 提供商类型
     * <p>
     * 插件使用的默认供应商, 从全局可用供应商列表中选取.
     * 全局供应商配置在 Settings → Tools → IntelliAI Engine 中管理.
     *
     * @see AIProviderSettings
     */
    public AIProviderConfig providerConfig;

    // ==================== 功能配置 ====================

    /**
     * 支持的编程语言
     *
     * <p> 插件支持的编程语言集合.
     * 当前支持 Java 和 Kotlin, 未来可扩展到 Python 等其他语言.
     *
     * <p> 设计考虑:
     * <ul>
     * <li> 使用 Set 集合避免重复 </li>
     * <li> 默认包含 "java"</li>
     * <li> 支持动态添加新语言 </li>
     * </ul>
     *
     * <p> 默认值: {"java"}
     */
    public Set<String> supportedLanguages = new HashSet<>() {{
        add(PluginContents.JAVA);
    }};

    /**
     * 是否为类生成文档
     *
     * <p> 控制是否为类元素生成 Javadoc 文档.
     * 用户可在设置界面中切换.
     *
     * <p> 默认值: true
     */
    public boolean generateForClass = true;

    /**
     * 是否为方法生成文档
     *
     * <p> 控制是否为方法元素生成 Javadoc 文档.
     * 包括普通方法和测试方法.
     *
     * <p> 默认值: true
     *
     * @see TaskCollector#collectFromFile(PsiFile)
     */
    public boolean generateForMethod = true;

    /**
     * 是否为字段生成文档
     *
     * <p>控制是否为字段 (成员变量) 元素生成 Javadoc 文档.
     * 默认关闭, 因为字段通常较简单.
     *
     * <p>默认值: false
     *
     * @see TaskCollector#collectFromFile(PsiFile)
     */
    public boolean generateForField = true;

    /**
     * 是否覆盖已有注释
     *
     * <p> 控制是否覆盖已经存在的文档注释.
     * - false(默认): 跳过已有注释的元素, 只对没有注释的元素生成文档
     * - true: 覆盖已有注释, 无论是否已有注释都会生成新的文档
     *
     * <p> 默认值: false(跳过已有注释)
     *
     * @see TaskCollector#shouldGenerateForElement(PsiElement)
     */
    public boolean overrideExisting = false;

    /**
     * 覆写模式
     *
     * <p>控制当覆盖已有注释时使用的模式。
     * - FIX：仅修复错误注释（保留原有注释，只修复其中的错误）
     * - REPLACE：删除原注释并重新生成（完全替换原有注释）
     *
     * <p>默认值: REPLACE（删除原注释并重新生成）
     *
     * @since 2.7.0
     */
    public OverrideMode overrideMode = OverrideMode.REPLACE;

    /**
     * 修复错误 Javadoc 的提示词模板
     *
     * <p> 当覆写模式为 "fix" 时, 使用此提示词模板来修复已有注释中的错误.
     * 该提示词会指导 AI 分析现有注释, 识别并修复其中的错误, 同时保留正确的部分.
     *
     * <p> 默认值: getDefaultFixJavadocPromptTemplate()
     *
     * @see #getDefaultFixJavadocPromptTemplate()
     * @since 2.7.0
     */
    public String fixJavadocPromptTemplate = getDefaultFixJavadocPromptTemplate();

    /**
     * 是否为文档生成提供类级别上下文信息
     *
     * <p>控制是否在生成注释时, 额外携带当前元素所属类 (或 Kotlin 类 / 对象) 的前若干行代码作为上下文.
     * 当开启时,{@link TaskCollector}会为每个任务构建
     * {@link GenerationContext}, 在提示词中附带类级别代码片段,
     * 以提升字段 / 方法注释的语境理解能力.
     *
     * <p>默认值: false(默认不传递额外上下文, 以减少 token 消耗)
     *
     * @since 2.5.1
     */
    public boolean enableGenerationContext = false;

    /**
     * 是否启用语义上下文（Beta）
     *
     * <p>控制是否为类级别的 Javadoc 生成添加语义上下文信息。
     * 当开启时, 会通过 PSI 分析类的架构位置、职责、使用场景、依赖关系等信息,
     * 将这些语义信息作为上下文传递给 AI, 以提升类注释的准确性和针对性.
     *
     * <p>默认值: false(默认不启用语义上下文分析)
     *
     * @since 2.8.0
     */
    public boolean enableSemanticContext = false;

    /**
     * 是否启用代码压缩以减少 token 使用量
     *
     * <p>控制是否为代码元素进行压缩处理以减少传递给 AI 的 token 数量。
     *
     * <p>对于类级别的代码，压缩包括：
     * <ul>
     *   <li>删除多余的空行和空白字符</li>
     *   <li>删除单行注释（// 注释）</li>
     *   <li>保留 Javadoc 注释（/** 注释）</li>
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
     * @see AiCodePreprocessor
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


    // ==================== Javadoc 标签配置 ====================

    /**
     * 自定义 Javadoc 标签列表
     *
     * <p>用户可以在设置页面配置自定义的 Javadoc 标签。
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
     * <p>默认值: 空列表
     * <p>用户可以在设置页面手动添加标签，例如 "date" 和 "email"。
     *
     * <p>示例：
     * <pre>
     * customJavaDocTags = [
     *   CustomJavaDocTag("date", "yyyy.MM.dd"),
     *   CustomJavaDocTag("email", "mailto:zeka.stack@gmail.com"),
     *   CustomJavaDocTag("custom", "default value")
     * ]
     * </pre>
     *
     * @see CustomJavadocTagRegistrar
     * @since 2.0.0
     */
    public List<CustomJavadocTag> customJavadocTags = new ArrayList<>() {
        {
            add(new CustomJavadocTag("author", "zeka.stack.team"));
            add(new CustomJavadocTag("date", "yyyy.MM.dd"));
            add(new CustomJavadocTag("email", "mailto:zeka.stack@gmail.com"));
        }
    };

    /**
     * 是否允许删除 Javadoc
     *
     * <p>控制是否允许删除已存在的 Javadoc 注释。
     * 启用后，在生成或更新 Javadoc 时可以删除现有的注释。
     *
     * <p>默认值: false（默认不允许删除）
     *
     * @since 2.6.0
     */
    public boolean allowDeleteJavadoc = false;

    /**
     * 是否在提交时执行 Javadoc 缺失检查
     *
     * <p>控制提交消息检查中是否启用 Javadoc 缺失检查。
     * 启用后，在提交前会对已勾选的变更文件进行缺失检测并给出警告提示。
     *
     * <p>默认值: true（默认启用）
     *
     * @since 2.8.0
     */
    public boolean enableCommitJavadocCheck = true;

    /**
     * 设置变更监听器
     * <p>
     * 用于在设置发生变化时广播通知，便于同步到其他 UI（如提交前检查面板）。
     */
    public interface SettingsChangeListener {
        /** 设置变更通知主题, 用于在设置变化时广播通知, 便于同步到其他 UI 组件. */
        Topic<SettingsChangeListener> TOPIC =
            Topic.create("JavadocSettingsChanged", SettingsChangeListener.class);

        /**
         * 当设置发生变化时被调用, 用于广播通知
         * <p> 在设置状态更新后, 通过此方法通知所有注册的监听器, 便于同步到其他 UI 组件 (如提交前检查面板).
         *
         * @param state 当前的设置状态对象, 不能为空
         */
        void settingsChanged(@NotNull SettingsState state);
    }

    /**
     * 发送设置变更通知
     */
    public void notifySettingsChanged() {
        ApplicationManager.getApplication()
            .getMessageBus()
            .syncPublisher(SettingsChangeListener.TOPIC)
            .settingsChanged(this);
    }

    /**
     * 是否显示自定义 Javadoc 标签配置面板
     *
     * <p>控制设置页面中自定义 Javadoc 标签配置表格的显示/隐藏。
     * 用户可以通过复选框控制是否显示标签管理表格，减少设置页面长度。
     *
     * <p>默认值: false（默认隐藏，减少页面长度）
     *
     * @since 1.4.0
     */
    public boolean showCustomJavaDocTags = false;

    /**
     * 是否启用类 Javadoc 模板
     *
     * <p>控制是否为新建的 Java 类自动添加 Javadoc 注释模板。
     * 启用后，在创建新的 Java 类时会自动添加包含作者、日期、邮箱等信息的 Javadoc 注释。
     *
     * <p>默认值: false（默认禁用）
     *
     * @see JavadocFileTemplatesHandler
     * @since 2.5.0
     */
    public boolean enableClassJavaDocTemplate = false;

    /**
     * 类 Javadoc 模板内容
     *
     * <p>用于生成 Java 类的 Javadoc 注释模板。
     * 支持使用变量：
     * <ul>
     *   <li>${description} - 类描述（由用户输入）</li>
     *   <li>${author} - 作者（从自定义标签配置中的 author 标签获取）</li>
     *   <li>${email} - 邮箱（从自定义标签配置中的 email 标签获取）</li>
     *   <li>${YEAR}, ${MONTH}, ${DAY}, ${HOUR}, ${MINUTE} - 日期时间变量</li>
     * </ul>
     *
     * <p>注意：${author} 和 ${email} 会在应用配置时自动替换为自定义标签中的实际值。
     *
     * <p>默认模板：
     * <pre>{@code
     * /**
     *  * ${description}
     *  *
     *  * @author ${author}
     *  * @version 1.0.0
     *  * @email ${email}
     *  * @date ${YEAR}.${MONTH}.${DAY} ${HOUR}:${MINUTE}
     *  * @since x.x.x
     * }
     * </pre>
     *
     * @see #customJavadocTags
     * @see JavadocFileTemplatesHandler
     * @since 2.5.0
     */
    public String classJavaDocTemplate = """
        /**
         * ${description}
         *
         * @author ${author}
         * @version 1.0.0
         * @email ${email}
         * @date ${YEAR}.${MONTH}.${DAY} ${HOUR}:${MINUTE}
         * @since x.x.x
         */
        """;

    /** 是否显示提示模板 */
    public boolean showPromptTemplates = false;

    /**
     * 是否显示"生成 Javadoc"的 Code Vision 提示
     *
     * <p>控制是否在代码上方显示"Generate Javadoc"的可点击提示链接。
     * 类似于 IDEA 中的"x usages"提示，会在没有 Javadoc 的代码元素上显示可点击的链接。
     *
     * <p>默认值: true（默认启用）
     *
     * @since 2.6.0
     */
    public boolean showGenerateJavadocHint = true;

    /**
     * 是否压缩单行 Javadoc 注释
     *
     * <p>控制是否将只有一行内容的 Javadoc 注释压缩为单行格式。
     * 例如：
     * <pre>{@code
     * /**
     *  * One-line comment
     *  /
     * }
     * </pre>
     * 会压缩为：
     * <pre>{@code
     * /** One-line comment /
     * }
     * </pre>
     *
     * <p>默认值: true（默认启用）
     *
     * @see JavadocSingleLineFormatter
     * @since 1.5.0
     */
    public boolean compressSingleLineJavaDoc = true;

    /**
     * 是否在中英文之间添加空格
     *
     * <p>控制是否在格式化 Javadoc 时，在中文字符和英文字符/数字之间自动添加空格。
     * 例如："这是一个User类" 会格式化为 "这是一个 User 类"。
     *
     * <p>默认值: true（默认启用，提升可读性）
     *
     * @see MessageFormatter
     * @since 1.4.0
     */
    public boolean addSpaceBetweenChineseAndEnglish = true;

    /**
     * 是否将中文标点符号转换为英文标点符号
     *
     * <p>控制是否在格式化 Javadoc 时，将中文标点符号替换为对应的英文标点符号。
     * 例如："这是一个类，用于处理数据。" 会格式化为 "这是一个类, 用于处理数据."。
     *
     * <p>默认值: true（默认启用，符合 Javadoc 规范）
     *
     * @see MessageFormatter
     * @since 1.4.0
     */
    public boolean replaceChinesePunctuation = true;

    /**
     * 是否在保存时自动生成注释
     *
     * <p>控制是否在文件保存时自动为缺少 Javadoc 的元素生成注释。
     * 启用后，每次保存文件时会自动检查并生成缺失的文档注释。
     *
     * <p>默认值: false（默认关闭，避免频繁生成影响性能）
     *
     * @since 2.8.0
     */
    public boolean generateOnSave = false;

    // ==================== 性能模式配置 ====================

    /**
     * 是否启用性能模式
     *
     * <p>控制是否使用多个已验证的 AI 服务提供商并行处理任务，以提高批量处理速度。
     * 启用后，批量生成 Javadoc 时会同时使用多个服务商，显著提高处理速度。
     *
     * <p>使用要求：
     * <ul>
     *   <li>需要先配置多个可用的服务商（通过"测试连接"验证）</li>
     *   <li>任务数量大于 5 个时才会启用并行处理</li>
     *   <li>会增加 API 调用次数，可能增加成本</li>
     * </ul>
     *
     * <p>默认值: false（默认关闭）
     *
     * @see TaskExecutor#processTasks(com.intellij.openapi.project.Project, List)
     * @since 2.0.0
     */
    public boolean performanceMode = false;

    /**
     * 是否显示任务统计信息
     *
     * <p>控制是否在任务完成后显示统计信息对话框。
     * 统计信息包括每个服务商的处理结果统计，帮助了解不同服务商的表现。
     *
     * <p>使用要求：
     * <ul>
     *   <li>只有启用"性能模式"后才有意义</li>
     *   <li>仅在并行处理任务时显示统计信息</li>
     * </ul>
     *
     * <p>默认值: false（默认关闭）
     *
     * @since 2.0.0
     */
    public boolean showProviderStatistics = false;

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
     *   <li>建立响应格式要求（中文 Javadoc）</li>
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
     *   <li>支持通过 ${language} 变量指定注释语言</li>
     *   <li>包含完整的 Javadoc/KDoc 格式</li>
     *   <li>提供 Java 和 Kotlin 两种示例</li>
     *   <li>使用 %s 作为代码占位符</li>
     *   <li>支持代码示例格式：使用 &lt;pre&gt;{@code ... }&lt;/pre&gt; 包裹代码片段</li>
     * </ul>
     *
     * @return 默认的类 Prompt 模板
     */
    @NotNull
    public static String getDefaultClassPromptTemplate() {
        return """
            请为以下类/接口/枚举/对象生成类级别的文档注释（**${language}**）。
            请自动识别代码语言（Java 或 Kotlin），如果是 Java 代码生成 Javadoc 格式，如果是 Kotlin 代码生成 KDoc 格式。

            # 重要说明
            - 下面的代码可能已经包含旧的文档注释，请忽略或改进它
            - 只返回类/接口/枚举/对象级别的文档注释，不要返回方法/函数、字段/属性等其他元素的注释
            - 不要返回代码本身，只返回注释
            - 不要使用任何 markdown 代码块标记（如 ```java 或 ```kotlin）

            # 格式要求
            1. **必须使用 ${language} 编写注释内容**，这是强制要求，不能使用其他语言
            2. 必须包含完整的文档注释格式，包括开始标记 /** 和结束标记 */
            3. 注释要准确描述类/接口/枚举/对象的职责、主要功能和使用场景
            4. Java: 如果是工具类，需要说明主要提供的功能
            5. Kotlin: 如果是工具对象（object），需要说明主要提供的功能；如果是数据类（data class），需要说明数据类的用途和主要属性
            6. 如果是接口，需要说明接口的用途和实现要求
            7. 如果是枚举，需要说明枚举的用途和各个值的含义
            8. 如果有特殊的设计模式，需要说明
            9. **代码格式规则（重要）**：如果注释中需要包含代码片段，必须使用以下格式：
               <pre>{@code
               代码片段
               }</pre>
               注意：代码片段必须放在 {@code} 标签内，并且外层使用 <pre> 标签包裹
            10. **URL 格式规则（重要）**：如果注释中存在 URL，必须使用以下格式：
                <a href="https://example.com">https://example.com</a>
               注意：URL 必须使用 <a href="URL">URL</a> 格式，href 和显示文本都使用完整的 URL
            11. 添加 @author、@version、@email、@date、@since 标签且保存顺序
                - 如果已存在 @author 且添加了作者信息则直接使用, 否则使用 ${author} 作为作者
                - 如果已存在 @version 则保存不变, 否则使用 1.0.0 作为版本号
                - 如果已存在 @email 则保持不变, 否则使用 ${email} 作为邮箱
                - 如果已存在 @date 需要格式化为 yyyy.mm.dd, 否则使用 ${date} 作为时间戳
                - 如果已存在 @since 则保存不变, 否则使用 ${since} 作为版本号
            12. **可参考语义上下文信息**, 确保注释准确反映类在系统中的实际角色

            # 示例说明
            **重要：以下示例仅用于展示格式，实际输出必须使用 ${language} 编写注释内容。**

            示例1 - Java 代码：
            输入代码：
            public class UserService {
                public User findById(int id) { ... }
                public void save(User user) { ... }
            }

            输出注释（中文示例）：
            /**
             * 用户服务类
             * <p>提供用户相关的业务逻辑处理，包括用户的查询、创建、更新和删除等操作
             *
             * @author dong4j
             * @version 1.0.0
             * @email "mailto:dong4j@gmail.com"
             * @date 2025.10.24
             * @since 1.0.0
             */

            示例2 - Kotlin 代码：
            输入代码：
            class UserService {
                fun findById(id: Int): User? { ... }
                fun save(user: User) { ... }
            }

            输出注释（中文示例）：
            /**
             * 用户服务类
             * <p>提供用户相关的业务逻辑处理，包括用户的查询、创建、更新和删除等操作
             *
             * @author dong4j
             * @version 1.0.0
             * @email "mailto:dong4j@gmail.com"
             * @date 2025.10.24
             * @since 1.0.0
             */

            ## 代码片段:
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
     *   <li>支持通过 ${language} 变量指定注释语言（中文或英文）</li>
     *   <li>强调 @param、@return、@throws 标签</li>
     *   <li>提供 Java 和 Kotlin 两种示例</li>
     *   <li>使用 %s 作为代码占位符</li>
     *   <li>支持代码示例格式：使用 &lt;pre&gt;{@code ... }&lt;/pre&gt; 包裹代码片段</li>
     * </ul>
     *
     * @return 默认的方法 Prompt 模板
     */
    @NotNull
    public static String getDefaultMethodPromptTemplate() {
        return """
            请为以下方法/函数生成文档注释（**${language}**）。
            请自动识别代码语言（Java 或 Kotlin），如果是 Java 代码生成 Javadoc 格式，如果是 Kotlin 代码生成 KDoc 格式。

            # 重要说明
            - 下面的代码可能已经包含旧的文档注释，请忽略或改进它
            - 只返回方法/函数级别的文档注释，不要返回类、字段/属性等其他元素的注释
            - 不要返回代码本身，只返回注释
            - 不要使用任何 markdown 代码块标记（如 ```java 或 ```kotlin）

            # 格式要求
            1. **必须使用 ${language} 编写注释内容**，这是强制要求，不能使用其他语言
            2. 必须包含完整的文档注释格式，包括开始标记 /** 和结束标记 */
            3. 注释要准确描述方法/函数的功能、参数、返回值、异常
            4. 如果有参数, 必须包含 @param 标签
            5. 如果有返回值（Java 方法或 Kotlin 非 Unit 函数），必须包含 @return 标签
            6. 如果有异常抛出，使用 @throws 标签
            7. 不要添加不存在的参数,返回值和异常的注释标签
            8. 可以使用 @since, @Deprecated 等标签
            9. Kotlin: 注意可空类型（如 String?）和默认参数
            10. **代码格式规则（重要）**：如果注释中需要包含代码片段，必须使用以下格式：
                <pre>{@code
                代码片段
                }</pre>
                注意：代码片段必须放在 {@code} 标签内，并且外层使用 <pre> 标签包裹
            11. **URL 格式规则（重要）**：如果注释中存在 URL，必须使用以下格式：
                <a href="https://example.com">https://example.com</a>
                注意：URL 必须使用 <a href="URL">URL</a> 格式，href 和显示文本都使用完整的 URL

            # 示例说明
            **重要：以下示例仅用于展示格式，实际输出必须使用 ${language} 编写注释内容。**

            示例1 - Java 代码：
            输入代码：
            public String getUserName(int userId) throws UserNotFoundException {
                return userService.findById(userId).getName();
            }

            输出注释（中文示例）：
            /**
             * 根据用户ID获取用户名称
             * <p>通过用户ID查找用户并返回用户名称
             *
             * @param userId 用户ID
             * @return 用户名称
             * @throws UserNotFoundException 当用户不存在时抛出
             */

            示例2 - Kotlin 代码：
            输入代码：
            fun getUserName(userId: Int): String? {
                return userService.findById(userId)?.name
            }

            输出注释（中文示例）：
            /**
             * 根据用户ID获取用户名称
             * <p>通过用户ID查找用户并返回用户名称，如果用户不存在则返回 null
             *
             * @param userId 用户ID
             * @return 用户名称，如果用户不存在则返回 null
             */

            ## 代码片段:
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
     *   <li>提供 Java 和 Kotlin 两种示例</li>
     *   <li>使用 %s 作为代码占位符</li>
     * </ul>
     *
     * @return 默认的字段 Prompt 模板
     */
    @NotNull
    public static String getDefaultFieldPromptTemplate() {
        return """
            请为以下字段/属性生成文档注释（**${language}**）。
            请自动识别代码语言（Java 或 Kotlin），如果是 Java 代码生成 Javadoc 格式，如果是 Kotlin 代码生成 KDoc 格式。

            # 重要说明
            - 下面的代码可能已经包含旧的文档注释，请忽略或改进它
            - 只返回字段/属性级别的文档注释，不要返回类、方法/函数等其他元素的注释
            - 不要返回代码本身，只返回注释
            - 不要使用任何 markdown 代码块标记（如 ```java 或 ```kotlin）

            # 格式要求
            1. **必须使用 ${language} 编写注释内容**，这是强制要求，不能使用其他语言
            2. 必须返回完整的文档注释格式，包括开始标记 /** 和结束标记 */
            3. 注释要准确描述字段/属性的用途和含义
            4. **格式规则（重要）**：
               - 如果字段/属性说明简单（不超过 80 个字符，没有 @tag 标签），必须使用单行格式：/** 字段/属性说明 */
               - 如果字段/属性说明复杂（包含多个信息点、有 @tag 标签、或超过 80 个字符），使用多行格式
            5. Kotlin: 注意属性的可空类型（如 String?）和可变性（var/val）
            6. **URL 格式规则（重要）**：如果注释中存在 URL，必须使用以下格式：
               <a href="https://example.com">https://example.com</a>
               注意：URL 必须使用 <a href="URL">URL</a> 格式，href 和显示文本都使用完整的 URL

            # 示例说明
            **重要：以下示例仅用于展示格式，实际输出必须使用 ${language} 编写注释内容。**

            示例1 - Java 简单字段：
            输入：private String username;
            输出：/** 用户名 */

            示例2 - Kotlin 简单属性：
            输入：private val username: String
            输出：/** 用户名 */

            示例3 - 带旧注释的字段/属性：
            Java: 输入：/** 旧注释 */ private String tokenValue;
            Kotlin: 输入：/** 旧注释 */ private var tokenValue: String?
            输出：/** AccessToken 值 */

            示例4 - 复杂字段/属性：
            Java: 输入：private UserConfig config;
            Kotlin: 输入：private val config: UserConfig
            输出（中文示例）：
            /**
             * 用户配置信息
             * <p>包含用户偏好设置、主题配置等
             *
             * @see UserConfig
             */

            ## 代码片段:
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
     *   <li>提供 Java 和 Kotlin 两种示例</li>
     *   <li>使用 %s 作为代码占位符</li>
     * </ul>
     *
     * @return 默认的测试方法 Prompt 模板
     */
    @NotNull
    public static String getDefaultTestPromptTemplate() {
        return """
            请为以下测试方法/函数生成文档注释（**${language}**）。
            请自动识别代码语言（Java 或 Kotlin），如果是 Java 代码生成 Javadoc 格式，如果是 Kotlin 代码生成 KDoc 格式。

            # 重要说明
            - 下面的代码可能已经包含旧的文档注释，请忽略或改进它
            - 只返回测试方法/函数级别的文档注释，不要返回类、字段/属性等其他元素的注释
            - 不要返回代码本身，只返回注释
            - 不要使用任何 markdown 代码块标记（如 ```java 或 ```kotlin）

            # 格式要求
            1. **必须使用 ${language} 编写注释内容**，这是强制要求，不能使用其他语言
            2. 必须包含完整的文档注释格式，包括开始标记 /** 和结束标记 */
            3. 注释应描述：测试目标、测试场景、预期结果
            4. 如果代码中有 @link 引用，请在注释中使用 {@link ClassName#methodName} 格式
            5. 如果运行单元测试需要特殊的场景, 尽量添加上说明
            6. **URL 格式规则（重要）**：如果注释中存在 URL，必须使用以下格式：
               <a href="https://example.com">https://example.com</a>
               注意：URL 必须使用 <a href="URL">URL</a> 格式，href 和显示文本都使用完整的 URL

            # 示例说明
            **重要：以下示例仅用于展示格式，实际输出必须使用 ${language} 编写注释内容。**

            示例1 - Java 代码：
            输入代码：
            @Test
            public void testGetUserName_whenUserExists_shouldReturnName() {
                User user = new User(1, "John");
                when(userService.findById(1)).thenReturn(user);
                assertEquals("John", service.getUserName(1));
            }

            输出注释（中文示例）：
            /**
             * 测试获取用户名称功能
             * <p>
             * 测试场景：当用户存在时
             * 预期结果：应返回正确的用户名称
             */

            示例2 - Kotlin 代码：
            输入代码：
            @Test
            fun testGetUserName_whenUserExists_shouldReturnName() {
                val user = User(1, "John")
                `when`(userService.findById(1)).thenReturn(user)
                assertEquals("John", service.getUserName(1))
            }

            输出注释（中文示例）：
            /**
             * 测试获取用户名称功能
             * <p>
             * 测试场景：当用户存在时
             * 预期结果：应返回正确的用户名称
             */

            ## 代码片段:
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
     *   <li>设定 AI 的专业角色（Java/Kotlin 开发工程师）</li>
     *   <li>建立响应格式要求（中文 Javadoc/KDoc）</li>
     *   <li>定义输出规范（只返回注释，不返回代码）</li>
     *   <li>确保一致性和专业性</li>
     * </ul>
     *
     * @return 默认的系统提示词模板
     */
    @NotNull
    public static String getDefaultSystemPromptTemplate() {
        return """
            你是一个专业的 Java/Kotlin 开发工程师，专门负责为代码生成高质量的文档注释,
            精通 Java 和 Kotlin 编程语言，以及 Javadoc 和 KDoc 规范，能够准确理解代码逻辑并生成清晰、准确的 **${language}** 注释。
            现在的任务是分析用户提供的代码片段，自动识别代码语言（Java 或 Kotlin），并生成符合相应标准的注释。

            重要要求：
            - 自动识别代码语言：如果是 Java 代码，生成 Javadoc 格式；如果是 Kotlin 代码，生成 KDoc 格式
            - 只返回文档注释，不要返回代码本身
            - 不要使用任何 markdown 代码块标记（如 ```java 或 ```kotlin）
            - Javadoc 和 KDoc 都使用相同的注释格式（/** */）和标签格式（@param, @return, @throws 等）
            - 注释内容要准确描述代码的功能和用途
            - **语言要求（强制）：必须使用 ${language} 编写所有注释内容，这是绝对要求，不能使用其他语言**
              * 无论示例使用什么语言，都必须严格按照 **${language}** 的要求输出
            """;
    }

    /**
     * 获取默认的修复错误 Javadoc 提示词模板
     *
     * <p>返回用于修复已有 Javadoc 注释中错误的默认提示词模板。
     * 该提示词会指导 AI 分析现有注释，识别并修复其中的错误，同时保留正确的部分。
     *
     * <p>模板特点:
     * <ul>
     *   <li>要求分析现有注释中的错误</li>
     *   <li>保留正确的注释内容</li>
     *   <li>修复错误的描述、参数、返回值等信息</li>
     *   <li>使用 %s 作为代码占位符</li>
     * </ul>
     *
     * @return 默认的修复错误 Javadoc 提示词模板
     * @since 2.7.0
     */
    @NotNull
    public static String getDefaultFixJavadocPromptTemplate() {
        return """
            请分析以下代码元素的现有 Javadoc/KDoc 注释，识别并修复其中的错误，同时保留正确的部分。
            请自动识别代码语言（Java 或 Kotlin），如果是 Java 代码生成 Javadoc 格式，如果是 Kotlin 代码生成 KDoc 格式。

            # 重要说明
            - 下面的代码已经包含现有的文档注释，请仔细分析这些注释
            - 只返回修复后的文档注释，不要返回代码本身
            - 不要使用任何 markdown 代码块标记（如 ```java 或 ```kotlin）
            - 保留原有注释中正确的部分，只修复错误的部分

            # 修复要求
            1. 重要规则: **检查注释语言是否为（${language}），如果不一致必须修改为 ${language}**
            2. 检查注释描述是否准确反映代码的功能, 如果原有注释与代码完全匹配，只需返回原注释（不做修改）
            3. 检查 @param 标签是否与方法的实际参数匹配, 如果参数不存在, 需要删除存在的 @param 标签
            4. 检查 @return 标签是否与方法的实际返回值匹配, 如果返回 void, 需要删除存在的 @return 标签
            5. 检查 @throws 标签是否与方法的实际异常匹配, 如果方法签名没有异常申明, 需要删除存在的 @throws 标签
            6. 检查注释格式是否符合 Javadoc/KDoc 规范

            # 格式要求
            1. **必须使用 ${language} 编写注释内容**，这是强制要求，不能使用其他语言
            2. 必须包含完整的文档注释格式，包括开始标记 /** 和结束标记 */
            3. 注释要准确描述代码的功能、参数、返回值、异常
            4. 如果有参数, 必须包含 @param 标签
            5. 如果有返回值（Java 方法或 Kotlin 非 Unit 函数），必须包含 @return 标签
            6. 如果有异常抛出，使用 @throws 标签
            7. 不要添加不存在的参数、返回值和异常的注释标签
            8. **URL 格式规则（重要）**：如果注释中存在 URL，必须使用以下格式：
               <a href="https://example.com">https://example.com</a>
               注意：URL 必须使用 <a href="URL">URL</a> 格式，href 和显示文本都使用完整的 URL

            # 示例说明
            **重要：以下示例仅用于展示格式，实际输出必须使用 ${language} 编写注释内容。**

            示例1 - 修复错误的参数描述
            输入代码：
            /**
             * 根据用户ID获取用户名称
             * @param userId 用户名（错误：应该是用户ID）
             * @return 用户名称
             */
            public String getUserName(int userId) { ... }

            输出注释（中文示例）：
            /**
             * 根据用户ID获取用户名称
             * @param userId 用户ID
             * @return 用户名称
             */

            示例2 - 修复缺失的返回值描述：
            输入代码：
            /**
             * 保存用户信息
             * @param user 用户对象
             */
            public boolean saveUser(User user) { ... }

            输出注释（中文示例）：
            /**
             * 保存用户信息
             * @param user 用户对象
             * @return 保存是否成功
             */

            ## 代码片段
            %s
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
     * 通过复制传入的 SettingsState 对象的状态数据到当前对象中。
     *
     * @param state 要加载的状态对象
     */
    @Override
    public void loadState(@NotNull SettingsState state) {
        XmlSerializerUtil.copyBean(state, this);

        // 确保 overrideMode 不为 null（兼容旧版本字符串类型配置）
        if (this.overrideMode == null) {
            this.overrideMode = OverrideMode.REPLACE;
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
     * 获取自定义 Javadoc 标签列表（去重、去空、转小写）
     *
     * <p>对标签列表进行规范化处理：
     * <ul>
     *   <li>去除 null 值</li>
     *   <li>去除空标签名称</li>
     *   <li>去除重复标签（基于标签名称，不区分大小写）</li>
     *   <li>转换为小写（标签不区分大小写）</li>
     *   <li>去除前后空格</li>
     * </ul>
     *
     * @return 规范化后的标签名称列表
     */
    @NotNull
    public List<String> getNormalizedCustomJavaDocTags() {
        if (customJavadocTags == null) {
            return new ArrayList<>();
        }

        return customJavadocTags.stream()
            .filter(tag -> tag != null && !tag.tagName.trim().isEmpty())
            .map(tag -> tag.tagName.trim().toLowerCase())
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
