package dev.dong4j.zeka.stack.idea.plugin.changelog.util;

import com.intellij.diff.comparison.ComparisonManager;
import com.intellij.diff.comparison.ComparisonPolicy;
import com.intellij.diff.fragments.LineFragment;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.progress.ProgressIndicatorProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectLocator;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ContentRevision;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import dev.dong4j.zeka.stack.idea.plugin.changelog.context.ContextResolverRegistry;
import dev.dong4j.zeka.stack.idea.plugin.changelog.model.CodeDiff;

/**
 * 代码差异工具类
 * <p>
 * 提供代码变更差异提取和分析功能, 用于处理版本控制系统中的文件变更. 可以从变更对象中提取代码差异信息, 包括变更类型, 新增行数, 删除行数和差异内容等.
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email "mailto:zeka.stack@gmail.com"
 * @date 2025.11.30
 * @since 1.0.0
 */
public final class CodeDiffUtil {

    /** 每个文件最多保留的 diff 块数量 */
    private static final int MAX_HUNKS_PER_FILE = 6;
    /** 单个 diff 块最多保留的行数 */
    private static final int MAX_LINES_PER_HUNK = 20;
    /** 单行最大输出长度，避免超长单行污染 diff */
    private static final int MAX_DIFF_LINE_CHARS = 240;
    /** 新增文件预览最大字符数 */
    private static final int MAX_ADDED_PREVIEW_CHARS = 4000;
    /** 超过该大小时不生成完整 diff（避免大文件污染上下文） */
    private static final long MAX_DIFF_FILE_BYTES = 1024L;
    /** 文本资源类扩展名的更低阈值 */
    private static final long MAX_TEXT_ASSET_BYTES = 1024L;
    /** 文本资源类扩展名集合（常见体积大且语义弱的文件） */
    private static final Set<String> TEXT_ASSET_EXTENSIONS = Set.of("svg", "map", "lock", "json");
    /** 单个文件触发摘要模式的最大行数阈值 */
    private static final int LARGE_FILE_MAX_LINES = 300;
    /** 变更行数超过该值时触发摘要模式 */
    private static final int LARGE_DIFF_MAX_CHANGED_LINES = 200;
    /** 内容长度超过该值时触发摘要模式 */
    private static final int LARGE_DIFF_MAX_CHARS = 6000;
    /** 摘要模式下 head/tail 保留行数 */
    private static final int LARGE_DIFF_HEAD_TAIL_LINES = 3;

    /**
     * 私有构造函数, 用于防止外部实例化
     * <p>
     * 该构造函数为私有, 确保 CodeDiffUtil 类只能通过静态方法使用, 不能被外部实例化
     */
    private CodeDiffUtil() {
        // 工具类，禁止实例化
    }

    /**
     * 从 Change 集合中提取代码变更信息
     *
     * @param changes 变更集合
     * @return 代码变更信息列表, 包含每个变更的路径, 类型, 增删行数等信息
     */
    @NotNull
    public static List<CodeDiff> extractCodeDiffs(@NotNull Collection<Change> changes) {
        List<CodeDiff> codeDiffs = new ArrayList<>();

        for (Change change : changes) {
            CodeDiff codeDiff = extractCodeDiff(change);
            if (codeDiff != null) {
                codeDiffs.add(codeDiff);
            }
        }

        return codeDiffs;
    }

    /**
     * 从单个 Change 对象中提取代码变更信息
     *
     * @param change 变更对象
     * @return 代码变更信息, 如果无法提取则返回 null
     */
    @Nullable
    public static CodeDiff extractCodeDiff(@NotNull Change change) {
        VirtualFile virtualFile = change.getVirtualFile();
        if (virtualFile == null) {
            return null;
        }

        String filePath = virtualFile.getPath();
        CodeDiff.ChangeType changeType = determineChangeType(change);
        boolean skipLargeContent = shouldSkipLargeFileDiff(virtualFile);
        DiffResult diffResult = null;
        String diffContent;
        if (changeType == CodeDiff.ChangeType.DELETE) {
            diffContent = null;
        } else if (changeType == CodeDiff.ChangeType.ADD) {
            if (skipLargeContent) {
                diffContent = buildLargeFileSummary("/dev/null", virtualFile.getName(), virtualFile.getLength(), "large file");
            } else {
                diffContent = buildAddedFilePreview(change, virtualFile);
            }
        } else {
            if (skipLargeContent) {
                if (changeType == CodeDiff.ChangeType.RENAME) {
                    diffContent = buildMovedFileSummary(change);
                } else {
                    String beforeName = change.getBeforeRevision() != null ? change.getBeforeRevision().getFile().getName() : "unknown";
                    String afterName = change.getAfterRevision() != null ? change.getAfterRevision().getFile().getName() : "unknown";
                    diffContent = buildLargeFileSummary(beforeName, afterName, virtualFile.getLength(), "large file");
                }
            } else {
                diffResult = extractDiffResult(change);
                diffContent = diffResult != null ? diffResult.diffContent() : null;
                if (changeType == CodeDiff.ChangeType.RENAME && isRenameWithoutContentChange(diffResult)) {
                    diffContent = buildMovedFileSummary(change);
                }
            }
        }
        String scopeHint = resolveScopeHint(virtualFile);
        String semanticSummary = resolveSemanticSummary(virtualFile, diffResult);

        // 计算新增和删除的行数
        int addedLines = 0;
        int deletedLines = 0;
        if (diffResult != null && !diffResult.fragments().isEmpty()) {
            int[] changed = countChangedLines(diffResult.fragments());
            addedLines = changed[0];
            deletedLines = changed[1];
        } else if (changeType == CodeDiff.ChangeType.DELETE) {
            ContentRevision beforeRevision = change.getBeforeRevision();
            String beforeContent = beforeRevision != null ? safeGetContent(beforeRevision) : null;
            deletedLines = beforeContent != null ? countLines(beforeContent) : 0;
        } else if (changeType == CodeDiff.ChangeType.ADD && diffContent != null && !skipLargeContent) {
            addedLines = countLines(diffContent);
        } else if (diffContent != null) {
            String[] lines = diffContent.split("\n");
            for (String line : lines) {
                if (line.startsWith("+") && !line.startsWith("+++")) {
                    addedLines++;
                } else if (line.startsWith("-") && !line.startsWith("---")) {
                    deletedLines++;
                }
            }
        }

        return new CodeDiff(filePath, changeType, addedLines, deletedLines, diffContent, scopeHint, semanticSummary);
    }

    /**
     * 确定变更类型
     *
     * @param change 变更对象
     * @return 变更类型
     */
    @NotNull
    private static CodeDiff.ChangeType determineChangeType(@NotNull Change change) {
        ContentRevision beforeRevision = change.getBeforeRevision();
        ContentRevision afterRevision = change.getAfterRevision();

        if (beforeRevision == null && afterRevision != null) {
            return CodeDiff.ChangeType.ADD;
        } else if (beforeRevision != null && afterRevision == null) {
            return CodeDiff.ChangeType.DELETE;
        } else if (beforeRevision != null) {
            // 检查是否是重命名或移动
            String beforePath = beforeRevision.getFile().getPath();
            String afterPath = afterRevision.getFile().getPath();
            if (!beforePath.equals(afterPath)) {
                return CodeDiff.ChangeType.RENAME;
            }
            return CodeDiff.ChangeType.MODIFY;
        }

        return CodeDiff.ChangeType.MODIFY;
    }

    /**
     * 提取 Diff 内容
     *
     * @param change 变更对象
     * @return 包含代码差异信息的字符串, 如果无差异则返回 null
     */
    @SuppressWarnings("D")
    @Nullable
    private static DiffResult extractDiffResult(@NotNull Change change) {
        return ApplicationManager.getApplication().runReadAction((Computable<DiffResult>) () -> {
            try {
                ContentRevision beforeRevision = change.getBeforeRevision();
                ContentRevision afterRevision = change.getAfterRevision();

                if (beforeRevision == null && afterRevision == null) {
                    return null;
                }

                String beforeContent = beforeRevision != null ? beforeRevision.getContent() : "";
                String afterContent = afterRevision != null ? afterRevision.getContent() : "";

                if (beforeContent == null) {
                    beforeContent = "";
                }
                if (afterContent == null) {
                    afterContent = "";
                }

                // 如果内容相同，返回 null
                if (beforeContent.equals(afterContent)) {
                    return null;
                }

                List<LineFragment> fragments = ComparisonManager.getInstance()
                    .compareLines(beforeContent, afterContent, ComparisonPolicy.DEFAULT,
                                  ProgressIndicatorProvider.getGlobalProgressIndicator());
                if (fragments.isEmpty()) {
                    return null;
                }

                String beforeFileName = beforeRevision != null ? beforeRevision.getFile().getName() : "null";
                String afterFileName = afterRevision != null ? afterRevision.getFile().getName() : "null";
                String diff;
                if (shouldSummarizeLargeDiff(beforeContent, afterContent, fragments)) {
                    diff = buildLargeDiffSummary(beforeFileName, afterFileName, beforeContent, afterContent, fragments);
                } else {
                    // 生成简单的 unified diff 格式
                    diff = generateUnifiedDiffInternal(
                        beforeFileName,
                        afterFileName,
                        beforeContent,
                        afterContent,
                        change.getVirtualFile(),
                        fragments
                                                      );
                }
                return diff.isEmpty() ? null : new DiffResult(diff, beforeContent, afterContent, fragments);
            } catch (com.intellij.openapi.progress.ProcessCanceledException e) {
                throw e;
            } catch (Exception e) {
                // 忽略异常，返回 null
                return null;
            }
        });
    }

    /**
     * 生成 Unified Diff 格式的字符串
     *
     * @param beforeFileName 修改前的文件名
     * @param afterFileName  修改后的文件名
     * @param beforeContent  修改前的内容
     * @param afterContent   修改后的内容
     * @param virtualFile    虚拟文件对象, 可以为空
     * @return Unified Diff 格式的字符串
     */
    @NotNull
    public static String generateUnifiedDiff(@NotNull String beforeFileName,
                                             @NotNull String afterFileName,
                                             @NotNull String beforeContent,
                                             @NotNull String afterContent,
                                             @Nullable VirtualFile virtualFile) {
        List<LineFragment> fragments = ComparisonManager.getInstance()
            .compareLines(beforeContent, afterContent, ComparisonPolicy.DEFAULT,
                          ProgressIndicatorProvider.getGlobalProgressIndicator());
        if (fragments.isEmpty()) {
            return "";
        }
        return generateUnifiedDiffInternal(beforeFileName, afterFileName, beforeContent, afterContent, virtualFile, fragments);
    }

    /**
     * 生成 Unified Diff 格式的字符串
     * <p> 根据变更片段生成统一的 diff 内容, 用于展示代码差异. 会处理多个 diff 块, 并跳过一些无意义的变更, 如仅空白字符, 导入语句, 注释或顺序调整等.
     *
     * @param beforeFileName 修改前的文件名
     * @param afterFileName  修改后的文件名
     * @param beforeContent  修改前的内容
     * @param afterContent   修改后的内容
     * @param virtualFile    虚拟文件对象, 可以为空
     * @return 生成的 Unified Diff 字符串, 如果无有效变更则返回空字符串
     */
    private static String generateUnifiedDiffInternal(@NotNull String beforeFileName,
                                                      @NotNull String afterFileName,
                                                      @NotNull String beforeContent,
                                                      @NotNull String afterContent,
                                                      @Nullable VirtualFile virtualFile,
                                                      @NotNull List<LineFragment> fragments) {
        boolean isJavaFile = virtualFile != null && "java".equalsIgnoreCase(virtualFile.getExtension());

        StringBuilder diff = new StringBuilder();
        diff.append("--- ").append(beforeFileName).append("\n");
        diff.append("+++ ").append(afterFileName).append("\n");
        boolean hasChanges = false;
        int hunkCount = 0;

        String[] beforeLines = beforeContent.split("\n", -1);
        String[] afterLines = afterContent.split("\n", -1);

        for (LineFragment fragment : fragments) {
            if (hunkCount >= MAX_HUNKS_PER_FILE) {
                break;
            }
            int beforeStart = fragment.getStartLine1();
            int beforeEnd = fragment.getEndLine1();
            int afterStart = fragment.getStartLine2();
            int afterEnd = fragment.getEndLine2();
            List<String> beforeChanged = extractLines(beforeLines, beforeStart, beforeEnd);
            List<String> afterChanged = extractLines(afterLines, afterStart, afterEnd);
            if (isWhitespaceOnlyChange(beforeChanged, afterChanged)) {
                continue;
            }
            if (isImportOnlyChange(beforeChanged, afterChanged)) {
                continue;
            }
            if (isJavaFile && isCommentOnlyChange(beforeChanged, afterChanged)) {
                continue;
            }
            if (isJavaFile && isReorderOnlyChange(beforeChanged, afterChanged)) {
                continue;
            }
            List<String> beforeOutput = filterNonIgnorableLines(beforeChanged);
            List<String> afterOutput = filterNonIgnorableLines(afterChanged);
            if (beforeOutput.isEmpty() && afterOutput.isEmpty()) {
                continue;
            }
            beforeOutput = trimLines(beforeOutput, MAX_LINES_PER_HUNK);
            afterOutput = trimLines(afterOutput, MAX_LINES_PER_HUNK);
            hasChanges = true;
            diff.append("@@ -").append(beforeStart + 1).append(",").append(beforeEnd - beforeStart)
                .append(" +").append(afterStart + 1).append(",").append(afterEnd - afterStart)
                .append(" @@\n");

            // 使用 SPI 解析上下文(需要扩展多种语言支持)
            String context = resolveSymbolContext(virtualFile, afterStart, beforeStart);
            if (context != null && !context.isEmpty()) {
                diff.append("上下文: ").append(context).append("\n");
            }
            for (String line : beforeOutput) {
                diff.append("-").append(truncateLine(line, MAX_DIFF_LINE_CHARS)).append("\n");
            }
            for (String line : afterOutput) {
                diff.append("+").append(truncateLine(line, MAX_DIFF_LINE_CHARS)).append("\n");
            }
            hunkCount++;
        }

        return hasChanges ? diff.toString() : "";
    }

    /**
     * 判断是否需要对代码差异进行摘要模式处理
     * <p> 当文件内容长度, 文件行数或变更行数超过预设阈值时, 返回 true, 表示应启用摘要模式以避免输出过长的差异内容.
     *
     * @param beforeContent 修改前的文件内容, 不能为空
     * @param afterContent  修改后的文件内容, 不能为空
     * @param fragments     差异片段列表, 不能为空
     * @return 如果满足任一摘要条件 (内容长度, 文件行数或变更行数超过阈值), 则返回 true, 否则返回 false
     */
    private static boolean shouldSummarizeLargeDiff(@NotNull String beforeContent,
                                                    @NotNull String afterContent,
                                                    @NotNull List<LineFragment> fragments) {
        if (beforeContent.length() > LARGE_DIFF_MAX_CHARS || afterContent.length() > LARGE_DIFF_MAX_CHARS) {
            return true;
        }
        int beforeLines = countLines(beforeContent);
        int afterLines = countLines(afterContent);
        if (beforeLines > LARGE_FILE_MAX_LINES || afterLines > LARGE_FILE_MAX_LINES) {
            return true;
        }
        int[] changed = countChangedLines(fragments);
        return Math.max(changed[0], changed[1]) > LARGE_DIFF_MAX_CHANGED_LINES;
    }

    /**
     * 构建大型差异文件的摘要信息
     * <p> 当文件差异过大时, 生成包含头部和尾部内容的摘要格式差异信息
     * <p> 该方法会统计变更行数并添加摘要说明, 然后调用 appendHeadTail 方法显示文件的头部和尾部内容
     *
     * @param beforeFileName 修改前的文件名
     * @param afterFileName  修改后的文件名
     * @param beforeContent  修改前的内容
     * @param afterContent   修改后的内容
     * @param fragments      差异片段列表
     * @return 包含摘要信息的差异字符串, 显示文件的变更统计和头尾内容
     */
    private static String buildLargeDiffSummary(@NotNull String beforeFileName,
                                                @NotNull String afterFileName,
                                                @NotNull String beforeContent,
                                                @NotNull String afterContent,
                                                @NotNull List<LineFragment> fragments) {
        int[] changed = countChangedLines(fragments);
        StringBuilder diff = new StringBuilder();
        diff.append("--- ").append(beforeFileName).append("\n");
        diff.append("+++ ").append(afterFileName).append("\n");
        diff.append("@@ summary @@\n");
        diff.append("文件: ").append(afterFileName).append("\n");
        diff.append("变更行数: +").append(changed[0]).append(" / -").append(changed[1]).append("\n");
        diff.append("diff too large, showing head/tail\n");

        appendHeadTail(diff, beforeContent, afterContent);
        return diff.toString();
    }

    /**
     * 构建大文件的摘要信息（不读取全文）
     */
    @NotNull
    private static String buildLargeFileSummary(@NotNull String beforeFileName,
                                                @NotNull String afterFileName,
                                                long fileSizeBytes,
                                                @NotNull String reason) {
        StringBuilder diff = new StringBuilder();
        diff.append("--- ").append(beforeFileName).append("\n");
        diff.append("+++ ").append(afterFileName).append("\n");
        diff.append("@@ summary @@\n");
        diff.append("文件: ").append(afterFileName).append("\n");
        diff.append("内容过大，已省略 (size: ").append(formatSize(fileSizeBytes))
            .append(", reason: ").append(reason).append(")\n");
        return diff.toString();
    }

    /**
     * 构建大差异摘要的头尾内容
     * <p> 当差异内容过大时, 此方法用于生成摘要模式下的头部和尾部展示内容.
     * 它会将修改前后的内容分别分割成行, 然后提取头部和尾部各 N 行进行展示,
     * 其中 N 由常量 LARGE_DIFF_HEAD_TAIL_LINES 定义
     *
     * @param diff          差异内容的字符串构建器, 用于追加头尾内容
     * @param beforeContent 修改前的文件内容
     * @param afterContent  修改后的文件内容
     */
    private static void appendHeadTail(@NotNull StringBuilder diff,
                                       @NotNull String beforeContent,
                                       @NotNull String afterContent) {
        List<String> beforeLines = splitLines(beforeContent);
        List<String> afterLines = splitLines(afterContent);

        diff.append("--- head ---\n");
        appendPrefixedLines(diff, "-", beforeLines, 0, Math.min(LARGE_DIFF_HEAD_TAIL_LINES, beforeLines.size()));
        appendPrefixedLines(diff, "+", afterLines, 0, Math.min(LARGE_DIFF_HEAD_TAIL_LINES, afterLines.size()));

        diff.append("--- tail ---\n");
        int beforeStart = Math.max(0, beforeLines.size() - LARGE_DIFF_HEAD_TAIL_LINES);
        int afterStart = Math.max(0, afterLines.size() - LARGE_DIFF_HEAD_TAIL_LINES);
        appendPrefixedLines(diff, "-", beforeLines, beforeStart, beforeLines.size());
        appendPrefixedLines(diff, "+", afterLines, afterStart, afterLines.size());
    }

    /**
     * 将指定范围内的行追加到 {@code diff}, 并在每行前加上 {@code prefix}.
     *
     * @param diff   目标 {@link StringBuilder}, 用于接收追加的内容
     * @param prefix 每行前追加的前缀字符串
     * @param lines  需要追加的行列表
     * @param start  起始索引 (包含)
     * @param end    结束索引 (不包含)
     */
    private static void appendPrefixedLines(@NotNull StringBuilder diff,
                                            @NotNull String prefix,
                                            @NotNull List<String> lines,
                                            int start,
                                            int end) {
        for (int i = start; i < end; i++) {
            diff.append(prefix).append(truncateLine(lines.get(i), MAX_DIFF_LINE_CHARS)).append("\n");
        }
    }

    /**
     * 将字符串按行分割为字符串列表
     * <p> 根据换行符将输入字符串分割成行, 并返回包含所有行的不可变列表. 如果输入字符串为空, 则返回空列表.
     *
     * @param content 要分割的字符串, 不能为空
     * @return 分割后的行列表, 如果输入为空则返回空列表
     */
    @NotNull
    private static List<String> splitLines(@NotNull String content) {
        if (content.isEmpty()) {
            return List.of();
        }
        return Arrays.asList(content.split("\n", -1));
    }

    /**
     * 统计代码变更中的新增和删除行数
     * <p> 遍历所有差异块, 计算每个块中新增和删除的行数, 并返回一个包含这两个值的数组.
     *
     * @param fragments 差异块列表, 不能为 null
     * @return 包含新增行数和删除行数的整数数组, 索引 0 表示新增行数, 索引 1 表示删除行数
     */
    private static int[] countChangedLines(@NotNull List<LineFragment> fragments) {
        int added = 0;
        int deleted = 0;
        for (LineFragment fragment : fragments) {
            deleted += fragment.getEndLine1() - fragment.getStartLine1();
            added += fragment.getEndLine2() - fragment.getStartLine2();
        }
        return new int[] {added, deleted};
    }

    /**
     * 统计字符串中换行符的数量以计算行数
     * <p>该方法用于计算给定字符串中包含的行数, 通过遍历字符串并统计换行符 (\\n) 的数量实现. 若字符串为空, 则返回 0.
     *
     * @param content 要统计行数的字符串, 不能为空
     * @return 字符串中的行数, 若内容为空则返回 0
     */
    private static int countLines(@NotNull String content) {
        if (content.isEmpty()) {
            return 0;
        }
        int count = 1;
        for (int i = 0; i < content.length(); i++) {
            if (content.charAt(i) == '\n') {
                count++;
            }
        }
        return count;
    }

    /**
     * 构建删除文件的差异内容
     * <p> 生成表示文件已被删除的统一差异格式字符串, 包含文件名, 删除标记和状态信息.
     * 该方法用于在代码差异分析中, 当文件被删除时提供标准化的差异输出.
     *
     * @param fileName 被删除文件的名称, 不能为空
     * @return 表示文件被删除的差异内容字符串, 格式为标准的 Unified Diff 格式
     */
    @NotNull
    private static String buildDeletedFileDiff(@NotNull String fileName) {
        return "--- " + fileName + "\n" +
               "+++ /dev/null\n" +
               "deleted file\n";
    }

    /**
     * 判断重命名操作是否未包含内容变更
     * <p> 当重命名操作的差异结果为 null, 无差异片段, 或差异内容为空时, 认为该重命名未包含内容变更.
     *
     * @param diffResult 差异结果对象, 可能为 null
     * @return 如果重命名未包含内容变更, 则返回 true, 否则返回 false
     */
    private static boolean isRenameWithoutContentChange(@Nullable DiffResult diffResult) {
        return diffResult == null || diffResult.fragments().isEmpty() || diffResult.diffContent().isBlank();
    }

    /**
     * 构建文件重命名的摘要信息
     * <p> 当文件被重命名时, 生成包含原始路径和目标路径的摘要字符串, 用于在代码差异中标识文件移动操作.
     * 该方法适用于在版本控制系统中识别文件重命名而非内容修改的场景.
     *
     * @param change 变更对象, 不能为空, 用于获取重命名前后的文件路径
     * @return 表示文件从原始路径移动到新路径的摘要字符串, 格式为 "moved from {beforePath} to {afterPath}"
     */
    @NotNull
    private static String buildMovedFileSummary(@NotNull Change change) {
        String beforePath = change.getBeforeRevision() != null ? change.getBeforeRevision().getFile().getPath() : "unknown";
        String afterPath = change.getAfterRevision() != null ? change.getAfterRevision().getFile().getPath() : "unknown";
        return "moved from " + beforePath + " to " + afterPath;
    }

    /**
     * 构建新增文件的预览内容
     * <p> 用于在文件被新增时, 生成其前几行内容作为预览摘要. 若文件为二进制文件或内容为空, 则返回 null.
     * 该方法会根据文件类型判断是否需要剥离导入语句 (如 Java/Kotlin 文件), 并最多保留 50 行内容.
     *
     * @param change      变更对象, 不能为空, 用于获取新增文件的修订内容
     * @param virtualFile 虚拟文件对象, 不能为空, 用于判断文件类型和加载文件内容
     * @return 新增文件的前几行内容组成的字符串, 最多 50 行, 若文件为二进制, 内容为空或加载失败则返回 null
     */
    @Nullable
    private static String buildAddedFilePreview(@NotNull Change change, @NotNull VirtualFile virtualFile) {
        if (virtualFile.getFileType().isBinary()) {
            return null;
        }
        if (shouldSkipLargeFileDiff(virtualFile)) {
            return buildLargeFileSummary("/dev/null", virtualFile.getName(), virtualFile.getLength(), "large file");
        }
        String content = null;
        ContentRevision afterRevision = change.getAfterRevision();
        if (afterRevision != null) {
            content = safeGetContent(afterRevision);
        }
        if (content == null) {
            try {
                content = VfsUtilCore.loadText(virtualFile);
            } catch (IOException ignored) {
                return null;
            }
        }
        if (content.isEmpty()) {
            return null;
        }
        List<String> lines = splitLines(content);
        boolean stripImports = isJavaLikeFile(virtualFile);
        StringBuilder preview = new StringBuilder();
        int lineCount = 0;
        for (String line : lines) {
            if (stripImports && line.trim().startsWith("import ")) {
                continue;
            }
            String truncated = truncateLine(line, MAX_DIFF_LINE_CHARS);
            if (preview.length() + truncated.length() + 1 > MAX_ADDED_PREVIEW_CHARS) {
                preview.append("...[truncated]\n");
                break;
            }
            preview.append(truncated).append("\n");
            lineCount++;
            if (lineCount >= 50) {
                break;
            }
        }
        String result = preview.toString().trim();
        return result.isEmpty() ? null : result;
    }

    /**
     * 判断文件是否为 Java 或 Kotlin 类型文件
     * <p> 根据文件扩展名判断是否为 Java,Kotlin 或 KTS 文件, 用于区分代码文件类型以进行特定处理.
     *
     * @param virtualFile 虚拟文件对象, 不能为空
     * @return 如果文件扩展名为 "java","kt" 或 "kts"(不区分大小写), 则返回 true, 否则返回 false
     */
    private static boolean isJavaLikeFile(@NotNull VirtualFile virtualFile) {
        String extension = virtualFile.getExtension();
        if (extension == null) {
            return false;
        }
        return "java".equalsIgnoreCase(extension)
               || "kt".equalsIgnoreCase(extension)
               || "kts".equalsIgnoreCase(extension);
    }

    /**
     * 安全获取内容
     * <p> 尝试从给定的 ContentRevision 对象中获取内容字符串. 如果在获取过程中发生异常, 则返回 null.
     *
     * @param revision 内容修订对象, 不能为空
     * @return 返回内容字符串, 若发生异常则返回 null
     */
    @Nullable
    private static String safeGetContent(@NotNull ContentRevision revision) {
        try {
            return revision.getContent();
        } catch (Exception e) {
            return null;
        }
    }
    /**
     * 从字符串数组中提取指定范围的行内容
     * <p> 根据起始索引和结束索引从字符串数组中提取行内容, 返回包含指定范围行的列表
     * <p> 注意: 提取范围为 [start, end), 即包含 start 但不包含 end, 且索引必须在有效范围内
     *
     * @param lines 字符串数组, 不能为 null
     * @param start 起始索引 (包含), 必须大于等于 0 且小于 lines.length
     * @param end   结束索引 (不包含), 必须大于等于 start 且小于等于 lines.length
     * @return 包含指定范围行的列表, 如果 lines 为空或范围无效则返回空列表
     */
    @NotNull
    private static List<String> extractLines(@NotNull String[] lines, int start, int end) {
        List<String> result = new ArrayList<>();
        for (int i = start; i < end && i < lines.length; i++) {
            result.add(lines[i]);
        }
        return result;
    }

    /**
     * 过滤非忽略行
     * <p> 从指定的行列表中移除所有被标记为可忽略的行 (如空行), 并返回包含非忽略行的新列表
     * <p> 此方法用于在代码差异分析中排除无关的变更内容, 例如空白行或注释行
     *
     * @param lines 待处理的行列表, 不能为 null
     * @return 过滤后的行列表, 不包含任何可忽略的行, 如果输入为空或所有行都被忽略, 则返回空列表
     */
    @NotNull
    private static List<String> filterNonIgnorableLines(@NotNull List<String> lines) {
        List<String> result = new ArrayList<>();
        for (String line : lines) {
            if (notIgnorableLine(line)) {
                result.add(line);
            }
        }
        return result;
    }

    /**
     * 判断两个代码行列表是否仅包含空白字符的变化
     * <p>比较两个代码行列表, 检查它们是否在内容上完全相同, 仅因空白字符 (如空格, 制表符) 不同而有所差异.
     * 如果两个列表大小不同, 或存在非空白字符的差异, 则返回 false.
     *
     * @param beforeLines 修改前的代码行列表, 不能为空
     * @param afterLines  修改后的代码行列表, 不能为空
     * @return 如果两个列表仅包含空白字符的变化, 则返回 true; 否则返回 false
     */
    private static boolean isWhitespaceOnlyChange(@NotNull List<String> beforeLines,
                                                  @NotNull List<String> afterLines) {
        if (beforeLines.size() != afterLines.size()) {
            return false;
        }
        for (int i = 0; i < beforeLines.size(); i++) {
            if (!normalizeLine(beforeLines.get(i)).equals(normalizeLine(afterLines.get(i)))) {
                return false;
            }
        }
        return true;
    }

    /**
     * 判断变更是否仅包含导入语句的修改
     * <p>
     * 检查变更中的所有行是否都为 import 语句或空行. 如果存在非 import 语句, 则认为该变更是非纯导入变更.
     *
     * @param beforeLines 修改前的代码行列表, 不能为 null
     * @param afterLines  修改后的代码行列表, 不能为 null
     * @return 如果变更仅包含 import 语句和空行, 则返回 true; 否则返回 false
     */
    private static boolean isImportOnlyChange(@NotNull List<String> beforeLines,
                                              @NotNull List<String> afterLines) {
        if (beforeLines.isEmpty() && afterLines.isEmpty()) {
            return false;
        }
        for (String line : beforeLines) {
            if (notImportLine(line) && notIgnorableLine(line)) {
                return false;
            }
        }
        for (String line : afterLines) {
            if (notImportLine(line) && notIgnorableLine(line)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 判断是否为仅注释变更
     * <p> 检查前后代码行列表是否仅包含注释变更, 即代码结构未变, 仅修改了注释内容
     * <p> 该方法会先剥离 Java 注释 (包括单行注释和多行注释), 然后比较处理后的文本内容
     * <p> 如果剥离注释后前后内容完全相同且为空, 则认为是仅注释变更
     *
     * @param beforeLines 修改前的代码行列表, 不能为 null
     * @param afterLines  修改后的代码行列表, 不能为 null
     * @return 如果前后代码仅存在注释变更, 则返回 true, 否则返回 false
     */
    private static boolean isCommentOnlyChange(@NotNull List<String> beforeLines,
                                               @NotNull List<String> afterLines) {
        String before = stripJavaComments(String.join("\n", beforeLines));
        String after = stripJavaComments(String.join("\n", afterLines));
        return normalizeLine(before).equals(normalizeLine(after))
               && normalizeLine(before).isEmpty()
               && normalizeLine(after).isEmpty();
    }

    /**
     * 判断两组行是否仅为顺序调整的变更
     * <p> 该方法首先检查两组行数是否相同, 若不同则直接返回 false.
     * 随后对两组行进行标准化处理 (去除空格和空白行), 并比较其内容是否一致.
     * 如果原始顺序不一致但排序后一致, 则判定为仅顺序调整的变更.
     *
     * @param beforeLines 变更前的代码行列表
     * @param afterLines  变更后的代码行列表
     * @return 如果是仅顺序调整的变更则返回 true, 否则返回 false
     */
    private static boolean isReorderOnlyChange(@NotNull List<String> beforeLines,
                                               @NotNull List<String> afterLines) {
        if (beforeLines.size() != afterLines.size()) {
            return false;
        }
        List<String> beforeNormalized = normalizeLines(beforeLines);
        List<String> afterNormalized = normalizeLines(afterLines);
        if (beforeNormalized.equals(afterNormalized)) {
            return false;
        }
        beforeNormalized.sort(String::compareTo);
        afterNormalized.sort(String::compareTo);
        return beforeNormalized.equals(afterNormalized);
    }

    /**
     * 判断字符串是否为导入语句
     * <p> 检查给定的代码行是否以 "import" 开头, 用于识别 Java 或 Kotlin 中的导入声明
     *
     * @param line 要检查的代码行, 不能为空
     * @return 如果行以 "import" 开头则返回 true, 否则返回 false
     */
    private static boolean notImportLine(@NotNull String line) {
        String trimmed = line.trim();
        return !trimmed.startsWith("import ");
    }

    /**
     * 判断一行是否为可忽略行
     * <p> 该方法用于判断某一行内容是否为空白行, 如果是空白行则返回 true
     *
     * @param line 要判断的行内容
     * @return 如果是空白行则返回 true, 否则返回 false
     */
    private static boolean notIgnorableLine(@NotNull String line) {
        String trimmed = line.trim();
        return !trimmed.isEmpty();
    }

    /**
     * 对字符串行进行规范化处理, 移除所有空白字符
     * <p> 该方法用于去除字符串中的所有空白字符 (包括空格, 制表符, 换行符等), 返回一个仅包含非空白字符的字符串
     *
     * @param line 需要规范化的字符串行
     * @return 移除所有空白字符后的字符串
     */
    @NotNull
    private static String normalizeLine(@NotNull String line) {
        return line.replaceAll("\\s+", "");
    }

    /**
     * 正常化行列表, 移除空行并标准化每行内容
     * <p> 遍历输入的行列表, 对每行应用 normalizeLine 方法进行标准化处理, 并过滤掉标准化后为空的行, 返回非空的标准化行列表
     * <p> 标准化过程包括移除行首尾空白字符, 并将行内连续空白字符替换为单个空格
     *
     * @param lines 输入的行列表, 不能为 null
     * @return 非空的标准化行列表, 如果输入为空或所有行标准化后为空, 则返回空列表
     */
    @NotNull
    private static List<String> normalizeLines(@NotNull List<String> lines) {
        List<String> result = new ArrayList<>();
        for (String line : lines) {
            String normalized = normalizeLine(line);
            if (!normalized.isEmpty()) {
                result.add(normalized);
            }
        }
        return result;
    }

    /**
     * 限制行数并截取列表
     * <p> 将输入的行列表限制为指定的最大行数, 如果行数超过限制, 则返回前 maxLines 行; 否则返回原始列表
     * <p> 使用示例:
     * <pre>{@code
     * List<String> limitedLines = trimLines(allLines, 10);
     * }</pre>
     *
     * @param lines    要处理的行列表, 不能为 null
     * @param maxLines 最大行数, 必须大于等于 0
     * @return 截取后的行列表, 如果行数不超过限制则返回原始列表, 否则返回前 maxLines 行
     */
    @NotNull
    private static List<String> trimLines(@NotNull List<String> lines, int maxLines) {
        if (lines.size() <= maxLines) {
            return lines;
        }
        return new ArrayList<>(lines.subList(0, maxLines));
    }

    /**
     * 判断是否应跳过大文件 diff 生成
     */
    private static boolean shouldSkipLargeFileDiff(@NotNull VirtualFile virtualFile) {
        long length = virtualFile.getLength();
        if (length <= 0) {
            return false;
        }
        if (length > MAX_DIFF_FILE_BYTES) {
            return true;
        }
        String extension = virtualFile.getExtension();
        if (extension == null) {
            return false;
        }
        return TEXT_ASSET_EXTENSIONS.contains(extension.toLowerCase(Locale.ROOT))
               && length >= MAX_TEXT_ASSET_BYTES;
    }

    /**
     * 截断超长单行，避免产生过长 diff
     */
    @NotNull
    private static String truncateLine(@NotNull String line, int maxChars) {
        if (line.length() <= maxChars) {
            return line;
        }
        String suffix = "...[truncated]";
        if (maxChars <= suffix.length()) {
            return line.substring(0, maxChars);
        }
        int limit = maxChars - suffix.length();
        return line.substring(0, limit) + suffix;
    }

    /**
     * 格式化文件大小
     */
    @NotNull
    private static String formatSize(long bytes) {
        if (bytes < 0) {
            return "unknown";
        }
        if (bytes < 1024) {
            return bytes + "B";
        }
        if (bytes < 1024 * 1024) {
            return (bytes / 1024) + "KB";
        }
        long mb = bytes / (1024 * 1024);
        long remainder = (bytes % (1024 * 1024)) / (1024 * 100);
        return mb + "." + remainder + "MB";
    }

    /**
     * 移除 Java 代码中的注释
     * <p> 该方法会移除字符串中所有的块注释 (/* ...
     */
    @NotNull
    private static String stripJavaComments(@NotNull String content) {
        String withoutBlock = content.replaceAll("(?s)/\\*.*?\\*/", "");
        return withoutBlock.replaceAll("(?m)//.*?$", "");
    }

    /**
     * 解析并返回文件的作用域提示信息
     * <p> 根据虚拟文件解析出项目, 模块或路径作用域, 用于标识变更的上下文范围.
     *
     * @param virtualFile 虚拟文件对象, 不能为 null
     * @return 作用域提示字符串, 如果无法解析则返回 null
     */
    @Nullable
    private static String resolveScopeHint(@NotNull VirtualFile virtualFile) {
        Project project = ProjectLocator.getInstance().guessProjectForFile(virtualFile);
        if (project == null || project.isDisposed()) {
            return null;
        }
        Module module = ModuleUtilCore.findModuleForFile(virtualFile, project);
        String moduleName = module != null ? module.getName() : null;
        String pathScope = resolvePathScope(project, virtualFile);
        String primarySymbol = resolvePrimarySymbolName(project, virtualFile);
        String raw = moduleName != null ? moduleName : (pathScope != null ? pathScope : primarySymbol);
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        return normalizeScope(raw);
    }

    /**
     * 解析文件路径的作用域提示
     * <p> 根据项目基础路径和文件相对路径, 解析出文件所属的路径作用域.
     * 该作用域通常用于标识代码在项目中的位置, 如模块, 包或目录结构.
     *
     * @param project     项目对象, 用于获取项目基础路径
     * @param virtualFile 虚拟文件对象, 表示具体的文件路径
     * @return 文件路径的作用域字符串, 如果无法解析则返回 null
     */
    @Nullable
    private static String resolvePathScope(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        String basePath = project.getBasePath();
        if (basePath == null) {
            return null;
        }
        String fullPath = virtualFile.getPath();
        if (!fullPath.startsWith(basePath)) {
            return null;
        }
        String relative = fullPath.substring(basePath.length());
        if (relative.startsWith("/")) {
            relative = relative.substring(1);
        }
        if (relative.isEmpty()) {
            return null;
        }
        String[] segments = relative.split("/");
        for (String segment : segments) {
            if (segment.isEmpty()) {
                continue;
            }
            if (isCommonSourceRoot(segment)) {
                continue;
            }
            return segment;
        }
        return null;
    }

    /**
     * 判断指定的文件夹段是否为常见的源代码根目录
     * <p> 该方法用于识别常见的源代码根目录名称, 如 "src", "main", "test" 等.
     *
     * @param segment 文件夹名称片段
     * @return 如果是常见源代码根目录则返回 true, 否则返回 false
     */
    private static boolean isCommonSourceRoot(@NotNull String segment) {
        String value = segment.toLowerCase(Locale.ROOT);
        return "src".equals(value)
               || "main".equals(value)
               || "test".equals(value)
               || "java".equals(value)
               || "kotlin".equals(value)
               || "resources".equals(value);
    }

    /**
     * 解析并获取文件中的主要符号名称
     * <p> 通过指定的项目和虚拟文件, 调用上下文解析器注册表获取文件中的主要符号名称 (如类名, 函数名等)
     * <p> 该方法通常用于代码分析, 重构或版本控制变更的上下文识别场景
     *
     * @param project     项目对象, 不能为 null
     * @param virtualFile 虚拟文件对象, 不能为 null
     * @return 主要符号名称, 如果无法解析则返回 null
     */
    @Nullable
    private static String resolvePrimarySymbolName(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        return ContextResolverRegistry.resolvePrimarySymbolName(project, virtualFile);
    }

    /**
     * 解析 PSI 语义摘要
     * <p> 基于 diff 片段尝试生成结构化语义说明，仅在 PSI 可用时启用。
     *
     * @param virtualFile  虚拟文件
     * @param diffResult   diff 结果
     * @return 语义摘要文本，无法解析时返回 null
     */
    @Nullable
    private static String resolveSemanticSummary(@NotNull VirtualFile virtualFile,
                                                 @Nullable DiffResult diffResult) {
        if (diffResult == null || diffResult.fragments().isEmpty()) {
            return null;
        }
        Project project = ProjectLocator.getInstance().guessProjectForFile(virtualFile);
        if (project == null || project.isDisposed()) {
            return null;
        }
        return ContextResolverRegistry.resolveSemanticSummary(project,
                                                              virtualFile,
                                                              diffResult.beforeContent(),
                                                              diffResult.afterContent(),
                                                              diffResult.fragments());
    }

    /**
     * 标准化作用域名称
     * <p> 将原始作用域名称转换为小写并使用连字符分隔的标准化格式
     * <p> 该方法会移除非字母数字字符, 将驼峰命名转换为连字符分隔, 并去除首尾连字符
     * <p> 使用示例:
     * <pre>{@code
     * String normalized = normalizeScope("MyComponent");
     * // 结果: "my-component"
     *
     * String normalized2 = normalizeScope("src/main/java");
     * // 结果: "src-main-java"
     *
     * String normalized3 = normalizeScope("test");
     * // 结果: "test"
     * }</pre>
     *
     * @param raw 原始作用域名称, 不能为空
     * @return 标准化后的作用域名称, 已转换为小写并使用连字符分隔
     */
    @NotNull
    private static String normalizeScope(@NotNull String raw) {
        String trimmed = raw.trim();
        String dashed = trimmed.replaceAll("([a-z0-9])([A-Z])", "$1-$2")
            .replaceAll("[^a-zA-Z0-9]+", "-")
            .replaceAll("^-+|-+$", "");
        return dashed.toLowerCase(Locale.ROOT);
    }

    /**
     * 解析文件中的符号上下文信息
     * <p> 根据指定的文件和行号解析出相关的代码上下文, 用于标识变更内容在项目结构中的位置
     *
     * @param virtualFile   虚拟文件对象, 表示需要解析的文件
     * @param preferredLine 优先使用的行号, 通常为修改后的起始行
     * @param fallbackLine  备用行号, 当首选行号无效时使用
     * @return 解析出的符号上下文字符串, 如果无法解析则返回 null
     */
    @Nullable
    private static String resolveSymbolContext(@Nullable VirtualFile virtualFile,
                                               int preferredLine,
                                               int fallbackLine) {
        if (virtualFile == null) {
            return null;
        }
        return ContextResolverRegistry.resolveContext(virtualFile, preferredLine, fallbackLine);
    }

    /**
     * diff 结果数据结构
     */
    private record DiffResult(@NotNull String diffContent,
                              @NotNull String beforeContent,
                              @NotNull String afterContent,
                              @NotNull List<LineFragment> fragments) {
    }
}
