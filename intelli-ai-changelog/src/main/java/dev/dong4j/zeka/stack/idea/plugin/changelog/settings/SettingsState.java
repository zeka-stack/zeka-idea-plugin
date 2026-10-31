package dev.dong4j.zeka.stack.idea.plugin.changelog.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.messages.Topic;
import com.intellij.util.xmlb.XmlSerializerUtil;

import dev.dong4j.zeka.stack.idea.plugin.common.changelog.ChangelogDefaults;

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
     * 是否生成 CHANGELOG.md 文件
     * <p>
     * 控制在执行“生成并保存 CHANGELOG.md”相关能力时，是否实际写入项目根目录下的 CHANGELOG.md 文件。
     * 可在设置页与状态栏快捷设置中快速切换。
     *
     * <p>默认值: true（默认开启，保持生成文件能力可用）
     */
    public boolean generateChangelogFile = true;

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

    /** 是否启用多 Git 仓库提交检查 */
    public boolean enableCommitMultiRepoCheck = true;

    /**
     * 是否启用 Conventional Commit 首行高亮辅助
     * <p>
     * 控制在 Git 提交消息编辑框中，是否对首行的 type/scope/breaking/separator/subject
     * 各语义片段进行分段高亮显示。
     *
     * <p>默认值: true（默认开启）
     */
    public boolean enableConventionalCommitAssist = true;

    /**
     * 设置变更监听器
     * <p>
     * 用于在设置发生变化时广播通知，便于同步到其他 UI（如提交前检查面板）。
     */
    public interface SettingsChangeListener {
        /** 设置变更通知主题, 用于在设置变化时广播通知, 便于同步到其他 UI 组件. */
        Topic<SettingsChangeListener> TOPIC =
            Topic.create("ChangelogSettingsChanged", SettingsChangeListener.class);

        /**
         * 当设置状态发生变化时被调用, 用于广播通知其他监听器
         * <p> 该方法在设置内容更新后被触发, 常用于同步到其他用户界面组件 (如提交前检查面板).
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
        return ChangelogDefaults.getDefaultChangelogSystemPrompt();
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
        return ChangelogDefaults.getDefaultChangelogUserPrompt();
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
        return ChangelogDefaults.getDefaultDailyReportUserPrompt();
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
        return ChangelogDefaults.getDefaultWeeklyReportUserPrompt();
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
        return ChangelogDefaults.getDefaultCommitMessageUserPrompt();
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
        return ChangelogDefaults.getDefaultCommitMessageSystemPrompt();
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
        return ChangelogDefaults.getDefaultGitCliffConfig();
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
        return ChangelogDefaults.getDefaultReleaseLogUserPrompt();
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
