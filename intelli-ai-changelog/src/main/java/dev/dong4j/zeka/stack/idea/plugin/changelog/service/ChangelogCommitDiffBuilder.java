package dev.dong4j.zeka.stack.idea.plugin.changelog.service;

import com.intellij.openapi.diff.impl.patch.FilePatch;
import com.intellij.openapi.diff.impl.patch.IdeaTextPatchBuilder;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.patch.PatchWriter;
import com.intellij.openapi.vfs.VirtualFile;

import org.eclipse.jgit.diff.DiffEntry;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import dev.dong4j.zeka.stack.idea.plugin.changelog.model.CodeDiff;
import dev.dong4j.zeka.stack.idea.plugin.changelog.settings.SettingsState;
import dev.dong4j.zeka.stack.idea.plugin.changelog.util.CodeDiffUtil;

/** 提交消息 diff 构建器 */
final class ChangelogCommitDiffBuilder {

    /** 最大 diff 文件数量限制 */
    private static final int MAX_DIFF_FILES = 50;
    /** 自动模式下使用原生 patch 的文件数量上限 */
    private static final int AUTO_PROVIDER_MAX_FILES = 10;
    /** 超大文件阈值（20MB） */
    private static final long MAX_FILE_SIZE_BYTES = 20L * 1024 * 1024;
    /** 单行内容过长时判定为噪音 */
    private static final int MAX_SINGLE_LINE_LENGTH = 300;

    /** 项目实例, 用于获取基础路径和执行相关操作 */
    private final Project project;

    /**
     * 构造函数, 初始化 ChangelogCommitDiffBuilder 对象
     * <p> 使用给定的项目对象进行初始化
     *
     * @param project 项目对象, 不能为 null
     */
    ChangelogCommitDiffBuilder(@NotNull Project project) {
        this.project = project;
    }

    /**
     * 构建提交消息的差异负载
     * <p> 根据配置的差异提供者类型选择不同的构建策略:
     * <ul>
     *   <li> 如果配置为 {@link SettingsState.CommitMessageDiffProvider#IDEA_PATCH}, 则使用 IDEA 补丁格式构建负载 </li>
     *   <li> 否则使用代码差异格式构建负载 </li>
     * </ul>
     * <p> 该方法用于生成包含代码差异, 元数据和补丁文本的差异负载对象, 供后续提交消息处理使用.
     *
     * @param changes 要处理的变更集合, 不能为 null
     * @return 差异负载对象, 包含代码差异, 路径元数据和完整补丁文本
     */
    @NotNull
    DiffPayload buildPayload(@NotNull Collection<Change> changes) {
        // 过滤无关/噪音变更，避免生成 commit message 时引入无效上下文。
        List<Change> filteredChanges = filterChanges(changes);
        SettingsState settings = SettingsState.getInstance();
        SettingsState.CommitMessageDiffProvider provider = settings.commitMessageDiffProvider;
        if (provider == SettingsState.CommitMessageDiffProvider.AUTO) {
            provider = filteredChanges.size() > AUTO_PROVIDER_MAX_FILES
                       ? SettingsState.CommitMessageDiffProvider.CODE_DIFF
                       : SettingsState.CommitMessageDiffProvider.IDEA_PATCH;
        }
        if (provider == SettingsState.CommitMessageDiffProvider.IDEA_PATCH) {
            return buildIdeaPatchDiffPayload(filteredChanges);
        }
        return buildCodeDiffPayload(filteredChanges);
    }

    /**
     * 构建代码差异数据负载
     * <p> 从给定的变更集合中提取代码差异, 并创建一个包含这些差异的 DiffPayload 对象.
     * <p> 该方法生成的 DiffPayload 不包含元数据和完整的补丁文本.
     *
     * @param changes 需要处理的变更集合, 不能为 null
     * @return 一个 DiffPayload 实例, 包含提取出的 CodeDiff 列表, 空的元数据映射和空的补丁文本
     */
    private @NotNull DiffPayload buildCodeDiffPayload(@NotNull Collection<Change> changes) {
        List<CodeDiff> codeDiffs = filterCodeDiffs(CodeDiffUtil.extractCodeDiffs(changes));
        return new DiffPayload(codeDiffs, Map.of(), "");
    }

    /**
     * 构建 IDEA 补丁格式的差异负载
     * <p> 根据给定的变化集合, 提取代码差异, 构建补丁文本, 并生成包含元数据的差异负载对象
     *
     * @param changes 变化集合, 不能为 null
     * @return 包含代码差异, 元数据和补丁文本的差异负载对象
     */
    private @NotNull DiffPayload buildIdeaPatchDiffPayload(@NotNull Collection<Change> changes) {
        List<CodeDiff> codeDiffs = filterCodeDiffs(CodeDiffUtil.extractCodeDiffs(changes));
        Map<String, String> metadataByPath = buildPatchMetadataByPath(changes);
        String patchText = buildPatchText(changes);
        return new DiffPayload(codeDiffs, metadataByPath, patchText);
    }

    /**
     * 过滤变更列表，剔除二进制、大文件和默认忽略规则命中的文件
     * <p> 该过滤是 commit message 场景的降噪第一步，避免无意义内容进入 diff 构建。
     */
    @NotNull
    private List<Change> filterChanges(@NotNull Collection<Change> changes) {
        List<Change> filtered = new ArrayList<>();
        for (Change change : changes) {
            if (filtered.size() >= MAX_DIFF_FILES) {
                break;
            }
            if (shouldExcludeChange(change)) {
                continue;
            }
            filtered.add(change);
        }
        return filtered;
    }

    /**
     * 判断是否应当忽略某个变更
     */
    private boolean shouldExcludeChange(@NotNull Change change) {
        VirtualFile file = change.getVirtualFile();
        if (file == null) {
            return true;
        }
        FileType fileType = file.getFileType();
        if (fileType.isBinary()) {
            return true;
        }
        if (file.getLength() > MAX_FILE_SIZE_BYTES) {
            return true;
        }
        return matchesIgnorePattern(file.getPath());
    }

    /**
     * 针对配置的忽略模式进行匹配
     */
    private boolean matchesIgnorePattern(@NotNull String filePath) {
        SettingsState settings = SettingsState.getInstance();
        List<String> excludePatterns = settings.excludePatterns != null && !settings.excludePatterns.isEmpty()
                                       ? settings.excludePatterns
                                       : SettingsState.getDefaultExcludePatterns();

        String basePath = project.getBasePath();
        Path candidatePath = Paths.get(filePath);
        Path relativePath = candidatePath;
        if (basePath != null) {
            try {
                Path base = Paths.get(basePath);
                if (candidatePath.startsWith(base)) {
                    relativePath = base.relativize(candidatePath);
                }
            } catch (Exception ignored) {
                // 路径异常时直接使用原始路径
            }
        }
        String normalized = relativePath.toString().replace('\\', '/');
        Path normalizedPath = Paths.get(normalized);
        String fileName = normalizedPath.getFileName() != null ? normalizedPath.getFileName().toString() : normalized;

        for (String pattern : excludePatterns) {
            if (pattern == null || pattern.trim().isEmpty()) {
                continue;
            }
            PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern.trim());
            if (matcher.matches(normalizedPath) || matcher.matches(Paths.get(fileName))) {
                return true;
            }
            // 兼容大小写差异
            if (matcher.matches(Paths.get(normalized.toLowerCase(Locale.ROOT)))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 对 CodeDiff 进行二次过滤，剔除明显噪音内容
     */
    @NotNull
    private List<CodeDiff> filterCodeDiffs(@NotNull List<CodeDiff> codeDiffs) {
        List<CodeDiff> filtered = new ArrayList<>();
        for (CodeDiff diff : codeDiffs) {
            if (filtered.size() >= MAX_DIFF_FILES) {
                break;
            }
            if (shouldSkipDiffContent(diff)) {
                continue;
            }
            filtered.add(diff);
        }
        return filtered;
    }

    /**
     * 当 diff 内容包含超长单行时，视为噪音并过滤
     */
    private boolean shouldSkipDiffContent(@NotNull CodeDiff diff) {
        if (diff.diffContent == null || diff.diffContent.isBlank()) {
            return false;
        }
        String[] lines = diff.diffContent.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if ((trimmed.startsWith("+") || trimmed.startsWith("-"))
                && trimmed.length() > MAX_SINGLE_LINE_LENGTH) {
                return true;
            }
        }
        return false;
    }

    /**
     * 根据变更集合构建补丁元数据映射
     * <p> 首先调用 {@link #buildPatchText(Collection)} 方法生成补丁文本, 然后解析该文本以提取每个文件的元数据
     * <p> 如果补丁文本为空, 则返回一个空映射
     *
     * @param changes 变更集合, 不能为 null
     * @return 文件路径到补丁元数据的映射
     */
    private @NotNull Map<String, String> buildPatchMetadataByPath(@NotNull Collection<Change> changes) {
        String patchText = buildPatchText(changes);
        if (patchText.isBlank()) {
            return Map.of();
        }
        return parsePatchMetadataByPath(patchText);
    }

    /**
     * 构建补丁文本内容
     * <p> 根据提供的变更集合生成补丁文本, 用于提交信息的差异展示. 如果无法生成补丁文本, 则返回空字符串.
     *
     * @param changes 变更集合, 不能为 null 且不能为空
     * @return 补丁文本内容, 如果生成失败或没有补丁内容则返回空字符串
     */
    private @NotNull String buildPatchText(@NotNull Collection<Change> changes) {
        String basePath = project.getBasePath();
        if (basePath == null || changes.isEmpty()) {
            return "";
        }
        List<FilePatch> patches;
        try {
            patches = IdeaTextPatchBuilder.buildPatch(project, new ArrayList<>(changes), basePath, false);
        } catch (Exception ignored) {
            return "";
        }
        if (patches.isEmpty()) {
            return "";
        }
        Path basePathValue = Path.of(basePath);
        try {
            Path tempFile = Files.createTempFile("commit-diff-", ".patch");
            try {
                PatchWriter.writePatches(project, tempFile, basePathValue, patches, (String) null, null, StandardCharsets.UTF_8);
                return Files.readString(tempFile, StandardCharsets.UTF_8);
            } finally {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException ignored) {
                    // 忽略临时文件删除失败
                }
            }
        } catch (IOException ignored) {
            return "";
        }
    }

    /**
     * 解析补丁文本并按文件路径提取元数据
     * <p> 该方法读取补丁格式的文本内容, 逐行解析以提取每个文件路径对应的补丁元数据.
     * 元数据包括 diff 块之前的头部信息, 例如文件名, 修改前后的路径等.</p>
     *
     * @param patchText 补丁文本内容, 通常为 Git 格式的 diff 输出
     * @return 一个映射表, 键为文件路径, 值为对应的补丁元数据字符串
     */
    private @NotNull Map<String, String> parsePatchMetadataByPath(@NotNull String patchText) {
        Map<String, String> metadataByPath = new HashMap<>();
        String basePath = project.getBasePath();
        String currentPath = null;
        StringBuilder currentMeta = null;
        try (java.io.BufferedReader reader = new java.io.BufferedReader(new StringReader(patchText))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("diff --git ")) {
                    flushPatchMetadata(metadataByPath, currentPath, currentMeta);
                    String[] parts = line.split(" ");
                    String beforePath = parts.length > 2 ? stripDiffPrefix(parts[2], "a/") : "";
                    String afterPath = parts.length > 3 ? stripDiffPrefix(parts[3], "b/") : "";
                    currentPath = resolvePatchPath(basePath, beforePath, afterPath);
                    currentMeta = new StringBuilder();
                    currentMeta.append(line).append("\n");
                    continue;
                }
                if (currentMeta == null) {
                    continue;
                }
                if (line.startsWith("@@") || line.startsWith("--- ") || line.startsWith("+++ ")) {
                    flushPatchMetadata(metadataByPath, currentPath, currentMeta);
                    currentMeta = null;
                    currentPath = null;
                    continue;
                }
                currentMeta.append(line).append("\n");
            }
        } catch (IOException ignored) {
            return metadataByPath;
        }
        flushPatchMetadata(metadataByPath, currentPath, currentMeta);
        return metadataByPath;
    }

    /**
     * 将当前补丁元数据刷新到元数据映射中
     * <p> 如果路径或元数据为空, 则不执行任何操作
     * <p> 将非空的元数据字符串与对应的路径关联并存储在元数据映射中
     *
     * @param metadataByPath 元数据映射, 用于存储路径和对应的元数据
     * @param path           当前补丁的路径
     * @param metadata       当前补丁的元数据
     */
    private void flushPatchMetadata(@NotNull Map<String, String> metadataByPath,
                                    String path,
                                    StringBuilder metadata) {
        if (path == null || metadata == null || metadata.isEmpty()) {
            return;
        }
        metadataByPath.put(path, metadata.toString());
    }

    /**
     * 去除字符串前缀
     * <p> 如果字符串以指定前缀开头, 则返回去除前缀后的字符串; 否则返回原字符串
     *
     * @param value  要处理的字符串, 不能为 null
     * @param prefix 要去除的前缀, 不能为 null
     * @return 去除前缀后的字符串
     */
    private @NotNull String stripDiffPrefix(@NotNull String value, @NotNull String prefix) {
        return value.startsWith(prefix) ? value.substring(prefix.length()) : value;
    }

    /**
     * 解析补丁文件的完整路径
     * <p> 根据基础路径和补丁中的相对路径构造绝对路径. 如果 afterPath 非空且非 DEV_NULL, 则使用 afterPath, 否则使用 beforePath.
     * <p> 如果基础路径为空或路径本身为空, 则直接返回原始路径.
     *
     * @param basePath   项目的基础路径, 可能为 null
     * @param beforePath 补丁中修改前的文件路径 (相对于基础路径)
     * @param afterPath  补丁中修改后的文件路径 (相对于基础路径)
     * @return 构造后的完整文件路径, 若无法构造则返回原始路径
     */
    private @NotNull String resolvePatchPath(String basePath,
                                             @NotNull String beforePath,
                                             @NotNull String afterPath) {
        String path = !afterPath.isBlank() && !DiffEntry.DEV_NULL.equals(afterPath) ? afterPath : beforePath;
        if (basePath == null || basePath.isBlank() || path.isBlank()) {
            return path;
        }
        return new File(basePath, path).getPath();
    }

    /**
     * 差异数据载体类
     * <p> 用于封装代码差异信息, 包含具体的代码差异列表, 元数据映射以及完整的补丁文本
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.05
     * @since 1.0.0
     */
    record DiffPayload(@NotNull List<CodeDiff> codeDiffs,
                       @NotNull Map<String, String> metadataByPath,
                       @NotNull String fullPatchText) {
    }
}
