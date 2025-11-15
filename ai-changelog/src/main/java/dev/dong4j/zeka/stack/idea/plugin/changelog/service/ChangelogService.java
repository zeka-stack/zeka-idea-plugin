package dev.dong4j.zeka.stack.idea.plugin.changelog.service;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;

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
import java.util.Date;
import java.util.List;

import dev.dong4j.zeka.stack.idea.plugin.changelog.settings.SettingsState;
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

    private final Project project;

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
    private List<CommitInfo> readCommits(@NotNull List<String> commitHashes) throws IOException {
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
     * 组装 prompt
     */
    @NotNull
    private String buildPrompt(@NotNull List<CommitInfo> commits) {
        SettingsState settings = SettingsState.getInstance();
        String template = settings.changelogTemplate;

        // 构建提交记录列表
        StringBuilder commitsText = new StringBuilder();
        for (CommitInfo commit : commits) {
            commitsText.append("- ").append(commit.shortMessage).append("\n");
        }

        // 替换模板变量
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        return template
            .replace("{version}", "v1.0.0")
            .replace("{date}", dateFormat.format(new Date()))
            .replace("{commits}", commitsText.toString().trim());
    }

    /**
     * 组装日报 prompt
     */
    @NotNull
    private String buildDailyReportPrompt(@NotNull List<CommitInfo> commits) {
        SettingsState settings = SettingsState.getInstance();
        String template = settings.dailyReportTemplate;

        StringBuilder commitsText = new StringBuilder();
        for (CommitInfo commit : commits) {
            commitsText.append("- ").append(commit.shortMessage).append("\n");
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        return template
            .replace("{date}", dateFormat.format(new Date()))
            .replace("{commits}", commitsText.toString().trim());
    }

    /**
     * 组装周报 prompt
     */
    @NotNull
    private String buildWeeklyReportPrompt(@NotNull List<CommitInfo> commits) {
        SettingsState settings = SettingsState.getInstance();
        String template = settings.weeklyReportTemplate;

        StringBuilder commitsText = new StringBuilder();
        for (CommitInfo commit : commits) {
            commitsText.append("- ").append(commit.shortMessage).append("\n");
        }

        // 计算日期范围
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.MONDAY);
        String weekStart = dateFormat.format(cal.getTime());
        cal.add(java.util.Calendar.DAY_OF_WEEK, 6);
        String weekEnd = dateFormat.format(cal.getTime());
        String dateRange = weekStart + " 至 " + weekEnd;

        return template
            .replace("{dateRange}", dateRange)
            .replace("{commits}", commitsText.toString().trim());
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
     * 提交信息
     */
    private record CommitInfo(String hash, String shortMessage, String fullMessage, Date date, String author) {
    }
}
