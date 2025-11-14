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
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIServiceFactory;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.AIServiceProvider;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AICredentialManager;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIModelParameters;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderSettings;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIRuntimeSettings;

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
        String changelog = callAIService(prompt);

        return changelog;
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
        if (repository == null) {
            return commits;
        }

        try {
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
        } finally {
            repository.close();
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
        String prompt = template
            .replace("{version}", "v1.0.0")
            .replace("{date}", dateFormat.format(new Date()))
            .replace("{commits}", commitsText.toString().trim());

        return prompt;
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
     */
    @NotNull
    private String callAIService(@NotNull String prompt) throws Exception {
        SettingsState settings = SettingsState.getInstance();
        AIProviderSettings providerSettings = settings.providerSettings;

        // 获取当前配置
        AIProviderConfig config = providerSettings.getDefaultProviderConfig(providerSettings.providerType);
        AIModelParameters modelParams = providerSettings.modelParameters;
        AIRuntimeSettings runtimeSettings = providerSettings.runtimeSettings;

        // 获取 API Key
        AICredentialManager credentialManager = new AICredentialManager("AI Changelog", "AI_CHANGELOG_API_KEY_");
        String apiKey = credentialManager.getApiKey(config.credentialId);

        // 创建 AI 服务提供者
        AIServiceProvider provider = AIServiceFactory.createProvider(
            config,
            modelParams,
            runtimeSettings,
            null, // console logger (暂时为 null)
            false // performance mode (暂时为 false)
                                                                    );

        // 使用配置的系统提示词
        String systemPrompt = settings.systemPrompt;

        // 构建请求
        AIChatRequest request = new AIChatRequest(systemPrompt, prompt);

        // 生成内容
        return provider.generateContent(request, apiKey, null);
    }

    /**
         * 提交信息
         */
        private record CommitInfo(String hash, String shortMessage, String fullMessage, Date date, String author) {
    }
}
