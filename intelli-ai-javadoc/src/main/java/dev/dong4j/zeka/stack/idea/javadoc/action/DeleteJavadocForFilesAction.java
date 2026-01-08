package dev.dong4j.zeka.stack.idea.javadoc.action;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.concurrency.AppExecutorUtil;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import dev.dong4j.zeka.stack.idea.javadoc.PluginContents;
import dev.dong4j.zeka.stack.idea.javadoc.service.JavadocDeletionService;
import dev.dong4j.zeka.stack.idea.javadoc.settings.SettingsState;
import dev.dong4j.zeka.stack.idea.javadoc.util.JavadocBundle;
import dev.dong4j.zeka.stack.idea.javadoc.util.NotificationUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 删除 Javadoc 文件动作类
 * <p>
 * 该类继承自 AnAction, 用于在 IntelliJ IDEA 中提供删除 Javadoc 的功能.
 * 支持为单个 Java/Kotlin 文件或整个目录中的文件批量删除 Javadoc 注释.
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @since 2.6.0
 */
@Slf4j
public class DeleteJavadocForFilesAction extends AnAction {

    /** 用于删除 Javadoc 注释的服务实例 */
    private final JavadocDeletionService deletionService = new JavadocDeletionService();

    /**
     * 处理动作事件, 用于删除选中的文件或目录中的 Javadoc 注释
     *
     * @param e 动作事件对象, 包含项目和选中的文件信息
     */
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        VirtualFile[] files = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY);

        if (project == null || project.isDisposed() || files == null || files.length == 0) {
            return;
        }

        // 检查配置是否允许删除
        SettingsState settings = SettingsState.getInstance();
        if (!settings.allowDeleteJavadoc) {
            NotificationUtil.showWarning(project, JavadocBundle.message("notification.delete.javadoc.not.enabled"));
            return;
        }

        // 检查项目是否处于 Dumb Mode（索引模式）
        if (DumbService.isDumb(project)) {
            NotificationUtil.notifyIndexing(project);
            return;
        }

        log.debug("为 {} 个文件/目录删除 Javadoc", files.length);

        // 在后台线程中收集文件（使用 ReadAction 保护 PSI 访问）
        ReadAction.nonBlocking(() -> {
            List<PsiFile> psiFiles = new ArrayList<>();
            PsiManager psiManager = PsiManager.getInstance(project);

            for (VirtualFile file : files) {
                if (file.isDirectory()) {
                    collectFilesFromDirectory(file, psiManager, psiFiles);
                } else if (isSupportedFile(file)) {
                    PsiFile psiFile = psiManager.findFile(file);
                    if (psiFile != null) {
                        psiFiles.add(psiFile);
                    }
                }
            }

            return psiFiles;
        }).finishOnUiThread(ModalityState.current(), psiFiles -> {
            // 在 EDT 上检查结果和显示确认对话框
            if (psiFiles == null || psiFiles.isEmpty()) {
                NotificationUtil.showInfo(project, JavadocBundle.message("notification.no.files.to.delete"));
                return;
            }

            // 确认是否继续
            int result = Messages.showYesNoDialog(
                project,
                JavadocBundle.message("confirmation.delete.javadoc.message", psiFiles.size()),
                JavadocBundle.message("confirmation.delete.javadoc.title"),
                Messages.getQuestionIcon()
                                                 );

            if (result != Messages.YES) {
                return;
            }

            // 在后台线程执行批量删除
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                int totalDeleted = psiFiles.stream().mapToInt(psiFile -> deletionService.deleteJavadocFromFile(project, psiFile)).sum();

                // 切回 EDT 显示完成信息
                ApplicationManager.getApplication().invokeLater(() -> {
                    NotificationUtil.showInfo(project, JavadocBundle.message("notification.delete.javadoc.completed", totalDeleted,
                                                                      psiFiles.size()));
                });
            });
        }).inSmartMode(project).submit(AppExecutorUtil.getAppExecutorService());
    }

    /**
     * 递归收集目录中的所有支持的文件
     *
     * @param directory  目录
     * @param psiManager PSI 管理器
     * @param results    结果列表
     */
    private void collectFilesFromDirectory(@NotNull VirtualFile directory,
                                           @NotNull PsiManager psiManager,
                                           @NotNull List<PsiFile> results) {
        VirtualFile[] children = directory.getChildren();
        if (children == null) {
            return;
        }

        for (VirtualFile child : children) {
            if (child.isDirectory()) {
                collectFilesFromDirectory(child, psiManager, results);
            } else if (isSupportedFile(child)) {
                PsiFile psiFile = psiManager.findFile(child);
                if (psiFile != null) {
                    results.add(psiFile);
                }
            }
        }
    }

    /**
     * 判断给定文件是否为支持的文件类型
     *
     * @param file 文件
     * @return 如果是支持的文件类型返回 true
     */
    private boolean isSupportedFile(@NotNull VirtualFile file) {
        String extension = file.getExtension();
        if (extension == null) {
            return false;
        }
        String extLower = extension.toLowerCase();

        // 检查是否为 Java 文件
        if (PluginContents.JAVA.equals(extLower)) {
            return true;
        }

        // 检查是否为 Kotlin 文件
        if ("kt".equals(extLower)) {
            SettingsState settings = SettingsState.getInstance();
            return settings.isLanguageSupported(PluginContents.KOTLIN);
        }

        return false;
    }

    /**
     * 获取用于更新操作的线程类型
     *
     * @return ActionUpdateThread.EDT 事件调度线程
     */
    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

    /**
     * 更新操作的呈现信息
     *
     * @param e 事件对象, 包含当前操作的上下文信息
     */
    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();

        // 检查项目状态
        if (project == null || project.isDisposed() || DumbService.isDumb(project)) {
            e.getPresentation().setEnabled(false);
            return;
        }

        // 检查配置是否允许删除
        SettingsState settings = SettingsState.getInstance();
        boolean enabled = settings.allowDeleteJavadoc;

        e.getPresentation().setEnabled(enabled);
        e.getPresentation().setVisible(enabled);
        e.getPresentation().setText(JavadocBundle.message("action.delete.javadoc"));
        e.getPresentation().setDescription(JavadocBundle.message("action.delete.javadoc.description"));
    }
}

