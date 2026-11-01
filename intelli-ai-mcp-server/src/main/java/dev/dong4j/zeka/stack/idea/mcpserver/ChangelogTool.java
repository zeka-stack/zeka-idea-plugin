package dev.dong4j.zeka.stack.idea.mcpserver;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIChatRequest;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIServiceException;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.AIServiceProvider;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIServiceFactory;
import dev.dong4j.zeka.stack.idea.plugin.common.changelog.ChangelogDefaults;
import dev.dong4j.zeka.stack.idea.plugin.common.changelog.ChangelogGitService;
import dev.dong4j.zeka.stack.idea.plugin.common.changelog.git.CommitMessageFormatter;
import dev.dong4j.zeka.stack.idea.plugin.common.changelog.service.ChangelogCommitModels;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * generate_changelog 工具处理器
 * <p>
 * 接收仓库路径与提交范围, 经 core 的纯 JGit {@link ChangelogGitService} 读取提交与 diff,
 * 按 type 选择 core 默认模板填充后调用 AI, 返回变更日志文本:
 * <ul>
 *   <li>changelog: 发布变更日志(按日期分组的提交记录 + diff 上下文)</li>
 *   <li>commit_message: 单条/少量提交的 commit message(走 CommitMessageFormatter 后处理)</li>
 * </ul>
 * 工具入参:
 * <ul>
 *   <li>repoPath(必填): 本地仓库绝对路径</li>
 *   <li>commitHashes(可选): 提交哈希列表, 与 range 二选一</li>
 *   <li>range(可选): 提交范围, 如 "v1.0.0..HEAD"</li>
 *   <li>type(可选): changelog / commit_message, 默认 changelog</li>
 *   <li>responseLanguage(可选): 输出语言, 默认 中文</li>
 * </ul>
 *
 * @author dong4j
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
public final class ChangelogTool {

    /** 供应商配置与密钥(启动时装配) */
    private final ServerConfig serverConfig;

    /**
     * 初始化工具处理器
     *
     * @param serverConfig 服务器配置
     */
    public ChangelogTool(@NotNull ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    /**
     * 执行 changelog 生成
     *
     * @param args 工具入参(来自 MCP 客户端)
     * @return 生成的变更日志文本
     * @throws Exception 仓库读取或 AI 调用失败时抛出
     */
    public @NotNull String execute(@NotNull Map<String, Object> args) throws Exception {
        String repoPath = JavadocTool.requireString(args, "repoPath");
        String type = JavadocTool.optionalString(args, "type", "changelog");
        String responseLanguage = JavadocTool.optionalString(args, "responseLanguage", "中文");

        Path repo = Path.of(repoPath);
        if (!Files.isDirectory(repo)) {
            throw new IllegalArgumentException("仓库路径不存在: " + repoPath);
        }

        ChangelogGitService gitService = new ChangelogGitService(repo);
        if (!gitService.isGitRepository()) {
            throw new IllegalArgumentException("路径不是有效的 Git 仓库: " + repoPath);
        }

        return switch (type) {
            case "commit_message" -> generateCommitMessage(gitService, args, responseLanguage);
            default -> generateChangelog(gitService, args, responseLanguage);
        };
    }

    /**
     * 生成发布变更日志
     *
     * @param gitService       Git 服务(core 纯 JGit 实现)
     * @param args             工具入参
     * @param responseLanguage 输出语言
     * @return changelog 文本
     * @throws Exception 读取或调用失败时抛出
     */
    private @NotNull String generateChangelog(@NotNull ChangelogGitService gitService,
                                              @NotNull Map<String, Object> args,
                                              @NotNull String responseLanguage) throws Exception {
        List<String> hashes = resolveCommitHashes(gitService, args);
        List<ChangelogCommitModels.CommitInfo> commits = gitService.readCommits(hashes);
        if (commits.isEmpty()) {
            return "未找到符合条件的提交记录";
        }

        // 提交记录按模板要求格式化(日期 + 消息), 与插件宿主的分组展示保持同构
        String commitsText = commits.stream()
            .map(c -> "- [" + formatDateTime(c.date()) + "] " + c.shortMessage())
            .collect(Collectors.joining("\n"));

        String template = ChangelogDefaults.getDefaultChangelogUserPrompt()
            .replace("${language}", responseLanguage)
            .replace("{version}", JavadocTool.optionalString(args, "version", "unreleased"))
            .replace("{commits}", commitsText);
        String systemPrompt = ChangelogDefaults.getDefaultChangelogSystemPrompt()
            .replace("${language}", responseLanguage);

        return callAi(systemPrompt, template, args);
    }

    /**
     * 生成 commit message
     *
     * @param gitService       Git 服务(core 纯 JGit 实现)
     * @param args             工具入参
     * @param responseLanguage 输出语言
     * @return 格式化后的 commit message
     * @throws Exception 读取或调用失败时抛出
     */
    private @NotNull String generateCommitMessage(@NotNull ChangelogGitService gitService,
                                                  @NotNull Map<String, Object> args,
                                                  @NotNull String responseLanguage) throws Exception {
        List<String> hashes = resolveCommitHashes(gitService, args);
        List<ChangelogCommitModels.DiffCommitInfo> diffs = gitService.readCommitDiffs(hashes);
        if (diffs.isEmpty()) {
            return "未找到符合条件的提交记录";
        }

        StringBuilder codeDiffs = new StringBuilder();
        for (ChangelogCommitModels.DiffCommitInfo info : diffs) {
            codeDiffs.append(info.diffText()).append("\n\n");
        }

        String template = ChangelogDefaults.getDefaultCommitMessageUserPrompt()
            .replace("${language}", responseLanguage)
            .replace("{codeDiffs}", codeDiffs.toString());
        String systemPrompt = ChangelogDefaults.getDefaultCommitMessageSystemPrompt()
            .replace("${language}", responseLanguage);

        String raw = callAi(systemPrompt, template, args);
        // 与插件链路一致: 去代码围栏/中文标点转换等格式化
        return CommitMessageFormatter.format(raw);
    }

    /**
     * 解析目标提交哈希列表
     * <p> 优先使用显式传入的 commitHashes; 传入 range 时按范围读取; 都未提供时
     * 返回空列表(由 readCommits 端报"未找到提交").
     *
     * @param gitService Git 服务
     * @param args       工具入参
     * @return 提交哈希列表
     * @throws Exception 仓库读取失败时抛出
     */
    private @NotNull List<String> resolveCommitHashes(@NotNull ChangelogGitService gitService,
                                                      @NotNull Map<String, Object> args) throws Exception {
        Object hashesArg = args.get("commitHashes");
        if (hashesArg instanceof List<?> list && !list.isEmpty()) {
            return list.stream().map(String::valueOf).collect(Collectors.toList());
        }
        String range = JavadocTool.optionalString(args, "range", null);
        if (range != null) {
            return gitService.readCommitsFromRange(gitService.getBasePath(), range)
                .stream().map(ChangelogCommitModels.CommitInfo::hash).toList();
        }
        return List.of();
    }

    /**
     * 调用 core AI 执行链
     *
     * @param systemPrompt 系统提示词
     * @param userPrompt   用户提示词
     * @return AI 生成内容
     * @throws AIServiceException 调用失败时抛出
     */
    private @NotNull String callAi(@NotNull String systemPrompt,
                                   @NotNull String userPrompt,
                                   @NotNull Map<String, Object> args) throws AIServiceException {
        ServerConfig.ResolvedCall resolved = serverConfig.resolveConfig(
            JavadocTool.optionalString(args, "provider", null),
            JavadocTool.optionalString(args, "model", null));
        AIServiceProvider provider = AIServiceFactory.createProvider(new JavadocTool.StderrLogger(),
                                                                     resolved.config());
        return provider.generateContent(new AIChatRequest(systemPrompt, userPrompt),
                                        resolved.apiKey(), null);
    }

    /**
     * 格式化提交时间(仅日期部分)
     *
     * @param date 提交时间
     * @return yyyy-MM-dd 文本
     */
    private static @NotNull String formatDateTime(@NotNull Date date) {
        return new java.text.SimpleDateFormat("yyyy-MM-dd").format(date);
    }
}
