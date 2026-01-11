package dev.dong4j.zeka.stack.idea.plugin.changelog.git;

import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vcs.CheckinProjectPanel;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.CommitContext;
import com.intellij.openapi.vcs.checkin.CheckinHandler;
import com.intellij.openapi.vcs.checkin.CheckinHandlerFactory;
import com.intellij.openapi.vcs.checkin.CommitCheck;
import com.intellij.openapi.vcs.checkin.CommitInfo;
import com.intellij.openapi.vcs.checkin.CommitProblem;
import com.intellij.openapi.vcs.checkin.CommitProblemWithDetails;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import com.intellij.ui.content.MessageView;
import com.intellij.util.ui.JBUI;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.swing.JPanel;

import dev.dong4j.zeka.stack.idea.plugin.changelog.util.ChangelogBundle;
import kotlin.coroutines.Continuation;
import kotlin.jvm.functions.Function2;
import kotlinx.coroutines.BuildersKt;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.Dispatchers;

/**
 * 多仓库提交检查处理器工厂
 * <p> 用于在提交前检测是否涉及多个 Git 仓库，并提供可点击的提示链接。
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.11
 * @since 1.0.0
 */
public class CommitMultiRepoCheckinHandlerFactory extends CheckinHandlerFactory {
    /**
     * 创建提交处理程序实例
     *
     * @param panel         提交面板
     * @param commitContext 提交上下文
     * @return 提交处理程序实例
     */
    @Override
    public @NotNull CheckinHandler createHandler(@NotNull CheckinProjectPanel panel,
                                                 @NotNull CommitContext commitContext) {
        return new CommitMultiRepoCheckinHandler(panel);
    }

    /**
     * 多仓库提交检查处理器
     */
    private static class CommitMultiRepoCheckinHandler extends CheckinHandler implements CommitCheck {
        /** 多仓库提交检查处理器使用的面板 */
        private final CheckinProjectPanel panel;

        /**
         * 构造多仓库提交检查处理器
         * <p> 用于初始化多仓库提交检查处理器, 接收提交项目面板用于后续的提交检查操作
         *
         * @param panel 提交项目面板, 提供项目上下文和相关信息
         */
        private CommitMultiRepoCheckinHandler(@NotNull CheckinProjectPanel panel) {
            this.panel = panel;
        }

        /**
         * 获取执行顺序
         * <p> 返回此提交检查处理器的执行顺序, 用于确定在提交检查流程中的执行时机 </p>
         *
         * @return 执行顺序枚举值, 此处返回 {@link ExecutionOrder#EARLY}, 表示该处理器在检查流程的早期阶段执行
         */
        @Override
        public @NotNull ExecutionOrder getExecutionOrder() {
            return ExecutionOrder.EARLY;
        }

        /**
         * 检查此处理器是否已启用
         * <p> 此方法用于确定多仓库提交检查处理器当前是否处于启用状态
         * <p> 在提交检查过程中, 系统会调用此方法来决定是否执行多仓库检查逻辑
         *
         * @return 始终返回 {@code true}, 表示此处理器已启用并将在检查过程中执行
         */
        @Override
        public boolean isEnabled() {
            return true;
        }

        /**
         * 执行多仓库提交检查
         * <p> 检查提交是否涉及多个仓库, 如果涉及多个仓库则返回相应的提交问题提示
         *
         * @param commitInfo   提交信息, 包含提交消息和已提交的变更列表
         * @param continuation 协程 continuation, 用于异步处理
         * @return 如果检测到多仓库提交, 返回 {@link MultiRepoCommitProblem} 对象; 否则返回 null
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

                    List<Change> changes = new ArrayList<>(commitInfo.getCommittedChanges());
                    if (changes.isEmpty()) {
                        Collection<Change> selectedChanges = panel.getSelectedChanges();
                        if (!selectedChanges.isEmpty()) {
                            changes.addAll(selectedChanges);
                        }
                    }
                    if (changes.isEmpty()) {
                        return null;
                    }

                    Map<String, List<Change>> changesByRoot = new CommitMessageGenerator(project).groupChangesByRoot(changes);
                    if (changesByRoot.size() <= 1) {
                        return null;
                    }

                    String repoList = changesByRoot.keySet().stream()
                        .map(path -> new java.io.File(path).getName())
                        .collect(java.util.stream.Collectors.joining("\n"));

                    String message = ChangelogBundle.message("commit.multi.repo.detected",
                                                             changesByRoot.size(),
                                                             repoList);

                    String commitMessage = commitInfo.getCommitMessage();
                    return new MultiRepoCommitProblem(message,
                                                      project,
                                                      commitMessage,
                                                      changesByRoot.size());
                },
                continuation
                                         );
        }
    }

    /**
     * 多仓库提交问题
     */
    private record MultiRepoCommitProblem(String text,
                                          Project project,
                                          String commitMessage,
                                          int repoCount) implements CommitProblemWithDetails {
        /**
         * 存储消息工具窗口中 JBTextArea 的键值
         *
         * @see Key
         */
        private static final Key<JBTextArea> MESSAGE_TEXT_AREA_KEY =
            Key.create("changelog.multi.repo.message.textarea");
        /** 存储消息工具窗口中用于显示提示信息的 JBTextArea 的键值 */
        private static final Key<JBTextArea> HINT_TEXT_AREA_KEY =
            Key.create("changelog.multi.repo.message.hint.textarea");

        /**
         * 构造一个表示多仓库提交问题的实例
         * <p> 该构造函数用于创建一个记录多仓库提交问题的对象, 包含问题描述, 所属项目, 提交信息和涉及的仓库数量.
         *
         * @param text          问题描述文本
         * @param project       所属项目对象
         * @param commitMessage 提交信息内容
         * @param repoCount     涉及的仓库数量
         */
        private MultiRepoCommitProblem(@NotNull String text,
                                       @NotNull Project project,
                                       @NotNull String commitMessage,
                                       int repoCount) {
            this.text = text;
            this.project = project;
            this.commitMessage = commitMessage;
            this.repoCount = repoCount;
        }

        /**
         * 获取问题的文本描述
         * <p> 返回当前多仓库提交问题的简要描述信息
         *
         * @return 问题的文本描述
         */
        @Override
        public @NotNull String getText() {
            return text;
        }

        /**
         * 显示模态解决方案界面
         * <p> 在提交过程中弹出模态对话框以供用户选择解决方案, 本实现直接取消提交操作
         *
         * @param project    当前项目实例, 用于获取工具窗口或显示内容
         * @param commitInfo 提交信息对象, 包含提交的详细内容和上下文
         * @return 返回 {@link CheckinHandler.ReturnResult#CANCEL}, 表示取消当前提交操作
         */
        @Override
        public @NotNull CheckinHandler.ReturnResult showModalSolution(@NotNull Project project,
                                                                      @NotNull CommitInfo commitInfo) {
            return CheckinHandler.ReturnResult.CANCEL;
        }

        /**
         * 返回查看详细信息的链接
         * <p> 该方法返回一个字符串, 表示查看多仓库提交问题详细信息的链接.
         *
         * @return 查看详细信息的链接
         */
        @Override
        public @NotNull String getShowDetailsLink() {
            return ChangelogBundle.message("commit.multi.repo.check.link");
        }

        /**
         * 获取显示详情操作的文本
         * <p> 返回用于在界面中触发显示多仓库提交问题详情操作的文本, 该文本来源于资源包中的键值 "commit.multi.repo.check.action"
         *
         * @return 显示详情操作的文本
         */
        @Override
        public @NotNull String getShowDetailsAction() {
            return ChangelogBundle.message("commit.multi.repo.check.action");
        }

        /**
         * 显示多仓库提交问题的详细信息
         * <p> 该方法会检查当前项目是否与实例所属项目一致, 且项目未被释放或处于 Dumb 模式.
         * 如果条件满足, 则构造提示信息并调用 {@link MultiRepoCommitProblem#showInMessageToolWindow(Project, String, String)} 方法显示在消息工具窗口中.
         *
         * @param project 当前项目上下文, 必须非空
         */
        @Override
        public void showDetails(@NotNull Project project) {
            if (this.project != project || project.isDisposed() || DumbService.isDumb(project)) {
                return;
            }

            String placeholder = ChangelogBundle.message("commit.multi.repo.placeholder", repoCount);
            showInMessageToolWindow(project, placeholder, commitMessage);
        }

        /**
         * 在消息工具窗口中显示指定内容
         * <p> 该方法用于在 IntelliJ IDEA 的消息工具窗口中展示指定内容, 支持显示已有标题的内容或创建新的内容面板. 内容包括提示文本和提交信息, 通过设置文本区域并激活工具窗口进行展示.
         *
         * @param project       项目对象, 用于获取消息视图和工具窗口
         * @param placeholder   需要显示的提示文本内容
         * @param commitMessage 提交信息内容
         */
        private void showInMessageToolWindow(@NotNull Project project,
                                             @NotNull String placeholder,
                                             @NotNull String commitMessage) {
            com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater(() -> {
                if (project.isDisposed()) {
                    return;
                }
                MessageView messageView = MessageView.getInstance(project);
                ContentManager manager = messageView.getContentManager();
                String title = ChangelogBundle.message("commit.multi.repo.session.title");
                Content existing = null;
                for (Content contentItem : manager.getContents()) {
                    if (title.equals(contentItem.getDisplayName())) {
                        existing = contentItem;
                        break;
                    }
                }

                Content target = existing;
                if (target == null) {
                    JBTextArea hintArea = createHintArea();
                    JBTextArea messageArea = createMessageArea();
                    JBScrollPane messageScroll = new JBScrollPane(messageArea);
                    JPanel panel = new JPanel(new BorderLayout());
                    panel.add(createHeaderPanel(hintArea), BorderLayout.NORTH);
                    panel.add(messageScroll, BorderLayout.CENTER);

                    target = ContentFactory.getInstance().createContent(panel, title, false);
                    manager.addContent(target);
                    target.putUserData(HINT_TEXT_AREA_KEY, hintArea);
                    target.putUserData(MESSAGE_TEXT_AREA_KEY, messageArea);
                }

                JBTextArea hintArea = target.getUserData(HINT_TEXT_AREA_KEY);
                JBTextArea messageArea = target.getUserData(MESSAGE_TEXT_AREA_KEY);
                if (hintArea != null) {
                    hintArea.setText(placeholder);
                }
                if (messageArea != null) {
                    messageArea.setText(commitMessage);
                    messageArea.setCaretPosition(0);
                }
                manager.setSelectedContent(target);

                ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Messages");
                if (toolWindow != null) {
                    toolWindow.activate(null, true);
                }
            });
        }

        /**
         * 创建用于显示提示信息的文本区域
         * <p> 该方法初始化一个不可编辑, 支持换行和单词换行的 JBTextArea, 设置为透明背景且无边框, 适用于在消息工具窗口中显示提示内容.
         *
         * @return 初始化后的 JBTextArea, 用于显示提示信息
         */
        private JBTextArea createHintArea() {
            JBTextArea hintArea = new JBTextArea();
            hintArea.setEditable(false);
            hintArea.setLineWrap(true);
            hintArea.setWrapStyleWord(true);
            hintArea.setOpaque(false);
            hintArea.setBorder(null);
            return hintArea;
        }

        /**
         * 创建用于显示提交信息的文本区域
         * <p> 该方法初始化一个可编辑的文本区域, 启用换行和单词换行功能, 用于展示提交消息内容.
         *
         * @return 初始化后的 JBTextArea 对象, 支持换行显示提交信息
         */
        private JBTextArea createMessageArea() {
            JBTextArea messageArea = new JBTextArea();
            messageArea.setLineWrap(true);
            messageArea.setWrapStyleWord(true);
            return messageArea;
        }

        /**
         * 创建包含提示文本和提交信息标题的头部面板
         * <p>该方法使用 GridBagLayout 布局创建一个包含两个组件的面板: 上方的提示文本区域 (hintArea) 和下方的“Commit Message”标签.
         * 提示文本区域占据第一行, 标题标签占据第二行, 两者均设置相同的边距.
         *
         * @param hintArea 用于显示提示信息的文本区域, 必须非空
         * @return 包含提示区域和标题标签的 JPanel 面板
         */
        private JPanel createHeaderPanel(@NotNull JBTextArea hintArea) {
            JPanel panel = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.weightx = 1.0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = JBUI.insets(8, 8, 4, 8);

            panel.add(hintArea, gbc);

            gbc.gridy = 1;
            gbc.insets = JBUI.insets(8, 8, 4, 8);
            panel.add(new JBLabel("Commit Message"), gbc);

            return panel;
        }
    }
}
