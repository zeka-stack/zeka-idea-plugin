package dev.dong4j.zeka.stack.idea.plugin.changelog.service;

import dev.dong4j.zeka.stack.idea.plugin.common.changelog.model.CodeDiff;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vfs.VirtualFile;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;

import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.Repository;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;

import dev.dong4j.zeka.stack.idea.plugin.changelog.util.CodeDiffUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * Git 变更日志服务类(IDE 宿主实现)
 * <p>
 * 继承 core 的纯 JVM {@link ChangelogGitService}, 在 IDE 环境中增强两处快路径:
 * <ul>
 *   <li>分支/仓库判定优先走 git4idea 仓库索引({@link GitRepositoryManager}),
 *       失败时回退 core 的 JGit 解析</li>
 *   <li>单条目 diff 生成优先走 IDE diff 引擎({@link CodeDiffUtil})做
 *       import-only/comment-only 等噪声过滤, 为空时回退 core 的 JGit 直出兜底</li>
 * </ul>
 * 其余提交读取/diff 构建逻辑全部复用 core 实现, 避免逻辑两处维护.
 * <p>
 * 使用示例:
 * <pre>{@code
 * ChangelogGitService service = new ChangelogGitService(project);
 * String currentBranch = service.getCurrentBranch();
 * }</pre>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.07
 * @since 1.0.0
 */
@Slf4j
public class ChangelogGitService extends dev.dong4j.zeka.stack.idea.plugin.common.changelog.ChangelogGitService {

    /** 当前项目上下文, 用于 git4idea 仓库索引与 VFS 解析 */
    private final Project project;

    /**
     * 构造函数, 使用项目根路径初始化
     *
     * @param project 项目对象, 不能为 null
     */
    public ChangelogGitService(@NotNull Project project) {
        super(Path.of(java.util.Objects.requireNonNull(project.getBasePath(), "project basePath")));
        this.project = project;
    }

    /**
     * 从 IDE 环境解析当前分支(git4idea 快路径)
     * <p> 通过 Git 仓库管理器获取项目根目录对应的 Git 仓库并读取当前分支名,
     * 失败时返回 null, 由 core 回退 JGit 解析.
     *
     * @return 当前 Git 分支名称, 无法获取时返回 null
     */
    @Override
    protected @Nullable String resolveBranchFromEnvironment() {
        try {
            GitRepositoryManager manager = GitRepositoryManager.getInstance(project);
            GitRepository repository = manager.getRepositoryForRoot(project.getBaseDir());
            if (repository == null) {
                var repositories = manager.getRepositories();
                if (repositories.isEmpty()) {
                    return null;
                }
                repository = repositories.getFirst();
            }
            String branch = repository.getCurrentBranchName();
            return branch != null && !branch.trim().isEmpty() ? branch : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * 从 IDE 环境判断是否为 Git 仓库(git4idea 快路径)
     *
     * @return 存在 Git 仓库返回 true; 无法判定返回 null
     */
    @Override
    protected @Nullable Boolean resolveIsGitRepositoryFromEnvironment() {
        try {
            GitRepositoryManager manager = GitRepositoryManager.getInstance(project);
            return !manager.getRepositories().isEmpty();
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * 使用 IDE diff 引擎生成单条目 unified diff(降噪优先)
     * <p> 覆盖 core 默认的 JGit 直出策略: 通过 {@link CodeDiffUtil} 过滤 import-only /
     * comment-only / reorder-only 等噪声变更; 生成结果为空时由 core 的兜底逻辑
     * 回退 JGit diff, 保证 commit diffText 不为空.
     *
     * @param repository Git 仓库对象
     * @param entry      差异条目
     * @param beforeName 统一化后的旧路径(a/ 前缀)
     * @param afterName  统一化后的新路径(b/ 前缀)
     * @param before     变更前文件内容
     * @param after      变更后文件内容
     * @return unified diff 文本, 无可输出内容时返回空字符串
     */
    @Override
    protected @NotNull String generateEntryDiff(@NotNull Repository repository,
                                                @NotNull DiffEntry entry,
                                                @NotNull String beforeName,
                                                @NotNull String afterName,
                                                @NotNull String before,
                                                @NotNull String after) {
        VirtualFile virtualFile = resolveVirtualFile(repository, entry);
        return CodeDiffUtil.generateUnifiedDiff(beforeName, afterName, before, after, virtualFile);
    }

    /**
     * 解析虚拟文件
     * <p> 根据提交中的路径信息解析出对应的虚拟文件, 供 IDE diff 引擎定位文件.
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
        java.io.File workTree = repository.getWorkTree();
        if (workTree == null) {
            return null;
        }
        String absolutePath = new java.io.File(workTree, path).getPath();
        return com.intellij.openapi.vfs.LocalFileSystem.getInstance().findFileByPath(absolutePath);
    }
}
