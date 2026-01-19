package dev.dong4j.zeka.stack.idea.plugin.changelog.service;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import dev.dong4j.zeka.stack.idea.plugin.changelog.settings.SettingsState;
import dev.dong4j.zeka.stack.idea.plugin.changelog.util.ChangelogBundle;
import dev.dong4j.zeka.stack.idea.plugin.changelog.util.UnifiedDiffParser;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIStreamResponseListener;
import dev.dong4j.zeka.stack.idea.plugin.common.statistics.StatisticsEventType;
import dev.dong4j.zeka.stack.idea.plugin.common.statistics.StatisticsUserAction;

/**
 * 变更日志服务类
 * <p>
 * 提供基于 Git 提交记录生成变更日志, 日报, 周报以及根据代码 diff 生成提交信息的功能. 该类负责从项目仓库中读取提交信息, 并通过 AI 服务生成结构化和自然语言的变更内容.
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email mailto:zeka.stack@gmail.com
 * @date 2025.11.30
 * @since 1.0.0
 */
@Service(Service.Level.PROJECT)
public final class ChangelogService {

    /** 最近提交记录数量, 用于限制最近提交信息的读取范围 */
    private static final int RECENT_COMMITS_LIMIT = 3;
    /** 控制台提示词截断长度（设置为最大值以输出完整内容） */
    private static final int PROMPT_LOG_MAX_LENGTH = 3000;

    /** Git 服务实例, 用于读取提交记录和处理 Git 相关操作 */
    private final ChangelogGitService gitService;
    /** 提交日志提示词构建器, 用于生成 AI 调用所需的提示文本 */
    private final ChangelogPromptBuilder promptBuilder;
    /** 差异构建器, 用于根据代码变更生成提交信息 */
    private final ChangelogCommitDiffBuilder diffBuilder;
    /** AI 服务执行器, 用于调用 AI 生成变更日志, 日报, 周报及提交信息 */
    private final ChangelogAiExecutor aiExecutor;

    /**
     * 初始化 ChangelogService 实例
     * <p>
     * 通过传入的 Project 对象进行初始化, 将 project 赋值给成员变量, 并初始化相关服务组件.
     *
     * @param project 项目对象, 不能为空
     */
    public ChangelogService(@NotNull Project project) {
        this.gitService = new ChangelogGitService(project);
        this.promptBuilder = new ChangelogPromptBuilder(project);
        this.diffBuilder = new ChangelogCommitDiffBuilder(project);
        this.aiExecutor = new ChangelogAiExecutor(project, PROMPT_LOG_MAX_LENGTH);
    }

    /**
     * 获取服务实例
     *
     * @param project 项目对象, 不能为空
     * @return ChangelogService 实例
     */
    public static ChangelogService getInstance(@NotNull Project project) {
        return project.getService(ChangelogService.class);
    }

    /**
     * 从选中的提交记录生成 Changelog
     * <p>
     * 根据提供的提交哈希列表, 读取对应的提交信息, 并通过 AI 服务生成结构化的 Markdown 格式 Changelog 内容.
     *
     * @param commitHashes 提交记录的 hash 列表, 不能为空
     * @return 生成的 Changelog 内容 (Markdown 格式)
     * @throws Exception 当 AI 服务调用失败时抛出, 包含友好的错误消息
     */
    @NotNull
    public String generateChangelog(@NotNull List<String> commitHashes) throws Exception {
        return generateChangelog(commitHashes, StatisticsUserAction.UNKNOWN);
    }

    public String generateChangelog(@NotNull List<String> commitHashes,
                                    @NotNull StatisticsUserAction userAction) throws Exception {
        List<ChangelogCommitModels.CommitInfo> commits = gitService.readCommits(commitHashes);
        String prompt = promptBuilder.buildChangelogPrompt(commits);
        return aiExecutor.callChangelog(prompt, StatisticsEventType.CHANGELOG_CHANGELOG, userAction);
    }

    /**
     * 从选中的提交记录生成 Changelog(流式)
     *
     * @param commitHashes 提交记录的 hash 列表
     * @param listener     流式监听器, 用于接收生成过程中的数据流
     * @return 生成的 Changelog 内容
     * @throws Exception 当 AI 服务调用失败时抛出, 包含友好的错误消息
     */
    @NotNull
    public String generateChangelogStream(@NotNull List<String> commitHashes,
                                          @NotNull AIStreamResponseListener listener) throws Exception {
        return generateChangelogStream(commitHashes, listener, StatisticsUserAction.UNKNOWN);
    }

    public String generateChangelogStream(@NotNull List<String> commitHashes,
                                          @NotNull AIStreamResponseListener listener,
                                          @NotNull StatisticsUserAction userAction) throws Exception {
        List<ChangelogCommitModels.CommitInfo> commits = gitService.readCommits(commitHashes);
        String prompt = promptBuilder.buildChangelogPrompt(commits);
        return aiExecutor.callChangelogStream(prompt, listener, StatisticsEventType.CHANGELOG_CHANGELOG, userAction);
    }

    /**
     * 从选中的提交记录生成工作日报
     *
     * @param commitHashes 提交记录的哈希列表
     * @return 生成的工作日报内容 (Markdown 格式)
     * @throws Exception 当生成日报内容失败时抛出
     */
    @NotNull
    public String generateDailyReport(@NotNull List<String> commitHashes) throws Exception {
        return generateDailyReport(commitHashes, StatisticsUserAction.UNKNOWN);
    }

    public String generateDailyReport(@NotNull List<String> commitHashes,
                                      @NotNull StatisticsUserAction userAction) throws Exception {
        List<ChangelogCommitModels.CommitInfo> commits = gitService.readCommits(commitHashes);
        String prompt = promptBuilder.buildDailyReportPrompt(commits);
        return aiExecutor.callChangelog(prompt, StatisticsEventType.CHANGELOG_DAILY_REPORT, userAction);
    }

    /**
     * 从选中的提交记录生成工作日报 (流式)
     *
     * @param commitHashes 提交记录的哈希列表
     * @param listener     流式监听器
     * @return 生成的工作日报内容
     * @throws Exception 当生成日报内容失败时抛出
     */
    @NotNull
    public String generateDailyReportStream(@NotNull List<String> commitHashes,
                                            @NotNull AIStreamResponseListener listener) throws Exception {
        return generateDailyReportStream(commitHashes, listener, StatisticsUserAction.UNKNOWN);
    }

    public String generateDailyReportStream(@NotNull List<String> commitHashes,
                                            @NotNull AIStreamResponseListener listener,
                                            @NotNull StatisticsUserAction userAction) throws Exception {
        List<ChangelogCommitModels.CommitInfo> commits = gitService.readCommits(commitHashes);
        String prompt = promptBuilder.buildDailyReportPrompt(commits);
        return aiExecutor.callChangelogStream(prompt, listener, StatisticsEventType.CHANGELOG_DAILY_REPORT, userAction);
    }

    /**
     * 从选中的提交记录生成工作周报
     *
     * @param commitHashes 提交记录的哈希列表
     * @return 生成的工作周报内容 (Markdown 格式)
     * @throws Exception 当读取提交记录或调用 AI 服务失败时抛出
     */
    @NotNull
    public String generateWeeklyReport(@NotNull List<String> commitHashes) throws Exception {
        return generateWeeklyReport(commitHashes, StatisticsUserAction.UNKNOWN);
    }

    public String generateWeeklyReport(@NotNull List<String> commitHashes,
                                       @NotNull StatisticsUserAction userAction) throws Exception {
        List<ChangelogCommitModels.CommitInfo> commits = gitService.readCommits(commitHashes);
        String prompt = promptBuilder.buildWeeklyReportPrompt(commits);
        return aiExecutor.callChangelog(prompt, StatisticsEventType.CHANGELOG_WEEKLY_REPORT, userAction);
    }

    /**
     * 从选中的提交记录生成工作周报 (流式)
     *
     * @param commitHashes 提交记录的哈希列表
     * @param listener     流式监听器
     * @return 生成的工作周报内容
     * @throws Exception 当读取提交记录或调用 AI 服务失败时抛出
     */
    @NotNull
    public String generateWeeklyReportStream(@NotNull List<String> commitHashes,
                                             @NotNull AIStreamResponseListener listener) throws Exception {
        return generateWeeklyReportStream(commitHashes, listener, StatisticsUserAction.UNKNOWN);
    }

    public String generateWeeklyReportStream(@NotNull List<String> commitHashes,
                                             @NotNull AIStreamResponseListener listener,
                                             @NotNull StatisticsUserAction userAction) throws Exception {
        List<ChangelogCommitModels.CommitInfo> commits = gitService.readCommits(commitHashes);
        String prompt = promptBuilder.buildWeeklyReportPrompt(commits);
        return aiExecutor.callChangelogStream(prompt, listener, StatisticsEventType.CHANGELOG_WEEKLY_REPORT, userAction);
    }

    /**
     * 基于 Git 范围生成 Release Log(AI)
     *
     * @param gitRoot  Git 仓库根目录
     * @param range    提交范围 (例如 tag..HEAD), 可为空
     * @param listener 流式监听器
     * @throws Exception 当读取提交记录或调用 AI 服务失败时抛出
     */
    public void generateReleaseLogByAiStream(@NotNull Path gitRoot,
                                             @Nullable String range,
                                             @NotNull AIStreamResponseListener listener) throws Exception {
        generateReleaseLogByAiStream(gitRoot, range, null, listener, StatisticsUserAction.UNKNOWN);
    }

    /**
     * 基于 Git 范围生成 Release Log(AI, 可覆盖提示词)
     *
     * @param gitRoot        Git 仓库根目录
     * @param range          提交范围 (例如 tag..HEAD), 可为空
     * @param promptTemplate 提示词模板, 用于覆盖默认的提示词内容, 可为空
     * @param listener       流式监听器, 用于接收 AI 生成过程中的响应数据
     * @throws Exception 当读取提交记录或调用 AI 服务失败时抛出
     */
    public void generateReleaseLogByAiStream(@NotNull Path gitRoot,
                                             @Nullable String range,
                                             @Nullable String promptTemplate,
                                             @NotNull AIStreamResponseListener listener) throws Exception {
        generateReleaseLogByAiStream(gitRoot, range, promptTemplate, listener, StatisticsUserAction.UNKNOWN);
    }

    public void generateReleaseLogByAiStream(@NotNull Path gitRoot,
                                             @Nullable String range,
                                             @Nullable String promptTemplate,
                                             @NotNull AIStreamResponseListener listener,
                                             @NotNull StatisticsUserAction userAction) throws Exception {
        List<ChangelogCommitModels.CommitInfo> commits = gitService.readCommitsFromRange(gitRoot, range);
        String prompt = promptBuilder.buildReleaseLogPrompt(commits, promptTemplate);
        aiExecutor.callChangelogStream(prompt, listener, StatisticsEventType.CHANGELOG_RELEASE_LOG, userAction);
    }

    /**
     * 基于 Git diff 生成变更日志
     *
     * @param commitHashes 提交哈希列表
     * @return 生成的变更日志内容
     * @throws Exception 当生成过程中发生错误
     */
    @NotNull
    public String generateChangelogFromDiff(@NotNull List<String> commitHashes) throws Exception {
        return generateChangelogFromDiff(commitHashes, StatisticsUserAction.UNKNOWN);
    }

    public String generateChangelogFromDiff(@NotNull List<String> commitHashes,
                                            @NotNull StatisticsUserAction userAction) throws Exception {
        List<ChangelogCommitModels.DiffCommitInfo> diffCommits = gitService.readCommitDiffs(commitHashes);
        String prompt = promptBuilder.buildDiffChangelogPrompt(diffCommits);
        return aiExecutor.callChangelog(prompt, StatisticsEventType.CHANGELOG_CHANGELOG, userAction);
    }

    /**
     * 基于 Git diff 生成变更日志 (流式)
     *
     * @param commitHashes 提交哈希列表
     * @param listener     流式监听器, 用于接收生成过程中的数据流
     * @throws Exception 当生成过程中发生错误时抛出异常
     */
    public void generateChangelogFromDiffStream(@NotNull List<String> commitHashes,
                                                @NotNull AIStreamResponseListener listener) throws Exception {
        generateChangelogFromDiffStream(commitHashes, listener, StatisticsUserAction.UNKNOWN);
    }

    public void generateChangelogFromDiffStream(@NotNull List<String> commitHashes,
                                                @NotNull AIStreamResponseListener listener,
                                                @NotNull StatisticsUserAction userAction) throws Exception {
        List<ChangelogCommitModels.DiffCommitInfo> diffCommits = gitService.readCommitDiffs(commitHashes);
        String prompt = promptBuilder.buildDiffChangelogPrompt(diffCommits);
        aiExecutor.callChangelogStream(prompt, listener, StatisticsEventType.CHANGELOG_CHANGELOG, userAction);
    }

    /**
     * 基于代码变更 (diff) 生成提交记录
     * <p>
     * 根据代码的实际改动生成提交记录, 而不是依赖提交信息.
     *
     * @param changes 代码变更集合
     * @return 生成的提交记录内容
     * @throws Exception 当 AI 服务调用失败时抛出, 包含友好的错误消息
     */
    @NotNull
    public String generateCommitMessageFromDiff(@NotNull Collection<Change> changes) throws Exception {
        return generateCommitMessageFromDiff(changes, null, StatisticsUserAction.UNKNOWN);
    }

    /**
     * 基于代码变更 (diff) 生成提交记录(带上下文)
     * <p>
     * 根据代码的实际改动生成提交记录, 可选提供用户输入的上下文说明.
     *
     * @param changes     代码变更集合
     * @param userContext 用户输入的上下文说明, 可为空
     * @return 生成的提交记录内容
     * @throws Exception 当 AI 服务调用失败时抛出, 包含友好的错误消息
     */
    @NotNull
    public String generateCommitMessageFromDiff(@NotNull Collection<Change> changes,
                                                @Nullable String userContext) throws Exception {
        return generateCommitMessageFromDiff(changes, userContext, StatisticsUserAction.UNKNOWN);
    }

    public String generateCommitMessageFromDiff(@NotNull Collection<Change> changes,
                                                @Nullable String userContext,
                                                @NotNull StatisticsUserAction userAction) throws Exception {
        return aiExecutor.callCommitMessage(buildPrompt(changes, userContext),
                                            StatisticsEventType.CHANGELOG_COMMIT_MESSAGE,
                                            userAction);
    }

    /**
     * 基于代码变更 (diff) 生成提交记录(流式回调)
     * <p>
     * 根据代码的实际改动生成提交记录, 而不是依赖提交信息. 使用流式响应监听器来处理生成的过程.
     *
     * @param changes  代码变更集合
     * @param listener 流式响应监听器, 用于接收生成过程中的数据流
     * @return 生成的提交记录内容
     * @throws Exception 当 AI 服务调用失败时抛出, 包含友好的错误消息
     */
    @NotNull
    public String generateCommitMessageFromDiffStream(@NotNull Collection<Change> changes,
                                                      @NotNull AIStreamResponseListener listener) throws Exception {
        return generateCommitMessageFromDiffStream(changes, listener, null, StatisticsUserAction.UNKNOWN);
    }

    /**
     * 基于代码变更 (diff) 生成提交记录 (流式回调, 带上下文)
     * <p>
     * 根据代码的实际改动生成提交记录, 可选提供用户输入的上下文说明. 使用流式响应监听器来处理生成的过程.
     *
     * @param changes     代码变更集合
     * @param listener    流式响应监听器, 用于接收生成过程中的数据流
     * @param userContext 用户输入的上下文说明, 可为空
     * @return 生成的提交记录内容
     * @throws Exception 当 AI 服务调用失败时抛出, 包含友好的错误消息
     */
    @NotNull
    public String generateCommitMessageFromDiffStream(@NotNull Collection<Change> changes,
                                                      @NotNull AIStreamResponseListener listener,
                                                      @Nullable String userContext) throws Exception {
        return generateCommitMessageFromDiffStream(changes, listener, userContext, StatisticsUserAction.UNKNOWN);
    }

    public String generateCommitMessageFromDiffStream(@NotNull Collection<Change> changes,
                                                      @NotNull AIStreamResponseListener listener,
                                                      @Nullable String userContext,
                                                      @NotNull StatisticsUserAction userAction) throws Exception {
        return aiExecutor.callCommitMessageStream(buildPrompt(changes, userContext),
                                                  listener,
                                                  StatisticsEventType.CHANGELOG_COMMIT_MESSAGE,
                                                  userAction);
    }

    /**
     * 基于 Git Log 中已提交记录的真实 diff 再生提交记录（流式回调）
     *
     * @param commitHash  提交 hash
     * @param listener    流式响应监听器
     * @param userContext 用户补充上下文说明，可为空
     * @return 生成的提交记录内容
     * @throws Exception 当读取 diff 或调用 AI 服务失败时抛出
     */
    @NotNull
    public String generateCommitMessageFromGitLogStream(@NotNull String commitHash,
                                                        @NotNull AIStreamResponseListener listener,
                                                        @Nullable String userContext) throws Exception {
        return generateCommitMessageFromGitLogStream(commitHash, listener, userContext, StatisticsUserAction.UNKNOWN);
    }

    public String generateCommitMessageFromGitLogStream(@NotNull String commitHash,
                                                        @NotNull AIStreamResponseListener listener,
                                                        @Nullable String userContext,
                                                        @NotNull StatisticsUserAction userAction) throws Exception {
        return generateCommitMessageFromGitLogSelectionStream(List.of(commitHash), List.of(), listener, userContext, userAction);
    }

    /**
     * 基于 Git Log 中多条已提交记录的真实 diff（压缩提交/Squash）再生提交记录（流式回调）
     *
     * @param commitHashes         提交 hash 列表（至少 2 条）
     * @param selectedCommitTitles 选中提交的原始 message（可为空，用于帮助模型合并语义）
     * @param listener             流式响应监听器
     * @param userContext          用户补充上下文说明，可为空
     * @return 生成的提交记录内容
     * @throws Exception 当读取 diff 或调用 AI 服务失败时抛出
     */
    @NotNull
    public String generateSquashCommitMessageFromGitLogStream(@NotNull List<String> commitHashes,
                                                              @NotNull List<String> selectedCommitTitles,
                                                              @NotNull AIStreamResponseListener listener,
                                                              @Nullable String userContext) throws Exception {
        return generateSquashCommitMessageFromGitLogStream(commitHashes,
                                                           selectedCommitTitles,
                                                           listener,
                                                           userContext,
                                                           StatisticsUserAction.UNKNOWN);
    }

    public String generateSquashCommitMessageFromGitLogStream(@NotNull List<String> commitHashes,
                                                              @NotNull List<String> selectedCommitTitles,
                                                              @NotNull AIStreamResponseListener listener,
                                                              @Nullable String userContext,
                                                              @NotNull StatisticsUserAction userAction) throws Exception {
        return generateCommitMessageFromGitLogSelectionStream(commitHashes, selectedCommitTitles, listener, userContext, userAction);
    }

    @NotNull
    private String generateCommitMessageFromGitLogSelectionStream(@NotNull List<String> commitHashes,
                                                                  @NotNull List<String> selectedCommitTitles,
                                                                  @NotNull AIStreamResponseListener listener,
                                                                  @Nullable String userContext,
                                                                  @NotNull StatisticsUserAction userAction) throws Exception {
        List<ChangelogCommitModels.DiffCommitInfo> diffCommits = gitService.readCommitDiffs(commitHashes);
        if (diffCommits.isEmpty()) {
            throw new Exception(ChangelogBundle.message("commit.regenerate.no.commit.diff"));
        }

        String diffText = joinCommitDiffTexts(diffCommits);
        if (diffText.isBlank()) {
            throw new Exception(ChangelogBundle.message("commit.no.changes"));
        }

        ChangelogCommitDiffBuilder.DiffPayload payload = buildPayloadFromUnifiedDiff(diffText);
        CommitSelectionMeta selectionMeta = new CommitSelectionMeta("git_log", commitHashes, selectedCommitTitles);
        String prompt = buildCommitMessagePrompt(payload, userContext, selectionMeta);
        return aiExecutor.callCommitMessageStream(prompt,
                                                  listener,
                                                  StatisticsEventType.CHANGELOG_COMMIT_MESSAGE,
                                                  userAction);
    }

    @NotNull
    private String joinCommitDiffTexts(@NotNull List<ChangelogCommitModels.DiffCommitInfo> diffCommits) {
        StringBuilder diffTextBuilder = new StringBuilder();
        for (ChangelogCommitModels.DiffCommitInfo diffCommit : diffCommits) {
            String diffText = diffCommit.diffText();
            if (diffText == null || diffText.isBlank()) {
                continue;
            }
            if (!diffTextBuilder.isEmpty()) {
                diffTextBuilder.append("\n");
            }
            diffTextBuilder.append(diffText.strip());
        }
        return diffTextBuilder.toString().trim();
    }

    @NotNull
    private ChangelogCommitDiffBuilder.DiffPayload buildPayloadFromUnifiedDiff(@NotNull String diffText) {
        List<dev.dong4j.zeka.stack.idea.plugin.changelog.model.CodeDiff> codeDiffs = UnifiedDiffParser.parseToCodeDiffs(diffText);
        return new ChangelogCommitDiffBuilder.DiffPayload(codeDiffs, Map.of(), diffText);
    }

    @NotNull
    private String buildCommitMessagePrompt(@NotNull ChangelogCommitDiffBuilder.DiffPayload payload,
                                            @Nullable String userContext,
                                            @Nullable CommitSelectionMeta selectionMeta) {
        String recentCommitsText = gitService.buildRecentCommitMessagesText(RECENT_COMMITS_LIMIT);
        String branch = gitService.getCurrentBranch();
        boolean isGitRepository = gitService.isGitRepository();
        return promptBuilder.buildCommitMessagePrompt(payload,
                                                     recentCommitsText,
                                                     userContext,
                                                     branch,
                                                     isGitRepository,
                                                     selectionMeta);
    }

    /**
     * 构建基于代码变更的提交消息提示词
     * <p>
     * 根据提供的代码变更集合和可选的用户上下文, 构建用于 AI 生成提交消息的提示词.
     * 首先验证是否存在实际代码变更, 若无则抛出异常. 随后获取最近提交信息, 当前分支和 Git 仓库状态, 最终组合成完整的提示词.
     *
     * @param changes     代码变更集合, 不能为空
     * @param userContext 用户输入的上下文说明, 可为空
     * @throws Exception 当代码变更集合中无实际代码变更时抛出, 提示用户未进行有效修改
     */
    private @NotNull String buildPrompt(@NotNull Collection<Change> changes, @Nullable String userContext) throws Exception {
        ChangelogCommitDiffBuilder.DiffPayload payload = diffBuilder.buildPayload(changes);
        if (payload.codeDiffs().isEmpty()) {
            throw new Exception(ChangelogBundle.message("commit.no.changes"));
        }
        return buildCommitMessagePrompt(payload, userContext, null);
    }

    /**
     * 生成并保存 CHANGELOG.md 文件
     * <p>
     * 根据提供的提交哈希列表生成变更日志内容, 并将其保存到项目根目录下的 CHANGELOG.md 文件中.
     * 如果文件已存在, 则更新文件内容; 如果不存在, 则创建新文件.
     *
     * @param project      项目对象
     * @param commitHashes 提交记录的 hash 列表
     * @return 生成的变更日志内容
     * @throws Exception 当生成或保存过程中发生错误时抛出
     */
    public @NotNull String generateAndSaveChangelogFile(@NotNull Project project, @NotNull List<String> commitHashes) throws Exception {
        return generateAndSaveChangelogFile(project, commitHashes, StatisticsUserAction.UNKNOWN);
    }

    public @NotNull String generateAndSaveChangelogFile(@NotNull Project project,
                                                        @NotNull List<String> commitHashes,
                                                        @NotNull StatisticsUserAction userAction) throws Exception {
        // 生成变更日志内容
        String changelogContent = generateChangelog(commitHashes, userAction);

        // 保存到文件
        saveChangelogToFile(project, changelogContent);

        return changelogContent;
    }

    /**
     * 获取项目对象
     * <p>
     * 返回当前 ChangelogService 关联的 Project 对象
     *
     * @return 项目对象
     */
    public @NotNull Project getProject() {
        return gitService.project;
    }

    /**
     * 保存生成的变更日志内容到 CHANGELOG.md 文件
     * <p>
     * 该方法负责将生成的变更日志内容保存到项目根目录下的 CHANGELOG.md 文件中
     * 如果文件已存在, 则将新内容添加到文件开头; 如果不存在, 则创建新文件.
     *
     * @param project 项目对象
     * @param content 生成的变更日志内容
     * @throws IOException 当保存文件时发生 I/O 错误
     */
    public void saveChangelogToFile(@NotNull Project project, @NotNull String content) throws IOException {
        if (!SettingsState.getInstance().generateChangelogFile) {
            return;
        }

        String basePath = project.getBasePath();
        if (basePath == null) {
            throw new IOException(ChangelogBundle.message("error.project.path.not.found"));
        }

        Path changelogPath = Paths.get(basePath, "CHANGELOG.md");
        File changelogFile = changelogPath.toFile();

        // 检查文件是否存在
        if (changelogFile.exists()) {
            // 读取现有内容
            String existingContent = Files.readString(changelogPath);

            // 合并内容：新内容放在开头，现有内容放在后面
            // 添加一个空行分隔新旧内容
            String mergedContent = content + "\n\n" + existingContent;

            // 写入合并后的内容
            Files.writeString(changelogPath, mergedContent);
        } else {
            // 文件不存在，创建并写入内容
            Files.createFile(changelogPath);
            Files.writeString(changelogPath, content);
        }

        // 刷新 VFS 以确保 IDE 能立即看到文件变化
        VirtualFile virtualFile = VfsUtil.findFileByIoFile(changelogFile, true);
        if (virtualFile != null) {
            virtualFile.refresh(false, true);
        }
    }
}
