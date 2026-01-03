package dev.dong4j.zeka.stack.idea.plugin.changelog.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import javax.swing.Icon;

import dev.dong4j.zeka.stack.idea.plugin.changelog.util.ChangelogBundle;
import dev.dong4j.zeka.stack.idea.plugin.changelog.util.NotificationUtil;
import icons.ChangelogIcons;

/**
 * 从项目中生成发布日志的动作类
 * <p> 该类继承自 AbstractReleaseLogAction, 用于在 IntelliJ IDEA 中生成项目的发布日志. 通过解析 Git 仓库根目录,
 * 并根据项目状态启用或禁用动作按钮, 并在动作执行时生成相应的发布日志.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.01
 * @since 1.0.0
 */
public class GenerateReleaseLogFromProjectAction extends AbstractReleaseLogAction {

    /**
     * 更新动作的可用性和显示信息
     * <p> 根据项目和 Git 仓库根目录的存在性设置动作的可用性, 并设置动作的文本, 描述和图标.
     *
     * @param e 动作事件对象, 包含上下文信息
     * @since hello.world
     */
    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        boolean enabled = project != null && resolveGitRoot(e) != null;
        e.getPresentation().setEnabled(enabled);
        e.getPresentation().setText(ChangelogBundle.message("action.generate.release.log"));
        e.getPresentation().setDescription(ChangelogBundle.message("action.generate.release.log.description"));
        e.getPresentation().setIcon(ChangelogIcons.CHANGELOG_16);
    }

    /**
     * 获取动作的图标
     *
     * @return 动作图标
     */
    @Override
    @NotNull
    protected Icon getIcon() {
        return ChangelogIcons.CHANGELOG_16;
    }

    /**
     * 处理用户触发的生成发布日志的动作
     * <p> 在用户触发动作时, 首先检查项目是否存在且未被销毁, 然后解析 Git 仓库根目录路径.
     * 如果找不到 Git 仓库, 则显示错误通知; 否则调用 generate 方法生成发布日志.
     *
     * @param e 当前操作事件, 包含上下文信息
     * @since hello.world
     */
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null || project.isDisposed()) {
            return;
        }

        Path gitRoot = resolveGitRoot(e);
        if (gitRoot == null) {
            NotificationUtil.showError(project, ChangelogBundle.message("gitcliff.no.git.repo"));
            return;
        }

        generate(project, gitRoot, List.of(), true);
    }

    /**
     * 解析 Git 仓库根目录路径
     * <p> 该方法尝试从当前操作事件中获取文件或项目信息, 并查找包含 .git 目录的 Git 仓库根路径.
     * 优先从选中的文件数组中查找, 若未找到则从单个文件中查找, 最后尝试从项目根目录查找.
     *
     * @param e 当前操作事件, 包含上下文信息
     * @return Git 仓库根路径, 若未找到则返回 null
     */
    @Nullable
    private Path resolveGitRoot(@NotNull AnActionEvent e) {
        VirtualFile[] files = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY);
        if (files != null) {
            for (VirtualFile file : files) {
                Path gitRoot = findGitRoot(file);
                if (gitRoot != null) {
                    return gitRoot;
                }
            }
        }

        VirtualFile file = e.getData(CommonDataKeys.VIRTUAL_FILE);
        if (file != null) {
            Path gitRoot = findGitRoot(file);
            if (gitRoot != null) {
                return gitRoot;
            }
        }

        Project project = e.getProject();
        if (project != null && project.getBasePath() != null) {
            Path candidate = Path.of(project.getBasePath());
            if (hasGitRepository(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    /**
     * 查找文件所在目录的 Git 仓库根路径
     * <p> 从给定的文件路径开始, 向上查找 Git 仓库根目录. 如果文件不是目录, 则先获取其父目录再进行查找.
     * 如果找到包含 .git 目录的路径, 则返回该路径; 否则返回 null.
     *
     * @param file 要查找的文件或目录
     * @return Git 仓库根路径, 如果未找到则返回 null
     */
    @Nullable
    private Path findGitRoot(@NotNull VirtualFile file) {
        Path current = Path.of(file.getPath());
        if (!file.isDirectory()) {
            current = current.getParent();
        }
        while (current != null) {
            if (hasGitRepository(current)) {
                return current;
            }
            current = current.getParent();
        }
        return null;
    }

    /**
     * 检查指定路径下是否存在 Git 仓库
     * <p> 通过检查路径下的 .git 目录是否存在来判断是否为有效的 Git 仓库
     *
     * @param basePath 要检查的路径
     * @return 如果存在 Git 仓库则返回 true, 否则返回 false
     */
    private boolean hasGitRepository(@NotNull Path basePath) {
        File gitDir = basePath.resolve(".git").toFile();
        return gitDir.exists();
    }

}
