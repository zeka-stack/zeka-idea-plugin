package dev.dong4j.zeka.stack.idea.plugin.changelog.action;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import dev.dong4j.zeka.stack.idea.plugin.changelog.PluginContents;
import dev.dong4j.zeka.stack.idea.plugin.changelog.git.GitCliffBinaryResolver;
import dev.dong4j.zeka.stack.idea.plugin.changelog.git.GitCliffRunner;
import dev.dong4j.zeka.stack.idea.plugin.changelog.service.ChangelogService;
import dev.dong4j.zeka.stack.idea.plugin.changelog.settings.ReleaseLogProvider;
import dev.dong4j.zeka.stack.idea.plugin.changelog.settings.SettingsState;
import dev.dong4j.zeka.stack.idea.plugin.changelog.ui.ChangelogToolWindowService;
import dev.dong4j.zeka.stack.idea.plugin.changelog.util.ChangelogBundle;
import dev.dong4j.zeka.stack.idea.plugin.changelog.util.NotificationUtil;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIStreamResponseListener;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.util.AIProviderUtils;

/**
 * 生成发布日志的动作类
 * <p> 该类继承自 AnAction, 用于在 IntelliJ IDEA 中生成项目的发布日志. 通过解析 Git 仓库, 根据配置生成相应的变更日志, 并在工具窗口中显示.</p>
 * <p> 支持两种方式生成日志: 使用 GitCliff 工具或 AI 提供商. 具体行为取决于设置中的日志提供者选择.</p>
 *
 * <p> 功能包括:</p>
 * <ul>
 *   <li> 检查项目是否有效且存在 Git 仓库 </li>
 *   <li> 根据配置构建命令行参数或范围字符串 </li>
 *   <li> 调用 GitCliff 或 AI 提供商生成日志 </li>
 *   <li> 更新最近使用的范围 </li>
 *   <li> 处理异常并显示错误通知 </li>
 * </ul>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2025.12.31
 * @since 1.0.0
 */
public class GenerateReleaseLogAction extends AnAction {

    /** 用于格式化时间的日期时间格式器, 格式为 "HH:mm:ss" */
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    /**
     * 更新操作按钮的状态
     * <p> 根据当前上下文判断是否启用该操作, 若项目有效且存在 Git 仓库, 则启用按钮并设置其文本和描述信息.
     *
     * @param e 动作事件, 包含当前上下文信息
     */
    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        boolean enabled = project != null && resolveGitRoot(e) != null;
        e.getPresentation().setEnabled(enabled);
        e.getPresentation().setText(ChangelogBundle.message("action.generate.release.log"));
        e.getPresentation().setDescription(ChangelogBundle.message("action.generate.release.log.description"));
    }

    /**
     * 执行生成发布日志的动作
     * <p> 根据当前项目配置, 调用 GitCliff 或 AI 提供商生成发布日志, 并在工具窗口中显示结果. 若项目未初始化 Git 仓库或缺少必要配置, 将显示错误提示.
     *
     * @param e 动作事件对象, 包含项目信息和操作上下文
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

        ReleaseLogProvider provider = SettingsState.getInstance().releaseLog;
        Path binary = GitCliffBinaryResolver.resolve();
        if (provider == ReleaseLogProvider.GIT_CLIFF && binary == null) {
            NotificationUtil.showError(project, ChangelogBundle.message("gitcliff.binary.missing"));
            return;
        }

        String title = ChangelogBundle.message(
            "toolwindow.title.simple",
            ChangelogBundle.message("action.generate.release.log"),
            LocalTime.now().format(TIME_FORMATTER)
                                              );
        ChangelogToolWindowService.ChangelogOutputSession outputSession =
            ChangelogToolWindowService.getInstance(project).openSession(title);

        ProgressManager.getInstance().run(new Task.Backgroundable(
            project, ChangelogBundle.message("action.generate.release.log.progress.title"), true) {
            /**
             * 执行生成发布日志的后台任务
             * <p> 此方法用于在后台执行生成发布日志的操作, 设置进度指示器并处理不同日志生成方式 (GitCliff 或 AI 生成).
             *
             * @param indicator 进度指示器, 用于显示任务执行状态
             */
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                indicator.setText(ChangelogBundle.message("action.generate.release.log.progress.text"));
                try {
                    outputSession.setText("");
                    SettingsState settings = SettingsState.getInstance();
                    if (provider == ReleaseLogProvider.GIT_CLIFF) {
                        String config = settings.gitCliffConfig;
                        List<String> args = buildGitCliffArgs(settings);
                        GitCliffRunner.run(binary, gitRoot, config, args, outputSession);
                        updateLastUsedRange(settings, gitRoot);
                    } else {
                        AIProviderConfig config = settings.providerConfig;
                        if (!AIProviderUtils.hasAIProvider(project, config, PluginContents.PLUGIN_NAME)) {
                            return;
                        }
                        String range = buildRangeForAi(settings, gitRoot);
                        ChangelogService service = ChangelogService.getInstance(project);
                        AIStreamResponseListener listener = new AIStreamResponseListener() {
                            /**
                             * 在流式响应开始时清空输出会话内容
                             * <p> 当接收到流式响应的开始信号时, 清空输出会话中的文本内容, 为新的响应内容做准备
                             *
                             */
                            @Override
                            public void onStart() {
                                outputSession.setText("");
                            }

                            /**
                             * 处理接收到的文本块数据
                             * <p> 当接收到分块的文本数据时, 将其追加到输出会话中
                             *
                             * @param chunk 分块的文本数据
                             */
                            @Override
                            public void onChunk(@NotNull String chunk) {
                                outputSession.append(chunk);
                            }

                            /**
                             * 处理完成时调用的方法
                             * <p> 当所有数据处理完成后, 将完整的文本设置到输出会话中
                             *
                             * @param fullText 完整的文本内容
                             */
                            @Override
                            public void onComplete(@NotNull String fullText) {
                                outputSession.setText(fullText);
                            }
                        };
                        service.generateReleaseLogByAiStream(gitRoot, range, listener);
                        updateLastUsedRange(settings, gitRoot);
                    }
                } catch (Exception ex) {
                    NotificationUtil.showError(project,
                                               ChangelogBundle.message("action.generate.release.log.error",
                                                                       ex.getMessage()));
                }
            }
        });
    }

    /**
     * 返回动作更新线程
     * <p> 此方法返回一个后台线程 (BGT), 用于在后台执行动作更新操作.
     *
     * @return 动作更新线程, 类型为 BGT
     */
    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
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

    /**
     * 构建 GitCliff 工具的命令行参数
     * <p> 根据配置的设置构建 GitCliff 命令行参数, 用于生成发布日志. 如果启用了基于 tag 的范围, 则使用指定的 tag 或默认的最新 tag; 否则使用指定的 commit hash 范围.
     *
     * @param settings 配置信息, 包含生成日志的相关设置
     * @return 构建好的 GitCliff 命令行参数列表
     */
    @NotNull
    private List<String> buildGitCliffArgs(@NotNull SettingsState settings) {
        List<String> args = new ArrayList<>();
        if (settings.useTagAsStart) {
            if (settings.lastUsedTag != null && !settings.lastUsedTag.isBlank()) {
                args.add(settings.lastUsedTag + "..HEAD");
            } else {
                // 未指定 tag 时，默认从最新 tag 之后开始生成
                args.add("--latest");
            }
            return args;
        }

        if (settings.lastUsedHash != null && !settings.lastUsedHash.isBlank()) {
            args.add(settings.lastUsedHash + "..HEAD");
        }
        return args;
    }

    /**
     * 构建用于 AI 提供商的日志生成范围字符串
     * <p> 根据配置和 Git 仓库信息构建日志生成的范围字符串. 如果使用标签作为起始点, 则优先使用 lastUsedTag, 若为空则尝试获取最新的标签;
     * 如果使用哈希作为起始点, 则使用 lastUsedHash. 如果没有可用的起始点, 则返回 null.
     *
     * @param settings 当前的设置状态, 包含生成日志的相关配置
     * @param gitRoot  项目的 Git 仓库根路径
     * @return 日志生成的范围字符串, 格式为 "起始点..HEAD", 如果无法确定起始点则返回 null
     */
    @Nullable
    private String buildRangeForAi(@NotNull SettingsState settings, @NotNull Path gitRoot) {
        if (settings.useTagAsStart) {
            if (settings.lastUsedTag != null && !settings.lastUsedTag.isBlank()) {
                return settings.lastUsedTag + "..HEAD";
            }
            String latestTag = resolveLatestTag(gitRoot);
            if (latestTag != null) {
                return latestTag + "..HEAD";
            }
            return null;
        }
        if (settings.lastUsedHash != null && !settings.lastUsedHash.isBlank()) {
            return settings.lastUsedHash + "..HEAD";
        }
        return null;
    }

    /**
     * 更新最近使用的版本范围
     * <p> 根据当前配置, 更新 settings 中记录的最近使用的标签或提交哈希. 如果配置为使用标签作为起始点, 则查找并设置最新的标签;
     * 否则查找并设置当前 HEAD 的提交哈希.</p>
     *
     * @param settings 配置状态对象, 用于读取和更新最近使用的标签或哈希
     * @param gitRoot  Git 仓库的根路径, 用于解析 Git 信息
     */
    private void updateLastUsedRange(@NotNull SettingsState settings, @NotNull Path gitRoot) {
        if (settings.useTagAsStart) {
            if (settings.lastUsedTag == null || settings.lastUsedTag.isBlank()) {
                String latestTag = resolveLatestTag(gitRoot);
                if (latestTag != null) {
                    settings.lastUsedTag = latestTag;
                }
            }
            return;
        }

        String headHash = resolveHeadHash(gitRoot);
        if (headHash != null) {
            settings.lastUsedHash = headHash;
        }
    }

    /**
     * 解析 Git 仓库中 HEAD 指针指向的提交哈希值
     * <p> 该方法尝试打开指定 Git 仓库的 .git 目录, 并解析 HEAD 引用, 返回其对应的提交哈希值.
     * 如果解析失败或 HEAD 不存在, 则返回 null.
     *
     * @param gitRoot Git 仓库的根目录路径
     * @return HEAD 指向的提交哈希值, 若解析失败或 HEAD 不存在则返回 null
     */
    @Nullable
    private String resolveHeadHash(@NotNull Path gitRoot) {
        try (Repository repository = new FileRepositoryBuilder()
            .setGitDir(gitRoot.resolve(".git").toFile())
            .build()) {
            ObjectId head = repository.resolve("HEAD");
            return head != null ? head.getName() : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * 获取最新的 Git 标签名称
     * <p> 通过解析指定的 Git 仓库目录, 获取最新的标签, 并返回其缩短后的名称. 如果没有找到标签, 则返回 null.
     *
     * @param gitRoot 包含 Git 仓库的路径
     * @return 最新的标签名称, 如果未找到标签则返回 null
     */
    @Nullable
    private String resolveLatestTag(@NotNull Path gitRoot) {
        try (Repository repository = new FileRepositoryBuilder()
            .setGitDir(gitRoot.resolve(".git").toFile())
            .build();
             Git git = new Git(repository);
             RevWalk walk = new RevWalk(repository)) {
            List<Ref> tags = git.tagList().call();
            Optional<Ref> latest = tags.stream()
                .max(Comparator.comparingInt(ref -> resolveCommitTime(walk, ref)));
            return latest.map(ref -> Repository.shortenRefName(ref.getName())).orElse(null);
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * 解析指定引用对象的提交时间
     * <p> 通过 RevWalk 解析指定引用对象对应的提交记录, 并返回其提交时间戳.
     *
     * @param walk RevWalk 对象, 用于遍历提交历史
     * @param ref  引用对象, 表示某个提交或标签
     * @return 提交时间戳, 单位为秒
     */
    private int resolveCommitTime(@NotNull RevWalk walk, @NotNull Ref ref) {
        try {
            ObjectId objectId = ref.getPeeledObjectId();
            ObjectId target = objectId != null ? objectId : ref.getObjectId();
            RevCommit commit = walk.parseCommit(target);
            return commit.getCommitTime();
        } catch (Exception ignored) {
            return 0;
        }
    }
}
