package dev.dong4j.zeka.stack.idea.plugin.changelog.git;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.changes.Change;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import dev.dong4j.zeka.stack.idea.plugin.changelog.service.ChangelogService;
import dev.dong4j.zeka.stack.idea.plugin.changelog.util.ChangelogBundle;
import dev.dong4j.zeka.stack.idea.plugin.changelog.util.NotificationUtil;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIStreamResponseListener;
import lombok.extern.slf4j.Slf4j;

/**
 * 提交消息生成器类
 * <p> 用于根据代码变更生成 Git 提交消息. 该类通过分析代码变更, 调用变更日志服务生成提交消息, 并在后台任务中处理生成过程.
 * 支持自定义的提交消息控制对象, 以便在不同的提交面板中设置提交消息.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2025.12.31
 * @since 1.0.0
 */
@Slf4j
public class CommitMessageGenerator {
    /**
     * 项目对象
     *
     * @see Project
     */
    private final Project project;

    /**
     * 初始化 CommitMessageGenerator 实例
     * <p> 构造函数, 用于创建 CommitMessageGenerator 对象, 并初始化项目对象
     *
     * @param project 项目对象
     */
    public CommitMessageGenerator(@NotNull Project project) {
        this.project = project;
    }

    /**
     * 处理代码变更, 生成提交记录
     * <p> 该方法用于处理提交的代码变更, 根据代码的实际改动生成提交记录. 此方法会调用另一个重载的 generateForChanges 方法, 并传入 null 作为提交面板的提交信息控件.
     *
     * @param changes 变更集合
     * @since 1.0.0
     */
    public void generateForChanges(@NotNull Collection<Change> changes) {
        generateForChanges(changes, null);
    }

    /**
     * 处理代码变更, 生成提交记录
     * <p> 该方法用于处理提交的代码变更, 根据代码的实际改动生成提交记录. 如果没有代码变更, 则记录警告日志并显示警告通知.
     * 如果存在代码变更, 则启动后台任务进行提交记录的生成, 并将结果写入提交面板.
     *
     * @param changes              变更集合
     * @param commitMessageControl 提交面板的提交信息控件, 可以为 null
     * @since 1.0.0
     */
    public void generateForChanges(@NotNull Collection<Change> changes,
                                   @Nullable Object commitMessageControl) {
        if (changes.isEmpty()) {
            log.warn("Git 提交页面：没有代码变更需要处理");
            NotificationUtil.showWarning(project, ChangelogBundle.message("commit.no.changes"));
            return;
        }

        // 在后台任务中执行生成
        ProgressManager.getInstance().run(
            new Task.Backgroundable(project, ChangelogBundle.message("commit.generating.progress"), true) {
                /**
                 * 执行提交信息生成任务
                 * <p> 设置进度指示器为不确定状态, 并显示分析更改中的提示信息. 调用 ChangelogService 生成提交信息, 并在 UI 线程中显示结果或错误信息.
                 *
                 * @param indicator 进度指示器, 用于显示任务进度和状态信息
                 */
                @Override
                public void run(@NotNull ProgressIndicator indicator) {
                    indicator.setIndeterminate(true);
                    indicator.setText(ChangelogBundle.message("commit.analyzing.changes"));

                    try {
                        ChangelogService service = ChangelogService.getInstance(project);
                        StringBuilder buffer = new StringBuilder();
                        AtomicReference<Boolean> updated = new AtomicReference<>(false);
                        // 流式回调中实时写入提交面板，保证内容可见且可编辑
                        AIStreamResponseListener listener = new AIStreamResponseListener() {
                            /**
                             * 在监听器启动时调用
                             * <p> 清空缓冲区并将提交消息文本设置为空
                             *
                             * @since 1.0
                             */
                            @Override
                            public void onStart() {
                                buffer.setLength(0);
                                ApplicationManager.getApplication().invokeLater(() -> {
                                    setCommitMessageText("", commitMessageControl);
                                });
                            }

                            /**
                             * 处理接收到的文本块
                             * <p> 将接收到的文本块追加到缓冲区, 并在事件调度线程中更新提交消息文本
                             *
                             * @param chunk 接收到的文本块
                             */
                            @Override
                            public void onChunk(@NotNull String chunk) {
                                buffer.append(chunk);
                                ApplicationManager.getApplication().invokeLater(() -> {
                                    if (setCommitMessageText(buffer.toString(), commitMessageControl)) {
                                        updated.set(true);
                                    }
                                });
                            }

                            /**
                             * 在异步操作完成后更新提交消息文本
                             * <p> 此方法在异步操作完成后被调用, 用于更新提交消息文本. 如果更新成功, 则将 updated 标志设置为 true.
                             *
                             * @param fullText 完整的文本内容
                             */
                            @Override
                            public void onComplete(@NotNull String fullText) {
                                ApplicationManager.getApplication().invokeLater(() -> {
                                    if (setCommitMessageText(fullText, commitMessageControl)) {
                                        updated.set(true);
                                    }
                                });
                            }
                        };

                        // 流式生成并同步返回最终结果
                        String commitMessage = service.generateCommitMessageFromDiffStream(changes, listener);

                        // 在 EDT 中显示结果
                        ApplicationManager.getApplication().invokeLater(() -> {
                            // 直接写入提交面板，避免弹窗打断提交流程
                            boolean applied = setCommitMessageText(commitMessage, commitMessageControl);
                            if (!applied && !updated.get()) {
                                log.warn("Git 提交页面：提交面板不可用，无法写入提交记录");
                            }
                        });

                        log.info("Git 提交页面：提交记录生成成功");
                    } catch (Exception e) {
                        log.error("Git 提交页面：生成提交记录失败", e);
                        ApplicationManager.getApplication().invokeLater(() -> {
                            String errorMessage = e.getMessage();
                            if (errorMessage != null && !errorMessage.isEmpty()) {
                                NotificationUtil.showError(project, errorMessage);
                            } else {
                                NotificationUtil.showError(project,
                                                           ChangelogBundle.message("commit.generation.error",
                                                                                   ChangelogBundle.message("error.ai.service.unknown")));
                            }
                        });
                    }
                }
            }
                                         );
    }

    /**
     * 设置提交消息文本
     * <p> 尝试通过反射调用提交面板对象的方法来设置提交消息文本. 支持的方法名包括 "setCommitMessage","setCommitMessageText" 和 "setText".
     *
     * @param commitMessage        提交消息文本
     * @param commitMessageControl 提交面板的控制对象
     * @return 如果成功设置提交消息文本, 则返回 true; 否则返回 false
     */
    private boolean setCommitMessageText(@NotNull String commitMessage, @Nullable Object commitMessageControl) {
        if (commitMessageControl == null) {
            return false;
        }
        // 兼容不同版本的提交面板控件 API
        for (String methodName : List.of("setCommitMessage", "setCommitMessageText", "setText")) {
            Method method = findMethod(commitMessageControl.getClass(), methodName, String.class);
            if (method != null) {
                try {
                    method.invoke(commitMessageControl, commitMessage);
                    return true;
                } catch (Exception e) {
                    log.warn("Git 提交页面：调用提交面板方法失败: {}", methodName, e);
                }
            }
        }
        return false;
    }

    /**
     * 查找指定类中的方法
     * <p> 根据方法名和参数类型查找目标类中的方法. 如果找到, 则返回该方法对象; 否则返回 null.
     *
     * @param target    目标类
     * @param name      方法名
     * @param paramType 参数类型
     * @return 匹配的方法对象, 如果未找到则返回 null
     */
    @Nullable
    private static Method findMethod(@NotNull Class<?> target, @NotNull String name, @NotNull Class<?> paramType) {
        try {
            return target.getMethod(name, paramType);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }
}
