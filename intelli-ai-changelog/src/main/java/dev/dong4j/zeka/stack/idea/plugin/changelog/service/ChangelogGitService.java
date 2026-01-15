package dev.dong4j.zeka.stack.idea.plugin.changelog.service;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.RawText;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.AbbreviatedObjectId;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.EmptyTreeIterator;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import dev.dong4j.zeka.stack.idea.plugin.changelog.util.CodeDiffUtil;

/**
 * Git 变更日志服务类
 * <p>用于从 Git 仓库中读取提交记录, 生成变更差异文本, 获取最近提交消息列表等操作, 支持按提交哈希, 提交范围, 分支等条件查询变更信息.
 * <p>主要功能包括:
 * <ul>
 *   <li>根据提交哈希列表读取提交信息(含作者, 时间, 消息等)</li>
 *   <li>根据提交范围 (如 "a..b") 读取指定范围内的提交信息</li>
 *   <li>生成每个提交的变更差异文本(含文件增删改内容)</li>
 *   <li>获取最近指定数量的提交消息摘要</li>
 *   <li>检测当前项目是否为 Git 仓库</li>
 *   <li>获取当前分支名称</li>
 * </ul>
 * <p>支持通过项目路径或指定 Git 根目录初始化服务, 内部使用 JGit 库进行 Git 操作.
 * <p>使用示例:
 * <pre>{@code
 * ChangelogGitService service = new ChangelogGitService(project);
 * List<ChangelogCommitModels.CommitInfo> commits = service.readCommits(commitHashes);
 * List<ChangelogCommitModels.DiffCommitInfo> diffs = service.readCommitDiffs(commitHashes);
 * String recentMessages = service.buildRecentCommitMessagesText(10);
 * String currentBranch = service.getCurrentBranch();
 * }</pre>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.07
 * @since 1.0.0
 */
final class ChangelogGitService {

    /** 项目实例, 用于获取基础路径和执行 Git 操作 */
    public final Project project;

    /**
     * 构造函数, 初始化 ChangelogGitService 对象
     * <p> 使用给定的项目对象来初始化服务
     *
     * @param project 项目对象, 不能为 null
     */
    ChangelogGitService(@NotNull Project project) {
        this.project = project;
    }

    /**
     * 根据提交哈希列表读取提交信息
     * <p> 通过提供的提交哈希列表, 从 Git 仓库中获取对应的提交信息, 并返回包含提交名称, 简要信息, 完整信息, 作者和日期的 CommitInfo 对象列表.
     *
     * @param commitHashes 提交哈希列表, 不能为空
     * @return compound 提交信息列表, 如果仓库不可用或没有找到任何提交, 则返回空列表
     */
    @NotNull
    List<ChangelogCommitModels.CommitInfo> readCommits(@NotNull List<String> commitHashes) {
        List<ChangelogCommitModels.CommitInfo> commits = new ArrayList<>();
        Repository repository = getRepository();
        if (repository == null) {
            return commits;
        }

        try (repository; RevWalk revWalk = new RevWalk(repository)) {
            for (String hash : commitHashes) {
                try {
                    ObjectId commitId = repository.resolve(hash);
                    if (commitId == null) {
                        continue;
                    }
                    RevCommit commit = revWalk.parseCommit(commitId);
                    convert(commits, commit);
                } catch (Exception ignored) {
                    // 忽略单条提交解析失败
                }
            }
        } catch (Exception ignored) {
            // 忽略仓库读取异常
        }

        return commits;
    }

    /**
     * 从指定 Git 仓库的提交范围中读取提交信息
     * <p> 根据提供的 Git 仓库路径和提交范围 (如 "abc123..def456"), 获取该范围内的所有提交记录, 并将每条提交转换为 CommitInfo 对象返回.
     * <p> 若未提供范围, 则返回整个仓库的提交历史; 如果无法解析或访问仓库, 将返回空列表.
     *
     * @param gitRoot Git 仓库根目录路径
     * @param range   提交范围字符串, 格式为 "起始提交.. 结束提交", 可为 null 或空白
     * @return 提交信息列表, 每个元素包含提交哈希, 简短消息, 完整消息, 作者和提交时间
     */
    @NotNull
    List<ChangelogCommitModels.CommitInfo> readCommitsFromRange(@NotNull Path gitRoot, @Nullable String range) {
        List<ChangelogCommitModels.CommitInfo> commits = new ArrayList<>();
        Repository repository = getRepository(gitRoot);
        if (repository == null) {
            return commits;
        }
        try (repository; Git git = new Git(repository)) {
            Iterable<RevCommit> gitCommits = range == null || range.isBlank()
                                             ? git.log().call()
                                             : git.log().addRange(repository.resolve(range.split("\\.\\.")[0]),
                                                                  repository.resolve(range.split("\\.\\.")[1])).call();
            for (RevCommit commit : gitCommits) {
                convert(commits, commit);
            }
        } catch (Exception ignored) {
            // 忽略异常，返回已有结果
        }
        return commits;
    }

    /**
     * 将 Git 提交对象转换为提交信息对象并添加到列表中
     * <p> 根据指定的提交对象, 提取其简要消息, 完整消息, 作者, 提交时间等信息, 并封装为 CommitInfo 对象后添加到传入的集合中.
     * <p> 该方法用于在遍历提交时统一处理每个提交的信息提取与封装.
     *
     * @param commits 用于存储提交信息的列表, 不能为 null
     * @param commit  当前处理的提交对象, 不能为 null
     */
    private void convert(List<ChangelogCommitModels.CommitInfo> commits, RevCommit commit) {
        String shortMessage = commit.getShortMessage();
        String fullMessage = commit.getFullMessage();
        String author = commit.getAuthorIdent().getName();
        Date date = new Date(commit.getCommitTime() * 1000L);
        commits.add(new ChangelogCommitModels.CommitInfo(commit.getName(), shortMessage, fullMessage, date, author));
    }

    /**
     * 读取指定提交哈希的变更信息
     * <p> 根据给定的提交哈希列表, 读取每个提交的变更信息, 并返回变更信息列表.
     * 如果提交无法解析或没有父提交, 则会忽略该提交.
     *
     * @param commitHashes 提交哈希列表, 不能为空
     * @return 包含每个提交的变更信息的对象列表
     * @since 1.0
     */
    @NotNull
    List<ChangelogCommitModels.DiffCommitInfo> readCommitDiffs(@NotNull List<String> commitHashes) {
        List<ChangelogCommitModels.DiffCommitInfo> diffCommits = new ArrayList<>();
        Repository repository = getRepository();
        if (repository == null) {
            return diffCommits;
        }

        try (repository; RevWalk revWalk = new RevWalk(repository)) {
            for (String hash : commitHashes) {
                try {
                    ObjectId commitId = repository.resolve(hash);
                    if (commitId == null) {
                        continue;
                    }
                    RevCommit commit = revWalk.parseCommit(commitId);
                    RevCommit parent = commit.getParentCount() > 0
                                       ? revWalk.parseCommit(commit.getParent(0).getId())
                                       : null;
                    String diffText = buildCommitDiffText(repository, parent, commit);
                    if (diffText.isEmpty()) {
                        diffText = "变更内容为空或无法解析。";
                    }
                    diffCommits.add(new ChangelogCommitModels.DiffCommitInfo(
                        commit.getName(),
                        new Date(commit.getCommitTime() * 1000L),
                        diffText
                    ));
                } catch (Exception ignored) {
                    // 忽略无法解析的提交
                }
            }
        } catch (Exception ignored) {
            // 忽略仓库读取异常
        }

        return diffCommits;
    }

    /**
     * 获取最近指定数量的提交消息列表
     * <p> 从 Git 仓库中读取最近的提交记录, 并返回指定数量的提交消息摘要, 每条消息前缀为 `-`, 用于生成变更日志.
     * <p> 如果无法获取 Git 仓库或没有匹配的提交消息, 则返回空字符串.
     *
     * @param limit 要获取的提交消息数量, 必须大于 0
     * @return 最近的提交消息列表, 每条消息以 `-` 开头, 按时间倒序排列, 如果没有匹配的提交消息则返回空字符串
     * @since 1.0
     */
    @NotNull
    String buildRecentCommitMessagesText(int limit) {
        Repository repository = getRepository();
        if (repository == null) {
            return "";
        }
        List<String> commitMessages = new ArrayList<>();
        try (repository; Git git = new Git(repository)) {
            Iterable<RevCommit> logs = git.log().setMaxCount(limit).call();
            for (RevCommit commit : logs) {
                String msg = commit.getShortMessage();
                if (msg != null && !msg.trim().isEmpty() && !msg.startsWith("Merge ")) {
                    commitMessages.add(msg);
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
     * 获取当前仓库分支名
     * <p>
     * 直接读取 Git 仓库的当前分支名称, 失败时返回 null.
     *
     * @return 当前分支名或 null
     */
    @Nullable
    String getCurrentBranch() {
        Repository repository = getRepository();
        if (repository == null) {
            return null;
        }
        try (repository) {
            return repository.getBranch();
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * 判断当前项目是否为 Git 仓库
     *
     * @return true 表示存在有效的 Git 仓库
     */
    boolean isGitRepository() {
        Repository repository = getRepository();
        if (repository == null) {
            return false;
        }
        try (repository) {
            return repository.getObjectDatabase().exists();
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * 获取 Git 仓库对象
     * <p> 根据当前项目路径查找 .git 目录, 并尝试构建 Git 仓库对象. 如果找不到 .git 目录或发生异常, 则返回 null.
     *
     * @return Git 仓库对象, 如果未找到或发生错误则返回 null
     */
    @Nullable
    Repository getRepository() {
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
                .readEnvironment()
                .findGitDir()
                .build();
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * 获取指定路径下的 Git 仓库对象
     * <p> 该方法尝试从给定的路径解析 Git 仓库目录, 并返回对应的 Repository 对象.
     * 如果路径不存在或解析失败, 则返回 null.
     *
     * @param gitRoot Git 仓库的根路径
     * @return 解析得到的 Repository 对象, 如果解析失败则返回 null
     */
    @Nullable
    Repository getRepository(@NotNull Path gitRoot) {
        File gitDir = gitRoot.resolve(".git").toFile();
        if (!gitDir.exists()) {
            return null;
        }
        try {
            return new FileRepositoryBuilder()
                .setGitDir(gitDir)
                .readEnvironment()
                .findGitDir()
                .build();
        } catch (IOException ignored) {
            return null;
        }
    }

    /**
     * 构建提交的差异内容文本
     * <p> 根据指定的提交及其父提交, 生成该提交的差异内容文本. 如果差异内容为空或无法解析, 则返回默认提示信息.
     *
     * @param repository Git 仓库对象, 不能为空
     * @param parent     父提交对象, 可以为 null
     * @param commit     当前提交对象, 不能为空
     * @return 提交的差异内容文本, 若无有效差异则返回空字符串
     * @throws IOException 如果在读取仓库内容时发生异常
     */
    @NotNull
    String buildCommitDiffText(@NotNull Repository repository,
                               @Nullable RevCommit parent,
                               @NotNull RevCommit commit) throws IOException {
        StringBuilder diffText = new StringBuilder();
        try (ObjectReader reader = repository.newObjectReader()) {
            AbstractTreeIterator parentIter = parent == null
                                              ? new EmptyTreeIterator()
                                              : new CanonicalTreeParser(null, reader, parent.getTree());
            CanonicalTreeParser commitIter = new CanonicalTreeParser();
            commitIter.reset(reader, commit.getTree());

            List<DiffEntry> diffs;
            try (DiffFormatter formatter = new DiffFormatter(new java.io.ByteArrayOutputStream())) {
                formatter.setRepository(repository);
                formatter.setDiffComparator(RawTextComparator.DEFAULT);
                formatter.setDetectRenames(true);
                diffs = formatter.scan(parentIter, commitIter);
            }

            for (DiffEntry entry : diffs) {
                if (entry.getChangeType() == DiffEntry.ChangeType.DELETE) {
                    String metadata = buildDiffMetadata(entry);
                    appendEntryDiff(diffText, metadata + buildDeletedFileMarker(entry));
                    continue;
                }
                FileContent before = loadFileContent(repository, parent, entry.getOldPath());
                FileContent after = loadFileContent(repository, commit, entry.getNewPath());
                if (before.binary || after.binary) {
                    appendEntryDiff(diffText, formatEntryWithJGit(repository, entry));
                    continue;
                }
                String beforeName = formatUnifiedPath(entry.getOldPath(), "a/");
                String afterName = formatUnifiedPath(entry.getNewPath(), "b/");
                String codeDiff = CodeDiffUtil.generateUnifiedDiff(
                    beforeName,
                    afterName,
                    before.content,
                    after.content,
                    resolveVirtualFile(repository, entry)
                                                                  );
                String metadata = buildDiffMetadata(entry);
                if (codeDiff.isBlank()) {
                    if (entry.getChangeType() != DiffEntry.ChangeType.MODIFY) {
                        appendEntryDiff(diffText, metadata);
                    }
                    continue;
                }
                appendEntryDiff(diffText, metadata + codeDiff);
            }
        }
        return diffText.toString().trim();
    }

    /**
     * 将差异条目内容追加到构建器中
     * <p> 如果文本不为空, 则将其追加到指定的 StringBuilder 中, 确保格式正确.
     *
     * @param builder 用于存储差异内容的 StringBuilder 对象
     * @param text    要追加的文本内容
     */
    private void appendEntryDiff(@NotNull StringBuilder builder, @NotNull String text) {
        if (text.isBlank()) {
            return;
        }
        if (!builder.isEmpty()) {
            builder.append("\n");
        }
        builder.append(text.stripTrailing()).append("\n");
    }

    /**
     * 构建提交差异的元数据信息
     * <p> 根据 DiffEntry 对象生成 Git 差异文件的元数据, 包括 diff 头部, 旧 ID, 新 ID, 模式等信息.
     *
     * @param entry 差异条目对象, 用于获取路径,ID, 模式等信息
     * @return 包含 Git 差异元数据的字符串
     */
    private @NotNull String buildDiffMetadata(@NotNull DiffEntry entry) {
        String oldPath = entry.getOldPath();
        String newPath = entry.getNewPath();
        String headerOld = DiffEntry.DEV_NULL.equals(oldPath) ? newPath : oldPath;
        String headerNew = DiffEntry.DEV_NULL.equals(newPath) ? oldPath : newPath;

        StringBuilder meta = new StringBuilder();
        meta.append("diff --git a/").append(headerOld).append(" b/").append(headerNew).append("\n");

        String oldId = abbreviateObjectId(entry.getOldId());
        String newId = abbreviateObjectId(entry.getNewId());
        String newMode = entry.getNewMode() != null ? entry.getNewMode().toString() : "";
        if (!oldId.isEmpty() || !newId.isEmpty()) {
            meta.append("index ").append(oldId).append("..").append(newId);
            if (!newMode.isBlank()) {
                meta.append(" ").append(newMode);
            }
            meta.append("\n");
        }

        switch (entry.getChangeType()) {
            case ADD -> {
                if (!newMode.isBlank()) {
                    meta.append("new file mode ").append(newMode).append("\n");
                }
            }
            case DELETE -> meta.append("deleted file mode ").append(entry.getOldMode()).append("\n");
            case RENAME -> appendSimilarityMetadata(meta, "rename", entry.getScore(), oldPath, newPath);
            case COPY -> appendSimilarityMetadata(meta, "copy", entry.getScore(), oldPath, newPath);
            default -> {
                // no extra metadata
            }
        }
        return meta.toString();
    }

    /**
     * 构建删除文件的标记文本
     * <p> 根据给定的 DiffEntry 生成 Git 差异格式中用于标识文件被删除的文本内容.
     *
     * @param entry 差异条目对象, 不能为 null
     * @return 标记文件被删除的 diff 文本
     */
    private @NotNull String buildDeletedFileMarker(@NotNull DiffEntry entry) {
        String beforeName = formatUnifiedPath(entry.getOldPath(), "a/");
        return "--- " + beforeName + "\n" +
               "+++ /dev/null\n" +
               "deleted file\n";
    }

    /**
     * 向差异元数据中追加相似度信息
     * <p>根据文件操作类型 (如重命名或复制) 和相似度分数, 构建差异的元数据描述.
     *
     * @param meta    用于存储生成的元数据的 StringBuilder 对象
     * @param action  操作类型字符串(例如 "rename" 或 "copy")
     * @param score   相似度分数, 范围为 0 到 100
     * @param oldPath 原始文件路径
     * @param newPath 新文件路径
     */
    private void appendSimilarityMetadata(@NotNull StringBuilder meta,
                                          @NotNull String action,
                                          int score,
                                          @NotNull String oldPath,
                                          @NotNull String newPath) {
        if (score > 0) {
            meta.append("similarity index ").append(score).append("%\n");
        }
        meta.append(action).append(" from ").append(oldPath).append("\n");
        meta.append(action).append(" to ").append(newPath).append("\n");
    }

    /**
     * 将 Git 对象 ID 缩写为短格式字符串
     *
     * @param id Git 的 AbbreviatedObjectId 实例
     * @return 缩写后的对象 ID 字符串表示形式
     */
    private @NotNull String abbreviateObjectId(@NotNull AbbreviatedObjectId id) {
        return id.name();
    }

    /**
     * 格式化统一路径
     * <p> 将指定的路径与前缀拼接, 用于生成统一的路径格式, 例如用于 diff 输出中的 a/ 和 b/ 前缀.
     *
     * @param path   要格式化的路径, 不能为 null
     * @param prefix 路径前缀, 用于标识路径类型 (如 "a/" 或 "b/"), 不能为 null
     * @return 格式化后的路径字符串
     */
    private @NotNull String formatUnifiedPath(@NotNull String path, @NotNull String prefix) {
        if (DiffEntry.DEV_NULL.equals(path)) {
            return DiffEntry.DEV_NULL;
        }
        return prefix + path;
    }

    /**
     * 加载指定路径下的文件内容
     * <p> 根据给定的 Repository,RevCommit 和路径加载文件的内容, 并返回 FileContent 对 batis
     * <p> 如果路径无效或者 commit 为空, 则返回一个包含空字符串的 FileContent 对象
     *
     * @param repository Repository 对象, 不能为空
     * @param commit     RevCommit 对象, 可以为空
     * @param path       文件路径, 不能为空
     * @return FileContent 包含文件内容的文本和表示是否二进制的标志
     * @throws IOException 当无法加载对象时抛出
     */
    private @NotNull FileContent loadFileContent(@NotNull Repository repository,
                                                 @Nullable RevCommit commit,
                                                 @NotNull String path) throws IOException {
        if (commit == null || DiffEntry.DEV_NULL.equals(path)) {
            return new FileContent("", false);
        }
        TreeWalk treeWalk = TreeWalk.forPath(repository, path, commit.getTree());
        if (treeWalk == null) {
            return new FileContent("", false);
        }
        ObjectId objectId = treeWalk.getObjectId(0);
        ObjectLoader loader = repository.open(objectId);
        byte[] bytes = loader.getBytes();
        if (RawText.isBinary(bytes)) {
            return new FileContent("", true);
        }
        return new FileContent(new String(bytes, StandardCharsets.UTF_8), false);
    }

    /**
     * 解析虚拟文件
     * <p> 根据提交中的路径信息解析出对应的虚拟文件
     *
     * @param repository Git 仓库对象
     * @param entry      差异条目
     * @return 对应的虚拟文件, 如果路径无效或工作树不存在则返回 null
     */
    private @Nullable VirtualFile resolveVirtualFile(@NotNull Repository repository, @NotNull DiffEntry entry) {
        String path = DiffEntry.DEV_NULL.equals(entry.getNewPath()) ? entry.getOldPath() : entry.getNewPath();
        if (DiffEntry.DEV_NULL.equals(path)) {
            return null;
        }
        File workTree = repository.getWorkTree();
        if (workTree == null) {
            return null;
        }
        String absolutePath = new File(workTree, path).getPath();
        return LocalFileSystem.getInstance().findFileByPath(absolutePath);
    }

    /**
     * 使用 JGit 格式化 DiffEntry 为文本格式
     * <p> 通过指定的仓库和 DiffEntry 对象, 使用 JGit 的 DiffFormatter 将变更内容格式化为标准文本输出
     * <p> 该方法通常用于生成 Git 变更的详细文本表示, 例如在日志或提交信息中展示文件变更内容
     * <p> 使用示例:
     * <pre>{@code
     * String diffText = formatEntryWithJGit(repository, diffEntry);
     * }</pre>
     *
     * @param repository Git 仓库对象, 不能为 null
     * @param entry      要格式化的 DiffEntry 对象, 不能为 null
     * @return 格式化后的变更文本内容, 如果发生异常则返回空字符串
     * @throws IOException 当读取或写入数据时发生 I/O 错误
     */
    private @NotNull String formatEntryWithJGit(@NotNull Repository repository, @NotNull DiffEntry entry) throws IOException {
        java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
        try (DiffFormatter formatter = new DiffFormatter(output)) {
            formatter.setRepository(repository);
            formatter.setDiffComparator(RawTextComparator.DEFAULT);
            formatter.setDetectRenames(true);
            formatter.format(entry);
        }
        return output.toString(StandardCharsets.UTF_8).trim();
    }

    /**
     * 文件内容记录类
     * <p> 用于封装文件的原始内容及其是否为二进制类型的标识, 适用于需要区分文本与二进制数据的场景
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.05
     * @since 1.0.0
     */
    private record FileContent(@NotNull String content, boolean binary) {
    }
}
