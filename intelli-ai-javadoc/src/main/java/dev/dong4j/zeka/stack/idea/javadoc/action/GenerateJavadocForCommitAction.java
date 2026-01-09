package dev.dong4j.zeka.stack.idea.javadoc.action;

import com.intellij.ide.HelpTooltip;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.actionSystem.Shortcut;
import com.intellij.openapi.actionSystem.impl.ActionButton;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsDataKeys;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ContentRevision;
import com.intellij.openapi.vcs.changes.CurrentContentRevision;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.vcs.commit.CommitWorkflowHandler;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Component;
import java.awt.Point;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import dev.dong4j.zeka.stack.idea.javadoc.PluginContents;
import dev.dong4j.zeka.stack.idea.javadoc.git.CommitJavadocGenerator;
import dev.dong4j.zeka.stack.idea.javadoc.settings.SettingsState;
import dev.dong4j.zeka.stack.idea.javadoc.util.JavadocBundle;
import dev.dong4j.zeka.stack.idea.javadoc.util.NotificationUtil;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.util.AIProviderUtils;
import icons.AIJicons;
import lombok.extern.slf4j.Slf4j;

/**
 * Git 提交页面 Javadoc 生成动作类
 * <p>
 * 该类继承自 AnAction, 用于在 Git 提交页面检测并生成缺失的 Javadoc 注释.
 * 提供了在提交代码时自动检测 Java 文件并为其生成 Javadoc 的功能,
 * 帮助开发者在提交代码前完善文档注释.
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email mailto:zeka.stack@gmail.com
 * @date 2025.11.30
 * @since 1.0.0
 */
@Slf4j
public class GenerateJavadocForCommitAction extends AnAction {

    /**
     * 更新动作状态
     *
     * <p> 检查是否有 Java 文件变更, 如果有则启用按钮, 否则禁用.
     * 在后台线程中执行, 需要使用 read-action 访问 VCS 数据.
     *
     * @param e 动作事件
     */
    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null || project.isDisposed()) {
            e.getPresentation().setEnabled(false);
            e.getPresentation().setVisible(false);
            return;
        }

        // 检查项目是否处于索引模式
        if (DumbService.isDumb(project)) {
            e.getPresentation().setEnabled(false);
            e.getPresentation().setVisible(true);
            return;
        }

        // 设置按钮文本和图标
        String description = JavadocBundle.message("commit.action.description");
        e.getPresentation().setText("");
        e.getPresentation().setDescription(description);
        e.getPresentation().setIcon(AIJicons.AIJ_16);
        e.getPresentation().putClientProperty(ActionButton.CUSTOM_HELP_TOOLTIP,
                                              new HelpTooltip()
                                                  .setDescription(description)
                                                  .setShortcut(getFirstShortcut())
                                                  .setTitle(JavadocBundle.message("commit.action.text")));

        e.getPresentation().setEnabled(true);
        e.getPresentation().setVisible(true);
    }

    /**
     * 获取更新线程
     *
     * <p> 在后台线程中执行更新操作, 避免阻塞 UI.
     *
     * @return ActionUpdateThread.BGT 后台线程
     */
    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    /**
     * 获取快捷键集合中的第一个快捷键
     * <p> 从当前动作的快捷键集合中返回第一个快捷键, 如果集合为空则返回 null.
     *
     * @return 第一个快捷键, 如果无快捷键则返回 null
     */
    private Shortcut getFirstShortcut() {
        Shortcut[] shortcuts = getShortcutSet().getShortcuts();
        return shortcuts.length > 0 ? shortcuts[0] : null;
    }

    /**
     * 执行动作: 在 Git 提交页面检测并生成缺失的 Javadoc 注释
     * <p> 当用户点击按钮时, 该方法会检测当前提交的 Java 文件中缺少 Javadoc 的元素, 并调用文档生成器批量生成注释.
     * <p> 执行流程如下:
     * <ul>
     *   <li> 检查项目是否有效且未被释放 </li>
     *   <li> 获取当前提交工作流处理器, 若未找到则提示用户未选择变更 </li>
     *   <li> 验证是否配置了有效的 AI 提供商, 若未配置则直接返回 </li>
     *   <li> 记录调试日志, 开始检测缺少 Javadoc 的代码 </li>
     *   <li> 获取用户选择的文件变更列表 </li>
     *   <li> 若无变更, 则提示用户未选择任何文件 </li>
     *   <li> 过滤出符合条件的 Java 或 Kotlin 文件 </li>
     *   <li> 若未找到任何 Java/Kotlin 文件, 则直接返回 </li>
     *   <li> 记录找到的文件数量 </li>
     *   <li> 创建文档生成器并调用其生成方法, 对变更和文件进行批量注释生成 </li>
     * </ul>
     *
     * @param e 动作事件, 用于获取项目上下文和提交工作流处理器
     */
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null || project.isDisposed()) {
            return;
        }

        // 检查项目是否处于索引模式
        if (DumbService.isDumb(project)) {
            NotificationUtil.notifyIndexing(project);
            return;
        }

        CommitWorkflowHandler commitWorkflowHandler = e.getData(VcsDataKeys.COMMIT_WORKFLOW_HANDLER);
        if (commitWorkflowHandler == null) {
            NotificationUtil.showWarning(project, JavadocBundle.message("commit.no.selected.changes"));
            return;
        }

        // 检查 AI Provider 配置
        AIProviderConfig config = SettingsState.getInstance().providerConfig;
        if (!AIProviderUtils.hasAIProvider(project, config, JavadocBundle.message("settings.display.name"), JavadocBundle.message(
            "settings.ai.provider.selection"))) {
            return;
        }

        log.debug("Git 提交页面：开始检测缺少 Javadoc 的代码");

        // 在 ReadAction 中获取提交的文件变更和过滤 Java 文件
        Collection<Change> changes = getSelectedChanges(commitWorkflowHandler);
        if (changes.isEmpty()) {
            log.debug("Git 提交页面：未选择任何文件变更");
            showActionTip(e, JavadocBundle.message("commit.no.selected.changes"));
            return;
        }

        // 在 ReadAction 中过滤 Java 文件
        List<VirtualFile> javaFiles = filterJavaFiles(project, changes);
        if (javaFiles.isEmpty()) {
            log.debug("Git 提交页面：没有找到 Java 文件");
            showActionTip(e, JavadocBundle.message("commit.no.java.files"));
            return;
        }

        log.debug("Git 提交页面：找到 {} 个 Java 文件", javaFiles.size());

        // 使用生成器检测和生成文档
        CommitJavadocGenerator generator = new CommitJavadocGenerator(project);
        generator.generateForChanges(javaFiles);
    }

    /**
     * 获取提交的文件变更
     * <p> 从给定的 CommitWorkflowHandler 中提取已选择的变更, 并返回变更列表. 如果未选择任何变更, 则返回空列表.
     *
     * @param commitWorkflowHandler 包含 VCS 数据的 CommitWorkflowHandler 对象
     * @return 文件变更列表, 如果未选择任何变更则返回空列表
     */
    @NotNull
    private Collection<Change> getSelectedChanges(@NotNull CommitWorkflowHandler commitWorkflowHandler) {
        Object ui = invoke(commitWorkflowHandler, "getUi");
        Object changes = invoke(ui, "getIncludedChanges");
        List<Change> result = new java.util.ArrayList<>();
        if (changes instanceof Collection<?> items) {
            for (Object item : items) {
                if (item instanceof Change change) {
                    result.add(change);
                }
            }
        }

        Object unversioned = invoke(ui, "getIncludedUnversionedFiles");
        if (unversioned instanceof Collection<?> items) {
            for (Object item : items) {
                if (item instanceof FilePath filePath) {
                    ContentRevision revision = new CurrentContentRevision(filePath);
                    result.add(new Change(null, revision));
                }
            }
        }

        return result;
    }

    /**
     * 过滤文件变更列表中的 Java 和 Kotlin 文件
     * <p>
     * 从传入的文件变更列表中筛选出扩展名为 ".java" 或 ".kt" 的虚拟文件, 并确保这些文件位于项目范围内, 排除 jar 包中的源码文件.
     * 该方法必须在 ReadAction 中执行, 以确保安全访问项目文件索引.
     *
     * @param project 项目对象, 用于获取项目文件索引和判断文件是否在项目范围内
     * @param changes 文件变更列表, 包含所有待筛选的文件变更对象
     * @return 包含所有符合条件的 Java 或 Kotlin 文件的虚拟文件列表, 如果无符合条件的文件则返回空列表
     */
    @NotNull
    private List<VirtualFile> filterJavaFiles(@NotNull Project project,
                                              @NotNull Collection<Change> changes) {
        if (project.isDisposed()) {
            return new ArrayList<>();
        }

        return ApplicationManager.getApplication().runReadAction(
            (Computable<List<VirtualFile>>) () -> {
                ProjectFileIndex fileIndex = ProjectFileIndex.getInstance(project);
                return changes.stream()
                    .map(Change::getVirtualFile)
                    .filter(file -> file != null
                                    && (PluginContents.JAVA.equalsIgnoreCase(file.getExtension())
                                        || PluginContents.KOTLIN_EXTENSION.equalsIgnoreCase(file.getExtension()))
                                    && isFileInProject(project, file, fileIndex))
                    .collect(Collectors.toList());
            }
                                                                );
    }

    /**
     * 检查文件是否在项目范围内
     * <p>
     * 该方法检查以下条件：
     * <ol>
     *   <li>文件是否在 JarFileSystem 中（jar 中的源码应该排除）</li>
     *   <li>文件是否在本地文件系统中（不是 jar 中的文件）</li>
     *   <li>文件是否在项目的源码根目录或资源根目录中</li>
     * </ol>
     * <p>
     * <b>重要：</b>该方法必须在 ReadAction 中调用，因为 {@link ProjectFileIndex#isInProject(VirtualFile)}
     * 需要访问项目文件索引，必须在 ReadAction 中执行。
     *
     * @param project   项目对象
     * @param file      虚拟文件
     * @param fileIndex 项目文件索引（已在 ReadAction 中获取）
     * @return 如果文件在项目内且不是 jar 中的源码，返回 true；否则返回 false
     */
    private boolean isFileInProject(@NotNull Project project,
                                    @NotNull VirtualFile file,
                                    @NotNull ProjectFileIndex fileIndex) {
        // 检查文件是否在 jar 中（jar 中的源码不应该处理）
        if (file.getFileSystem() instanceof JarFileSystem) {
            return false;
        }

        // 检查文件是否在本地文件系统中
        if (!(file.getFileSystem() instanceof LocalFileSystem)) {
            return false;
        }

        // 检查文件是否在项目的源码根目录或资源根目录中
        return fileIndex.isInProject(file);
    }

    /**
     * 显示动作提示气泡
     * <p> 在指定组件下方显示一个带警告样式的 HTML 气泡提示, 用于向用户展示提示信息.
     * <p> 该方法会尝试从动作事件中获取组件上下文, 若失败则尝试从项目窗口获取框架组件作为显示位置.
     * <p> 气泡提示将在 2500 毫秒后自动淡出.
     *
     * @param e       动作事件, 用于获取上下文组件或项目框架
     * @param message 提示内容的文本, 支持 HTML 格式
     */
    private void showActionTip(@NotNull AnActionEvent e, @NotNull String message) {
        Component component = null;
        if (e.getInputEvent() != null) {
            component = e.getInputEvent().getComponent();
        }
        if (component == null) {
            component = e.getData(PlatformDataKeys.CONTEXT_COMPONENT);
        }
        if (component == null) {
            Project project = e.getProject();
            if (project != null) {
                component = WindowManager.getInstance().getFrame(project);
            }
        }
        if (component == null) {
            return;
        }

        JBPopupFactory.getInstance()
            .createHtmlTextBalloonBuilder(message, MessageType.WARNING, null)
            .setFadeoutTime(2500)
            .createBalloon()
            .show(new RelativePoint(component, new Point(component.getWidth(), component.getHeight())), Balloon.Position.below);
    }

    /**
     * 调用目标对象的指定方法
     * <p> 通过反射机制获取目标对象的指定方法并调用, 如果目标对象为 null 或者方法调用失败, 则返回 null.
     *
     * @param target     目标对象
     * @param methodName 方法名
     * @return 方法调用的结果, 如果目标对象为 null 或者方法调用失败, 则返回 null
     */
    @Nullable
    private static Object invoke(@Nullable Object target, @NotNull String methodName) {
        if (target == null) {
            return null;
        }
        try {
            Method method = target.getClass().getMethod(methodName);
            return method.invoke(target);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

}
