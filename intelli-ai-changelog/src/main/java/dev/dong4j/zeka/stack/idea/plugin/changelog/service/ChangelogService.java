package dev.dong4j.zeka.stack.idea.plugin.changelog.service;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.changes.Change;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.EmptyTreeIterator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import dev.dong4j.zeka.stack.idea.plugin.changelog.ai.ChangelogAIResponseListener;
import dev.dong4j.zeka.stack.idea.plugin.changelog.ai.ChangelogAIStreamResponseListener;
import dev.dong4j.zeka.stack.idea.plugin.changelog.model.CodeDiff;
import dev.dong4j.zeka.stack.idea.plugin.changelog.settings.SettingsState;
import dev.dong4j.zeka.stack.idea.plugin.changelog.util.ChangelogBundle;
import dev.dong4j.zeka.stack.idea.plugin.changelog.util.CodeDiffUtil;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIChatRequest;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIResponseListener;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIServiceException;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIStreamResponseListener;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.service.AIService;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.service.AIServiceImpl;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderSettings;
import lombok.extern.slf4j.Slf4j;

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
@Slf4j
@Service(Service.Level.PROJECT)
public final class ChangelogService {

    /** 项目对象, 用于表示当前操作所关联的项目信息 */
    private final Project project;

    /**
     * 初始化 ChangelogService 实例
     * <p>
     * 通过传入的 Project 对象进行初始化, 将 project 赋值给成员变量
     *
     * @param project 项目对象, 不能为空
     */
    public ChangelogService(@NotNull Project project) {
        this.project = project;
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
     *
     * @param commitHashes 提交记录的 hash 列表
     * @return 生成的 Changelog 内容 (Markdown 格式)
     * @throws Exception 当 AI 服务调用失败时抛出, 包含友好的错误消息
     */
    @NotNull
    public String generateChangelog(@NotNull List<String> commitHashes) throws Exception {
        // 1. 读取提交记录
        List<CommitInfo> commits = readCommits(commitHashes);

        // 2. 组装 prompt
        String prompt = buildPrompt(commits);

        // 3. 调用 AI 服务生成 Changelog
        return callAIService(prompt);
    }

    /**
     * 从选中的提交记录生成 Changelog（流式）
     *
     * @param commitHashes 提交记录的 hash 列表
     * @param listener     流式监听器
     * @return 生成的 Changelog 内容
     * @throws Exception 当 AI 服务调用失败时抛出, 包含友好的错误消息
     */
    @NotNull
    public String generateChangelogStream(@NotNull List<String> commitHashes,
                                          @NotNull AIStreamResponseListener listener) throws Exception {
        List<CommitInfo> commits = readCommits(commitHashes);
        String prompt = buildPrompt(commits);
        return callAIServiceStreamForChangelog(prompt, listener);
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
        List<CommitInfo> commits = readCommits(commitHashes);
        String prompt = buildDailyReportPrompt(commits);
        return callAIService(prompt);
    }

    /**
     * 从选中的提交记录生成工作日报（流式）
     *
     * @param commitHashes 提交记录的哈希列表
     * @param listener     流式监听器
     * @return 生成的工作日报内容
     * @throws Exception 当生成日报内容失败时抛出
     */
    @NotNull
    public String generateDailyReportStream(@NotNull List<String> commitHashes,
                                            @NotNull AIStreamResponseListener listener) throws Exception {
        List<CommitInfo> commits = readCommits(commitHashes);
        String prompt = buildDailyReportPrompt(commits);
        return callAIServiceStreamForChangelog(prompt, listener);
    }

    /**
     * 从选中的提交记录生成工作周报
     *
     * @param commitHashes 提交记录的 hash 列表
     * @return 生成的工作周报内容 (Markdown 格式)
     * @throws Exception 当读取提交记录或调用 AI 服务失败时抛出
     */
    @NotNull
    public String generateWeeklyReport(@NotNull List<String> commitHashes) throws Exception {
        List<CommitInfo> commits = readCommits(commitHashes);
        String prompt = buildWeeklyReportPrompt(commits);
        return callAIService(prompt);
    }

    /**
     * 从选中的提交记录生成工作周报（流式）
     *
     * @param commitHashes 提交记录的 hash 列表
     * @param listener     流式监听器
     * @return 生成的工作周报内容
     * @throws Exception 当读取提交记录或调用 AI 服务失败时抛出
     */
    @NotNull
    public String generateWeeklyReportStream(@NotNull List<String> commitHashes,
                                             @NotNull AIStreamResponseListener listener) throws Exception {
        List<CommitInfo> commits = readCommits(commitHashes);
        String prompt = buildWeeklyReportPrompt(commits);
        return callAIServiceStreamForChangelog(prompt, listener);
    }

    /**
     * 基于 Git 范围生成 Release Log（AI）
     *
     * @param gitRoot  Git 仓库根目录
     * @param range    提交范围（例如 tag..HEAD），可为空
     * @param listener 流式监听器
     * @throws Exception 当读取提交记录或调用 AI 服务失败时抛出
     */
    public void generateReleaseLogByAiStream(@NotNull Path gitRoot,
                                             @Nullable String range,
                                             @NotNull AIStreamResponseListener listener) throws Exception {
        generateReleaseLogByAiStream(gitRoot, range, null, listener);
    }

    /**
     * 基于 Git 范围生成 Release Log（AI，可覆盖提示词）
     *
     * @param gitRoot        Git 仓库根目录
     * @param range          提交范围（例如 tag..HEAD），可为空
     * @param promptTemplate 提示词模板，可为空
     * @param listener       流式监听器
     * @throws Exception 当读取提交记录或调用 AI 服务失败时抛出
     */
    public void generateReleaseLogByAiStream(@NotNull Path gitRoot,
                                             @Nullable String range,
                                             @Nullable String promptTemplate,
                                             @NotNull AIStreamResponseListener listener) throws Exception {
        List<CommitInfo> commits = readCommitsFromRange(gitRoot, range);
        String prompt = buildReleaseLogPrompt(commits, promptTemplate);
        callAIServiceStreamForChangelog(prompt, listener);
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
        List<DiffCommitInfo> diffCommits = readCommitDiffs(commitHashes);
        String prompt = buildDiffChangelogPrompt(diffCommits);
        return callAIService(prompt);
    }

    /**
     * 基于 Git diff 生成变更日志（流式）
     *
     * @param commitHashes 提交哈希列表
     * @param listener     流式监听器
     * @return 生成的变更日志内容
     * @throws Exception 当生成过程中发生错误
     */
    @NotNull
    public String generateChangelogFromDiffStream(@NotNull List<String> commitHashes,
                                                  @NotNull AIStreamResponseListener listener) throws Exception {
        List<DiffCommitInfo> diffCommits = readCommitDiffs(commitHashes);
        String prompt = buildDiffChangelogPrompt(diffCommits);
        return callAIServiceStreamForChangelog(prompt, listener);
    }

    /**
     * 读取提交记录
     * <p>
     * 从指定的提交哈希列表中读取提交信息, 并返回包含提交详情的 CommitInfo 列表.
     *
     * @param commitHashes 提交记录的哈希列表, 不能为空
     * @return 包含提交详情的 CommitInfo 列表
     */
    @NotNull
    private List<CommitInfo> readCommits(@NotNull List<String> commitHashes) {
        List<CommitInfo> commits = new ArrayList<>();

        Repository repository = getRepository();

        try (repository) {
            if (repository == null) {
                return commits;
            }
            try (Git git = new Git(repository)) {
                for (String hash : commitHashes) {
                    try {
                        ObjectId commitId = repository.resolve(hash);
                        if (commitId != null) {
                            RevCommit commit = git.log().add(commitId).setMaxCount(1).call().iterator().next();
                            commits.add(new CommitInfo(
                                commit.getName(),
                                commit.getShortMessage(),
                                commit.getFullMessage(),
                                new Date(commit.getCommitTime() * 1000L),
                                commit.getAuthorIdent().getName()
                            ));
                        }
                    } catch (Exception e) {
                        // 忽略无法解析的提交
                    }
                }
            }
        }

        return commits;
    }

    /**
     * 获取 Git 仓库
     * <p>
     * 通过项目的基路径查找并返回 Git 仓库对象. 如果基路径为空或 Git 目录不存在, 则返回 null.
     *
     * @return Git 仓库对象, 如果找不到则返回 null
     */
    @Nullable
    private Repository getRepository() {
        String basePath = project.getBasePath();
        if (basePath == null) {
            return null;
        }

        File gitDir = new File(basePath, ".git");
        if (!gitDir.exists()) {
            return null;
        }

        try {
            return new FileRepositoryBuilder()
                .setGitDir(gitDir)
                .build();
        } catch (IOException e) {
            return null;
        }
    }

    @Nullable
    private Repository getRepository(@NotNull Path gitRoot) {
        File gitDir = gitRoot.resolve(".git").toFile();
        if (!gitDir.exists()) {
            return null;
        }
        try {
            return new FileRepositoryBuilder()
                .setGitDir(gitDir)
                .build();
        } catch (IOException e) {
            return null;
        }
    }

    @NotNull
    private List<CommitInfo> readCommitsFromRange(@NotNull Path gitRoot, @Nullable String range) {
        List<CommitInfo> commits = new ArrayList<>();
        Repository repository = getRepository(gitRoot);

        try (repository) {
            if (repository == null) {
                return commits;
            }
            try (Git git = new Git(repository)) {
                Iterable<RevCommit> logIterator;
                if (range != null && !range.isBlank() && range.contains("..")) {
                    String[] parts = range.split("\\.\\.", 2);
                    ObjectId from = repository.resolve(parts[0]);
                    ObjectId to = repository.resolve(parts[1]);
                    if (from != null && to != null) {
                        logIterator = git.log().addRange(from, to).call();
                    } else {
                        logIterator = git.log().call();
                    }
                } else {
                    logIterator = git.log().call();
                }
                for (RevCommit commit : logIterator) {
                    commits.add(new CommitInfo(
                        commit.getName(),
                        commit.getShortMessage(),
                        commit.getFullMessage(),
                        new Date(commit.getCommitTime() * 1000L),
                        commit.getAuthorIdent().getName()
                    ));
                }
            }
        } catch (Exception ignored) {
            // 忽略无法解析的提交
        }

        return commits;
    }

    /**
     * 构建提交记录文本
     * <p>
     * 按照提交日期对提交记录进行分组, 每个日期一个分组.
     * 分组后的格式便于 AI 理解并按日期生成变更日志.
     *
     * @param commits 提交记录列表
     * @return 格式化后的提交记录文本 (已按日期分组)
     */
    @NotNull
    private String buildCommitsText(@NotNull List<CommitInfo> commits) {
        if (commits.isEmpty()) {
            return "";
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        // 按日期分组提交记录
        Map<String, List<CommitInfo>> commitsByDate = new LinkedHashMap<>();
        for (CommitInfo commit : commits) {
            String dateStr = dateFormat.format(commit.date);
            commitsByDate.computeIfAbsent(dateStr, k -> new ArrayList<>()).add(commit);
        }

        // 构建分组后的文本
        StringBuilder commitsText = new StringBuilder();
        for (Map.Entry<String, List<CommitInfo>> entry : commitsByDate.entrySet()) {
            String dateStr = entry.getKey();
            List<CommitInfo> dateCommits = entry.getValue();

            // 添加日期分组标题
            commitsText.append("### ").append(dateStr).append("\n\n");

            // 添加该日期下的所有提交记录
            for (CommitInfo commit : dateCommits) {
                commitsText.append("- ").append(commit.shortMessage).append("\n");
            }

            // 日期分组之间添加空行
            commitsText.append("\n");
        }

        return commitsText.toString().trim();
    }

    @NotNull
    private String buildDiffChangelogPrompt(@NotNull List<DiffCommitInfo> diffCommits) {
        SettingsState settings = SettingsState.getInstance();
        String template = settings.changelogTemplate;
        String diffText = buildDiffCommitsText(diffCommits);
        return template.replace("{commits}", diffText);
    }

    @NotNull
    private String buildDiffCommitsText(@NotNull List<DiffCommitInfo> diffCommits) {
        if (diffCommits.isEmpty()) {
            return "";
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Map<String, List<DiffCommitInfo>> commitsByDate = new LinkedHashMap<>();
        for (DiffCommitInfo commit : diffCommits) {
            String dateStr = dateFormat.format(commit.date);
            commitsByDate.computeIfAbsent(dateStr, k -> new ArrayList<>()).add(commit);
        }

        StringBuilder commitsText = new StringBuilder();
        for (Map.Entry<String, List<DiffCommitInfo>> entry : commitsByDate.entrySet()) {
            String dateStr = entry.getKey();
            commitsText.append("### ").append(dateStr).append("\n\n");
            for (DiffCommitInfo commit : entry.getValue()) {
                commitsText.append("- 提交 ").append(commit.hash, 0, Math.min(8, commit.hash.length()))
                    .append(":\n");
                commitsText.append(commit.diffText).append("\n");
            }
            commitsText.append("\n");
        }

        return commitsText.toString().trim();
    }

    /**
     * 格式化当前日期
     *
     * @return 格式化后的日期字符串 (yyyy-MM-dd)
     */
    @NotNull
    private String formatCurrentDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        return dateFormat.format(new Date());
    }

    /**
     * 计算并格式化周报日期范围
     * <p>
     * 该方法计算当前周的周一和周日的日期, 并返回格式化的日期范围字符串.
     *
     * @return 格式化后的日期范围字符串 (yyyy-MM-dd 至 yyyy-MM-dd)
     */
    @NotNull
    private String formatWeeklyDateRange() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.MONDAY);
        String weekStart = dateFormat.format(cal.getTime());
        cal.add(java.util.Calendar.DAY_OF_WEEK, 6);
        String weekEnd = dateFormat.format(cal.getTime());
        return weekStart + " 至 " + weekEnd;
    }

    /**
     * 组装 prompt
     * <p>
     * 根据提交记录列表和模板生成用于 AI 服务的 prompt.
     *
     * @param commits 提交记录列表
     * @return 组装好的 prompt 字符串
     */
    @NotNull
    private String buildPrompt(@NotNull List<CommitInfo> commits) {
        SettingsState settings = SettingsState.getInstance();
        String template = settings.changelogTemplate;
        String commitsText = buildCommitsText(commits);

        return template
            .replace("{version}", "v1.0.0")
            .replace("{commits}", commitsText);
    }

    /**
     * 组装 Release Log prompt（AI）
     *
     * @param commits 提交记录列表
     * @return 组装好的 Release Log prompt
     */
    @NotNull
    private String buildReleaseLogPrompt(@NotNull List<CommitInfo> commits, @Nullable String promptTemplate) {
        SettingsState settings = SettingsState.getInstance();
        String template = promptTemplate != null && !promptTemplate.isBlank()
                          ? promptTemplate
                          : settings.aiReleaseLogPrompt;
        String commitsText = buildCommitsText(commits);
        String date = formatCurrentDate();

        return template
            .replace("{version}", "Unreleased")
            .replace("{date}", date)
            .replace("{commits}", commitsText);
    }

    /**
     * 组装日报 prompt
     * <p>
     * 根据给定的提交记录列表和模板生成日报的 prompt.
     *
     * @param commits 提交记录列表, 包含每日的提交信息
     * @return 生成的日报 prompt 字符串
     */
    @NotNull
    private String buildDailyReportPrompt(@NotNull List<CommitInfo> commits) {
        SettingsState settings = SettingsState.getInstance();
        String template = settings.dailyReportTemplate;
        String commitsText = buildCommitsText(commits);
        String date = formatCurrentDate();

        return template
            .replace("{date}", date)
            .replace("{commits}", commitsText);
    }

    /**
     * 组装周报 prompt
     *
     * @param commits 提交记录列表
     * @return 组装好的周报 prompt 字符串
     */
    @NotNull
    private String buildWeeklyReportPrompt(@NotNull List<CommitInfo> commits) {
        SettingsState settings = SettingsState.getInstance();
        String template = settings.weeklyReportTemplate;
        String commitsText = buildCommitsText(commits);
        String dateRange = formatWeeklyDateRange();

        return template
            .replace("{dateRange}", dateRange)
            .replace("{commits}", commitsText);
    }

    /**
     * 调用 AI 服务生成 Changelog
     * <p>
     * 使用 AIService API 生成内容, 参考 TaskExecutor#processTask 的实现.
     *
     * @param userPrompt 用户提示词 (已组装好的 prompt)
     * @return 生成的 Changelog 内容
     * @throws Exception 当 AI 服务调用失败时抛出, 包含友好的错误消息
     */
    @NotNull
    private String callAIService(@NotNull String userPrompt) throws Exception {
        SettingsState settings = SettingsState.getInstance();

        // 获取当前配置的供应商
        AIProviderConfig config = settings.providerConfig;

        // 获取系统提示词
        String systemPrompt = settings.systemPrompt;
        if (systemPrompt == null || systemPrompt.trim().isEmpty()) {
            // 使用默认系统提示词
            systemPrompt = SettingsState.getDefaultSystemPrompt();
        }

        // 创建 AI 聊天请求
        AIChatRequest request = new AIChatRequest(systemPrompt, userPrompt);

        boolean verboseLogging = AIProviderSettings.getInstance().verboseLogging;
        AIService aiService = AIServiceImpl.getInstance();

        try {
            String result;
            if (verboseLogging) {
                result = callAIServiceStream(aiService, request, config);
            } else {
                AIResponseListener listener = new ChangelogAIResponseListener(project);
                result = aiService.generateContent(project, request, config, listener);
            }

            // 检查结果是否为空
            if (result.trim().isEmpty()) {
                throw new Exception(ChangelogBundle.message("error.ai.service.empty.result"));
            }

            return result;
        } catch (AIServiceException e) {
            // 捕获 AIServiceException 并转换为友好的错误消息
            String errorMessage = e.getMessage();
            if (errorMessage != null && !errorMessage.isEmpty()) {
                throw new Exception(errorMessage);
            } else {
                throw new Exception("未知错误");
            }
        }
    }

    /**
     * 使用变更日志系统提示词进行流式生成
     *
     * @param userPrompt 用户提示词
     * @param listener   流式监听器
     * @return 生成的内容
     * @throws Exception 当 AI 服务调用失败时抛出
     */
    @NotNull
    private String callAIServiceStreamForChangelog(@NotNull String userPrompt,
                                                   @NotNull AIStreamResponseListener listener) throws Exception {
        SettingsState settings = SettingsState.getInstance();
        AIProviderConfig config = settings.providerConfig;
        AIChatRequest request = buildChangelogRequest(userPrompt);
        AIService aiService = AIServiceImpl.getInstance();
        return callAIServiceStreamWithListener(aiService, request, config, listener);
    }

    /**
     * 构建变更日志类请求
     * <p> 使用设置中的系统提示词，若为空则回退到默认提示词。
     *
     * @param userPrompt 用户提示词
     * @return AIChatRequest
     */
    @NotNull
    private AIChatRequest buildChangelogRequest(@NotNull String userPrompt) {
        SettingsState settings = SettingsState.getInstance();
        String systemPrompt = settings.systemPrompt;
        if (systemPrompt == null || systemPrompt.trim().isEmpty()) {
            systemPrompt = SettingsState.getDefaultSystemPrompt();
        }
        return new AIChatRequest(systemPrompt, userPrompt);
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
        // 1. 提取代码变更信息
        List<CodeDiff> codeDiffs = CodeDiffUtil.extractCodeDiffs(changes);

        if (codeDiffs.isEmpty()) {
            throw new Exception(ChangelogBundle.message("commit.no.changes"));
        }

        // 2. 构建 prompt
        String prompt = buildPromptFromCodeDiff(codeDiffs);

        // 3. 调用 AI 服务生成提交记录
        return callAIServiceForCommitMessage(prompt);
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
        List<CodeDiff> codeDiffs = CodeDiffUtil.extractCodeDiffs(changes);
        if (codeDiffs.isEmpty()) {
            throw new Exception(ChangelogBundle.message("commit.no.changes"));
        }
        log.debug("Generating commit message from diff:\n{}", codeDiffs);
        String prompt = buildPromptFromCodeDiff(codeDiffs);
        return callAIServiceForCommitMessageStream(prompt, listener);
    }

    /**
     * 基于代码变更构建 prompt
     *
     * @param codeDiffs 代码变更列表
     * @return 构建好的 prompt
     */
    @NotNull
    private String buildPromptFromCodeDiff(@NotNull List<CodeDiff> codeDiffs) {
        SettingsState settings = SettingsState.getInstance();
        String template = settings.commitMessageTemplate;

        // 构建代码变更文本（限制长度与文件数，避免提示词过长）
        final int maxDiffChars = 10_000;
        final int maxFiles = 30;
        StringBuilder codeDiffsText = new StringBuilder();
        int fileCount = 0;
        for (CodeDiff diff : codeDiffs) {
            if (fileCount >= maxFiles || codeDiffsText.length() >= maxDiffChars) {
                break;
            }
            codeDiffsText.append("文件: ").append(diff.filePath).append("\n");
            codeDiffsText.append("变更类型: ").append(diff.changeType.name()).append("\n");
            codeDiffsText.append("新增行数: ").append(diff.addedLines).append("\n");
            codeDiffsText.append("删除行数: ").append(diff.deletedLines).append("\n");
            if (codeDiffsText.length() >= maxDiffChars) {
                codeDiffsText.setLength(maxDiffChars);
                break;
            }
            if (diff.diffContent != null && !diff.diffContent.isEmpty()) {
                codeDiffsText.append("变更内容:\n");
                int remaining = maxDiffChars - codeDiffsText.length();
                if (remaining > 0) {
                    String diffContent = diff.diffContent;
                    if (diffContent.length() > remaining) {
                        diffContent = diffContent.substring(0, remaining);
                    }
                    codeDiffsText.append(diffContent).append("\n");
                }
            }
            codeDiffsText.append("\n");
            fileCount++;
        }

        String recentCommitsText = buildRecentCommitMessagesText(3);

        String diffText = codeDiffsText.toString().trim();
        String prompt = template.replace("{codeDiffs}", diffText)
            .replace("{recentCommits}", recentCommitsText);
        if (!template.contains("{recentCommits}") && !recentCommitsText.isEmpty()) {
            // 模板未显式包含占位符时，追加最近提交记录
            prompt = prompt + "\n\n历史提交(最近3条):\n" + recentCommitsText;
        }
        return prompt;
    }

    /**
     * 调用 AI 服务生成提交记录
     *
     * @param userPrompt 用户提示词, 描述代码变更的具体内容
     * @return 生成的提交记录
     * @throws Exception 当 AI 服务调用失败时抛出, 包含友好的错误消息
     */
    @NotNull
    private String callAIServiceForCommitMessage(@NotNull String userPrompt) throws Exception {
        // 获取系统提示词（使用专门的提交记录生成提示词）
        final AIChatRequest request = getAiChatRequest(userPrompt);

        // 获取 AIService 实例
        AIService aiService = AIServiceImpl.getInstance();

        try {
            AIProviderConfig config = SettingsState.getInstance().providerConfig;
            // 提交记录采用流式调用，保证结果来源一致
            String result = callAIServiceStream(aiService, request, config);

            // 检查结果是否为空
            if (result.trim().isEmpty()) {
                throw new Exception(ChangelogBundle.message("error.ai.service.empty.result"));
            }

            return result.trim();
        } catch (AIServiceException e) {
            // 捕获 AIServiceException 并转换为友好的错误消息
            String errorMessage = e.getMessage();
            if (errorMessage != null && !errorMessage.isEmpty()) {
                throw new Exception(ChangelogBundle.message("error.ai.service.failed", errorMessage));
            } else {
                throw new Exception(ChangelogBundle.message("error.ai.service.failed",
                                                            ChangelogBundle.message("error.ai.service.unknown")));
            }
        }
    }

    /**
     * 调用 AI 服务生成内容 (流式)
     * <p> 使用流式响应监听器从 AI 服务获取生成的内容.
     *
     * @param aiService AI 服务实例
     * @param request   包含系统提示和用户提示的 AIChatRequest 对象
     * @param config    AI 服务配置
     * @return 生成的完整内容
     * @throws Exception 当 AI 服务调用失败时抛出, 包含友好的错误消息
     */
    @NotNull
    private String callAIServiceStream(@NotNull AIService aiService,
                                       @NotNull AIChatRequest request,
                                       @NotNull AIProviderConfig config) throws Exception {
        AIStreamResponseListener streamListener =
            new ChangelogAIStreamResponseListener(project, new StringBuilder(),
                                                  new CountDownLatch(1), new AtomicReference<>());
        return callAIServiceStreamWithListener(aiService, request, config, streamListener);
    }

    /**
     * 基于流式响应监听器调用 AI 服务生成提交记录
     * <p> 此方法用于在流式模式下生成提交记录, 通过监听器接收 AI 服务的逐步响应.
     *
     * @param userPrompt 用户提示词, 描述代码变更的具体内容
     * @param listener   流式响应监听器, 用于处理 AI 服务的逐步响应
     * @return 生成的提交记录内容
     * @throws Exception 当 AI 服务调用失败时抛出, 包含友好的错误消息
     */
    @NotNull
    private String callAIServiceForCommitMessageStream(@NotNull String userPrompt,
                                                       @NotNull AIStreamResponseListener listener) throws Exception {
        final AIChatRequest request = getAiChatRequest(userPrompt);
        AIService aiService = AIServiceImpl.getInstance();
        AIProviderConfig config = SettingsState.getInstance().providerConfig;
        return callAIServiceStreamWithListener(aiService, request, config, listener);
    }

    /**
     * 带监听器调用 AI 服务进行流式内容生成
     * <p> 通过指定的 AI 服务, 请求, 配置和外部监听器, 进行流式内容生成, 并处理生成结果或错误.
     *
     * @param aiService        AI 服务实例
     * @param request          包含系统提示和用户提示的 AIChatRequest 对象
     * @param config           AI 服务提供商的配置
     * @param externalListener 外部监听器, 用于接收生成过程中的事件通知
     * @return 生成的内容文本
     * @throws Exception 当 AI 服务调用失败或流式生成被中断时抛出
     */
    @NotNull
    private String callAIServiceStreamWithListener(@NotNull AIService aiService,
                                                   @NotNull AIChatRequest request,
                                                   @NotNull AIProviderConfig config,
                                                   @NotNull AIStreamResponseListener externalListener) throws Exception {
        StringBuilder buffer = new StringBuilder();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Exception> errorRef = new AtomicReference<>();
        AtomicReference<String> resultRef = new AtomicReference<>();

        AIStreamResponseListener listener = new AIStreamResponseListener() {
            /**
             * 调用外部监听器的 onStart 方法
             * <p> 此方法在 AIStreamResponseListener 的生命周期中被调用, 用于通知外部监听器开始事件
             *
             * @since 1.0
             */
            @Override
            public void onStart() {
                externalListener.onStart();
            }

            /**
             * 处理数据流中的一个数据块
             * <p> 将接收到的数据块追加到缓冲区, 并通知外部监听器处理该数据块
             *
             * @param chunk 数据块内容
             */
            @Override
            public void onChunk(@NotNull String chunk) {
                buffer.append(chunk);
                externalListener.onChunk(chunk);
            }

            /**
             * 完成处理操作
             * <p> 当所有数据处理完成后调用此方法. 设置结果引用, 通知外部监听器, 并减少计数器.
             *
             * @param fullText 完整的处理结果文本
             */
            public void onComplete(@NotNull String fullText) {
                resultRef.set(fullText);
                externalListener.onComplete(fullText);
                latch.countDown();
            }

            /**
             * 处理错误事件
             * <p> 当发生错误时调用此方法. 设置错误信息到 errorRef, 并通知外部监听器.
             * 同时减少计数器 latch 的计数.
             *
             * @param error     错误信息
             * @param exception 可能导致错误的异常对象
             */
            @Override
            public void onError(@NotNull String error, @Nullable Throwable exception) {
                errorRef.set(new Exception(error, exception));
                externalListener.onError(error, exception);
                latch.countDown();
            }
        };

        aiService.generateContentStream(project, request, config, listener);
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new Exception("流式生成被中断", e);
        }

        Exception error = errorRef.get();
        if (error != null) {
            throw error;
        }

        String result = resultRef.get();
        if (result == null || result.trim().isEmpty()) {
            result = buffer.toString();
        }
        if (result.trim().isEmpty()) {
            throw new Exception(ChangelogBundle.message("error.ai.service.empty.result"));
        }
        return result;
    }

    /**
     * 获取最近的提交信息文本
     * <p> 从项目的 Git 仓库中读取最近的提交记录, 并将其格式化为 Markdown 格式的文本.
     * 每条提交信息前加上编号, 方便阅读.
     *
     * @param limit 获取的提交记录数量限制
     * @return 格式化后的提交信息文本 (Markdown 格式)
     */
    @NotNull
    private String buildRecentCommitMessagesText(int limit) {
        List<String> commitMessages = new ArrayList<>();
        try {
            String basePathValue = project.getBasePath();
            if (basePathValue == null || basePathValue.isBlank()) {
                return "";
            }
            File basePath = new File(basePathValue);
            FileRepositoryBuilder builder = new FileRepositoryBuilder();
            try (Repository repository = builder.findGitDir(basePath).build()) {
                try (Git git = new Git(repository)) {
                    for (RevCommit commit : git.log().setMaxCount(limit).call()) {
                        String message = commit.getFullMessage();
                        if (message != null && !message.trim().isEmpty()) {
                            commitMessages.add(message.trim());
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            // 忽略获取历史提交失败的情况
        }

        if (commitMessages.isEmpty()) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        for (String message : commitMessages) {
            result.append("- ").append(message).append("\n");
        }
        return result.toString().trim();
    }

    /**
     * 读取提交记录的差异信息
     * <p> 从指定的提交哈希列表中读取提交信息, 并返回包含提交差异详情的 DiffCommitInfo 列表.
     *
     * @param commitHashes 提交记录的哈希列表, 不能为空
     * @return 包含提交差异详情的 DiffCommitInfo 列表
     */
    @NotNull
    private List<DiffCommitInfo> readCommitDiffs(@NotNull List<String> commitHashes) {
        List<DiffCommitInfo> diffCommits = new ArrayList<>();
        Repository repository = getRepository();
        if (repository == null) {
            return diffCommits;
        }

        try (repository; RevWalk revWalk = new RevWalk(repository)) {
            for (String hash : commitHashes) {
                try {
                    ObjectId commitId = repository.resolve(hash);
                    if (commitId == null) {
                        continue;
                    }
                    RevCommit commit = revWalk.parseCommit(commitId);
                    RevCommit parent = commit.getParentCount() > 0
                                       ? revWalk.parseCommit(commit.getParent(0).getId())
                                       : null;
                    String diffText = buildCommitDiffText(repository, parent, commit);
                    if (diffText.isEmpty()) {
                        diffText = "变更内容为空或无法解析。";
                    }
                    diffCommits.add(new DiffCommitInfo(
                        commit.getName(),
                        new Date(commit.getCommitTime() * 1000L),
                        diffText
                    ));
                } catch (Exception ignored) {
                    // 忽略无法解析的提交
                }
            }
        } catch (Exception ignored) {
            // 忽略仓库读取异常
        }

        return diffCommits;
    }

    /**
     * 构建提交差异文本
     * <p> 根据父提交和当前提交之间的差异生成差异文本. 如果父提交为空, 则与空树进行比较.
     *
     * @param repository 仓库对象, 不能为空
     * @param parent     父提交对象, 可以为空
     * @param commit     当前提交对象, 不能为空
     * @return 差异文本, 包含文件的增删改信息
     * @throws IOException 当读取仓库对象或解析提交树时发生 I/O 错误
     */
    @NotNull
    private String buildCommitDiffText(@NotNull Repository repository,
                                       @Nullable RevCommit parent,
                                       @NotNull RevCommit commit) throws IOException {
        java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
        try (ObjectReader reader = repository.newObjectReader();
             DiffFormatter formatter = new DiffFormatter(output)) {
            formatter.setRepository(repository);
            formatter.setDiffComparator(RawTextComparator.DEFAULT);
            formatter.setDetectRenames(true);

            AbstractTreeIterator parentIter = parent == null
                                              ? new EmptyTreeIterator()
                                              : new CanonicalTreeParser(null, reader, parent.getTree());
            CanonicalTreeParser commitIter = new CanonicalTreeParser();
            commitIter.reset(reader, commit.getTree());

            List<DiffEntry> diffs = formatter.scan(parentIter, commitIter);
            for (DiffEntry entry : diffs) {
                formatter.format(entry);
            }
            formatter.flush();
        }
        return output.toString(java.nio.charset.StandardCharsets.UTF_8).trim();
    }

    /**
     * 提供提交信息的不可变记录类
     * <p>
     * 用于封装 Git 提交的相关信息, 包括提交哈希, 简短信息, 完整信息, 提交日期和作者等字段
     *
     * @author zeka.stack.team
     * @version 1.0.0
     * @email mailto:zeka.stack@gmail.com
     * @date 2025.11.30
     * @since 1.0.0
     */
    private record CommitInfo(String hash, String shortMessage, String fullMessage, Date date, String author) {
    }

    /**
     * 差异提交信息记录类
     * <p> 用于存储和表示 Git 提交的差异信息, 包括提交哈希, 日期和差异文本.
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2025.12.31
     * @since 1.0.0
     */
    private record DiffCommitInfo(String hash, Date date, String diffText) {
    }

    /**
     * 创建 AIChatRequest 对象
     * <p> 根据给定的用户提示词和系统提示词创建 AIChatRequest 对象, 用于生成提交记录.
     *
     * @param userPrompt 用户提示词, 描述代码变更的具体内容
     * @return AIChatRequest 对象, 包含系统提示和用户提示
     */
    private static @NotNull AIChatRequest getAiChatRequest(@NotNull String userPrompt) {
        String systemPrompt = """
            你是一位经验丰富的代码审查专家和技术文档编写者。
            你的任务是根据代码的实际改动（diff）生成准确、简洁的提交记录（commit message），
            输出格式为 Conventional Commits：
            <type>(<scope>): <subject>

            <body(可选，说明动机、影响、兼容性)>

            你需要：
            1. 分析代码变更的实际内容，而不是依赖提交记录
            2. 识别代码变更的类型（新功能、Bug 修复、重构等）
            3. 优先表达设计意图 / 约束变化 / 行为变化
            4. 如果是重构，请说明“为什么现在要重构”
            5. 避免描述实现细节
            6. 忽略无意义的变更（如格式化、空白字符等）

            重要要求：
            - 只输出提交记录正文，不要解释过程或附加说明
            - 第一行是简短摘要，使用祈使语气，不要句号
            - 如需详细说明，空一行后给出正文描述
            - 避免无意义的空白行或多余的格式符号
            - 输出必须为中文（type 和 scope 使用常见英文约定）
            """;

        // 创建 AI 聊天请求
        return new AIChatRequest(systemPrompt, userPrompt);
    }
}
