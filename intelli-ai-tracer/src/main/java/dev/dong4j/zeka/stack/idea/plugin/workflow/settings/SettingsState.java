package dev.dong4j.zeka.stack.idea.plugin.workflow.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;

/**
 * IntelliAI Tracer 插件设置状态
 * <p>
 * 负责持久化默认 AI 提供商选择和提示词模板。
 *
 * @author dong4j
 * @version 1.0.1
 * @since 1.0.1
 */
@State(
    name = "IntelliAITracerSettingsState",
    storages = @Storage("zeka.stack.intelliai.tracer.xml")
)
public class SettingsState implements PersistentStateComponent<SettingsState> {
    /**
     * 上下文 JSON 占位符。
     */
    public static final String CONTEXT_PLACEHOLDER = "{contextJson}";

    /**
     * 选中的 AI 提供商配置。
     */
    public AIProviderConfig providerConfig;

    /**
     * 是否显示提示词设置区域。
     */
    public boolean showPromptSettings = true;

    /**
     * 系统提示词。
     */
    public String systemPrompt = getDefaultSystemPrompt();

    /**
     * 工作流模板。
     */
    public String workflowTemplate = getDefaultWorkflowTemplate();

    /**
     * 方法调用模板。
     */
    public String methodCallTemplate = getDefaultMethodCallTemplate();

    /**
     * 方法调用链模板。
     */
    public String methodCallerChainTemplate = getDefaultMethodCallerChainTemplate();

    /**
     * 类关系模板。
     */
    public String classRelationshipTemplate = getDefaultClassRelationshipTemplate();

    /**
     * 获取单例。
     *
     * @return SettingsState
     */
    public static SettingsState getInstance() {
        return ApplicationManager.getApplication().getService(SettingsState.class);
    }

    @Override
    public @Nullable SettingsState getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull SettingsState state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    /**
     * 默认系统提示词。
     *
     * @return 默认值
     */
    @NotNull
    public static String getDefaultSystemPrompt() {
        return """
            你是一名资深的系统架构师和技术分析师。
            请根据提供的方法上下文信息，深入分析该方法的业务流程，并生成一份详细的技术说明文档。
            
            要求：
            1. **调用链分析**：
               - 首先分析调用者（callers）：谁调用了这个方法，调用者的职责是什么
               - 然后分析被调用者（callees）：这个方法调用了哪些其他方法，每个被调用方法的职责是什么
               - 理解完整的调用链路，从入口到出口
               - 重点关注核心业务逻辑，忽略简单的 getter/setter 和日志记录方法
            
            2. **绘制时序图**：
               - 使用 Mermaid 语法绘制核心业务流程的调用时序图（sequenceDiagram）
               - 只展示关键的业务方法调用，过滤掉以下类型的方法：
                 * getter/setter 方法（如 getName、setName 等）
                 * 日志记录方法（如 log.info、logger.debug 等）
                 * 简单的工具方法（如 toString、equals、hashCode 等）
               - 标注关键的方法调用和参数传递
            
            3. **详细技术说明**：
               - 按照调用链的顺序，逐个解释每个核心环节的作用
               - 说明方法的业务作用、关键参数的含义、返回值的作用、在整个流程中的位置
               - 说明要具体、详细，聚焦于业务逻辑，不要泛泛而谈
            
            4. **总结说明**：
               - 在详细解释之后，提供一段总结性的说明
               - 总结该方法在整个系统中的定位和作用
               - 说明该方法解决的核心问题和业务价值
            
            5. **过滤规则**：
               - 忽略所有 getter/setter 方法（方法名以 get/set/is 开头的简单属性访问方法）
               - 忽略所有日志相关方法（包含 log、logger、debug、info、warn、error 等关键词）
               - 忽略工具方法（toString、equals、hashCode、clone 等）
               - 重点关注业务逻辑方法、数据处理方法、外部调用方法
            
            6. **注意事项**：
               - 只基于提供的上下文信息进行分析，不要编造不存在的细节
               - 如果某些信息缺失，明确说明哪些信息无法确定
               - 使用专业但易懂的技术语言
               - 如果调用链中全部都是被过滤的方法，则说明该方法主要是简单的数据访问操作
            """;
    }

    /**
     * 默认工作流模板。
     *
     * @return 默认模板
     */
    @NotNull
    public static String getDefaultWorkflowTemplate() {
        return """
            以下是方法上下文（JSON）：
            
            %s
            
            请按照以下格式输出，重点关注核心业务逻辑：
            
            ## 调用时序图
            
            ```mermaid
            sequenceDiagram
                ...
            ```
            
            （只绘制核心业务方法，过滤 getter/setter 和日志方法）
            
            ## 技术说明
            
            ### 调用链分析
            
            #### 1. 调用者分析
            （详细说明谁调用了这个方法，调用场景和触发条件，聚焦于业务调用者）
            
            #### 2. 目标方法核心逻辑
            （详细说明目标方法的业务作用、关键参数含义、核心处理逻辑）
            
            #### 3. 被调用方法分析
            （逐个详细说明目标方法调用的核心业务方法的作用、参数、返回值及其在整个流程中的意义，忽略简单的属性访问和日志记录）
            
            #### 4. 返回流程分析
            （说明业务数据如何从被调用方法返回到调用者，整个调用链的返回流程）
            
            ### 总结
            
            （总结该方法在整个系统中的定位、作用、解决的核心问题和业务价值）
            
            ### 过滤说明
            
            本分析已自动过滤以下类型的方法：
            - getter/setter 方法（get*/set*/is* 开头）
            - 日志记录方法（log/logger/debug/info/warn/error 相关）
            - 工具方法（toString/equals/hashCode 等）
            """.formatted(CONTEXT_PLACEHOLDER);
    }

    /**
     * 默认方法调用模板。
     *
     * @return 默认模板
     */
    @NotNull
    public static String getDefaultMethodCallTemplate() {
        return getDefaultWorkflowTemplate(); // 使用相同的模板
    }

    /**
     * 默认方法调用链模板。
     *
     * @return 默认模板
     */
    @NotNull
    public static String getDefaultMethodCallerChainTemplate() {
        return """
            请分析以下方法的调用链信息，生成详细的调用链分析报告。
            
            分析要求：
            1. 分析谁调用了这个方法（直接调用者和调用链）
            2. 分析这个方法的业务职责和作用
            3. 分析调用链的业务流程
            4. 识别潜在的设计问题或优化建议
            5. 重点关注核心业务逻辑，过滤掉 getter/setter 和日志方法
            
            请用 Markdown 格式输出，包含：
            
            ## 方法概述
            
            （描述目标方法的基本信息、职责和作用）
            
            ## 调用链分析
            
            ### 直接调用者
            
            （列出直接调用该方法的核心业务方法，说明调用场景和原因，忽略简单的属性访问和日志记录）
            
            ### 调用链路径
            
            ```mermaid
            graph TD
                ...
            ```
            
            （使用 Mermaid 图表展示核心业务方法的调用链路径，过滤 get/set/log 等方法）
            
            ## 业务流程说明
            
            （详细说明该方法在业务流程中的位置和作用，聚焦于业务逻辑）
            
            ## 设计分析
            
            ### 优点
            
            （分析当前设计的优点）
            
            ### 潜在问题
            
            （识别可能的设计问题或改进点）
            
            ### 优化建议
            
            （提供具体的优化建议）
            
            ## 过滤说明
            
            分析中已自动过滤以下类型的方法：
            - getter/setter 方法（get*/set*/is* 开头的属性访问方法）
            - 日志记录方法（包含 log/logger/debug/info/warn/error 等关键词）
            - 工具方法（toString/equals/hashCode 等）
            
            上下文信息：
            %s
            """.formatted(CONTEXT_PLACEHOLDER);
    }

    /**
     * 默认类关系模板。
     *
     * @return 默认模板
     */
    @NotNull
    public static String getDefaultClassRelationshipTemplate() {
        return """
            请分析以下类的关系链信息，生成详细的类关系分析报告。
            
            分析要求：
            1. 分析类的继承关系和接口实现
            2. 分析类的依赖关系和被依赖关系
            3. 分析类在系统中的职责和作用
            4. 识别设计模式的使用
            5. 提供架构优化建议
            
            请用 Markdown 格式输出，包含：
            
            ## 类概述
            
            （描述目标类的基本信息、职责和在系统中的作用）
            
            ## 继承关系图
            
            ```mermaid
            classDiagram
                ...
            ```
            
            （使用 Mermaid 类图展示继承关系）
            
            ## 依赖关系分析
            
            ### 依赖的类
            
            （列出该类依赖的其他类，按依赖类型分类）
            
            ### 被依赖情况
            
            （列出依赖该类的其他类）
            
            ## 设计模式识别
            
            （识别该类使用或参与的设计模式）
            
            ## 架构分析
            
            ### 职责分析
            
            （分析该类的职责是否单一、合理）
            
            ### 耦合度分析
            
            （分析该类与其他类的耦合程度）
            
            ### 优化建议
            
            （提供架构优化和重构建议）
            
            上下文信息：
            %s
            """.formatted(CONTEXT_PLACEHOLDER);
    }
}

