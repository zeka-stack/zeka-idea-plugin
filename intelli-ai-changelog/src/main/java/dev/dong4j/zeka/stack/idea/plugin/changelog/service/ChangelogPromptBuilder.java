package dev.dong4j.zeka.stack.idea.plugin.changelog.service;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import dev.dong4j.zeka.stack.idea.plugin.changelog.model.CodeDiff;
import dev.dong4j.zeka.stack.idea.plugin.changelog.settings.SettingsState;
import dev.dong4j.zeka.stack.idea.plugin.changelog.util.ProjectVersionResolver;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderSettings;
import dev.dong4j.zeka.stack.idea.plugin.common.config.ResponseLanguage;

/**
 * 用于构建各类变更日志, 提交信息, 发布说明及每日 / 每周报告的提示词生成器
 * <p> 该类封装了从提交记录中提取变更内容, 生成结构化上下文, 构建摘要文本, 拼接完整 diff 内容等功能, 适用于 AI 驱动的自动化发布日志生成场景.
 * <p> 支持多种模板替换, 包括版本号, 提交内容, 日期, 变更统计, 文件摘要, 上下文 JSON 等, 可灵活适配不同输出格式.
 * <p> 内部通过限制文件数量, 字符长度, 摘要降噪等策略, 确保生成内容在合理长度内, 避免超出模型上下文限制.
 * <p> 主要用途:
 * <ul>
 *   <li> 生成标准格式的变更日志 (Changelog)</li>
 *   <li> 构建提交消息提示词 (用于 AI 生成 commit message)</li>
 *   <li> 生成每日 / 每周项目报告 </li>
 *   <li> 构建包含结构化变更数据的 JSON 上下文 </li>
 * </ul>
 * <p> 设计模式:Builder 模式, 通过构造函数注入项目上下文, 提供多个 build 方法以适配不同场景.
 * <p> 使用示例:
 * <pre>{@code
 * ChangelogPromptBuilder builder = new ChangelogPromptBuilder(project);
 * String prompt = builder.buildChangelogPrompt(commits);
 * }</pre>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.07
 * @since 1.0.0
 */
final class ChangelogPromptBuilder {

    /** 提交消息中差异内容的最大字符数限制 */
    private static final int MAX_COMMIT_MESSAGE_DIFF_CHARS = 500_000;
    /** 最大提交消息中包含的文件数量限制 */
    private static final int MAX_COMMIT_MESSAGE_FILES = 50;
    /** 批量删除操作的阈值, 当删除行数超过该值时触发批量删除统计逻辑 */
    private static final int BULK_DELETE_THRESHOLD = 10;
    /** 批量删除文件时采样的最大数量, 用于降噪和性能优化 */
    private static final int BULK_DELETE_SAMPLE_SIZE = 5;
    /** 目录统计的最大数量限制 */
    private static final int MAX_DIR_STATS = 5;

    /** 当前项目对象 */
    private final Project project;

    /**
     * 初始化 Changelog 提示词构建器
     * <p> 创建一个新的 Changelog 提示词构建器实例, 并设置项目对象
     *
     * @param project 项目对象, 不能为空
     */
    ChangelogPromptBuilder(@NotNull Project project) {
        this.project = project;
    }

    /**
     * 构建版本更新日志提示文本
     * <p> 根据指定的提交信息列表和模板, 生成用于生成 changelog 的提示内容.
     *
     * @param commits 提交信息列表, 不能为 null 或空
     * @return 生成好的提示文本字符串
     */
    @NotNull
    String buildChangelogPrompt(@NotNull List<ChangelogCommitModels.CommitInfo> commits) {
        SettingsState settings = SettingsState.getInstance();
        String template = settings.changelogTemplate;
        String commitsText = buildCommitsText(commits);
        String version = ProjectVersionResolver.resolveVersion(project);

        return template
            .replace("{version}", version)
            .replace("{commits}", commitsText);
    }

    /**
     * 构建差异变更日志提示词
     * <p> 根据差异提交信息生成变更日志提示词
     *
     * @param diffCommits 差异提交信息列表, 不能为空
     * @return 构建后的变更日志提示词字符串
     */
    @NotNull
    String buildDiffChangelogPrompt(@NotNull List<ChangelogCommitModels.DiffCommitInfo> diffCommits) {
        SettingsState settings = SettingsState.getInstance();
        String template = settings.changelogTemplate;
        String diffText = buildDiffCommitsText(diffCommits);
        return template.replace("{commits}", diffText);
    }

    /**
     * 构建发布日志提示内容
     * <p> 根据提供的提交信息和可选的模板, 构建用于生成发布日志的提示文本. 如果未提供模板, 则使用默认的 AI 发布日志模板.
     *
     * @param commits        提交信息列表, 不能为 null
     * @param promptTemplate 可选的提示模板, 若为 null 或空白则使用默认模板
     * @return 构建好的发布日志提示文本, 不会为 null
     */
    @NotNull
    String buildReleaseLogPrompt(@NotNull List<ChangelogCommitModels.CommitInfo> commits, @Nullable String promptTemplate) {
        SettingsState settings = SettingsState.getInstance();
        String template = promptTemplate != null && !promptTemplate.isBlank()
                          ? promptTemplate
                          : settings.aiReleaseLogPrompt;
        String commitsText = buildCommitsText(commits);
        String date = formatCurrentDate();
        String version = ProjectVersionResolver.resolveVersion(project);

        return template
            .replace("{version}", version)
            .replace("{date}", date)
            .replace("{commits}", commitsText);
    }

    /**
     * 构建每日报告提示词
     * <p> 根据提交列表生成每日报告的提示词, 包含日期和提交信息
     *
     * @param commits 提交列表, 不能为 null
     * @return 每日报告的提示词, 包含格式化的日期和提交信息
     */
    @NotNull
    String buildDailyReportPrompt(@NotNull List<ChangelogCommitModels.CommitInfo> commits) {
        SettingsState settings = SettingsState.getInstance();
        String template = settings.dailyReportTemplate;
        String commitsText = buildCommitsText(commits);
        String date = formatCurrentDate();

        return template
            .replace("{date}", date)
            .replace("{commits}", commitsText);
    }

    /**
     * 构建每周报告提示词
     * <p> 根据给定的提交列表和模板, 生成包含日期范围和提交信息的每周报告提示词
     *
     * @param commits 提交列表, 不能为空
     * @return 包含日期范围和提交信息的每周报告提示词
     */
    @NotNull
    String buildWeeklyReportPrompt(@NotNull List<ChangelogCommitModels.CommitInfo> commits) {
        SettingsState settings = SettingsState.getInstance();
        String template = settings.weeklyReportTemplate;
        String commitsText = buildCommitsText(commits);
        String dateRange = formatWeeklyDateRange();

        return template
            .replace("{dateRange}", dateRange)
            .replace("{commits}", commitsText);
    }

    /**
     * 构建提交信息提示词
     * <p> 根据代码差异, 最近提交记录和用户补充上下文生成用于提交信息生成的提示词
     * <p> 该方法会处理代码差异内容, 限制文件数量和变更内容长度, 并根据模板替换占位符
     * <p> 使用示例:
     * <pre>{@code
     * String prompt = buildCommitMessagePrompt(payload, recentCommitsText, userContext, branch, true);
     * }</pre>
     *
     * @param payload           代码差异负载对象, 包含文件变更信息, 不能为 null
     * @param recentCommitsText 最近提交记录文本, 不能为 null
     * @param userContext       用户补充上下文信息, 可以为 null
     * @return 生成的提交信息提示词, 不能为空
     */
    @NotNull
    String buildCommitMessagePrompt(@NotNull ChangelogCommitDiffBuilder.DiffPayload payload,
                                    @NotNull String recentCommitsText,
                                    @Nullable String userContext,
                                    @Nullable String branch,
                                    boolean isGitRepository) {
        SettingsState settings = SettingsState.getInstance();
        String template = settings.commitMessageTemplate;
        int maxFiles = MAX_COMMIT_MESSAGE_FILES;

        // 1) 结构化 JSON 上下文：作为核心上下文，单独暴露为 {codeDiffs}。
        String contextJson = buildStructuredContext(payload, recentCommitsText, userContext, branch, isGitRepository, maxFiles);

        // 2) 原生 patch 与降噪摘要：分别提供为独立占位符，便于用户自行裁剪。
        String diffSummary = buildDiffSummaryText(payload, maxFiles, MAX_COMMIT_MESSAGE_DIFF_CHARS);
        String rawPatch = payload.fullPatchText().trim();

        // 3) 仅做占位符替换，不做额外追加，避免模板中删除占位符后仍被强行拼接。
        return template
            .replace("{codeDiffs}", contextJson)
            .replace("{rawPatch}", rawPatch)
            .replace("{diffSummary}", diffSummary);
    }

    /**
     * 构建结构化上下文（JSON）
     * <p>
     * 该 JSON 会放在 prompt 开头，优先提供项目、统计与文件级变更信息，
     * 帮助模型在阅读 diff 之前建立全局结构感知。
     */
    @NotNull
    private String buildStructuredContext(@NotNull ChangelogCommitDiffBuilder.DiffPayload payload,
                                          @NotNull String recentCommitsText,
                                          @Nullable String userContext,
                                          @Nullable String branch,
                                          boolean isGitRepository,
                                          int maxFiles) {
        List<CodeDiff> allDiffs = payload.codeDiffs();
        List<CodeDiff> limitedDiffs = limitDiffs(allDiffs, maxFiles);
        ChangeStats stats = buildChangeStats(allDiffs);

        StringBuilder json = new StringBuilder();
        json.append("{\n");

        // 项目信息
        json.append("  \"project\": {\n");
        json.append("    \"name\": \"").append(escapeJson(project.getName())).append("\",\n");
        json.append("    \"branch\": \"").append(escapeJson(normalizeBranch(branch))).append("\",\n");
        json.append("    \"is_git_repository\": ").append(isGitRepository).append("\n");
        json.append("  },\n");

        // 统计信息
        json.append("  \"statistics\": {\n");
        json.append("    \"files_changed\": ").append(stats.filesChanged()).append(",\n");
        json.append("    \"lines_added\": ").append(stats.linesAdded()).append(",\n");
        json.append("    \"lines_deleted\": ").append(stats.linesDeleted()).append(",\n");
        json.append("    \"change_type\": \"").append(escapeJson(stats.primaryType())).append("\",\n");
        json.append("    \"scope\": \"").append(escapeJson(stats.scope())).append("\"\n");
        json.append("  },\n");

        // 文件级变更列表
        json.append("  \"changes\": [\n");
        for (int i = 0; i < limitedDiffs.size(); i++) {
            CodeDiff diff = limitedDiffs.get(i);
            String filePath = diff.filePath;
            String language = resolveLanguage(filePath);
            String extension = extractExtension(filePath);
            String summary = buildFileSummary(diff);
            String diffSummary = buildDiffSummary(diff);
            String fullDiff = buildFileFullDiff(payload, diff);
            String semanticSummary = diff.semanticSummary != null ? diff.semanticSummary : "";

            json.append("    {\n");
            json.append("      \"path\": \"").append(escapeJson(filePath)).append("\",\n");
            json.append("      \"type\": \"").append(escapeJson(diff.changeType.name())).append("\",\n");
            json.append("      \"language\": \"").append(escapeJson(language)).append("\",\n");
            json.append("      \"extension\": \"").append(escapeJson(extension)).append("\",\n");
            json.append("      \"lines_added\": ").append(diff.addedLines).append(",\n");
            json.append("      \"lines_deleted\": ").append(diff.deletedLines).append(",\n");
            json.append("      \"summary\": \"").append(escapeJson(summary)).append("\",\n");
            json.append("      \"diff_summary\": \"").append(escapeJson(diffSummary)).append("\",\n");
            json.append("      \"semantic_summary\": \"").append(escapeJson(semanticSummary)).append("\",\n");
            json.append("      \"full_diff_content\": \"").append(escapeJson(fullDiff)).append("\"\n");
            json.append("    }");
            if (i < limitedDiffs.size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }
        json.append("  ],\n");

        // 元数据补充（最近提交、语言偏好、用户说明等）
        json.append("  \"metadata\": {\n");
        json.append("    \"recent_commits\": ").append(buildRecentCommitsJson(recentCommitsText)).append(",\n");
        json.append("    \"preferred_language\": \"").append(escapeJson(resolvePreferredLanguage())).append("\"");
        if (userContext != null && !userContext.trim().isEmpty()) {
            json.append(",\n    \"extra_context\": \"").append(escapeJson(userContext.trim())).append("\"");
        }
        json.append(",\n    \"commit_template\": \"type(scope): subject\"\n");
        json.append("  }\n");

        json.append("}");
        return json.toString();
    }

    /**
     * 构建降噪 diff 摘要（基于 CodeDiffUtil 输出）
     * <p> 该摘要用于突出语义变更，避免原生 patch 太长导致的注意力分散。
     */
    @NotNull
    private String buildDiffSummaryText(@NotNull ChangelogCommitDiffBuilder.DiffPayload payload,
                                        int maxFiles,
                                        int maxDiffChars) {
        StringBuilder summary = new StringBuilder();
        List<CodeDiff> allDiffs = payload.codeDiffs();
        List<CodeDiff> limitedDiffs = limitDiffs(allDiffs, maxFiles);

        appendBulkDeleteSummary(summary, allDiffs, maxDiffChars);

        int fileCount = 0;
        for (CodeDiff diff : limitedDiffs) {
            if (summary.length() >= maxDiffChars) {
                break;
            }
            summary.append("文件: ").append(diff.filePath).append("\n");
            summary.append("变更类型: ").append(diff.changeType.name()).append("\n");
            if (diff.scopeHint != null && !diff.scopeHint.trim().isEmpty()) {
                summary.append("建议scope: ").append(diff.scopeHint).append("\n");
            }
            summary.append("新增行数: ").append(diff.addedLines).append("\n");
            summary.append("删除行数: ").append(diff.deletedLines).append("\n");

            if (diff.diffContent != null && !diff.diffContent.isEmpty()) {
                summary.append("变更内容:\n");
                String metadata = payload.metadataByPath().get(diff.filePath);
                if (metadata != null && !metadata.isBlank()) {
                    summary.append(metadata);
                    if (!metadata.endsWith("\n")) {
                        summary.append("\n");
                    }
                }
                int remaining = maxDiffChars - summary.length();
                if (remaining > 0) {
                    String diffContent = diff.diffContent;
                    if (diffContent.length() > remaining) {
                        diffContent = diffContent.substring(0, remaining);
                    }
                    summary.append(diffContent).append("\n");
                }
            }
            summary.append("\n");
            fileCount++;
        }

        if (allDiffs.size() > fileCount && summary.length() < maxDiffChars) {
            appendRemainingFilesSummary(summary, allDiffs, fileCount, maxDiffChars);
        }

        String summaryText = summary.toString().trim();
        if (summaryText.length() > maxDiffChars) {
            String suffix = "\n...[diff truncated]";
            int limit = Math.max(0, maxDiffChars - suffix.length());
            summaryText = summaryText.substring(0, limit) + suffix;
        }
        return summaryText;
    }

    /**
     * 拼接 IDEA 原生 patch 与降噪摘要
     * <p> 遵循 “原生 patch → 降噪摘要” 的顺序，保证上下文优先。
     */
    @NotNull
    private String buildCombinedDiffBlock(@NotNull ChangelogCommitDiffBuilder.DiffPayload payload,
                                          @NotNull String diffSummary,
                                          int maxDiffChars) {
        StringBuilder diffBlock = new StringBuilder();

        if (!payload.fullPatchText().isBlank()) {
            appendWithLimit(diffBlock, payload.fullPatchText().trim(), maxDiffChars);
        }

        if (diffBlock.length() < maxDiffChars && !diffSummary.isBlank()) {
            if (!diffBlock.isEmpty()) {
                diffBlock.append("\n\n=== 降噪摘要 ===\n");
            }
            appendWithLimit(diffBlock, diffSummary, maxDiffChars);
        }

        if (diffBlock.length() > maxDiffChars) {
            String suffix = "\n...[diff truncated]";
            int limit = Math.max(0, maxDiffChars - suffix.length());
            diffBlock.setLength(limit);
            diffBlock.append(suffix);
        }

        return diffBlock.toString().trim();
    }

    /**
     * 在保证长度上限的前提下追加文本
     */
    private void appendWithLimit(@NotNull StringBuilder builder, @NotNull String content, int maxLength) {
        if (builder.length() >= maxLength) {
            return;
        }
        int remaining = maxLength - builder.length();
        if (content.length() <= remaining) {
            builder.append(content);
        } else {
            builder.append(content, 0, remaining);
        }
    }

    /**
     * 统计变更数据，用于结构化上下文中的 statistics 字段
     */
    @NotNull
    private ChangeStats buildChangeStats(@NotNull List<CodeDiff> diffs) {
        int filesChanged = diffs.size();
        int linesAdded = 0;
        int linesDeleted = 0;
        for (CodeDiff diff : diffs) {
            linesAdded += diff.addedLines;
            linesDeleted += diff.deletedLines;
        }
        String primaryType = inferPrimaryChangeType(diffs);
        String scope = inferScope(diffs);
        return new ChangeStats(filesChanged, linesAdded, linesDeleted, primaryType, scope);
    }

    /**
     * 推断主要变更类型
     * <p> 按出现次数最多的变更类型作为 primary type。
     */
    @NotNull
    private String inferPrimaryChangeType(@NotNull List<CodeDiff> diffs) {
        if (diffs.isEmpty()) {
            return "UNKNOWN";
        }
        Map<String, Integer> counters = new LinkedHashMap<>();
        for (CodeDiff diff : diffs) {
            String key = diff.changeType.name();
            counters.put(key, counters.getOrDefault(key, 0) + 1);
        }
        return infer(counters, "MODIFY");
    }

    /**
     * 推断 scope
     * <p> 优先使用已有的 scopeHint，否则基于路径启发式规则。
     */
    @NotNull
    private String inferScope(@NotNull List<CodeDiff> diffs) {
        Map<String, Integer> counters = new LinkedHashMap<>();
        for (CodeDiff diff : diffs) {
            String scopeHint = diff.scopeHint != null ? diff.scopeHint.trim() : "";
            String scope = !scopeHint.isEmpty() ? scopeHint : extractScopeFromPath(diff.filePath);
            counters.put(scope, counters.getOrDefault(scope, 0) + 1);
        }
        if (counters.isEmpty()) {
            return "core";
        }
        return infer(counters, "core");
    }

    /**
     * 从变更类型计数器中推断主要变更类型
     * <p> 遍历计数器, 选择出现次数最多的变更类型作为主要类型; 若无有效计数, 则默认返回指定的默认类型.
     *
     * @param counters 变更类型计数映射, 键为变更类型名称, 值为出现次数, 不能为 null
     * @param modify   默认变更类型, 当计数器为空或无有效计数时返回该值, 不能为 null
     * @return 出现次数最多的变更类型名称, 若无有效计数则返回默认值
     */
    private String infer(Map<String, Integer> counters, String modify) {
        String primary = modify;
        int max = 0;
        for (Map.Entry<String, Integer> entry : counters.entrySet()) {
            if (entry.getValue() > max) {
                max = entry.getValue();
                primary = entry.getKey();
            }
        }
        return primary;
    }

    /**
     * 从路径中提取 scope（启发式规则）
     */
    @NotNull
    private String extractScopeFromPath(@NotNull String filePath) {
        String normalized = filePath.replace('\\', '/').toLowerCase(Locale.ROOT);
        if (normalized.contains("/service/")) {
            return "service";
        }
        if (normalized.contains("/util/") || normalized.contains("/utils/")) {
            return "util";
        }
        if (normalized.contains("/config/")) {
            return "config";
        }
        if (normalized.contains("/test/") || normalized.contains("/tests/")) {
            return "test";
        }
        if (normalized.contains("/ui/") || normalized.contains("/view/") || normalized.contains("/views/")) {
            return "ui";
        }
        return "core";
    }

    /**
     * 生成文件级摘要文本
     */
    @NotNull
    private String buildFileSummary(@NotNull CodeDiff diff) {
        String fileName = extractFileName(diff.filePath);
        return String.format("%s %s (+%d/-%d lines)",
                             diff.changeType.name(),
                             fileName,
                             diff.addedLines,
                             diff.deletedLines);
    }

    /**
     * 生成文件级 diff 摘要（仅标识类型与路径）
     */
    @NotNull
    private String buildDiffSummary(@NotNull CodeDiff diff) {
        return "[" + diff.changeType.name() + "]: " + diff.filePath;
    }

    /**
     * 构建单文件完整 diff 内容
     * <p>
     * 这里优先拼接 patch 元数据与 CodeDiffUtil 的 diff 内容，确保格式统一。
     */
    @NotNull
    private String buildFileFullDiff(@NotNull ChangelogCommitDiffBuilder.DiffPayload payload,
                                     @NotNull CodeDiff diff) {
        StringBuilder content = new StringBuilder();
        String metadata = payload.metadataByPath().get(diff.filePath);
        if (metadata != null && !metadata.isBlank()) {
            content.append(metadata);
            if (!metadata.endsWith("\n")) {
                content.append("\n");
            }
        }
        if (diff.diffContent != null && !diff.diffContent.isBlank()) {
            content.append(diff.diffContent.stripTrailing());
        }
        return content.toString().trim();
    }

    /**
     * 解析最近提交记录文本为 JSON 数组
     */
    @NotNull
    private String buildRecentCommitsJson(@NotNull String recentCommitsText) {
        List<String> commits = new ArrayList<>();
        if (!recentCommitsText.isBlank()) {
            String[] lines = recentCommitsText.split("\n");
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.startsWith("- ")) {
                    trimmed = trimmed.substring(2).trim();
                }
                if (!trimmed.isEmpty()) {
                    commits.add(trimmed);
                }
            }
        }
        StringBuilder json = new StringBuilder();
        json.append("[");
        for (int i = 0; i < commits.size(); i++) {
            json.append("\"").append(escapeJson(commits.get(i))).append("\"");
            if (i < commits.size() - 1) {
                json.append(", ");
            }
        }
        json.append("]");
        return json.toString();
    }

    /**
     * 获取用户偏好语言描述
     */
    @NotNull
    private String resolvePreferredLanguage() {
        AIProviderSettings providerSettings = AIProviderSettings.getInstance();
        ResponseLanguage responseLanguage = providerSettings != null && providerSettings.responseLanguage != null
                                            ? providerSettings.responseLanguage
                                            : ResponseLanguage.ZH;
        return responseLanguage.getDescForPrompt();
    }

    /**
     * 解析文件的语言名称
     */
    @NotNull
    private String resolveLanguage(@NotNull String filePath) {
        String fileName = extractFileName(filePath);
        FileType fileType = FileTypeManager.getInstance().getFileTypeByFileName(fileName);
        return fileType.getName();
    }

    /**
     * 提取文件扩展名
     */
    @NotNull
    private String extractExtension(@NotNull String filePath) {
        String fileName = extractFileName(filePath);
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dotIndex + 1);
    }

    /**
     * 规范化分支名称
     */
    @NotNull
    private String normalizeBranch(@Nullable String branch) {
        if (branch == null || branch.trim().isEmpty()) {
            return "unknown";
        }
        return branch.trim();
    }

    /**
     * 提取文件名
     */
    @NotNull
    private String extractFileName(@NotNull String filePath) {
        int index = Math.max(filePath.lastIndexOf('/'), filePath.lastIndexOf('\\'));
        return index >= 0 ? filePath.substring(index + 1) : filePath;
    }

    /**
     * 控制最大文件数量
     */
    @NotNull
    private List<CodeDiff> limitDiffs(@NotNull List<CodeDiff> diffs, int maxFiles) {
        if (diffs.size() <= maxFiles) {
            return diffs;
        }
        List<CodeDiff> sorted = new ArrayList<>(diffs);
        sorted.sort((left, right) -> Integer.compare(
            (right.addedLines + right.deletedLines),
            (left.addedLines + left.deletedLines)
                                                    ));
        return new ArrayList<>(sorted.subList(0, maxFiles));
    }

    /**
     * 添加批量删除文件摘要信息
     * <p>当检测到大量删除文件 (超过阈值) 时, 向摘要中追加批量删除文件的统计信息和示例路径, 以帮助用户快速识别大规模删除操作.
     * <p>该方法仅在删除文件数量超过阈值且摘要长度未达上限时执行.
     *
     * @param summary      用于追加摘要内容的 StringBuilder 对象, 不能为空
     * @param diffs        所有代码差异对象列表, 不能为空
     * @param maxDiffChars 摘要内容的最大字符数限制, 不能为负数
     */
    private void appendBulkDeleteSummary(@NotNull StringBuilder summary,
                                         @NotNull List<CodeDiff> diffs,
                                         int maxDiffChars) {
        List<CodeDiff> deletes = diffs.stream()
            .filter(diff -> diff.changeType == CodeDiff.ChangeType.DELETE)
            .toList();
        if (deletes.size() < BULK_DELETE_THRESHOLD || summary.length() >= maxDiffChars) {
            return;
        }
        summary.append("删除文件总数: ").append(deletes.size()).append("\n");
        for (int i = 0; i < BULK_DELETE_SAMPLE_SIZE; i++) {
            summary.append("- ").append(deletes.get(i).filePath).append("\n");
        }
        summary.append("\n");
    }

    /**
     * 构建剩余文件的摘要统计信息
     * <p> 当文件数量超出显示限制时, 追加包含剩余文件数量, 变更类型分布和目录分布的统计信息
     *
     * @param summary      用于追加统计信息的字符串构建器, 不能为 null
     * @param diffs        所有代码差异列表, 不能为 null
     * @param displayed    已显示的文件数量
     * @param maxDiffChars 差异内容的最大字符数限制
     */
    private void appendRemainingFilesSummary(@NotNull StringBuilder summary,
                                             @NotNull List<CodeDiff> diffs,
                                             int displayed,
                                             int maxDiffChars) {
        int remaining = diffs.size() - displayed;
        summary.append("其余文件: ").append(remaining).append("\n");
        summary.append("变更类型分布: ").append(buildChangeTypeSummary(diffs)).append("\n");

        List<String> dirStats = buildDirectoryStats(diffs);
        if (!dirStats.isEmpty() && summary.length() < maxDiffChars) {
            summary.append("目录分布(Top ").append(MAX_DIR_STATS).append("):\n");
            for (String line : dirStats) {
                summary.append("- ").append(line).append("\n");
            }
        }
    }

    /**
     * 生成变更类型摘要
     * <p> 遍历给定的 {@code List<CodeDiff>}, 统计各类型变更的数量, 并按
     * {@code MODIFY},{@code ADD},{@code DELETE},{@code RENAME} 的顺序生成摘要字符串.
     * 仅在对应计数不为 0 时才将该条目追加到结果中, 最终返回一个
     * 空格或分号分隔的摘要文本; 若所有计数均为 0, 返回空字符串.
     *
     * @param diffs 代码差异列表, 不能为空
     * @return 变更类型摘要字符串, 例如“MODIFY 3 ADD 2 DELETE 1 RENAME 0”,
     *     计数为 0 的项将被省略; 若所有计数为 0, 则返回空字符串
     */
    @NotNull
    private String buildChangeTypeSummary(@NotNull List<CodeDiff> diffs) {
        int add = 0;
        int modify = 0;
        int delete = 0;
        int rename = 0;
        for (CodeDiff diff : diffs) {
            switch (diff.changeType) {
                case ADD -> add++;
                case MODIFY -> modify++;
                case DELETE -> delete++;
                case RENAME -> rename++;
                default -> {
                }
            }
        }
        StringBuilder summary = new StringBuilder();
        appendIfNonZero(summary, "MODIFY", modify);
        appendIfNonZero(summary, "ADD", add);
        appendIfNonZero(summary, "DELETE", delete);
        appendIfNonZero(summary, "RENAME", rename);
        return summary.toString();
    }

    /**
     * 如果给定的值大于零, 则将标签和值追加到构建器中
     * <p> 此方法会在构建器不为空的情况下添加一个逗号, 然后追加标签和值的组合
     *
     * @param builder 构建器对象, 不能为空
     * @param label   标签, 不能为空
     * @param value   要检查的数值, 如果大于零则会被追加
     */
    private void appendIfNonZero(@NotNull StringBuilder builder, @NotNull String label, int value) {
        if (value <= 0) {
            return;
        }
        if (!builder.isEmpty()) {
            builder.append(", ");
        }
        builder.append(label).append("=").append(value);
    }

    /**
     * 构建目录统计信息
     * <p> 根据代码差异列表, 统计每个目录下的文件数量, 并返回按文件数量从高到低排序的前 MAX_DIR_STATS 个目录及其文件数量
     *
     * @param diffs 代码差异列表, 不能为空
     * @return 包含目录及其文件数量的列表, 按文件数量从高到低排序
     */
    @NotNull
    private List<String> buildDirectoryStats(@NotNull List<CodeDiff> diffs) {
        Map<String, Integer> counts = new java.util.HashMap<>();
        for (CodeDiff diff : diffs) {
            String dir = extractDirectory(diff.filePath);
            counts.put(dir, counts.getOrDefault(dir, 0) + 1);
        }
        return counts.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(MAX_DIR_STATS)
            .map(entry -> entry.getKey() + " (" + entry.getValue() + ")")
            .toList();
    }

    /**
     * 提取文件路径的目录部分
     * <p> 将给定文件路径转换为标准化格式 (使用正斜杠), 并返回其所在目录的路径.
     * 如果路径中不包含目录信息, 则返回 "." 表示当前目录.
     *
     * @param filePath 文件路径字符串, 不能为 null 或空
     * @return 提取后的目录路径字符串, 不会为 null
     */
    @NotNull
    private String extractDirectory(@NotNull String filePath) {
        String normalized = filePath.replace('\\', '/');
        int lastSlash = normalized.lastIndexOf('/');
        return lastSlash > 0 ? normalized.substring(0, lastSlash) : ".";
    }

    /**
     * 进行 JSON 字符串转义
     */
    @NotNull
    private String escapeJson(@NotNull String value) {
        return value.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\r", "\\r")
            .replace("\n", "\\n");
    }

    /**
     * 结构化统计信息
     */
    private record ChangeStats(int filesChanged, int linesAdded, int linesDeleted,
                               @NotNull String primaryType, @NotNull String scope) {
    }

    /**
     * 构建提交信息文本
     * <p> 将提交列表按日期分组, 并按照 Markdown 格式组织为可读的文本内容.
     *
     * @param commits 提交信息列表, 必须不为空
     * @return 按日期分类的提交摘要文本, 格式为 "### 日期 \n\n- 提交简短信息 \n\n"
     */
    @NotNull
    private String buildCommitsText(@NotNull List<ChangelogCommitModels.CommitInfo> commits) {
        if (commits.isEmpty()) {
            return "";
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Map<String, List<ChangelogCommitModels.CommitInfo>> commitsByDate = new LinkedHashMap<>();
        for (ChangelogCommitModels.CommitInfo commit : commits) {
            String dateStr = dateFormat.format(commit.date());
            commitsByDate.computeIfAbsent(dateStr, k -> new ArrayList<>()).add(commit);
        }

        StringBuilder commitsText = new StringBuilder();
        for (Map.Entry<String, List<ChangelogCommitModels.CommitInfo>> entry : commitsByDate.entrySet()) {
            String dateStr = entry.getKey();
            List<ChangelogCommitModels.CommitInfo> dateCommits = entry.getValue();

            commitsText.append("### ").append(dateStr).append("\n\n");

            for (ChangelogCommitModels.CommitInfo commit : dateCommits) {
                // 输出短消息作为主标题
                commitsText.append("- ").append(commit.shortMessage()).append("\n");

                // 处理完整消息，提取详细描述
                String fullMessage = commit.fullMessage();
                String shortMessage = commit.shortMessage();

                // 如果完整消息包含更多内容，提取详细描述部分
                if (fullMessage != null && !fullMessage.trim().isEmpty()
                    && !fullMessage.trim().equals(shortMessage.trim())) {
                    // 移除第一行（通常是 shortMessage）和可能的空行
                    String[] lines = fullMessage.split("\n");

                    for (int i = 0; i < lines.length; i++) {
                        String line = lines[i].trim();
                        // 跳过第一行（shortMessage）和空行
                        if (i == 0 && line.equals(shortMessage.trim())) {
                            continue;
                        }
                        if (line.isEmpty()) {
                            continue;
                        }
                        // 处理详细描述内容
                        // 如果行已经以 "- " 开头，只加缩进；否则加缩进和 "- "
                        if (line.startsWith("- ")) {
                            commitsText.append("  ").append(line).append("\n");
                        } else {
                            commitsText.append("  - ").append(line).append("\n");
                        }
                    }
                }
            }

            commitsText.append("\n");
        }

        return commitsText.toString().trim();
    }

    /**
     * 构建差异提交信息的文本
     * <p> 根据提交日期对差异提交信息进行分组, 并生成格式化的文本
     *
     * @param diffCommits 差异提交信息列表, 不能为空
     * @return 格式化后的差异提交信息文本
     */
    @NotNull
    private String buildDiffCommitsText(@NotNull List<ChangelogCommitModels.DiffCommitInfo> diffCommits) {
        if (diffCommits.isEmpty()) {
            return "";
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Map<String, List<ChangelogCommitModels.DiffCommitInfo>> commitsByDate = new LinkedHashMap<>();
        for (ChangelogCommitModels.DiffCommitInfo commit : diffCommits) {
            String dateStr = dateFormat.format(commit.date());
            commitsByDate.computeIfAbsent(dateStr, k -> new ArrayList<>()).add(commit);
        }

        StringBuilder commitsText = new StringBuilder();
        for (Map.Entry<String, List<ChangelogCommitModels.DiffCommitInfo>> entry : commitsByDate.entrySet()) {
            String dateStr = entry.getKey();
            commitsText.append("### ").append(dateStr).append("\n\n");
            for (ChangelogCommitModels.DiffCommitInfo commit : entry.getValue()) {
                commitsText.append("- 提交 ").append(commit.hash(), 0, Math.min(8, commit.hash().length()))
                    .append(":\n");
                commitsText.append(commit.diffText()).append("\n");
            }
            commitsText.append("\n");
        }

        return commitsText.toString().trim();
    }

    /**
     * 格式化当前日期为字符串形式
     * <p> 将当前日期格式化为 "yyyy-MM-dd" 的字符串格式
     *
     * @return 当前日期的字符串表示, 格式为 "yyyy-MM-dd"
     */
    @NotNull
    private String formatCurrentDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        return dateFormat.format(new Date());
    }

    /**
     * 格式化当前周的日期范围
     * <p> 计算并返回当前周的周一至周日的日期字符串, 格式为 "yyyy-MM-dd"
     * <p> 例如:2024-01-01 至 2024-01-07
     *
     * @return 当前周的日期范围字符串, 格式为 "开始日期 至 结束日期"
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
}
