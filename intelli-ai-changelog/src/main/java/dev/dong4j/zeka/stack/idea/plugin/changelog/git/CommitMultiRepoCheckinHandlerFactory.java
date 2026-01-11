package dev.dong4j.zeka.stack.idea.plugin.changelog.git;

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
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.swing.JPanel;

import dev.dong4j.zeka.stack.idea.plugin.changelog.settings.SettingsState;
import dev.dong4j.zeka.stack.idea.plugin.changelog.util.ChangelogBundle;
import kotlin.coroutines.Continuation;
import kotlin.jvm.functions.Function2;
import kotlinx.coroutines.BuildersKt;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.Dispatchers;

/**
 * CommitMultiRepoCheckinHandlerFactory
 * <p> 用于创建和管理多仓库提交检查处理器的工厂类, 主要职责是根据传入的面板和提交上下文生成对应的处理器实例. 该类封装了多仓库提交检查的业务逻辑, 避免基础设施关注, 专注于封装业务规则, 面向对象设计, 不负责请求处理.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.11
 * @since 1.0.0
 */
public class CommitMultiRepoCheckinHandlerFactory extends CheckinHandlerFactory {
    /**
     * 创建多仓库提交检查处理器实例
     * <p> 该方法用于创建并返回一个 {@link CommitMultiRepoCheckinHandler} 实例, 用于在提交前检测是否涉及多个 Git 仓库.</p>
     *
     * @param panel         提交面板, 提供项目上下文和相关信息
     * @param commitContext 提交上下文, 包含与当前提交相关的额外信息
     * @return 返回新创建的 {@link CommitMultiRepoCheckinHandler} 实例
     */
    @Override
    public @NotNull CheckinHandler createHandler(@NotNull CheckinProjectPanel panel,
                                                 @NotNull CommitContext commitContext) {
        return new CommitMultiRepoCheckinHandler(panel);
    }

    /**
     * 多仓库提交检查处理器
     * <p> 用于在提交代码时检测是否涉及多个仓库的变更, 若检测到多个仓库的变更, 则提示用户并阻止提交, 以避免提交到错误的仓库.
     * 该处理器在提交流程早期执行, 仅在启用多仓库提交检查功能时生效, 且不负责请求处理, 仅提供检查逻辑.
     * 通过将变更按根目录分组, 统计涉及的仓库数量, 并生成提示信息, 引导用户确认提交意图.
     * 该类为内部使用, 避免基础设施关注, 符合面向对象设计原则.
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.11
     * @since 1.0.0
     */
    private static class CommitMultiRepoCheckinHandler extends CheckinHandler implements CommitCheck {
        /** 提交项目面板, 提供项目上下文和相关信息 */
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
         * <p> 此方法用于确定多仓库提交检查处理器当前是否处于启用状态.
         * 在提交检查过程中, 系统会调用此方法来决定是否执行多仓库检查逻辑.
         *
         * @return 返回 {@code true} 表示此处理器已启用并将在检查过程中执行; 返回 {@code false} 表示未启用.
         */
        @Override
        public boolean isEnabled() {
            return SettingsState.getInstance().enableCommitMultiRepoCheck;
        }

        /**
         * 执行多仓库提交检查
         * <p> 检查提交是否涉及多个仓库, 如果涉及多个仓库则返回相应的提交问题提示.
         * 如果未启用多仓库提交检查功能或项目已废弃或处于哑模式, 则返回 null.</p>
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
                    if (!SettingsState.getInstance().enableCommitMultiRepoCheck) {
                        return null;
                    }
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
                        .map(path -> "- " + new java.io.File(path).getName())
                        .collect(java.util.stream.Collectors.joining("\n"));

                    String message = ChangelogBundle.message("commit.multi.repo.detected",
                                                             changesByRoot.size());

                    String commitMessage = commitInfo.getCommitMessage();
                    return new MultiRepoCommitProblem(message,
                                                      project,
                                                      commitMessage,
                                                      changesByRoot.size(),
                                                      repoList);
                },
                continuation
                                         );
        }

        /**
         * 获取提交前配置面板
         * <p> 创建并返回一个布尔类型的提交前配置面板, 用于控制多仓库提交检查功能的启用状态.
         * 该面板允许用户在提交前选择是否启用多仓库提交检查, 其状态与全局设置同步.
         *
         * @return 提交前配置面板, 类型为 {@link com.intellij.openapi.vcs.ui.RefreshableOnComponent}
         * @see BooleanCommitOption
         * @see SettingsState
         * @see ChangelogBundle
         */
        @Override
        public com.intellij.openapi.vcs.ui.RefreshableOnComponent getBeforeCheckinConfigurationPanel() {
            return BooleanCommitOption.create(
                panel.getProject(),
                this,
                false,
                ChangelogBundle.message("commit.multi.repo.check.option"),
                () -> SettingsState.getInstance().enableCommitMultiRepoCheck,
                value -> SettingsState.getInstance().enableCommitMultiRepoCheck = value
                                             );
        }
    }

    /**
     * 多仓库提交问题数据记录类
     * <p> 用于封装在多仓库环境下提交时遇到的问题详情, 包括提交信息, 仓库数量, 仓库列表等, 并提供在 IDE 中以消息工具窗口形式展示问题详情的交互能力.
     * 该类作为不可变数据记录类 (record), 主要用于在提交流程中传递和展示多仓库冲突或提示信息, 避免直接暴露基础设施细节.
     * 通过 {@link #showDetails(Project)} 方法, 可在指定项目中弹出消息工具窗口, 展示问题详情和提交信息, 支持动态调整文本区域高度和滚动行为.
     * 该类不负责请求处理, 仅作为数据载体和展示逻辑的封装, 符合面向对象设计原则, 职责单一.
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.11
     * @since 1.0.0
     */
    private record MultiRepoCommitProblem(String text,
                                          Project project,
                                          String commitMessage,
                                          int repoCount,
                                          String repoList) implements CommitProblemWithDetails {
        /** 消息工具窗口文本区域的键值 */
        private static final Key<JBTextArea> MESSAGE_TEXT_AREA_KEY =
            Key.create("changelog.multi.repo.message.textarea");
        /** 存储消息工具窗口中用于显示提示信息的 JBTextArea 的键值 */
        private static final Key<JBTextArea> HINT_TEXT_AREA_KEY =
            Key.create("changelog.multi.repo.message.hint.textarea");
        /** 存储消息工具窗口中用于承载提交信息输入框的滚动面板 */
        private static final Key<JBScrollPane> MESSAGE_SCROLL_PANE_KEY =
            Key.create("changelog.multi.repo.message.scrollpane");

        /**
         * 构造一个表示多仓库提交问题的实例
         * <p> 该构造函数用于创建一个记录多仓库提交问题的对象, 包含问题描述, 所属项目, 提交信息以及涉及的仓库数量.
         * 该构造函数创建的是不可变对象, 所有属性在对象创建后不可修改.
         *
         * @param text          问题描述文本, 不能为空
         * @param project       所属项目对象, 不能为空
         * @param commitMessage 提交信息内容, 不能为空
         * @param repoCount     涉及的仓库数量
         * @param repoList      仓库列表字符串, 可能包含多个仓库标识, 可以为空
         */
        private MultiRepoCommitProblem(@NotNull String text,
                                       @NotNull Project project,
                                       @NotNull String commitMessage,
                                       int repoCount,
                                       String repoList) {
            this.text = text;
            this.project = project;
            this.commitMessage = commitMessage;
            this.repoCount = repoCount;
            this.repoList = repoList;
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
         * <p> 在提交过程中弹出模态对话框以供用户选择解决方案, 本实现直接取消提交操作.
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
         * <p>该方法会检查当前项目是否与实例所属项目一致, 且项目未被释放或处于 Dumb 模式.
         * 如果条件满足, 则构造提示信息并调用 {@link MultiRepoCommitProblem#showInMessageToolWindow(Project, String, String)} 方法显示在消息工具窗口中.
         *
         * @param project 当前项目上下文, 必须非空
         */
        @Override
        public void showDetails(@NotNull Project project) {
            if (this.project != project || project.isDisposed() || DumbService.isDumb(project)) {
                return;
            }

            String placeholder = ChangelogBundle.message("commit.multi.repo.placeholder", repoCount, repoList);
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
                    target.putUserData(MESSAGE_SCROLL_PANE_KEY, messageScroll);
                }

                JBTextArea hintArea = target.getUserData(HINT_TEXT_AREA_KEY);
                JBTextArea messageArea = target.getUserData(MESSAGE_TEXT_AREA_KEY);
                JBScrollPane messageScroll = target.getUserData(MESSAGE_SCROLL_PANE_KEY);
                if (hintArea != null) {
                    hintArea.setText(placeholder);
                }
                if (messageArea != null) {
                    messageArea.setText(commitMessage);
                    messageArea.setCaretPosition(0);
                    updateMessageAreaHeight(messageArea, messageScroll);
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
         * 根据文本区域内容动态调整其高度以适应显示内容
         * <p> 该方法用于在消息工具窗口中自动调整提交信息文本区域的高度, 使其能完整显示所有行内容, 同时保持水平宽度不变. 如果传入的滚动面板非空, 则同步调整其尺寸. 通过计算文本行数和字体行高, 动态设置文本区域的行数和首选尺寸,
         * 并触发重验证以确保界面更新.
         *
         * @param messageArea 需要调整高度的文本区域对象, 必须非空
         * @param scrollPane  用于包裹文本区域的滚动面板, 可为空
         */
        private void updateMessageAreaHeight(@NotNull JBTextArea messageArea, @Nullable JBScrollPane scrollPane) {
            int width = 0;
            if (scrollPane != null) {
                width = scrollPane.getViewport().getWidth();
            }
            if (width <= 0) {
                width = messageArea.getPreferredSize().width;
            }
            if (width <= 0) {
                return;
            }
            messageArea.setSize(new Dimension(width, Integer.MAX_VALUE));
            int lineCount = Math.max(1, messageArea.getLineCount());
            messageArea.setRows(lineCount);
            int lineHeight = messageArea.getFontMetrics(messageArea.getFont()).getHeight();
            int targetHeight = lineCount * lineHeight;
            messageArea.setPreferredSize(new Dimension(width, targetHeight));
            messageArea.revalidate();
            if (scrollPane != null) {
                scrollPane.setPreferredSize(new Dimension(width, targetHeight));
                scrollPane.revalidate();
            }
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
            panel.add(new JBLabel(ChangelogBundle.message("commit.message.label")), gbc);

            return panel;
        }
    }
}
