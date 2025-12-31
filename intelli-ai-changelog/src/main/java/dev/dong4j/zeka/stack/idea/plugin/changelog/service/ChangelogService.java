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
     * <p>
     * 按照提交日期对提交记录进行分组，每个日期一个分组。
     * 分组后的格式便于 AI 理解并按日期生成变更日志。
     *
     * @param commits 提交记录列表
     * @return 格式化后的提交记录文本（已按日期分组）
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

        return template
            .replace("{version}", "v1.0.0")
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
     * @throws Exception 当 AI 服务调用失败时抛出，包含友好的错误消息
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
     * 基于代码变更（diff）生成提交记录
     * <p>
     * 根据代码的实际改动生成提交记录，而不是依赖提交信息。
     *
     * @param changes 代码变更集合
     * @return 生成的提交记录内容
     * @throws Exception 当 AI 服务调用失败时抛出，包含友好的错误消息
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
     * 基于代码变更（diff）生成提交记录（流式回调）
     *
     * @param changes  代码变更集合
     * @param listener 流式响应监听器
     * @return 生成的提交记录内容
     * @throws Exception 当 AI 服务调用失败时抛出
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

        String recentCommitsText = buildRecentCommitMessagesText(3);

        // 替换模板变量，兼容 {diff}/{codeDiffs}/{recentCommits}
        String diffText = codeDiffsText.toString().trim();
        String prompt = template.replace("{diff}", diffText)
            .replace("{codeDiffs}", diffText)
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
     * @param userPrompt 用户提示词
     * @return 生成的提交记录
     * @throws Exception 当 AI 服务调用失败时抛出，包含友好的错误消息
     */
    @NotNull
    private String callAIServiceForCommitMessage(@NotNull String userPrompt) throws Exception {
        // 获取系统提示词（使用专门的提交记录生成提示词）
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
        AIChatRequest request = new AIChatRequest(systemPrompt, userPrompt);

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

    @NotNull
    private String callAIServiceStream(@NotNull AIService aiService,
                                       @NotNull AIChatRequest request,
                                       @NotNull AIProviderConfig config) throws Exception {
        AIStreamResponseListener streamListener =
            new ChangelogAIStreamResponseListener(project, new StringBuilder(),
                                                  new CountDownLatch(1), new AtomicReference<>());
        return callAIServiceStreamWithListener(aiService, request, config, streamListener);
    }

    @NotNull
    private String callAIServiceForCommitMessageStream(@NotNull String userPrompt,
                                                       @NotNull AIStreamResponseListener listener) throws Exception {
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

        AIChatRequest request = new AIChatRequest(systemPrompt, userPrompt);
        AIService aiService = AIServiceImpl.getInstance();
        AIProviderConfig config = SettingsState.getInstance().providerConfig;
        return callAIServiceStreamWithListener(aiService, request, config, listener);
    }

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
            @Override
            public void onStart() {
                externalListener.onStart();
            }

            @Override
            public void onChunk(@NotNull String chunk) {
                buffer.append(chunk);
                externalListener.onChunk(chunk);
            }

            public void onComplete(@NotNull String fullText) {
                resultRef.set(fullText);
                externalListener.onComplete(fullText);
                latch.countDown();
            }

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
}
