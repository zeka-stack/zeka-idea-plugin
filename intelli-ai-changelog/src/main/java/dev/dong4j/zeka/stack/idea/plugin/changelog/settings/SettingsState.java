package dev.dong4j.zeka.stack.idea.plugin.changelog.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderSettings;

/**
 * 变更日志插件设置状态类
 * <p>
 * 该类用于管理变更日志插件的各种配置设置, 包括 AI 提供者配置, 系统提示,
 * 变更日志模板, 日报模板, 周报模板和提交消息模板等. 实现了持久化状态组件,
 * 可以将配置保存到 XML 文件中并在应用重启后恢复.
 * <p>
 * 该类采用单例模式, 通过 getInstance() 方法获取全局唯一的实例.
 * 提供了默认的模板内容, 包括变更日志生成模板, 日报生成模板,
 * 周报生成模板和提交消息生成模板, 用于指导 AI 生成相应的内容.
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email mailto:zeka.stack@gmail.com
 * @date 2025.11.30
 * @since 1.0.0
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
     * 是否显示提示词设置
     * <p>
     * 控制设置页面中提示词设置区域的显示/隐藏。
     * 提示词设置包括系统提示词和模板配置。
     * 用户可以通过复选框控制是否显示提示词设置，减少设置页面长度。
     *
     * <p>默认值: false（默认隐藏，减少页面长度）
     */
    public boolean showPromptSettings = false;

    /**
     * 系统提示词模板
     *
     * <p>用于设定 AI 角色和行为准则的系统提示词。
     * 这个提示词会作为 system 消息发送给 AI 服务，
     * 用于建立 AI 的基本角色和响应风格。
     *
     * <p>默认值: getDefaultSystemPrompt()
     *
     * @see #getDefaultChangelogSystemPrompt()
     */
    public String systemPrompt = getDefaultChangelogSystemPrompt();

    /**
     * 变更日志模板
     *
     * <p>用于生成变更日志的提示词模板。
     * 使用 {version}、{date}、{commits} 作为占位符。
     *
     * <p>默认值: getDefaultChangelogTemplate()
     *
     * @see #getDefaultChangelogUserPrompt()
     */
    public String changelogTemplate = getDefaultChangelogUserPrompt();

    /**
     * 日报模板
     *
     * <p>用于生成工作日报的提示词模板。
     * 使用 {date}、{commits} 作为占位符。
     *
     * <p>默认值: getDefaultDailyReportTemplate()
     *
     * @see #getDefaultDailyReportUserPrompt()
     */
    public String dailyReportTemplate = getDefaultDailyReportUserPrompt();

    /**
     * 周报模板
     *
     * <p>用于生成工作周报的提示词模板。
     * 使用 {dateRange}、{commits} 作为占位符。
     *
     * <p>默认值: getDefaultWeeklyReportTemplate()
     *
     * @see #getDefaultWeeklyReportUserPrompt()
     */
    public String weeklyReportTemplate = getDefaultWeeklyReportUserPrompt();

    /**
     * 提交记录模板
     *
     * <p>用于生成提交记录的提示词模板。
     * 使用 {diff} 或 {codeDiffs} 作为占位符，会被替换为格式化的代码变更信息。
     *
     * <p>默认值: getDefaultCommitMessageTemplate()
     *
     * @see #getDefaultCommitMessageUserPrompt()
     */
    public String commitMessageTemplate = getDefaultCommitMessageUserPrompt();

    /**
     * 提交消息系统提示词
     *
     * <p>用于生成提交消息的系统提示词。
     * 这个提示词会作为 system 消息发送给 AI 服务，
     * 用于设定 AI 在生成提交消息时的角色和行为准则。
     *
     * <p>默认值: getDefaultCommitMessageSystemPrompt()
     *
     * @see #getDefaultCommitMessageSystemPrompt()
     */
    public String commitMessageSystemPrompt = getDefaultCommitMessageSystemPrompt();

    /**
     * 是否将提交面板输入的说明作为上下文
     * <p>启用后，会读取 Git 提交消息输入框中的自然语言说明，并作为上下文提供给 AI。
     *
     * <p>默认值: false（默认关闭，避免误用已有提交内容）
     */
    public boolean useCommitMessageInputAsContext = false;

    /**
     * 提交消息 diff 生成方式
     * <p>控制提交消息生成时使用的 diff 方案，支持 CodeDiffUtil 与 IdeaTextPatchBuilder 两种方案。
     *
     * <p>默认值: AUTO
     */
    public CommitMessageDiffProvider commitMessageDiffProvider = CommitMessageDiffProvider.AUTO;

    /**
     * 提交消息 diff 生成方式
     */
    public enum CommitMessageDiffProvider {
        /** 自动检测并生成提交消息 diff 的方式 */
        AUTO,
        /** 表示使用代码差异生成提交消息的方式 */
        CODE_DIFF,
        /** 使用 IntelliJ IDEA 生成的补丁格式提交消息 */
        IDEA_PATCH
    }

    /**
     * 是否显示提交消息提示词设置
     * <p>
     * 控制设置页面中提交消息提示词设置区域的显示/隐藏。
     * 提示词设置包括系统提示词和用户提示词模板。
     * 用户可以通过复选框控制是否显示提示词设置，减少设置页面长度。
     *
     * <p>默认值: false（默认隐藏，减少页面长度）
     */
    public boolean showCommitMessagePrompt = false;

    /**
     * git-cliff 配置
     * <p>用于生成项目级别变更日志的最小配置，后续可在设置中调整。
     *
     * <p>默认值: getDefaultGitCliffConfig()
     *
     * @see #getDefaultGitCliffConfig()
     */
    public String gitCliffConfig = getDefaultGitCliffConfig();

    /**
     * Release Log 生成方式
     * <p>控制 Release Log 的生成方式。
     * 使用枚举类型，支持 AI 和 git-cliff 两种方式，后续可以扩展更多生成器。
     *
     * <p>默认值: AI（默认使用 AI 生成）
     *
     * @see ReleaseLogProvider
     */
    public ReleaseLogProvider releaseLog = ReleaseLogProvider.AI;

    /**
     * 是否使用 tag 作为起点
     * <p>true 表示使用 tag 作为起点，false 表示使用 hash 作为起点。
     *
     * <p>默认值: true（默认使用 tag）
     */
    public boolean useTagAsStart = true;

    /**
     * 最近使用的 tag
     * <p>保存最近使用的 tag 值，用于回显到设置页面。
     *
     * <p>默认值: 空字符串
     */
    public String lastUsedTag = "";

    /**
     * 最近使用的 hash
     * <p>保存最近使用的 hash 值，用于回显到设置页面。
     *
     * <p>默认值: 空字符串
     */
    public String lastUsedHash = "";

    /**
     * 是否显示 Git-cliff 配置
     * <p>控制设置页面中 Git-cliff 配置区域的显示/隐藏。
     * 用户可以通过复选框控制是否显示配置，减少设置页面长度。
     *
     * <p>默认值: false（默认隐藏，减少页面长度）
     */
    public boolean showGitCliffConfig = false;

    /**
     * 是否显示 AI Release Log 提示词
     * <p>控制设置页面中 AI Release Log 提示词区域的显示/隐藏。
     * 用户可以通过复选框控制是否显示提示词，减少设置页面长度。
     *
     * <p>默认值: false（默认隐藏，减少页面长度）
     */
    public boolean showAiReleaseLogPrompt = false;

    /**
     * AI Release Log 提示词
     * <p>用于生成 Release Log 的 AI 提示词模板。
     * 使用 {version}、{date}、{commits} 等作为占位符。
     *
     * <p>默认值: getDefaultAiReleaseLogPrompt()
     *
     * @see #getDefaultReleaseLogUserPrompt()
     */
    public String aiReleaseLogPrompt = getDefaultReleaseLogUserPrompt();

    /**
     * 提交消息排除模式列表
     * <p>用于配置在生成提交消息时应该忽略的文件/目录模式。
     * 每行一个模式，支持 glob 模式匹配。
     *
     * <p>默认值: getDefaultExcludePatterns()
     *
     * @see #getDefaultExcludePatterns()
     */
    public List<String> excludePatterns = getDefaultExcludePatterns();

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
    public static String getDefaultChangelogSystemPrompt() {
        return """
            你是一位经验丰富的软件发布经理和技术文档编写者。
            你的目标是根据 Git 提交记录为软件项目生成清晰、结构化、简洁的变更日志。
            你总是输出格式良好的 Markdown，包含一致的章节结构。

            重要要求：
            - **必须使用 ${language} 编写所有内容**，这是强制要求，不能使用其他语言
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
    public static String getDefaultChangelogUserPrompt() {
        return """
            请根据以下 Git 提交记录生成发布变更日志（**${language}**）。

            要求：
            1. **必须使用 ${language} 编写所有内容**，这是强制要求，不能使用其他语言
            2. 提交记录已经按照提交日期进行了分组，每个日期分组使用三级标题（### 日期）标识。
            3. 将每个日期分组内的提交记录分类到以下类别：
               - ⭐ 新功能
               - 🪲 问题修复
               - ♻️ 代码重构
               - 📓 文档更新
               - 🔧 其他改进
            4. 每个条目应重写为简洁、易读的描述。
            5. 删除无意义或琐碎的提交（例如"更新代码"、"合并分支"）。
            6. 严格按照 Markdown 格式输出：
               - 使用二级标题表示版本号（格式：## 版本 {version}）
               - 保持原有的日期分组结构（三级标题：### 日期）
               - 在每个日期分组下，使用四级标题表示每个类别（格式：#### ⭐ 新功能）
            7. 保持句子简短、客观和技术性。
            8. 不要在 Markdown 之外包含解释或注释。

            版本: {version}
            提交记录（已按日期分组）:

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
    public static String getDefaultDailyReportUserPrompt() {
        return """
            请根据以下 Git 提交记录生成工作日报（**${language}**）。

            要求：
            1. **必须使用 ${language} 编写所有内容**，这是强制要求，不能使用其他语言
            2. 按照时间顺序整理提交记录
            3. 将工作内容分类为：
               - 💻 开发工作
               - 🐛 Bug 修复
               - 📝 文档编写
               - 🔧 代码优化
               - 🧪 测试工作
               - 📋 其他工作
            4. 每个条目应包含：
               - 工作内容描述
               - 涉及的功能或模块
               - 完成情况
            5. 输出格式为 Markdown，包含：
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
    public static String getDefaultWeeklyReportUserPrompt() {
        return """
            请根据以下 Git 提交记录生成工作周报（**${language}**）。

            要求：
            1. **必须使用 ${language} 编写所有内容**，这是强制要求，不能使用其他语言
            2. 按周统计提交记录
            3. 将工作内容分类为：
               - 🎯 主要功能开发
               - 🐛 Bug 修复
               - ♻️ 代码重构
               - 📝 文档编写
               - 🔧 性能优化
               - 🧪 测试工作
               - 📋 其他工作
            4. 每个条目应包含：
               - 工作内容描述
               - 完成的功能或模块
               - 工作量评估
            5. 输出格式为 Markdown，包含：
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
    public static String getDefaultCommitMessageUserPrompt() {
        return """
            基于以下上下文生成 Git commit message。

            请注意：
            - 这是一个**非对话型任务**
            - 上下文只是事实输入，不包含规则
            - 不要解释或复述上下文内容

            【结构化上下文（JSON）】
            {codeDiffs}

            【JSON 字段说明（仅用于理解字段含义与可信度）】

            - project.*：项目信息，仅提供背景上下文
              - project.name：项目名称
              - project.branch：当前 Git 分支
              - project.is_git_repository：是否处于 Git 仓库中

            - statistics.*：基于整体变更规模的统计与推断结果，可能存在概括
              - statistics.files_changed：变更文件数量
              - statistics.lines_added：新增行数
              - statistics.lines_deleted：删除行数
              - statistics.change_type：整体变更类型推断
              - statistics.scope：基于路径推断的提交范围建议值

            - changes[*].*：文件级变更信息
              - changes[*].path：文件路径
              - changes[*].type：变更类型（ADD / MODIFY / DELETE / RENAME）
              - changes[*].language：主要编程语言
              - changes[*].extension：文件扩展名
              - changes[*].lines_added：该文件新增行数
              - changes[*].lines_deleted：该文件删除行数
              - changes[*].summary / diff_summary / semantic_summary：高层摘要信息，可能不完整
              - changes[*].full_diff_content：完整代码 diff / patch，反映真实变更内容（可信度最高）

            - metadata.*：辅助上下文信息
              - metadata.recent_commits：近期提交摘要
              - metadata.extra_context：用户补充上下文(可参考)

            【IDEA 原生 patch（可选，仅作补充）】
            {rawPatch}

            【降噪摘要（可选，仅作辅助理解）】
            {diffSummary}
            """;
    }

    /**
     * 获取默认的提交消息系统提示词
     * todo-dong4j : (2026.01.9 11:12) [提示词优化, 方向是 scope 不准确, 考虑使用自定义映射]
     *
     * <p>返回用于生成提交消息的默认系统提示词。
     * 这个提示词会作为 system 消息发送给 AI 服务，
     * 用于设定 AI 在生成提交消息时的角色和行为准则。
     *
     * @return 默认的提交消息系统提示词
     */
    @NotNull
    public static String getDefaultCommitMessageSystemPrompt() {
        return """
            你是一名严格遵守规范的 Git 提交记录生成器（不是解释器、不是分析器）。

            你的唯一任务是：
            基于给定的代码 diff，生成符合 Conventional Commits 规范的 Git 提交记录。

            【强制输出规则（最高优先级）】

            1. **你只能输出 Git 提交记录本身**
            2. **禁止输出以下任何内容：**
               - 解释、分析、推理过程
               - 标题、前言、后记、说明文字
               - 代码块标记（```）
               - JSON、YAML 或任何结构化包装
            3. **如果输出中包含提交记录以外的任意字符，视为失败**

            【提交格式（必须严格一致）】

            <type>(<scope>): <subject>

            <body（可选）>

            - 不允许多余空行
            - subject 使用祈使语气，不要句号
            - scope 必须来自统计信息或 diff 语义，不允许编造

            【type 白名单（只能从以下枚举中选择）】

            只允许使用以下 type，不得创造新 type：

            - feat       // 新增功能或能力
            - fix        // 修复缺陷或错误行为
            - refactor   // 不改变外部行为的结构性调整
            - perf       // 性能优化
            - docs       // 文档或注释变更
            - test       // 测试相关变更
            - build      // 构建系统或依赖变更
            - chore      // 非业务、非功能性杂项变更
            - style      // 纯格式、风格调整（无语义变化）
            - revert     // 回滚提交

            如果无法明确匹配，使用 chore

            【body 编写规则（如需要，必须遵守）】

            - body 必须使用 Markdown 无序列表
            - 每一行必须且只能以一个 `- ` 作为行首前缀（禁止 `- -`、`*`、`+` 等）
            - 列表项内容必须是对“变更语义”的描述，而不是对文档结构或 Markdown 符号的复述
            - 每条只表达一个明确观点
            - 建议 0～3 条，最多不超过 5 条
            - 只允许包含以下类型的信息：
              - Why：为什么要改
              - What：语义 / 行为发生了什么变化
              - Impact：影响范围、兼容性
              - Note：风险、注意事项

            【语义判断原则】

            - **仅基于代码 diff 判断**
            - 优先参考 `changes[].full_diff_content`
            - 忽略以下不产生语义变化的修改：
              - 代码格式化（缩进、对齐、换行、行宽）
              - 空白字符变化（空行、行尾空格、制表符）
              - 等价重排（import、方法、常量等顺序调整）
              - 注释或文档的排版调整（语义未变）
              - 文件末尾换行或换行符格式变化
            - 如果是 refactor，**必须明确说明“为什么现在需要重构”**
            - 不得引入 diff 中不存在的动机或结论

            【语言要求（强制）】

            - 提交消息内容 **必须使用 ${language}**
            - type 与 scope 使用英文

            【最终输出要求】

            - 只输出最终提交记录
            - 不要解释
            - 不要补充任何说明
            """;
    }

    /**
     * 获取默认的 git-cliff 配置
     * <p>
     * 配置包含表情符号和优化的模板格式，使生成的 release log 更加美观和易读。
     *
     * @return git-cliff 配置内容
     */
    @NotNull
    public static String getDefaultGitCliffConfig() {
        return """
            [changelog]
            body = ""\"
            {% if version %}\\
                ## [{{ version | trim_start_matches(pat="v") }}] - {{ timestamp | date(format="%Y-%m-%d") }}
            {% else %}\\
                ## [unreleased]
            {% endif %}\\
            {% for group, commits in commits | group_by(attribute="group") %}
                ### {{ group | striptags | trim | upper_first }}
                {% for commit in commits %}
                    - {% if commit.scope %}*({{ commit.scope }})* {% endif %}\\
                        {% if commit.breaking %}[**breaking**] {% endif %}\\
                        {{ commit.message | upper_first }}\\
                {% endfor %}
            {% endfor %}
            ""\"
            trim = true
            render_always = true

            [git]
            conventional_commits = true
            filter_unconventional = false
            require_conventional = false
            split_commits = false
            protect_breaking_commits = false
            commit_parsers = [
                { message = "^feat", group = "<!-- 0 -->🚀 Features" },
                { message = "^fix", group = "<!-- 1 -->🐛 Bug Fixes" },
                { message = "^doc", group = "<!-- 3 -->📚 Documentation" },
                { message = "^perf", group = "<!-- 4 -->⚡ Performance" },
                { message = "^refactor", group = "<!-- 2 -->🚜 Refactor" },
                { message = "^style", group = "<!-- 5 -->🎨 Styling" },
                { message = "^test", group = "<!-- 6 -->🧪 Testing" },
                { message = "^chore\\\\(release\\\\): prepare for", skip = true },
                { message = "^chore\\\\(deps.*\\\\)", skip = true },
                { message = "^chore\\\\(pr\\\\)", skip = true },
                { message = "^chore\\\\(pull\\\\)", skip = true },
                { message = "^chore|^ci", group = "<!-- 7 -->⚙️ Miscellaneous Tasks" },
                { body = ".*security", group = "<!-- 8 -->🛡️ Security" },
                { message = "^revert", group = "<!-- 9 -->◀️ Revert" },
                { message = ".*", group = "<!-- 10 -->💼 Other" },
            ]
            filter_commits = false
            fail_on_unmatched_commit = false
            link_parsers = []
            use_branch_tags = false
            topo_order = false
            topo_order_commits = true
            sort_commits = "oldest"
            recurse_submodules = false
            """;
    }

    /**
     * 获取默认的 Release Log 提示词
     * <p>
     * 返回用于生成 Release Log 的默认提示词模板。
     * 输出格式与 git-cliff 保持一致，按照类别分组，不包含日期分组。
     *
     * @return 默认的 Release Log 提示词
     */
    @NotNull
    public static String getDefaultReleaseLogUserPrompt() {
        return """
            请根据以下 Git 提交记录生成项目 Release Log（**${language}**）。

            要求：
            1. **必须使用 ${language} 编写所有内容**，这是强制要求，不能使用其他语言
            2. 将提交记录分类到以下类别（使用二级标题，格式与 git-cliff 保持一致）：
               - ## ✨ Features（新功能）
               - ## 🐛 Bug Fixes（问题修复）
               - ## ♻️ Refactor（代码重构）
               - ## 📚 Documentation（文档更新）
               - ## ⚡ Performance（性能优化）
               - ## ✅ Tests（测试相关）
               - ## 📦 Build（构建相关）
               - ## 🔧 Chore（其他改进）
               - ## 🔄 CI（CI/CD 相关）
               - ## ⚙️ Miscellaneous Tasks（其他任务）
            3. 每个条目应重写为简洁、易读的描述，删除提交信息中的类型前缀（如 feat:、fix: 等）。
            4. 删除无意义或琐碎的提交（例如"更新代码"、"合并分支"、"chore: update"等）。
            5. **合并相似提交记录：** 如果存在内容相似或重复的提交记录（例如多个提交都是修复同一个问题、更新同一个功能、优化同一个模块等），必须合并为一条更新记录，避免重复。
            6. **合并相关提交记录：** 如果存在逻辑相关的提交记录（例如：实现某个功能及其测试、修复某个问题及其文档更新、重构某个模块及其相关优化等），应该合并为一条更新记录，使变更日志更加简洁和易读。
            7. 合并后的描述应该概括所有相关提交的核心内容，使用更通用的表述方式。
            8. 严格按照 Markdown 格式输出：
               - 使用二级标题（##）表示类别，格式：## [表情符号] [类别名称]
               - 每个提交使用列表项（-）表示
               - 只输出分类后的内容，不要添加版本号标题或其他额外信息
            9. 保持句子简短、客观和技术性。
            10. 不要在 Markdown 之外包含解释或注释。
            11. 如果某个类别没有提交记录，则不要输出该类别。无法明确分类到上述类别的提交，应归类到 ## ⚙️ Miscellaneous Tasks 类别中。

            提交记录:

            {commits}
            """;
    }

    /**
     * 获取默认的排除模式列表
     * <p>
     * 返回用于过滤提交消息生成时应该忽略的文件/目录的默认模式列表。
     * 这些模式使用 glob 语法，包括常见的生成文件、依赖锁定文件、构建产物等。
     *
     * @return 默认的排除模式列表
     */
    @NotNull
    public static List<String> getDefaultExcludePatterns() {
        return new ArrayList<>(Arrays.asList(
            "*.pb.go",
            "*.pb.cc",
            "*.pb.h",
            "go.sum",
            "go.mod",
            "package-lock.json",
            "yarn.lock",
            "pnpm-lock.yaml",
            "Cargo.lock",
            "Pipfile.lock",
            "poetry.lock",
            "*.generated.*",
            "*.gen.*",
            "*_generated.*",
            "*_gen.*",
            "vendor/**",
            "node_modules/**",
            ".next/**",
            "dist/**",
            "build/**",
            "target/**",
            "*.min.js",
            "*.min.css",
            "*.bundle.*",
            "*.chunk.*",
            "coverage/**",
            ".nyc_output/**",
            "*.lcov",
            "*.log",
            "*.tmp",
            "*.temp",
            ".DS_Store",
            "Thumbs.db",
            "*.swp",
            "*.swo",
            "*~"
                                            ));
    }

}
