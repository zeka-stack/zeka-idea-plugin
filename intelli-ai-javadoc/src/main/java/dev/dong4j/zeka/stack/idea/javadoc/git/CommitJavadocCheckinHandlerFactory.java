package dev.dong4j.zeka.stack.idea.javadoc.git;

import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vcs.CheckinProjectPanel;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.CommitContext;
import com.intellij.openapi.vcs.changes.ui.BooleanCommitOption;
import com.intellij.openapi.vcs.checkin.CheckinHandler;
import com.intellij.openapi.vcs.checkin.CheckinHandlerFactory;
import com.intellij.openapi.vcs.checkin.CommitCheck;
import com.intellij.openapi.vcs.checkin.CommitInfo;
import com.intellij.openapi.vcs.checkin.CommitProblem;
import com.intellij.openapi.vfs.VirtualFile;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import dev.dong4j.zeka.stack.idea.javadoc.settings.SettingsState;
import dev.dong4j.zeka.stack.idea.javadoc.task.DocumentationTask;
import dev.dong4j.zeka.stack.idea.javadoc.util.JavadocBundle;
import kotlin.coroutines.Continuation;
import kotlin.jvm.functions.Function2;
import kotlinx.coroutines.BuildersKt;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.Dispatchers;

/**
 * CommitJavadocCheckinHandlerFactory 类
 * <p> 用于创建和管理 CommitJavadocCheckinHandler 实例, 该处理器负责在提交代码前检查 Java 文件的 Javadoc 注释是否完整. 通过封装业务逻辑, 避免基础设施关注, 实现面向对象设计, 确保不直接处理请求.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.11
 * @since 1.0.0
 */
public class CommitJavadocCheckinHandlerFactory extends CheckinHandlerFactory {
    /**
     * 创建提交处理程序实例
     * <p> 返回一个用于检查 JavaDoc 的提交处理程序 {@link CommitJavadocCheckinHandler}
     *
     * @param panel         提交面板, 用于获取项目上下文信息
     * @param commitContext 提交上下文, 包含提交相关的数据和配置
     * @return 返回一个非空的提交处理程序实例
     */
    @Override
    public @NotNull CheckinHandler createHandler(@NotNull CheckinProjectPanel panel,
                                                 @NotNull CommitContext commitContext) {
        return new CommitJavadocCheckinHandler(panel);
    }

    /**
     * 提交时 JavaDoc 检查处理器类
     * <p> 该内部类用于在代码提交前检查是否存在缺失的 JavaDoc 注释, 确保符合项目规范.
     * 仅在启用相关设置的情况下执行检查, 并通过异步方式处理文件扫描和问题汇总.
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.11
     * @since 1.0.0
     */
    private static class CommitJavadocCheckinHandler extends CheckinHandler implements CommitCheck {
        /** 跳过本次提交时的 Javadoc 检查标记键, 用于在提交后临时禁用检查, 避免重复提示 <a href="https://example.com">https://example.com</a> */
        private static final Key<Boolean> SKIP_ONCE_KEY =
            Key.create("dev.dong4j.zeka.stack.idea.javadoc.commit.check.skip.once");

        /** 提交项目面板, 用于访问提交相关的项目信息和配置 */
        private final CheckinProjectPanel panel;

        /**
         * 初始化 CommitJavadocCheckinHandler 实例
         * <p> 通过传入的 CheckinProjectPanel 对象进行初始化, 用于后续的提交检查操作.
         *
         * @param panel 提交面板对象, 用于获取项目信息和配置选项
         */
        private CommitJavadocCheckinHandler(@NotNull CheckinProjectPanel panel) {
            this.panel = panel;
        }

        /**
         * 返回检查执行顺序
         * <p> 此方法用于指定在提交检查中的执行顺序, 返回值为 ExecutionOrder.LATE 表示该检查将在其他检查之后执行.
         *
         * @return 执行顺序, 固定为 ExecutionOrder.LATE
         */
        @Override
        public @NotNull ExecutionOrder getExecutionOrder() {
            return ExecutionOrder.LATE;
        }

        /**
         * 判断当前是否启用提交时检查 JavaDoc 的功能
         * <p> 该方法用于确定是否在提交代码时进行 JavaDoc 检查, 根据配置状态返回相应布尔值.
         *
         * @return 如果启用了 JavaDoc 检查功能, 返回 true; 否则返回 false
         */
        @Override
        public boolean isEnabled() {
            return SettingsState.getInstance().enableCommitJavadocCheck;
        }

        /**
         * 执行提交前的 JavaDoc 检查逻辑
         * <p> 该方法在代码提交时运行, 用于检测是否有缺失的 JavaDoc 注释. 如果发现缺失注释, 则返回一个 {@link JavadocCommitProblem} 问题对象.</p>
         *
         * @param commitInfo   提交信息, 包含当前提交的上下文和变更内容
         * @param continuation 协程继续回调, 用于异步执行检查任务
         * @return 如果检测到 JavaDoc 缺失, 返回一个 {@link JavadocCommitProblem} 对象; 否则返回 null
         */
        @Override
        public @Nullable Object runCheck(@NotNull CommitInfo commitInfo,
                                         @NotNull Continuation<? super CommitProblem> continuation) {
            return BuildersKt.withContext(
                Dispatchers.getDefault(),
                (Function2<CoroutineScope, Continuation<? super CommitProblem>, Object>) (scope, cont) -> {
                    Project project = panel.getProject();
                    if (project.isDisposed() || DumbService.isDumb(project)) {
                        return null;
                    }

                    CommitContext context = commitInfo.getCommitContext();
                    if (Boolean.TRUE.equals(context.getUserData(SKIP_ONCE_KEY))) {
                        context.putUserData(SKIP_ONCE_KEY, null);
                        return null;
                    }

                    List<Change> changes = commitInfo.getCommittedChanges();
                    if (changes.isEmpty()) {
                        return null;
                    }

                    List<VirtualFile> javaFiles = CommitJavadocChecker.filterJavaFiles(project, changes);
                    if (javaFiles.isEmpty()) {
                        return null;
                    }

                    List<DocumentationTask> tasks = CommitJavadocChecker.detectMissingJavaDoc(project, javaFiles, null);
                    if (tasks.isEmpty()) {
                        return null;
                    }

                    CommitJavadocChecker.DetectionSummary summary = CommitJavadocChecker.buildDetectionSummary(tasks);
                    String message = JavadocBundle.message("commit.check.javadoc.warning", summary.summary());
                    context.putUserData(SKIP_ONCE_KEY, Boolean.TRUE);
                    return new JavadocCommitProblem(message);
                },
                continuation
                                         );
        }

        /**
         * 获取提交前配置面板, 用于控制是否在提交时检查 Javadoc
         * <p> 创建一个布尔类型的提交选项, 允许用户启用或禁用提交时的 Javadoc 检查功能.
         * 该面板在提交对话框中显示, 用户可通过勾选或取消勾选来控制是否启用检查.
         *
         * @return 返回一个可刷新的配置面板组件, 用于在提交前展示 Javadoc 检查选项
         * @see BooleanCommitOption
         * @see JavadocBundle
         * @see SettingsState
         */
        @Override
        public com.intellij.openapi.vcs.ui.RefreshableOnComponent getBeforeCheckinConfigurationPanel() {
            return BooleanCommitOption.create(
                panel.getProject(),
                this,
                false,
                JavadocBundle.message("commit.check.javadoc.option"),
                () -> SettingsState.getInstance().enableCommitJavadocCheck,
                value -> SettingsState.getInstance().enableCommitJavadocCheck = value
                                             );
        }
    }

    /**
     * JavadocCommitProblem 类
     * <p> 用于表示与 Javadoc 相关的提交问题, 主要负责查询和检索 Javadoc 提交问题的文本信息. 该类仅在内部使用, 不参与请求处理, 专注于面向对象设计, 避免基础设施关注.
     *
     * @param text 提交问题的文本描述
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.11
     * @since 1.0.0
     */
        private record JavadocCommitProblem(String text) implements CommitProblem {
            /**
             * 初始化 JavadocCommitProblem 实例
             * <p> 构造函数, 用于创建一个包含指定文本的 JavadocCommitProblem 对象
             *
             * @param text 问题描述文本, 不能为空
             */
            private JavadocCommitProblem(@NotNull String text) {
                this.text = text;
            }

            /**
             * 获取文本内容
             * <p> 返回该实例存储的文本信息.
             *
             * @return 文本内容, 保证不为 null
             */
            @Override
            public @NotNull String text() {
                return text;
            }

            /**
             * 显示解决方案对话框并返回处理结果
             * <p> 此方法被调用时, 会显示一个解决方案对话框. 当前实现总是返回取消操作的结果.
             *
             * @param project    当前项目对象
             * @param commitInfo 提交信息对象
             * @return 操作结果, 当前总是返回 {@code CheckinHandler.ReturnResult.CANCEL}
             */
            @Override
            public @NotNull CheckinHandler.ReturnResult showModalSolution(@NotNull Project project,
                                                                          @NotNull CommitInfo commitInfo) {
                return CheckinHandler.ReturnResult.CANCEL;
            }
        }
}
