package dev.dong4j.zeka.stack.idea.javadoc.listener;

import com.intellij.ide.actionsOnSave.impl.ActionsOnSaveFileDocumentManagerListener;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileDocumentManagerListener;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.concurrency.AppExecutorUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.psi.KtFile;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import dev.dong4j.zeka.stack.idea.javadoc.PluginContents;
import dev.dong4j.zeka.stack.idea.javadoc.service.DocumentationGenerationService;
import dev.dong4j.zeka.stack.idea.javadoc.settings.SettingsState;
import dev.dong4j.zeka.stack.idea.javadoc.task.DocumentationTask;
import dev.dong4j.zeka.stack.idea.javadoc.task.TaskCollector;
import dev.dong4j.zeka.stack.idea.javadoc.util.JavadocBundle;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.util.AIProviderUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * 文件保存时自动生成 Javadoc 监听器
 * <p>
 * 监听文件保存事件，当用户启用"保存时生成注释"功能时，自动为保存的文件生成 Javadoc 注释。
 * 只处理 Java 和 Kotlin 文件，并确保在后台线程中执行，避免阻塞文件保存操作。
 *
 * @author zeka.stack.team
 * @version 2.8.0
 * @see ActionsOnSaveFileDocumentManagerListener
 * @since 2.8.0
 */
@Slf4j
public class GenerateOnSaveListener implements FileDocumentManagerListener {

    /**
     * 防止重复触发的标志
     * <p>
     * 使用 AtomicBoolean 确保在多线程环境下的线程安全性。
     * 当正在生成文档时，忽略后续的保存事件，避免重复生成。
     */
    private static final AtomicBoolean isGenerating = new AtomicBoolean(false);

    /** 记录文档改动范围，用于保存时只处理修改过的元素 */
    private static final ConcurrentMap<Document, List<RangeMarker>> changedRangesByDocument = new ConcurrentHashMap<>();

    /**
     * 表示文档监听器是否已注册的标志
     * <p>
     * 使用 AtomicBoolean 确保在多线程环境下的线程安全性.
     * 当文档监听器首次被注册时, 该标志会被设置为 true, 以避免重复注册.
     */
    private static final AtomicBoolean changeListenerRegistered = new AtomicBoolean(false);

    /**
     * 构造函数
     * <p> 初始化 GenerateOnSaveListener 对象, 并注册文档监听器.
     * <p> 确保文档监听器只被注册一次, 避免重复注册.
     *
     * @since 2.8.0
     */
    public GenerateOnSaveListener() {
        if (changeListenerRegistered.compareAndSet(false, true)) {
            EditorFactory.getInstance().getEventMulticaster().addDocumentListener(new SaveDocumentChangeTracker(),
                                                                                  ApplicationManager.getApplication());
        }
    }

    /**
     * 在文档保存之后触发
     * <p>
     * 当文件保存完成后, 检查是否启用了 "保存时生成注释" 功能, 如果启用则触发 Javadoc 生成.
     * 使用延迟执行确保在保存完成后再生成, 避免在保存过程中修改文档导致循环保存.
     *
     * @param document 已保存的文档对象
     * @param explicit 是否显式保存
     */
    @Override
    public void beforeAnyDocumentSaving(@NotNull Document document, boolean explicit) {
        if (!explicit) {
            return;
        }

        // 检查是否启用了保存时生成注释功能
        SettingsState settings = SettingsState.getInstance();
        if (!settings.generateOnSave) {
            return;
        }

        // 如果正在生成文档，忽略本次保存事件，避免重复触发
        if (isGenerating.get()) {
            return;
        }

        // 获取文件对象
        VirtualFile virtualFile = FileDocumentManager.getInstance().getFile(document);
        if (virtualFile == null) {
            return;
        }

        // 检查文件类型（只处理 Java 和 Kotlin 文件）
        if (!isSupportedFile(virtualFile)) {
            return;
        }

        // 获取项目对象
        Project project = getProjectForFile(virtualFile);
        if (project == null || project.isDisposed()) {
            return;
        }

        if (!isCurrentEditorFile(project, document, virtualFile)) {
            return;
        }

        // 检查项目是否处于 Dumb Mode（索引模式）
        if (DumbService.isDumb(project)) {
            return;
        }

        // 检查 AI Provider 配置
        AIProviderConfig config = settings.providerConfig;
        if (!AIProviderUtils.hasAIProvider(project, config, JavadocBundle.message("settings.display.name"), JavadocBundle.message(
            "settings.ai.provider.selection"))) {
            return;
        }

        List<TextRange> changedRanges = consumeChangedRanges(document);
        if (changedRanges.isEmpty()) {
            return;
        }

        // 延迟执行，确保在保存完成后再生成（延迟 500ms，避免与保存操作冲突）
        AppExecutorUtil.getAppScheduledExecutorService().schedule(() -> {
            // 再次检查项目状态
            if (project.isDisposed()) {
                return;
            }

            // 在后台线程中执行生成逻辑，避免阻塞
            ReadAction.nonBlocking(() -> {
                    // 重新获取文件（保存后可能需要刷新）
                    VirtualFile file = FileDocumentManager.getInstance().getFile(document);
                    if (file == null || !file.isValid()) {
                        return null;
                    }

                    // 获取 PsiFile 对象
                    PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
                    if (psiFile == null) {
                        return null;
                    }

                    // 检查文件类型
                    if (!(psiFile instanceof PsiJavaFile) && !(psiFile instanceof KtFile)) {
                        return null;
                    }

                    // 检查是否支持 Kotlin
                    if (psiFile instanceof KtFile) {
                        if (!settings.isLanguageSupported(PluginContents.KOTLIN)) {
                            return null;
                        }
                    }

                    // 收集任务
                    TaskCollector collector = new TaskCollector(project);
                    List<DocumentationTask> tasks = collector.collectFromModifiedElements(psiFile, changedRanges, true);

                    // 如果没有任务，直接返回
                    if (tasks.isEmpty()) {
                        return null;
                    }

                    return new GenerationContext(project, tasks, psiFile.getName());
                })
                .finishOnUiThread(ModalityState.nonModal(), context -> {
                    // 在 UI 线程中执行生成逻辑
                    if (context == null) {
                        return;
                    }

                    // 再次检查项目状态
                    if (context.project.isDisposed()) {
                        return;
                    }

                    // 设置生成标志，防止重复触发
                    if (!isGenerating.compareAndSet(false, true)) {
                        return;
                    }

                    try {
                        // 使用文档生成服务处理任务
                        DocumentationGenerationService service = new DocumentationGenerationService();
                        if (service.checkEmptyTasks(context.project, context.tasks,
                                                    JavadocBundle.message("notification.no.task.selection"))) {
                            isGenerating.set(false);
                            return;
                        }

                        // 生成文档（异步执行，不阻塞 UI）
                        service.generateDocumentation(context.project, context.tasks,
                                                      JavadocBundle.message("task.target.save", context.fileName),
                                                      stats -> {
                                                          // 生成完成后重置标志
                                                          isGenerating.set(false);
                                                          log.debug("保存时自动生成 Javadoc 完成: {}", stats);
                                                      });
                    } catch (Exception e) {
                        // 发生异常时重置标志
                        isGenerating.set(false);
                        log.debug("保存时自动生成 Javadoc 失败", e);
                    }
                })
                .submit(AppExecutorUtil.getAppExecutorService());
        }, 500, TimeUnit.MILLISECONDS);
    }

    /**
     * 检查文件是否为支持的文件类型
     * <p>
     * 只处理 Java 和 Kotlin 文件，其他文件类型直接忽略。
     *
     * @param file 虚拟文件对象
     * @return 如果文件是 Java 或 Kotlin 文件，返回 true；否则返回 false
     */
    private boolean isSupportedFile(@NotNull VirtualFile file) {
        String extension = file.getExtension();
        return PluginContents.JAVA.equalsIgnoreCase(extension) || PluginContents.KOTLIN.equalsIgnoreCase(extension);
    }

    /**
     * 获取文件所属的项目
     * <p>
     * 通过遍历所有打开的项目，查找包含该文件的项目。
     * 如果找不到，返回默认项目（但通常不会使用默认项目）。
     *
     * @param file 虚拟文件对象
     * @return 文件所属的项目，如果找不到则返回 null
     */
    private Project getProjectForFile(@NotNull VirtualFile file) {
        Project[] openProjects = ProjectManager.getInstance().getOpenProjects();
        for (Project project : openProjects) {
            if (!project.isDisposed() && com.intellij.openapi.module.ModuleUtilCore.findModuleForFile(file, project) != null) {
                return project;
            }
        }
        // 如果找不到，尝试使用默认项目（但通常不推荐）
        return null;
    }

    /**
     * 判断保存的文档是否属于当前编辑器中的文件
     *
     * @param project     当前项目
     * @param document    保存的文档
     * @param virtualFile 保存的文件
     * @return 当前编辑器文件则返回 true，否则返回 false
     */
    private boolean isCurrentEditorFile(@NotNull Project project, @NotNull Document document, @NotNull VirtualFile virtualFile) {
        FileEditorManager editorManager = FileEditorManager.getInstance(project);
        Editor editor = editorManager.getSelectedTextEditor();
        if (editor == null) {
            return false;
        }

        if (!document.equals(editor.getDocument())) {
            return false;
        }

        VirtualFile selectedFile = FileDocumentManager.getInstance().getFile(editor.getDocument());
        return virtualFile.equals(selectedFile);
    }

    /**
     * 消费并转换文档中的更改范围
     * <p> 从已移除的范围标记列表中提取有效的文本范围, 并将其转换为 TextRange 对象列表.
     * 如果没有有效的范围标记, 则返回一个空的不可变列表.
     *
     * @param document 文档对象
     * @return 包含有效更改范围的 TextRange 对象列表, 如果没有有效的范围则返回空列表
     */
    private List<TextRange> consumeChangedRanges(@NotNull Document document) {
        List<RangeMarker> markers = changedRangesByDocument.remove(document);
        if (markers == null || markers.isEmpty()) {
            return List.of();
        }

        int textLength = document.getTextLength();
        List<TextRange> ranges = new ArrayList<>();
        for (RangeMarker marker : markers) {
            if (!marker.isValid()) {
                continue;
            }
            int start = marker.getStartOffset();
            int end = marker.getEndOffset();
            if (start == end && textLength > 0) {
                end = Math.min(textLength, start + 1);
            }
            if (start <= end) {
                ranges.add(new TextRange(start, end));
            }
        }

        return ranges;
    }

    /**
     * 生成上下文
     * <p>
     * 用于在后台线程和 UI 线程之间传递生成任务的相关信息。
     */
    private record GenerationContext(Project project, List<DocumentationTask> tasks, String fileName) {
        /**
         * 初始化生成上下文对象
         * <p> 构造函数用于创建一个生成上下文对象, 包含项目, 文档任务列表和文件名
         *
         * @param project  项目对象, 不能为空
         * @param tasks    文档任务列表, 不能为空
         * @param fileName 文件名, 不能为空
         */
        private GenerationContext(@NotNull Project project, @NotNull List<DocumentationTask> tasks, @NotNull String fileName) {
            this.project = project;
            this.tasks = tasks;
            this.fileName = fileName;
        }
    }

    /**
     * 保存文档更改跟踪器类
     * <p> 实现了 DocumentListener 接口, 用于监听文档的变化事件, 并记录文档中发生更改的范围
     * <p> 该类通过维护一个 Map, 将每个文档与其对应的更改范围列表关联起来, 以便后续处理
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.07
     * @since 1.0.0
     */
    private static final class SaveDocumentChangeTracker implements DocumentListener {
        /**
         * 处理文档更改事件
         * <p> 当文档发生更改时, 记录更改的范围
         * <p> 具体步骤包括:
         * <ul>
         * <li> 获取更改事件对应的文档对象 </li>
         * <li> 根据文档对象从缓存中获取或创建范围标记列表 </li>
         * <li> 计算更改事件的起始和结束位置 </li>
         * <li> 在文档中创建范围标记并设置贪婪模式 </li>
         * <li> 将范围标记添加到范围标记列表中 </li>
         * </ul>
         *
         * @param event 文档更改事件对象, 不能为 null
         */
        @Override
        public void documentChanged(@NotNull DocumentEvent event) {
            Document document = event.getDocument();
            List<RangeMarker> ranges = changedRangesByDocument.computeIfAbsent(document,
                                                                               ignored -> new CopyOnWriteArrayList<>());
            int start = event.getOffset();
            int end = event.getOffset() + event.getNewLength();
            RangeMarker marker = document.createRangeMarker(start, end);
            marker.setGreedyToLeft(true);
            marker.setGreedyToRight(true);
            ranges.add(marker);
        }
    }
}
