package dev.dong4j.zeka.stack.idea.plugin.changelog.service;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import dev.dong4j.zeka.stack.idea.plugin.changelog.model.CodeDiff;
import dev.dong4j.zeka.stack.idea.plugin.changelog.settings.SettingsState;

/** Changelog 提示词构建器 */
final class ChangelogPromptBuilder {

    /** 提交消息中差异内容的最大字符数限制 */
    private static final int MAX_COMMIT_MESSAGE_DIFF_CHARS = 10_000;
    /** 最大提交消息中包含的文件数量限制 */
    private static final int MAX_COMMIT_MESSAGE_FILES = 30;

    /** 最近提交数量限制 */
    private final int recentCommitsLimit;

    /**
     * 初始化 Changelog 提示词构建器
     * <p> 创建一个新的 Changelog 提示词构建器实例, 并设置最近提交的限制数量
     *
     * @param recentCommitsLimit 最近提交的限制数量, 用于控制历史提交信息的范围
     */
    ChangelogPromptBuilder(int recentCommitsLimit) {
        this.recentCommitsLimit = recentCommitsLimit;
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

        return template
            .replace("{version}", "v1.0.0")
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

        return template
            .replace("{version}", "Unreleased")
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
     * String prompt = buildCommitMessagePrompt(payload, recentCommitsText, userContext);
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
                                    @Nullable String userContext) {
        SettingsState settings = SettingsState.getInstance();
        String template = settings.commitMessageTemplate;
        int maxFiles = MAX_COMMIT_MESSAGE_FILES;
        int maxDiffChars = MAX_COMMIT_MESSAGE_DIFF_CHARS;

        StringBuilder codeDiffsText = new StringBuilder();
        int fileCount = 0;
        for (CodeDiff diff : payload.codeDiffs()) {
            if (fileCount >= maxFiles || codeDiffsText.length() >= maxDiffChars) {
                break;
            }
            codeDiffsText.append("文件: ").append(diff.filePath).append("\n");
            codeDiffsText.append("变更类型: ").append(diff.changeType.name()).append("\n");
            if (diff.scopeHint != null && !diff.scopeHint.trim().isEmpty()) {
                codeDiffsText.append("建议scope: ").append(diff.scopeHint).append("\n");
            }
            codeDiffsText.append("新增行数: ").append(diff.addedLines).append("\n");
            codeDiffsText.append("删除行数: ").append(diff.deletedLines).append("\n");
            if (codeDiffsText.length() >= maxDiffChars) {
                codeDiffsText.setLength(maxDiffChars);
                break;
            }
            if (diff.diffContent != null && !diff.diffContent.isEmpty()) {
                codeDiffsText.append("变更内容:\n");
                String metadata = payload.metadataByPath().get(diff.filePath);
                if (metadata != null && !metadata.isBlank()) {
                    codeDiffsText.append(metadata);
                    if (!metadata.endsWith("\n")) {
                        codeDiffsText.append("\n");
                    }
                }
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

        String diffText = codeDiffsText.toString().trim();
        if (!payload.fullPatchText().isBlank()) {
            diffText = payload.fullPatchText().trim() + "\n\n=== 降噪摘要 ===\n" + diffText;
        }
        String contextText = userContext != null ? userContext.trim() : "";
        String prompt = template.replace("{codeDiffs}", diffText)
            .replace("{recentCommits}", recentCommitsText)
            .replace("{extraContext}", contextText);
        if (!template.contains("{extraContext}") && !contextText.isEmpty()) {
            prompt = prompt + "\n\n用户补充说明:\n" + contextText;
        }
        if (!template.contains("{recentCommits}") && !recentCommitsText.isEmpty()) {
            prompt = prompt + "\n\n历史提交(最近" + recentCommitsLimit + "条):\n" + recentCommitsText;
        }
        return prompt;
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
                commitsText.append("- ").append(commit.shortMessage()).append("\n");
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
