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
        StringBuilder prompt = new StringBuilder();

        // Instruction
        prompt.append("Please generate a release changelog based on the following Git commits.\n\n");
        prompt.append("Requirements:\n");
        prompt.append("1. Classify the commits into the following categories:\n");
        prompt.append("   - Features\n");
        prompt.append("   - Bug Fixes\n");
        prompt.append("   - Refactors\n");
        prompt.append("   - Documentation\n");
        prompt.append("   - Chores / Others\n");
        prompt.append("2. Each entry should be rewritten as a concise human-readable description.\n");
        prompt.append("3. Remove meaningless or trivial commits (e.g., \"update code\", \"merge branch\").\n");
        prompt.append("4. Format the output strictly as Markdown:\n");
        prompt.append("   - Use level-2 heading for version and date\n");
        prompt.append("   - Use level-3 heading for each category\n");
        prompt.append("5. Keep sentences short, objective, and technical.\n");
        prompt.append("6. Do not include explanations or notes outside of Markdown.\n\n");

        // Context: commits
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        prompt.append("Version: v1.0.0\n");
        prompt.append("Date: ").append(dateFormat.format(new Date())).append("\n");
        prompt.append("Commits:\n\n");

        for (CommitInfo commit : commits) {
            prompt.append("- ").append(commit.shortMessage).append("\n");
        }

        return prompt.toString();
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

        // System prompt
        String systemPrompt = """
            You are an experienced software release manager and technical writer.
            Your goal is to generate clear, structured, and concise changelogs for software projects based on Git commit messages.
            You always output well-formatted Markdown with consistent sections.
            
            """;

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
