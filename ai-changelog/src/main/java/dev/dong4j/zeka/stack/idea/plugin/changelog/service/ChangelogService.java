package dev.dong4j.zeka.stack.idea.plugin.changelog.service;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.changes.Change;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import dev.dong4j.zeka.stack.idea.plugin.changelog.model.CodeDiff;
import dev.dong4j.zeka.stack.idea.plugin.changelog.settings.SettingsState;
import dev.dong4j.zeka.stack.idea.plugin.changelog.util.CodeDiffUtil;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIChatRequest;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIServiceException;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.service.AIService;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.service.AIServiceImpl;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;

/**
 * Changelog 生成服务
 * <p>
 * 负责从 Git 提交记录生成 Changelog
 */
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
     */
    public static ChangelogService getInstance(@NotNull Project project) {
        return project.getService(ChangelogService.class);
    }

    /**
     * 从选中的提交记录生成 Changelog
     *
     * @param commitHashes 提交记录的 hash 列表
     * @return 生成的 Changelog 内容（Markdown 格式）
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
     * 从选中的提交记录生成工作日报
     *
     * @param commitHashes 提交记录的 hash 列表
     * @return 生成的工作日报内容（Markdown 格式）
     */
    @NotNull
    public String generateDailyReport(@NotNull List<String> commitHashes) throws Exception {
        List<CommitInfo> commits = readCommits(commitHashes);
        String prompt = buildDailyReportPrompt(commits);
        return callAIService(prompt);
    }

    /**
     * 从选中的提交记录生成工作周报
     *
     * @param commitHashes 提交记录的 hash 列表
     * @return 生成的工作周报内容（Markdown 格式）
     */
    @NotNull
    public String generateWeeklyReport(@NotNull List<String> commitHashes) throws Exception {
        List<CommitInfo> commits = readCommits(commitHashes);
        String prompt = buildWeeklyReportPrompt(commits);
        return callAIService(prompt);
    }

    /**
     * 读取提交记录
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

    /**
     * 构建提交记录文本
     *
     * @param commits 提交记录列表
     * @return 格式化后的提交记录文本
     */
    @NotNull
    private String buildCommitsText(@NotNull List<CommitInfo> commits) {
        StringBuilder commitsText = new StringBuilder();
        for (CommitInfo commit : commits) {
            commitsText.append("- ").append(commit.shortMessage).append("\n");
        }
        return commitsText.toString().trim();
    }

    /**
     * 格式化当前日期
     *
     * @return 格式化后的日期字符串（yyyy-MM-dd）
     */
    @NotNull
    private String formatCurrentDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        return dateFormat.format(new Date());
    }

    /**
     * 计算并格式化周报日期范围
     *
     * @return 格式化后的日期范围字符串（yyyy-MM-dd 至 yyyy-MM-dd）
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
     */
    @NotNull
    private String buildPrompt(@NotNull List<CommitInfo> commits) {
        SettingsState settings = SettingsState.getInstance();
        String template = settings.changelogTemplate;
        String commitsText = buildCommitsText(commits);
        String date = formatCurrentDate();

        return template
            .replace("{version}", "v1.0.0")
            .replace("{date}", date)
            .replace("{commits}", commitsText);
    }

    /**
     * 组装日报 prompt
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
     * 使用 AIService API 生成内容，参考 TaskExecutor#processTask 的实现。
     *
     * @param userPrompt 用户提示词（已组装好的 prompt）
     * @return 生成的 Changelog 内容
     * @throws AIServiceException 当 AI 服务调用失败时抛出
     */
    @NotNull
    private String callAIService(@NotNull String userPrompt) throws AIServiceException {
        SettingsState settings = SettingsState.getInstance();

        // 获取当前配置的供应商
        AIProviderConfig config = settings.providerConfig;
        if (config == null) {
            throw new AIServiceException("AI provider not configured. Please configure in Settings → Tools → AI Changelog");
        }

        // 获取系统提示词
        String systemPrompt = settings.systemPrompt;
        if (systemPrompt == null || systemPrompt.trim().isEmpty()) {
            // 使用默认系统提示词
            systemPrompt = SettingsState.getDefaultSystemPrompt();
        }

        // 创建 AI 聊天请求
        AIChatRequest request = new AIChatRequest(systemPrompt, userPrompt);

        // 获取 AIService 实例
        AIService aiService = AIServiceImpl.getInstance();

        // 使用 AIService API 生成内容
        // listener 参数传 null，因为 changelog 插件可能不需要详细的响应监听
        String result = aiService.generateContent(project, request, config, null);

        // 检查结果是否为空
        if (result.trim().isEmpty()) {
            throw new AIServiceException("AI service returned empty result");
        }

        return result;
    }

    /**
     * 基于代码变更（diff）生成提交记录
     * <p>
     * 根据代码的实际改动生成提交记录，而不是依赖提交信息。
     *
     * @param changes 代码变更集合
     * @return 生成的提交记录内容
     * @throws AIServiceException 当 AI 服务调用失败时抛出
     */
    @NotNull
    public String generateCommitMessageFromDiff(@NotNull Collection<Change> changes) throws AIServiceException {
        // 1. 提取代码变更信息
        List<CodeDiff> codeDiffs = CodeDiffUtil.extractCodeDiffs(changes);

        if (codeDiffs.isEmpty()) {
            throw new AIServiceException("No code changes found");
        }

        // 2. 构建 prompt
        String prompt = buildPromptFromCodeDiff(codeDiffs);

        // 3. 调用 AI 服务生成提交记录
        return callAIServiceForCommitMessage(prompt);
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

        // 构建代码变更文本
        StringBuilder codeDiffsText = new StringBuilder();
        for (CodeDiff diff : codeDiffs) {
            codeDiffsText.append("文件: ").append(diff.filePath).append("\n");
            codeDiffsText.append("变更类型: ").append(diff.changeType.name()).append("\n");
            codeDiffsText.append("新增行数: ").append(diff.addedLines).append("\n");
            codeDiffsText.append("删除行数: ").append(diff.deletedLines).append("\n");
            if (diff.diffContent != null && !diff.diffContent.isEmpty()) {
                codeDiffsText.append("变更内容:\n");
                codeDiffsText.append(diff.diffContent).append("\n");
            }
            codeDiffsText.append("\n");
        }

        // 替换模板变量
        return template.replace("{codeDiffs}", codeDiffsText.toString().trim());
    }

    /**
     * 调用 AI 服务生成提交记录
     *
     * @param userPrompt 用户提示词
     * @return 生成的提交记录
     * @throws AIServiceException 当 AI 服务调用失败时抛出
     */
    @NotNull
    private String callAIServiceForCommitMessage(@NotNull String userPrompt) throws AIServiceException {
        SettingsState settings = SettingsState.getInstance();

        // 获取当前配置的供应商
        AIProviderConfig config = settings.providerConfig;
        if (config == null) {
            throw new AIServiceException("AI provider not configured. Please configure in Settings → Tools → AI Changelog");
        }

        // 获取系统提示词（使用专门的提交记录生成提示词）
        String systemPrompt = """
            你是一位经验丰富的代码审查专家和技术文档编写者。
            你的任务是根据代码的实际改动（diff）生成准确、简洁的提交记录（commit message）。
            
            你需要：
            1. 分析代码变更的实际内容，而不是依赖提交记录
            2. 识别代码变更的类型（新功能、Bug 修复、重构等）
            3. 生成准确、简洁的提交记录，符合常见的提交记录规范
            4. 忽略无意义的变更（如格式化、空白字符等）
            
            重要要求：
            - 输出的内容不要使用 markdown 代码块包裹（如 ```markdown）
            - 直接输出提交记录内容，不要添加任何代码块标记
            """;

        // 创建 AI 聊天请求
        AIChatRequest request = new AIChatRequest(systemPrompt, userPrompt);

        // 获取 AIService 实例
        AIService aiService = AIServiceImpl.getInstance();

        // 使用 AIService API 生成内容
        String result = aiService.generateContent(project, request, config, null);

        // 检查结果是否为空
        if (result.trim().isEmpty()) {
            throw new AIServiceException("AI service returned empty result");
        }

        return result.trim();
    }

    /**
     * 提交信息
     */
    private record CommitInfo(String hash, String shortMessage, String fullMessage, Date date, String author) {
    }
}
