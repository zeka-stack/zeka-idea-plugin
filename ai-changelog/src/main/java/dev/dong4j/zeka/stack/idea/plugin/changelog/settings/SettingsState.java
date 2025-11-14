package dev.dong4j.zeka.stack.idea.plugin.changelog.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderSettings;

/**
 * 插件设置状态管理
 * 使用 @State 注解自动持久化配置
 */
@State(
        name = "ChangelogPluginSettings",
        storages = @Storage("changelog-plugin.xml")
)
public class SettingsState implements PersistentStateComponent<SettingsState> {

    // AI 提供商配置
    public AIProviderSettings providerSettings = new AIProviderSettings();

    // 提示词配置
    public String systemPrompt = """
        你是一位经验丰富的软件发布经理和技术文档编写者。
        你的目标是根据 Git 提交记录为软件项目生成清晰、结构化、简洁的变更日志。
        你总是输出格式良好的 Markdown，包含一致的章节结构。
        """;

    public String changelogTemplate = """
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

    public String dailyReportTemplate = """
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

    public String weeklyReportTemplate = """
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

}
