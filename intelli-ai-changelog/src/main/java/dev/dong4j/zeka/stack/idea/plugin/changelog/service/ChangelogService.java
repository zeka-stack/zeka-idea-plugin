package dev.dong4j.zeka.stack.idea.plugin.changelog.service;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.changes.Change;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

import dev.dong4j.zeka.stack.idea.plugin.changelog.util.ChangelogBundle;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIStreamResponseListener;

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
    private static final int PROMPT_LOG_MAX_LENGTH = Integer.MAX_VALUE;

    /** 项目对象, 用于表示当前操作所关联的项目信息 */
    private final Project project;
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
        this.project = project;
        this.gitService = new ChangelogGitService(project);
        this.promptBuilder = new ChangelogPromptBuilder(project, RECENT_COMMITS_LIMIT);
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
        List<ChangelogCommitModels.CommitInfo> commits = gitService.readCommits(commitHashes);
        String prompt = promptBuilder.buildChangelogPrompt(commits);
        return aiExecutor.callChangelog(prompt);
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
        List<ChangelogCommitModels.CommitInfo> commits = gitService.readCommits(commitHashes);
        String prompt = promptBuilder.buildChangelogPrompt(commits);
        return aiExecutor.callChangelogStream(prompt, listener);
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
        List<ChangelogCommitModels.CommitInfo> commits = gitService.readCommits(commitHashes);
        String prompt = promptBuilder.buildDailyReportPrompt(commits);
        return aiExecutor.callChangelog(prompt);
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
        List<ChangelogCommitModels.CommitInfo> commits = gitService.readCommits(commitHashes);
        String prompt = promptBuilder.buildDailyReportPrompt(commits);
        return aiExecutor.callChangelogStream(prompt, listener);
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
        List<ChangelogCommitModels.CommitInfo> commits = gitService.readCommits(commitHashes);
        String prompt = promptBuilder.buildWeeklyReportPrompt(commits);
        return aiExecutor.callChangelog(prompt);
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
        List<ChangelogCommitModels.CommitInfo> commits = gitService.readCommits(commitHashes);
        String prompt = promptBuilder.buildWeeklyReportPrompt(commits);
        return aiExecutor.callChangelogStream(prompt, listener);
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
        generateReleaseLogByAiStream(gitRoot, range, null, listener);
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
        List<ChangelogCommitModels.CommitInfo> commits = gitService.readCommitsFromRange(gitRoot, range);
        String prompt = promptBuilder.buildReleaseLogPrompt(commits, promptTemplate);
        aiExecutor.callChangelogStream(prompt, listener);
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
        List<ChangelogCommitModels.DiffCommitInfo> diffCommits = gitService.readCommitDiffs(commitHashes);
        String prompt = promptBuilder.buildDiffChangelogPrompt(diffCommits);
        return aiExecutor.callChangelog(prompt);
    }

    /**
     * 基于 Git diff 生成变更日志 (流式)
     *
     * @param commitHashes 提交哈希列表
     * @param listener     流式监听器, 用于接收生成过程中的数据流
     * @return 生成的变更日志内容 (Markdown 格式)
     * @throws Exception 当生成过程中发生错误时抛出异常
     */
    @NotNull
    public String generateChangelogFromDiffStream(@NotNull List<String> commitHashes,
                                                  @NotNull AIStreamResponseListener listener) throws Exception {
        List<ChangelogCommitModels.DiffCommitInfo> diffCommits = gitService.readCommitDiffs(commitHashes);
        String prompt = promptBuilder.buildDiffChangelogPrompt(diffCommits);
        return aiExecutor.callChangelogStream(prompt, listener);
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
        return generateCommitMessageFromDiff(changes, null);
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
        ChangelogCommitDiffBuilder.DiffPayload payload = diffBuilder.buildPayload(changes);
        if (payload.codeDiffs().isEmpty()) {
            throw new Exception(ChangelogBundle.message("commit.no.changes"));
        }

        String recentCommitsText = gitService.buildRecentCommitMessagesText(RECENT_COMMITS_LIMIT);
        String branch = gitService.getCurrentBranch();
        boolean isGitRepository = gitService.isGitRepository();
        String prompt = promptBuilder.buildCommitMessagePrompt(payload, recentCommitsText, userContext, branch, isGitRepository);
        return aiExecutor.callCommitMessage(prompt);
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
        return generateCommitMessageFromDiffStream(changes, listener, null);
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
        ChangelogCommitDiffBuilder.DiffPayload payload = diffBuilder.buildPayload(changes);
        if (payload.codeDiffs().isEmpty()) {
            throw new Exception(ChangelogBundle.message("commit.no.changes"));
        }
        String recentCommitsText = gitService.buildRecentCommitMessagesText(RECENT_COMMITS_LIMIT);
        String branch = gitService.getCurrentBranch();
        boolean isGitRepository = gitService.isGitRepository();
        String prompt = promptBuilder.buildCommitMessagePrompt(payload, recentCommitsText, userContext, branch, isGitRepository);
        return aiExecutor.callCommitMessageStream(prompt, listener);
    }
}
