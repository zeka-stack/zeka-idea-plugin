package dev.dong4j.zeka.stack.idea.plugin.changelog.ui;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.ex.ToolWindowEx;
import com.intellij.serviceContainer.NonInjectable;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.util.ui.JBUI;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.datatransfer.StringSelection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JPanel;

import dev.dong4j.zeka.stack.idea.plugin.changelog.PluginContents;
import dev.dong4j.zeka.stack.idea.plugin.changelog.util.ChangelogBundle;
import dev.dong4j.zeka.stack.idea.plugin.changelog.util.NotificationUtil;

/**
 * 变更日志工具窗口输出服务
 * <p> 负责创建可复制的文本输出面板, 并支持追加与替换内容.
 *
 * @author dong4j
 * @since 1.0.0
 */
@Service(Service.Level.PROJECT)
public final class ChangelogToolWindowService {

    /**
     * 当前项目实例
     * <p> 用于获取项目的相关服务和上下文信息
     *
     * @see Project
     */
    private final Project project;

    /**
     * 存储每个输出会话的取消状态
     * <p> 键为输出会话的标题，值为取消标志
     */
    private static final Map<String, AtomicBoolean> CANCELLATION_FLAGS = new ConcurrentHashMap<>();

    /**
     * 获取 ChangelogToolWindowService 的单例实例
     * <p> 通过项目对象获取 ChangelogToolWindowService 的唯一实例
     *
     * @param project 项目对象
     * @return ChangelogToolWindowService 的单例实例
     */
    public static @NotNull ChangelogToolWindowService getInstance(@NotNull Project project) {
        return project.getService(ChangelogToolWindowService.class);
    }

    /**
     * 构造函数, 用于初始化变更日志工具窗口服务
     * <p> 该构造函数被标记为 @NonInjectable, 表示不应通过依赖注入的方式调用.
     *
     * @param project IDEA 项目实例
     */
    @NonInjectable
    public ChangelogToolWindowService(@NotNull Project project) {
        this.project = project;
    }

    /**
     * 创建一个新的输出会话
     *
     * @param title 输出标题
     * @return 输出会话
     */
    public @NotNull ChangelogOutputSession openSession(@NotNull String title) {
        if (ApplicationManager.getApplication().isDispatchThread()) {
            return createSession(title);
        }
        AtomicReference<ChangelogOutputSession> ref = new AtomicReference<>();
        ApplicationManager.getApplication().invokeAndWait(() -> ref.set(createSession(title)));
        return ref.get();
    }

    /**
     * 直接显示最终结果 (非流式)
     *
     * @param title   输出标题
     * @param content 输出内容
     */
    public void showResult(@NotNull String title, @NotNull String content) {
        ChangelogOutputSession session = openSession(title);
        session.setText(content);
    }

    /**
     * 创建一个新的输出会话
     * <p> 该方法用于创建一个带有指定标题的工具窗口输出会话. 如果工具窗口不可用, 则显示错误通知并返回一个无效的输出会话.
     *
     * @param title 输出会话的标题
     * @return 包含 JBTextArea 的 ChangelogOutputSession 对象
     */
    private @NotNull ChangelogOutputSession createSession(@NotNull String title) {
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(PluginContents.PLUGIN_NAME);
        if (toolWindow == null) {
            NotificationUtil.showError(project, ChangelogBundle.message("toolwindow.unavailable"));
            return new ChangelogOutputSession(null, null);
        }

        // 首次使用时，动态设置 toolwindow 的布局
        if (toolWindow.getContentManager().getContentCount() == 0) {
            setRightBottomDock(toolWindow);
        }

        JBTextArea textArea = new JBTextArea();
        // 允许用户在工具窗口中编辑内容
        textArea.setEditable(true);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setBackground(JBColor.background());
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, textArea.getFont().getSize()));
        textArea.setBorder(JBUI.Borders.empty(8));

        String fullTitle = buildTitle(title);
        // 为每个会话创建取消标志
        AtomicBoolean cancellationFlag = new AtomicBoolean(false);
        CANCELLATION_FLAGS.put(fullTitle, cancellationFlag);

        JPanel panel = new JPanel(new BorderLayout());
        // ActionToolbar 需要添加其组件实例到容器中
        panel.add(buildToolbar(textArea, fullTitle).getComponent(), BorderLayout.NORTH);
        panel.add(new JBScrollPane(textArea), BorderLayout.CENTER);

        Content content = ContentFactory.getInstance().createContent(panel, fullTitle, false);
        content.setCloseable(true);
        // 当内容关闭时，清理取消标志
        content.setCloseableDelegate(() -> {
            CANCELLATION_FLAGS.remove(fullTitle);
            return true;
        });
        toolWindow.getContentManager().addContent(content);
        toolWindow.getContentManager().setSelectedContent(content);

        if (toolWindow instanceof ToolWindowEx toolWindowEx) {
            toolWindowEx.activate(null, true, true);
        } else {
            toolWindow.activate(null, true, true);
        }

        return new ChangelogOutputSession(textArea, cancellationFlag);
    }

    /**
     * 检查指定会话是否已取消
     *
     * @param sessionTitle 会话标题
     * @return 如果已取消返回 true，否则返回 false
     */
    public static boolean isCancelled(@NotNull String sessionTitle) {
        AtomicBoolean flag = CANCELLATION_FLAGS.get(sessionTitle);
        return flag != null && flag.get();
    }

    /**
     * 停止指定会话的输出
     *
     * @param sessionTitle 会话标题
     */
    public static void stopOutput(@NotNull String sessionTitle) {
        AtomicBoolean flag = CANCELLATION_FLAGS.get(sessionTitle);
        if (flag != null) {
            flag.set(true);
        }
    }

    /**
     * 构建工具栏
     * <p> 为给定的文本区域创建一个包含复制和停止操作的工具栏.
     *
     * @param textArea 文本区域对象
     * @param sessionTitle 输出会话的标题，用于标识取消标志
     * @return 包含复制和停止操作的工具栏
     */
    private @NotNull ActionToolbar buildToolbar(@NotNull JBTextArea textArea, @NotNull String sessionTitle) {
        DefaultActionGroup group = new DefaultActionGroup();
        group.add(new CopyAllAction(textArea));
        group.add(new StopOutputAction(sessionTitle));
        ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar("ChangelogToolWindow", group, true);
        toolbar.setTargetComponent(textArea);
        return toolbar;
    }

    /**
     * 构建输出会话的标题
     * <p> 将传入的标题直接返回作为输出会话的标题
     *
     * @param title 输入的标题
     * @return 返回构建后的标题, 即传入的标题
     */
    private @NotNull String buildTitle(@NotNull String title) {
        return title;
    }

    /**
     * 设置工具窗口的右下角布局
     * <p> 尝试调用不同的版本方法以实现将右侧区域拆分为上下两块的效果. 优先使用带有 Runnable 参数的 setSplitMode 方法,
     * 如果失败, 则尝试调用不带参数的 setSplitMode 方法.
     *
     * @param toolWindow 工具窗口实例
     */
    private void setRightBottomDock(@NotNull ToolWindow toolWindow) {
        // 兼容不同版本的 API，优先使用 split mode 将右侧区域拆分为上下两块
        boolean applied = invokeBooleanMethod(toolWindow, "setSplitMode", true, Runnable.class);
        if (!applied) {
            invokeBooleanMethod(toolWindow, "setSplitMode", true);
        }
    }

    /**
     * 尝试调用指定的布尔方法并设置其参数
     * <p> 该方法通过反射机制调用工具窗口对象上的指定布尔方法, 并传递相应的参数. 如果方法调用成功, 则返回 true; 否则返回 false.
     *
     * @param toolWindow 工具窗口对象
     * @param methodName 要调用的方法名
     * @param value      布尔参数值
     * @param extraTypes 额外的参数类型数组
     * @return 如果方法调用成功则返回 true, 否则返回 false
     */
    private boolean invokeBooleanMethod(@NotNull ToolWindow toolWindow,
                                        @NotNull String methodName,
                                        boolean value,
                                        Class<?>... extraTypes) {
        try {
            Class<?>[] paramTypes = new Class<?>[1 + extraTypes.length];
            paramTypes[0] = boolean.class;
            System.arraycopy(extraTypes, 0, paramTypes, 1, extraTypes.length);
            Object[] params = new Object[1 + extraTypes.length];
            params[0] = value;
            for (int i = 0; i < extraTypes.length; i++) {
                params[i + 1] = null;
            }
            toolWindow.getClass().getMethod(methodName, paramTypes).invoke(toolWindow, params);
            return true;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    /**
     * 输出会话
     * <p> 支持追加输出与全量替换.
     */
    public static final class ChangelogOutputSession {
        /**
         * 文本区域组件, 用于显示输出内容
         *
         * @see JBTextArea
         */
        private final @Nullable JBTextArea textArea;

        /**
         * 取消标志, 用于停止流式输出
         */
        private final @Nullable AtomicBoolean cancellationFlag;

        /**
         * 构造函数, 初始化 ChangelogOutputSession 对象
         * <p> 用于创建一个输出会话对象, 并设置关联的文本区域和取消标志
         *
         * @param textArea 关联的文本区域, 可以为 null
         * @param cancellationFlag 取消标志, 可以为 null
         */
        private ChangelogOutputSession(@Nullable JBTextArea textArea, @Nullable AtomicBoolean cancellationFlag) {
            this.textArea = textArea;
            this.cancellationFlag = cancellationFlag;
        }

        /**
         * 检查是否已取消
         *
         * @return 如果已取消返回 true，否则返回 false
         */
        public boolean isCancelled() {
            return cancellationFlag != null && cancellationFlag.get();
        }

        /**
         * 追加文本到文本区域
         * <p> 在文本区域的末尾追加指定的文本. 如果文本区域为空或未初始化, 则不执行任何操作.
         *
         * @param text 要追加的文本
         */
        public void append(@NotNull String text) {
            if (textArea == null || text.isEmpty()) {
                return;
            }
            ApplicationManager.getApplication().invokeLater(() -> {
                textArea.append(text);
                textArea.setCaretPosition(textArea.getDocument().getLength());
            });
        }

        /**
         * 设置文本区域的内容
         * <p> 将指定的文本设置到文本区域中, 并将光标移动到文本末尾. 如果文本区域为空, 则不进行任何操作.
         *
         * @param text 要设置的文本
         */
        public void setText(@NotNull String text) {
            if (textArea == null) {
                return;
            }
            ApplicationManager.getApplication().invokeLater(() -> {
                textArea.setText(text);
                textArea.setCaretPosition(textArea.getDocument().getLength());
            });
        }
    }

    /** 停止输出的动作类 */
    private static final class StopOutputAction extends AnAction {
        /**
         * 会话标题, 用于标识要停止的输出会话
         */
        private final String sessionTitle;

        /**
         * 构造函数, 初始化 StopOutputAction 对象
         * <p> 该构造函数用于创建一个 StopOutputAction 实例, 并设置其名称和图标.
         *
         * @param sessionTitle 要停止的输出会话标题
         */
        private StopOutputAction(@NotNull String sessionTitle) {
            super(ChangelogBundle.message("toolwindow.stop.text"),
                  ChangelogBundle.message("toolwindow.stop.text"),
                  AllIcons.Actions.Suspend);
            this.sessionTitle = sessionTitle;
        }

        /**
         * 停止输出会话的流式输出
         * <p> 设置取消标志并中断相关线程, 以停止流式输出.
         *
         * @param e AnActionEvent 对象, 包含动作事件信息
         */
        @Override
        public void actionPerformed(@NotNull com.intellij.openapi.actionSystem.AnActionEvent e) {
            stopOutput(sessionTitle);
        }
    }

    /** 一键复制全部内容的动作类 */
    private static final class CopyAllAction extends AnAction {
        /**
         * 文本区域组件, 用于显示和编辑文本内容
         *
         * @see JBTextArea
         */
        private final JBTextArea textArea;

        /**
         * 构造函数, 初始化 CopyAllAction 对象
         * <p> 该构造函数用于创建一个 CopyAllAction 实例, 并设置其名称和图标.
         *
         * @param textArea 要复制文本的 JBTextArea 组件
         */
        private CopyAllAction(@NotNull JBTextArea textArea) {
            super(ChangelogBundle.message("toolwindow.copy.text"),
                  ChangelogBundle.message("toolwindow.copy.text"),
                  AllIcons.Actions.Copy);
            this.textArea = textArea;
        }

        /**
         * 复制文本区域中的所有文本到剪贴板
         * <p> 获取文本区域中的文本, 并将其复制到系统剪贴板中. 如果文本为空, 则不执行任何操作.
         *
         * @param e AnActionEvent 对象, 包含动作事件信息
         */
        @Override
        public void actionPerformed(@NotNull com.intellij.openapi.actionSystem.AnActionEvent e) {
            String text = textArea.getText();
            if (text == null || text.isEmpty()) {
                return;
            }
            com.intellij.openapi.ide.CopyPasteManager.getInstance()
                .setContents(new StringSelection(text));
        }
    }
}
