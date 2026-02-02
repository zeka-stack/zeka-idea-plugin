package dev.dong4j.zeka.stack.idea.plugin.repairer.action;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import dev.dong4j.zeka.stack.idea.plugin.common.util.NotificationUtil;
import dev.dong4j.zeka.stack.idea.plugin.repairer.adapter.CheckstyleXmlAdapter;
import dev.dong4j.zeka.stack.idea.plugin.repairer.adapter.PmdXmlAdapter;
import dev.dong4j.zeka.stack.idea.plugin.repairer.problems.RepairerProblemsViewPanel;
import dev.dong4j.zeka.stack.idea.plugin.repairer.service.ViolationCache;
import dev.dong4j.zeka.stack.idea.plugin.repairer.util.RepairerBundle;
import dev.dong4j.zeka.stack.idea.plugin.repairer.violation.CodeViolation;
import icons.AIRepairerIcons;

/**
 * 导入静态报告操作类
 * <p> 继承自 AnAction, 用于在 IDE 中提供导入静态代码分析报告的功能, 支持 PMD 和 Checkstyle 格式的 XML 报告文件. 用户在项目树或编辑器中选中文件后, 解析内容并加载到当前项目中, 更新代码违规缓存并重启代码分析器,
 * 最后显示导入结果通知.</p>
 * <p> 该类根据当前上下文选中的 XML 报告文件, 自动选择对应的解析器 (PMD 或 Checkstyle), 解析后将违规信息缓存并刷新代码分析状态.</p>
 * <p> 主要用途: 集成静态代码分析工具报告, 便于开发者在 IDE 中集中查看和管理代码违规项.</p>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.20
 * @since 1.0.0
 */
public class ImportStaticReportAction extends AnAction {
    /**
     * 构造函数, 初始化导入静态报告操作
     * <p> 设置动作的标题, 描述和图标
     *
     * @since 1.0
     */
    public ImportStaticReportAction() {
        super(
            RepairerBundle.message("action.import.report.title"),
            RepairerBundle.message("action.import.report.description"),
            AIRepairerIcons.REPAIRER_16
             );
    }

    /**
     * 执行导入报告动作时的回调方法
     * <p> 该方法从上下文获取当前选中的 XML 报告文件.
     * 根据文件名中的关键词自动判断报告类型, 解析违规记录并将其加载到项目的违规缓存中,
     * 随后触发后台代码分析器的重启以更新显示, 并弹出通知告知导入的违规数量.
     *
     * @param e 动作事件对象, 包含上下文信息
     */
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }
        VirtualFile file = getSelectedXmlFile(e);
        if (file == null) {
            return;
        }

        List<CodeViolation> violations = new ArrayList<>();
        File ioFile = new File(file.getPath());

        // 改进报告类型判断
        String fileName = file.getName().toLowerCase();
        if (fileName.contains("pmd")) {
            violations.addAll(new PmdXmlAdapter().parse(ioFile));
        } else if (fileName.contains("checkstyle")) {
            violations.addAll(new CheckstyleXmlAdapter().parse(ioFile));
        } else {
            // 尝试两种解析器，选择解析结果多的一种
            List<CodeViolation> checkstyleViolations = new CheckstyleXmlAdapter().parse(ioFile);
            List<CodeViolation> pmdViolations = new PmdXmlAdapter().parse(ioFile);

            if (checkstyleViolations.size() > pmdViolations.size()) {
                violations.addAll(checkstyleViolations);
            } else {
                violations.addAll(pmdViolations);
            }
        }

        ViolationCache.getInstance(project).setAll(violations);
        DaemonCodeAnalyzer.getInstance(project).restart();
        openProblemsView(project);

        if (violations.isEmpty()) {
            NotificationUtil.showWarning(project, "No violations found in the report.");
        } else {
            NotificationUtil.showInfo(project, RepairerBundle.message("notify.import.count", violations.size()));
        }
    }

    /**
     * 更新动作的可见性和可用性状态
     * <p> 根据当前上下文中的项目是否有效以及是否选中了有效的 XML 报告文件, 动态设置动作在 IDE 界面上的可见性和可用性.</p>
     * <p> 如果项目无效或未选中有效文件, 则动作不可见或不可用; 否则动作可见且可用.</p>
     *
     * @param e 动作事件对象, 包含当前上下文信息
     * @since 1.0
     */
    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        boolean visible = project != null;
        boolean enabled = visible && getSelectedXmlFile(e) != null;
        e.getPresentation().setVisible(visible);
        e.getPresentation().setEnabled(enabled);
    }

    /**
     * 获取当前动作事件中选中的单个 XML 文件
     * <p> 从动作事件中提取选中的文件数组, 若数组长度不为 1 则返回 null; 否则检查第一个文件是否为 XML 文件, 若是则返回该文件, 否则返回 null. 若文件数组为空, 则从单文件数据键中获取文件并判断是否为 XML 文件.</p>
     *
     * @param e 动作事件对象, 包含当前上下文选中文件的信息
     * @return 选中的 XML 文件, 若不满足条件则返回 null
     */
    private static VirtualFile getSelectedXmlFile(@NotNull AnActionEvent e) {
        VirtualFile[] files = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY);
        if (files != null) {
            if (files.length != 1) {
                return null;
            }
            return isXmlFile(files[0]) ? files[0] : null;
        }
        VirtualFile file = e.getData(CommonDataKeys.VIRTUAL_FILE);
        return isXmlFile(file) ? file : null;
    }

    /**
     * 判断指定文件是否为 XML 文件
     * <p> 检查文件是否为 null 或目录, 若为则返回 false; 否则提取文件扩展名并判断是否为 "xml"(不区分大小写)</p>
     *
     * @param file 待判断的虚拟文件对象
     * @return 如果文件存在且扩展名为 "xml"(不区分大小写), 则返回 true, 否则返回 false
     */
    private static boolean isXmlFile(VirtualFile file) {
        if (file == null || file.isDirectory()) {
            return false;
        }
        String extension = file.getExtension();
        return extension != null && "xml".equals(extension.toLowerCase(Locale.ROOT));
    }

    /**
     * 在指定项目中激活问题视图工具窗口并切换到修复器标签页
     * <p> 通过应用的异步事件调度器, 在 UI 线程中执行操作, 获取当前项目的问题视图工具窗口并激活它, 若名为 "ProblemsView" 的工具窗口不存在, 则尝试获取 "Problems" 工具窗口. 激活后, 会进一步调用方法切换到修复器专用标签页
     * .</p>
     *
     * @param project 目标项目对象, 用于获取工具窗口管理器和上下文
     */
    private static void openProblemsView(@NotNull Project project) {
        ApplicationManager.getApplication().invokeLater(() -> {
            ToolWindowManager manager = ToolWindowManager.getInstance(project);
            ToolWindow toolWindow = manager.getToolWindow("ProblemsView");
            if (toolWindow == null) {
                toolWindow = manager.getToolWindow("Problems");
            }
            if (toolWindow != null) {
                toolWindow.activate(null);
            }
            selectRepairerTab(project);
        });
    }

    /**
     * 选择并激活修复器问题视图的标签页
     * <p>通过反射获取 IntelliJ IDEA 的 ProblemsView 工具窗口实例, 并尝试调用其方法 (如 selectTab 或 openTab) 来定位并激活修复器专用的问题标签页. 若方法调用失败则忽略异常, 不中断执行.</p>
     * <p>该方法旨在确保在导入静态报告后, 用户界面能自动跳转到修复器相关的问题视图, 提升用户体验.</p>
     *
     * @param project 当前项目对象, 用于获取 ProblemsView 实例
     * @since 1.0.0
     */
    private static void selectRepairerTab(@NotNull Project project) {
        try {
            Class<?> viewClass = Class.forName("com.intellij.analysis.problemsView.toolWindow.ProblemsView");
            Method getInstance = viewClass.getMethod("getInstance", Project.class);
            Object problemsView = getInstance.invoke(null, project);
            if (problemsView == null) {
                return;
            }
            if (trySelectTab(viewClass, problemsView, "selectTab")) {
                return;
            }
            trySelectTab(viewClass, problemsView, "openTab");
        } catch (Throwable ignored) {
        }
    }

    /**
     * 尝试通过反射调用指定方法选择问题视图中的标签页
     * <p> 根据传入的类, 实例对象和方法名, 动态调用对应方法并传入标签 ID, 若调用成功则返回 true, 否则捕获异常并返回 false</p>
     *
     * @param viewClass    问题视图类对象
     * @param problemsView 问题视图实例对象
     * @param methodName   要调用的方法名, 例如 "selectTab" 或 "openTab"
     * @return 如果方法调用成功则返回 true, 否则返回 false
     */
    private static boolean trySelectTab(Class<?> viewClass, Object problemsView, String methodName) {
        try {
            Method method = viewClass.getMethod(methodName, String.class);
            method.invoke(problemsView, RepairerProblemsViewPanel.TAB_ID);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
