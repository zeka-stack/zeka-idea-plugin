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
    public boolean showPromptSettings = false;

    /**
     * 系统提示词。
     */
    public String systemPrompt = getDefaultSystemPrompt();

    /**
     * 工作流模板。
     */
    public String workflowTemplate = getDefaultWorkflowTemplate();

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
            
            2. **绘制时序图**：
               - 使用 Mermaid 语法绘制完整的调用时序图（sequenceDiagram）
               - 展示从调用者到目标方法，再到被调用者的完整调用流程
               - 标注关键的方法调用和参数传递
            
            3. **详细技术说明**：
               - 按照调用链的顺序，逐个解释每个环节的作用
               - 说明方法的作用、参数的含义、返回值的作用、在整个流程中的位置
               - 说明要具体、详细，不要泛泛而谈
            
            4. **总结说明**：
               - 在详细解释之后，提供一段总结性的说明
               - 总结该方法在整个系统中的定位和作用
               - 说明该方法解决的核心问题和业务价值
            
            5. **注意事项**：
               - 只基于提供的上下文信息进行分析，不要编造不存在的细节
               - 如果某些信息缺失，明确说明哪些信息无法确定
               - 使用专业但易懂的技术语言
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
            
            请按照以下格式输出：
            
            ## 调用时序图
            
            ```mermaid
            sequenceDiagram
                ...
            ```
            
            ## 技术说明
            
            ### 调用链分析
            
            #### 1. 调用者分析
            （详细说明谁调用了这个方法，调用场景和触发条件）
            
            #### 2. 目标方法核心逻辑
            （详细说明目标方法的作用、参数含义、核心处理逻辑）
            
            #### 3. 被调用方法分析
            （逐个详细说明目标方法调用的每个子方法的作用、参数、返回值及其在整个流程中的意义）
            
            #### 4. 返回流程分析
            （说明数据如何从被调用方法返回到调用者，整个调用链的返回流程）
            
            ### 总结
            
            （总结该方法在整个系统中的定位、作用、解决的核心问题和业务价值）
            """.formatted(CONTEXT_PLACEHOLDER);
    }
}

