package dev.dong4j.zeka.stack.idea.plugin.changelog.action;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcs.log.VcsFullCommitDetails;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import javax.swing.Icon;

import dev.dong4j.zeka.stack.idea.plugin.changelog.git.GitCliffBinaryResolver;
import dev.dong4j.zeka.stack.idea.plugin.changelog.git.GitCliffDownloadManager;
import dev.dong4j.zeka.stack.idea.plugin.changelog.git.GitCliffRunner;
import dev.dong4j.zeka.stack.idea.plugin.changelog.service.ChangelogService;
import dev.dong4j.zeka.stack.idea.plugin.changelog.settings.ReleaseLogProvider;
import dev.dong4j.zeka.stack.idea.plugin.changelog.settings.SettingsState;
import dev.dong4j.zeka.stack.idea.plugin.changelog.ui.ChangelogToolWindowService;
import dev.dong4j.zeka.stack.idea.plugin.changelog.util.ChangelogBundle;
import dev.dong4j.zeka.stack.idea.plugin.changelog.util.NotificationUtil;
import dev.dong4j.zeka.stack.idea.plugin.changelog.util.ToolWindowTitleUtil;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIStreamResponseListener;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.util.AIProviderUtils;
import dev.dong4j.zeka.stack.idea.plugin.kit.MessageFormatter;
import lombok.extern.slf4j.Slf4j;

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
@Slf4j
public abstract class AbstractReleaseLogAction extends AnAction {

    /**
     * 获取动作的图标
     * <p> 子类应该重写此方法以返回对应的图标
     *
     * @return 动作图标，可以为 null
     */
    @Nullable
    protected Icon getIcon() {
        return null;
    }

    /**
     * 检查项目是否处于索引模式
     * <p> 在索引期间，某些操作可能不可用。子类应该在 update 和 actionPerformed 方法中调用此方法进行检查。
     *
     * @param project 项目对象
     * @return 如果项目正在索引则返回 true，否则返回 false
     */
    protected static boolean isIndexing(@Nullable Project project) {
        return project != null && DumbService.isDumb(project);
    }

    /**
     * 检查并处理索引模式
     * <p> 如果项目正在索引，显示警告通知并返回 true。子类应该在 actionPerformed 方法开始时调用此方法。
     *
     * @param project 项目对象
     * @return 如果项目正在索引则返回 true，否则返回 false
     */
    protected static boolean checkAndHandleIndexing(@NotNull Project project) {
        if (isIndexing(project)) {
            NotificationUtil.showWarning(project, ChangelogBundle.message("commit.indexing.warning"));
            return true;
        }
        return false;
    }

    /**
     * 生成发布日志信息
     * <p> 根据指定的项目,Git 根目录, 选定的提交记录以及是否更新最后使用的范围, 生成发布日志.
     * 如果使用 Git Cliff 作为日志提供者且未找到二进制文件, 则显示下载通知并返回.
     *
     * @param project             项目对象, 不能为 null
     * @param gitRoot             Git 根目录路径, 不能为 null
     * @param selectedCommits     选定的提交记录列表, 不能为 null
     * @param updateLastUsedRange 是否更新最后使用的范围
     */
    protected void generate(@NotNull Project project,
                            @NotNull Path gitRoot,
                            @NotNull List<VcsFullCommitDetails> selectedCommits,
                            boolean updateLastUsedRange) {
        ReleaseLogProvider provider = SettingsState.getInstance().releaseLog;
        Path binary = GitCliffBinaryResolver.resolve();
        if (provider == ReleaseLogProvider.GIT_CLIFF && binary == null) {
            showGitCliffDownloadNotification(project, gitRoot, selectedCommits, updateLastUsedRange);
            return;
        }

        // 执行生成操作
        executeGenerateWithProvider(project, gitRoot, selectedCommits, updateLastUsedRange, provider, binary);
    }

    /**
     * 执行生成操作（根据 provider 选择不同的生成方式）
     *
     * @param project             项目对象
     * @param gitRoot             Git 仓库根路径
     * @param selectedCommits     选定的提交列表
     * @param updateLastUsedRange 是否更新最近使用的范围
     * @param provider            发布日志提供者
     * @param binary              git-cliff 二进制文件路径（如果使用 git-cliff，可以为 null）
     */
    private void executeGenerateWithProvider(@NotNull Project project,
                                             @NotNull Path gitRoot,
                                             @NotNull List<VcsFullCommitDetails> selectedCommits,
                                             boolean updateLastUsedRange,
                                             @NotNull ReleaseLogProvider provider,
                                             @Nullable Path binary) {
        String title = ToolWindowTitleUtil.buildToolWindowTitle("action.generate.release.log");
        SettingsState settings = SettingsState.getInstance();
        RangePoints points = buildReleaseRangePoints(settings, gitRoot);
        String providerText = provider == ReleaseLogProvider.GIT_CLIFF
                              ? buildGitCliffProviderText()
                              : ToolWindowTitleUtil.resolveProviderText();
        ChangelogToolWindowService.ChangelogOutputSession outputSession =
            ChangelogToolWindowService.getInstance(project).openSession(title, points.start, points.end, providerText);

        ProgressManager.getInstance().run(new Task.Backgroundable(
            project, ChangelogBundle.message("action.generate.release.log.progress.title"), true) {
            /**
             * 执行生成发布日志的逻辑
             * <p> 此方法负责根据选择的提交记录和设置配置, 调用相应的工具或 AI 服务来生成发布日志.
             * <p> 如果使用 git-cliff 工具, 则构建相关参数并执行; 否则, 通过 AI 服务流式生成日志内容.
             *
             * @param indicator 进度指示器, 用于显示操作进度和状态信息
             */
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                indicator.setText(ChangelogBundle.message("action.generate.release.log.progress.text"));
                try {
                    outputSession.setText("");
                    String range = buildRangeForSelection(selectedCommits, settings, gitRoot);
                    if (provider == ReleaseLogProvider.GIT_CLIFF) {
                        if (binary == null) {
                            throw new IllegalStateException("git-cliff binary is required but not found");
                        }
                        Path cliffConfigPath = resolveCliffConfigPath(gitRoot);
                        String config = cliffConfigPath == null ? settings.gitCliffConfig : null;
                        List<String> args = buildGitCliffArgs(settings, range, cliffConfigPath);
                        GitCliffRunner.run(binary, gitRoot, config, args, outputSession);
                    } else {
                        AIProviderConfig config = settings.providerConfig;
                        if (!AIProviderUtils.hasAIProvider(project, config, ChangelogBundle.message("settings.display.name"),
                                                           ChangelogBundle.message("settings.ai.provider.selection"))) {
                            return;
                        }
                        ChangelogService service = ChangelogService.getInstance(project);
                        String promptTemplate = readReleasePrompt(gitRoot);
                        AIStreamResponseListener listener = new AIStreamResponseListener() {
                            /**
                             * 在组件启动时执行的操作
                             * <p> 检查输出会话是否已取消, 如果已取消则中断当前线程并返回. 否则, 清空输出会话中的文本内容.
                             *
                             */
                            @Override
                            public void onStart() {
                                if (outputSession.isCancelled()) {
                                    Thread.currentThread().interrupt();
                                    return;
                                }
                                outputSession.setText("");
                            }

                            /**
                             * 处理接收到的文本块数据
                             * <p> 当接收到文本块时, 检查输出会话是否已取消, 若已取消则中断当前线程; 否则将文本块追加到输出会话中.
                             *
                             * @param chunk 接收的文本块数据, 不能为空
                             */
                            @Override
                            public void onChunk(@NotNull String chunk) {
                                if (outputSession.isCancelled()) {
                                    Thread.currentThread().interrupt();
                                    return;
                                }
                                outputSession.append(chunk);
                            }

                            /**
                             * 处理完成事件, 将格式化后的文本传递给输出会话
                             * <p> 当操作完成时调用此方法, 首先检查输出会话是否已被取消, 若未取消则对输入的完整文本进行格式化, 并将其传递给输出会话完成.
                             *
                             * @param fullText 完整的文本内容, 不能为 null
                             */
                            @Override
                            public void onComplete(@NotNull String fullText) {
                                if (outputSession.isCancelled()) {
                                    return;
                                }
                                String formattedText = MessageFormatter.format(fullText);
                                outputSession.complete(formattedText);
                            }

                            /**
                             * 处理提示信息
                             * <p> 将提示信息输出到日志, 供 UI 后续使用
                             *
                             * @param message 提示信息内容, 不能为空
                             */
                            @Override
                            public void onNotice(@NotNull String message) {
                                if (outputSession.isCancelled()) {
                                    return;
                                }
                                log.debug("Changelog notice: {}", message);
                            }

                            /**
                             * 处理错误事件
                             * <p> 当发生错误时调用此方法, 若输出会话未被取消, 则显示错误通知
                             *
                             * @param error     错误信息字符串
                             * @param exception 可能的异常对象, 可以为 null
                             */
                            @Override
                            public void onError(@NotNull String error, @Nullable Throwable exception) {
                                if (!outputSession.isCancelled()) {
                                    NotificationUtil.showError(project, error);
                                }
                            }
                        };
                        service.generateReleaseLogByAiStream(gitRoot, range, promptTemplate, listener);
                    }
                    if (updateLastUsedRange) {
                        updateLastUsedRange(settings, gitRoot, selectedCommits);
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
     * 构建 GitCliff 工具的命令行参数
     * <p> 根据配置的设置构建 GitCliff 命令行参数, 用于生成发布日志. 如果启用了基于 tag 的范围, 则使用指定的 tag 或默认的最新 tag; 否则使用指定的 commit hash 范围.
     *
     * @param settings 配置信息, 包含生成日志的相关设置
     * @return 构建好的 GitCliff 命令行参数列表
     */
    @NotNull
    private List<String> buildGitCliffArgs(@NotNull SettingsState settings,
                                           @Nullable String range,
                                           @Nullable Path cliffConfigPath) {
        List<String> args = new ArrayList<>();
        if (cliffConfigPath != null) {
            // 优先使用项目内的 cliff.toml 配置
            args.add("-c");
            args.add(cliffConfigPath.toString());
        }
        if (range != null && !range.isBlank()) {
            args.add(range);
            return args;
        }
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
     * 解析 Git 仓库中的 Cliff 配置文件路径
     * <p> 根据给定的 Git 仓库根路径, 构建并检查 "cliff.toml" 配置文件是否存在. 如果存在, 则返回其路径; 否则返回 null.</p>
     *
     * @param gitRoot Git 仓库的根路径
     * @return "cliff.toml" 文件的路径, 如果文件不存在则返回 null
     */
    @Nullable
    private Path resolveCliffConfigPath(@NotNull Path gitRoot) {
        Path configPath = gitRoot.resolve("cliff.toml");
        return Files.exists(configPath) ? configPath : null;
    }

    /**
     * 读取发布提示文件的内容
     * <p> 从指定的 Git 仓库根路径中读取 "release.prompt" 文件的内容. 如果文件不存在, 则返回 null.</p>
     *
     * @param gitRoot Git 仓库的根路径
     * @return 文件内容的字符串表示, 如果文件不存在或读取失败则返回 null
     */
    @Nullable
    private String readReleasePrompt(@NotNull Path gitRoot) {
        Path promptPath = gitRoot.resolve("release.prompt");
        if (!Files.exists(promptPath)) {
            return null;
        }
        try {
            return Files.readString(promptPath);
        } catch (IOException e) {
            return null;
        }
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
     * 构建用于生成发布日志的范围字符串
     * <p> 根据选定的提交列表和配置, 构建生成发布日志的范围字符串. 如果选定的提交列表为空, 则调用 {@link #buildRangeForAi(SettingsState, Path)} 方法获取范围;
     * 如果只有一个提交, 则返回该提交的哈希值到 HEAD 的范围; 如果有多个提交, 则返回最早和最晚提交的哈希值之间的范围.</p>
     *
     * @param selectedCommits 选定的提交列表
     * @param settings        当前的设置状态, 包含生成日志的相关配置
     * @param gitRoot         项目的 Git 仓库根路径
     * @return 日志生成的范围字符串, 格式为 "起始点.. 结束点", 如果无法确定范围则返回 null
     */
    @Nullable
    private String buildRangeForSelection(@NotNull List<VcsFullCommitDetails> selectedCommits,
                                          @NotNull SettingsState settings,
                                          @NotNull Path gitRoot) {
        if (selectedCommits.isEmpty()) {
            return buildRangeForAi(settings, gitRoot);
        }
        if (selectedCommits.size() == 1) {
            return selectedCommits.get(0).getId().asString() + "..HEAD";
        }
        VcsFullCommitDetails oldest = selectedCommits.stream()
            .min(Comparator.comparingLong(VcsFullCommitDetails::getCommitTime))
            .orElse(null);
        VcsFullCommitDetails newest = selectedCommits.stream()
            .max(Comparator.comparingLong(VcsFullCommitDetails::getCommitTime))
            .orElse(null);
        if (newest == null) {
            return buildRangeForAi(settings, gitRoot);
        }
        return oldest.getId().asString() + ".." + newest.getId().asString();
    }

    /**
     * 解析选定提交的 Git 仓库根路径
     * <p> 根据传入的提交列表, 获取第一个提交的根路径, 并将其转换为 Path 对象. 如果提交列表为空, 则返回 null.</p>
     *
     * @param selectedCommits 选定的提交列表
     * @return 提交对应的 Git 仓库根路径, 如果提交列表为空则返回 null
     */
    @Nullable
    protected Path resolveGitRootForLog(@NotNull List<VcsFullCommitDetails> selectedCommits) {
        if (selectedCommits.isEmpty()) {
            return null;
        }
        VirtualFile root = selectedCommits.get(0).getRoot();
        return Path.of(root.getPath());
    }

    /**
     * 更新最近使用的版本范围
     * <p> 根据当前配置, 更新 settings 中记录的最近使用的标签或提交哈希. 如果配置为使用标签作为起始点, 则查找并设置最新的标签;
     * 否则查找并设置当前 HEAD 的提交哈希.</p>
     *
     * @param settings 配置状态对象, 用于读取和更新最近使用的标签或哈希
     * @param gitRoot  Git 仓库的根路径, 用于解析 Git 信息
     */
    private void updateLastUsedRange(@NotNull SettingsState settings,
                                     @NotNull Path gitRoot,
                                     @NotNull List<VcsFullCommitDetails> selectedCommits) {
        if (!selectedCommits.isEmpty()) {
            selectedCommits.stream()
                .min(Comparator.comparingLong(VcsFullCommitDetails::getCommitTime))
                .ifPresent(oldest -> settings.lastUsedHash = oldest.getId().asString());
            return;
        }
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
     * 构建发布日志的范围点
     *
     * @param settings 配置
     * @param gitRoot Git 根目录
     * @return 范围点
     */
    @NotNull
    private RangePoints buildReleaseRangePoints(@NotNull SettingsState settings, @NotNull Path gitRoot) {
        String start = "";
        if (settings.useTagAsStart) {
            if (settings.lastUsedTag != null && !settings.lastUsedTag.isBlank()) {
                start = settings.lastUsedTag;
            } else {
                String latestTag = resolveLatestTag(gitRoot);
                if (latestTag != null) {
                    start = latestTag;
                }
            }
        } else if (settings.lastUsedHash != null && !settings.lastUsedHash.isBlank()) {
            start = settings.lastUsedHash;
        }
        String end = "";
        String headHash = resolveHeadHash(gitRoot);
        if (headHash != null) {
            end = headHash;
        }
        return new RangePoints(start, end);
    }

    /**
     * 发布日志范围点
     */
    private record RangePoints(@NotNull String start, @NotNull String end) {
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
     * 构建 git-cliff provider 信息
     *
     * @return provider 文本
     */
    private @NotNull String buildGitCliffProviderText() {
        String version = GitCliffDownloadManager.getInstalledVersion();
        if (version == null || version.isBlank()) {
            return "git-cliff";
        }
        return "git-cliff(" + version + ")";
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

    /**
     * 显示 git-cliff 下载通知
     * <p> 当检测到 git-cliff 二进制文件不存在时，显示通知并提供下载操作
     *
     * @param project             项目对象
     * @param gitRoot             Git 仓库根路径
     * @param selectedCommits     选定的提交列表
     * @param updateLastUsedRange 是否更新最近使用的范围
     */
    private void showGitCliffDownloadNotification(@NotNull Project project,
                                                  @NotNull Path gitRoot,
                                                  @NotNull List<VcsFullCommitDetails> selectedCommits,
                                                  boolean updateLastUsedRange) {
        NotificationGroup notificationGroup = NotificationGroupManager.getInstance()
            .getNotificationGroup("IntelliAI Changelog Notifications");
        if (notificationGroup == null) {
            NotificationUtil.showError(project, ChangelogBundle.message("gitcliff.binary.missing"));
            return;
        }

        Notification notification = notificationGroup.createNotification(
            ChangelogBundle.message("gitcliff.binary.missing"),
            ChangelogBundle.message("gitcliff.download.prompt"),
            NotificationType.INFORMATION
                                                                        );

        // 添加下载操作
        notification.addAction(new NotificationAction(
            ChangelogBundle.message("gitcliff.download.action")) {
            /**
             * 处理动作事件, 执行下载并安装 GitCliff 的操作
             * <p> 当用户触发该动作时, 会清除通知并调用下载和安装 GitCliff 的方法
             *
             * @param e            动作事件对象, 不能为 null
             * @param notification 通知对象, 用于显示信息或提示, 不能为 null
             */
            @Override
            public void actionPerformed(@NotNull AnActionEvent e,
                                        @NotNull Notification notification) {
                notification.expire();
                downloadAndInstallGitCliff(project, gitRoot, selectedCommits, updateLastUsedRange);
            }
        });

        notification.notify(project);
    }

    /**
     * 下载并安装 git-cliff，然后继续执行生成操作
     *
     * @param project             项目对象
     * @param gitRoot             Git 仓库根路径
     * @param selectedCommits     选定的提交列表
     * @param updateLastUsedRange 是否更新最近使用的范围
     */
    private void downloadAndInstallGitCliff(@NotNull Project project,
                                            @NotNull Path gitRoot,
                                            @NotNull List<VcsFullCommitDetails> selectedCommits,
                                            boolean updateLastUsedRange) {
        ProgressManager.getInstance().run(new Task.Backgroundable(
            project, ChangelogBundle.message("gitcliff.download.progress.title"), true) {
            /**
             * 执行 GitCliff 下载和安装操作, 并继续执行生成变更日志的逻辑
             * <p> 此方法在运行时被调用, 用于下载并安装 GitCliff 工具, 设置其为默认的发布日志提供者, 并在完成后触发生成变更日志的操作.
             *
             * @param indicator 进度指示器, 用于显示下载进度
             */
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    // 下载并安装 git-cliff
                    Path binary = GitCliffDownloadManager.downloadAndInstall(indicator, null);

                    // 下载完成后，设置 provider 为 GIT_CLIFF
                    SettingsState settings = SettingsState.getInstance();
                    settings.releaseLog = ReleaseLogProvider.GIT_CLIFF;
                    ApplicationManager.getApplication().saveSettings();

                    // 继续执行生成操作（直接执行生成逻辑，不再检查二进制文件）
                    ApplicationManager.getApplication().invokeLater(() -> {
                        if (project.isDisposed()) {
                            return;
                        }
                        executeGenerate(project, gitRoot, selectedCommits, updateLastUsedRange, binary);
                    });
                } catch (Exception e) {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        NotificationUtil.showError(project,
                                                   ChangelogBundle.message("gitcliff.download.error", e.getMessage()));
                    });
                }
            }
        });
    }

    /**
     * 执行生成操作（内部方法，不检查二进制文件）
     * <p> 直接使用 git-cliff 执行生成操作，用于下载完成后的后续操作
     *
     * @param project             项目对象
     * @param gitRoot             Git 仓库根路径
     * @param selectedCommits     选定的提交列表
     * @param updateLastUsedRange 是否更新最近使用的范围
     * @param binary              git-cliff 二进制文件路径
     */
    private void executeGenerate(@NotNull Project project,
                                 @NotNull Path gitRoot,
                                 @NotNull List<VcsFullCommitDetails> selectedCommits,
                                 boolean updateLastUsedRange,
                                 @NotNull Path binary) {
        // 直接调用 executeGenerateWithProvider，传入 GIT_CLIFF provider 和 binary
        executeGenerateWithProvider(project, gitRoot, selectedCommits, updateLastUsedRange,
                                    ReleaseLogProvider.GIT_CLIFF, binary);
    }
}
