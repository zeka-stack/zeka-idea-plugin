package dev.dong4j.zeka.stack.idea.plugin.changelog.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderSettings;

/**
 * 插件设置状态管理
 * 使用 @State 注解自动持久化配置
 */
@State(
        name = "ChangelogPluginSettings",
        storages = @Storage("zeka.stack.intelliai.changelog.xml")
)
public class SettingsState implements PersistentStateComponent<SettingsState> {

    /**
     * 默认 AI 提供商类型
     * <p>
     * 插件使用的默认供应商，从全局可用供应商列表中选取。
     * 全局供应商配置在 Settings → Tools → IntelliAI Engine 中管理。
     *
     * @see AIProviderSettings
     */
    public AIProviderConfig providerConfig;

    /**
     * 是否显示高级设置
     * <p>
     * 控制设置页面中高级设置区域的显示/隐藏。
     * 高级设置包括 Prompt 模板配置。
     * 用户可以通过复选框控制是否显示高级设置，减少设置页面长度。
     *
     * <p>默认值: false（默认隐藏，减少页面长度）
     */
    public boolean showAdvancedSettings = false;

    // 提示词配置
    /**
     * 系统提示词模板
     *
     * <p>用于设定 AI 角色和行为准则的系统提示词。
     * 这个提示词会作为 system 消息发送给 AI 服务，
     * 用于建立 AI 的基本角色和响应风格。
     *
     * <p>默认值: getDefaultSystemPrompt()
     *
     * @see #getDefaultSystemPrompt()
     */
    public String systemPrompt = getDefaultSystemPrompt();

    /**
     * 变更日志模板
     *
     * <p>用于生成变更日志的提示词模板。
     * 使用 {version}、{date}、{commits} 作为占位符。
     *
     * <p>默认值: getDefaultChangelogTemplate()
     *
     * @see #getDefaultChangelogTemplate()
     */
    public String changelogTemplate = getDefaultChangelogTemplate();

    /**
     * 日报模板
     *
     * <p>用于生成工作日报的提示词模板。
     * 使用 {date}、{commits} 作为占位符。
     *
     * <p>默认值: getDefaultDailyReportTemplate()
     *
     * @see #getDefaultDailyReportTemplate()
     */
    public String dailyReportTemplate = getDefaultDailyReportTemplate();

    /**
     * 周报模板
     *
     * <p>用于生成工作周报的提示词模板。
     * 使用 {dateRange}、{commits} 作为占位符。
     *
     * <p>默认值: getDefaultWeeklyReportTemplate()
     *
     * @see #getDefaultWeeklyReportTemplate()
     */
    public String weeklyReportTemplate = getDefaultWeeklyReportTemplate();

    /**
     * 提交记录模板
     *
     * <p>用于生成提交记录的提示词模板。
     * 使用 {codeDiffs} 作为占位符，会被替换为格式化的代码变更信息。
     *
     * <p>默认值: getDefaultCommitMessageTemplate()
     *
     * @see #getDefaultCommitMessageTemplate()
     */
    public String commitMessageTemplate = getDefaultCommitMessageTemplate();

    /**
     * 获取 SettingsState 的单例实例
     * <p>
     * 通过 ApplicationManager 获取当前应用的 SettingsState 服务实例
     *
     * @return SettingsState 的实例
     * @since 1.0
     */
    public static SettingsState getInstance() {
        return ApplicationManager.getApplication().getService(SettingsState.class);
    }

    /**
     * 获取当前设置状态
     * <p>
     * 返回当前对象作为设置状态, 用于支持链式调用或状态传递
     *
     * @return 当前设置状态, 可能为 null
     */
    @Override
    public @Nullable SettingsState getState() {
        return this;
    }

    /**
     * 加载设置状态
     * <p>
     * 将传入的 {@link SettingsState} 对象的属性复制到当前实例中.
     *
     * @param state 要加载的设置状态, 不能为空
     */
    @Override
    public void loadState(@NotNull SettingsState state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    // ==================== 默认提示词模板方法 ====================

    /**
     * 获取默认的系统提示词
     *
     * <p>返回用于设定 AI 角色和行为准则的默认系统提示词。
     *
     * @return 默认的系统提示词
     */
    @NotNull
    public static String getDefaultSystemPrompt() {
        return """
            你是一位经验丰富的软件发布经理和技术文档编写者。
            你的目标是根据 Git 提交记录为软件项目生成清晰、结构化、简洁的变更日志。
            你总是输出格式良好的 Markdown，包含一致的章节结构。
            
            重要要求：
            - 输出的 markdown 内容不要使用 markdown 代码块包裹（如 ```markdown）
            - 直接输出 markdown 格式的内容，不要添加任何代码块标记
            """;
    }

    /**
     * 获取默认的变更日志模板
     *
     * <p>返回用于生成变更日志的默认提示词模板。
     *
     * @return 默认的变更日志模板
     */
    @NotNull
    public static String getDefaultChangelogTemplate() {
        return """
            请根据以下 Git 提交记录生成发布变更日志。
            
            要求：
            1. 将提交记录分类到以下类别：
               - ✨ 新功能
               - 🐛 问题修复
               - ♻️ 代码重构
               - 📝 文档更新
               - 🔧 其他改进
            2. 每个条目应重写为简洁、易读的描述。
            3. 删除无意义或琐碎的提交（例如"更新代码"、"合并分支"）。
            4. 严格按照 Markdown 格式输出：
               - 使用二级标题表示版本和日期
               - 使用三级标题表示每个类别
            5. 保持句子简短、客观和技术性。
            6. 不要在 Markdown 之外包含解释或注释。
            
            版本: {version}
            日期: {date}
            提交记录:
            
            {commits}
            """;
    }

    /**
     * 获取默认的日报模板
     *
     * <p>返回用于生成工作日报的默认提示词模板。
     *
     * @return 默认的日报模板
     */
    @NotNull
    public static String getDefaultDailyReportTemplate() {
        return """
            请根据以下 Git 提交记录生成工作日报。
            
            要求：
            1. 按照时间顺序整理提交记录
            2. 将工作内容分类为：
               - 💻 开发工作
               - 🐛 Bug 修复
               - 📝 文档编写
               - 🔧 代码优化
               - 🧪 测试工作
               - 📋 其他工作
            3. 每个条目应包含：
               - 工作内容描述
               - 涉及的功能或模块
               - 完成情况
            4. 输出格式为 Markdown，包含：
               - 日期标题
               - 工作摘要
               - 详细工作内容（按类别分组）
               - 遇到的问题和解决方案（如有）
               - 明日计划（可选）
            
            日期: {date}
            提交记录:
            
            {commits}
            """;
    }

    /**
     * 获取默认的周报模板
     *
     * <p>返回用于生成工作周报的默认提示词模板。
     *
     * @return 默认的周报模板
     */
    @NotNull
    public static String getDefaultWeeklyReportTemplate() {
        return """
            请根据以下 Git 提交记录生成工作周报。
            
            要求：
            1. 按周统计提交记录
            2. 将工作内容分类为：
               - 🎯 主要功能开发
               - 🐛 Bug 修复
               - ♻️ 代码重构
               - 📝 文档编写
               - 🔧 性能优化
               - 🧪 测试工作
               - 📋 其他工作
            3. 每个条目应包含：
               - 工作内容描述
               - 完成的功能或模块
               - 工作量评估
            4. 输出格式为 Markdown，包含：
               - 周报标题（包含日期范围）
               - 本周工作摘要
               - 详细工作内容（按类别分组）
               - 本周完成的主要成果
               - 遇到的问题和解决方案
               - 下周工作计划
            
            日期范围: {dateRange}
            提交记录:
            
            {commits}
            """;
    }

    /**
     * 获取默认的提交记录模板
     *
     * <p>返回用于生成提交记录的默认提示词模板。
     * 使用 {codeDiffs} 作为占位符，会被替换为格式化的代码变更信息。
     *
     * @return 默认的提交记录模板
     */
    @NotNull
    public static String getDefaultCommitMessageTemplate() {
        return """
            请根据以下代码变更生成简洁、准确的提交记录（commit message）：
            
            代码变更：
            {codeDiffs}
            
            要求：
            1. 根据代码变更的实际内容生成描述，准确反映变更的本质
            2. 识别变更类型（新功能、Bug 修复、重构、文档更新等）
            3. 提交记录要简洁、清晰，符合常见的提交记录规范
            4. 忽略无意义的变更（如格式化、空白字符等）
            5. 如果变更涉及多个文件，请总结主要变更点
            6. 输出格式：第一行是简短摘要（50字以内），空一行后是详细描述（可选）
            """;
    }

}
