package dev.dong4j.zeka.stack.idea.plugin.repairer.action;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import dev.dong4j.zeka.stack.idea.plugin.common.util.NotificationUtil;
import dev.dong4j.zeka.stack.idea.plugin.repairer.adapter.CheckstyleXmlAdapter;
import dev.dong4j.zeka.stack.idea.plugin.repairer.adapter.PmdXmlAdapter;
import dev.dong4j.zeka.stack.idea.plugin.repairer.service.ViolationCache;
import dev.dong4j.zeka.stack.idea.plugin.repairer.util.RepairerBundle;
import dev.dong4j.zeka.stack.idea.plugin.repairer.violation.CodeViolation;
import icons.AIRepairerIcons;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 导入静态报告操作类
 * <p> 继承自 AnAction, 用于在 IDE 中提供导入静态代码分析报告的功能, 支持 PMD 和 Checkstyle 格式的 XML 报告文件. 用户选择文件后, 解析内容并加载到当前项目中, 更新代码违规缓存并重启代码分析器, 最后显示导入结果通知.</p>
 * <p> 该类通过文件选择器引导用户选择报告文件, 根据文件名后缀自动选择对应的解析器 (PMD 或 Checkstyle), 解析后将违规信息缓存并刷新代码分析状态.</p>
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
     * <p> 该方法引导用户选择一个包含 Checkstyle 或 PMD 分析结果的文件.
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

        FileChooserDescriptor descriptor = new FileChooserDescriptor(true, false, false, false, false, false)
            .withTitle(RepairerBundle.message("dialog.import.title"))
            .withDescription(RepairerBundle.message("dialog.import.description"))
            .withFileFilter(file -> file.getName().toLowerCase().endsWith(".xml"));
        VirtualFile file = FileChooser.chooseFile(descriptor, project, null);
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

        if (violations.isEmpty()) {
            NotificationUtil.showWarning(project, "No violations found in the report.");
        } else {
            NotificationUtil.showInfo(project, RepairerBundle.message("notify.import.count", violations.size()));
        }
    }
}
