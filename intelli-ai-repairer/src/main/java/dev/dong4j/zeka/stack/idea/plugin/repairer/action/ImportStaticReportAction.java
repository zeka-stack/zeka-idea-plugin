package dev.dong4j.zeka.stack.idea.plugin.repairer.action;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import dev.dong4j.zeka.stack.idea.plugin.common.util.NotificationUtil;
import dev.dong4j.zeka.stack.idea.plugin.repairer.adapter.CheckstyleXmlAdapter;
import dev.dong4j.zeka.stack.idea.plugin.repairer.adapter.PmdXmlAdapter;
import dev.dong4j.zeka.stack.idea.plugin.repairer.service.ViolationCache;
import dev.dong4j.zeka.stack.idea.plugin.repairer.util.RepairerBundle;
import dev.dong4j.zeka.stack.idea.plugin.repairer.violation.CodeViolation;
import icons.AIRepairerIcons;

/**
 * 手动导入 Checkstyle/PMD 报告.
 */
public class ImportStaticReportAction extends AnAction {
    public ImportStaticReportAction() {
        super(
            RepairerBundle.message("action.import.report.title"),
            RepairerBundle.message("action.import.report.description"),
            AIRepairerIcons.REPAIRER_16
             );
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }

        FileChooserDescriptor descriptor = new FileChooserDescriptor(true, false, false, false, false, false)
            .withTitle(RepairerBundle.message("dialog.import.title"))
            .withDescription(RepairerBundle.message("dialog.import.description"));
        VirtualFile file = FileChooser.chooseFile(descriptor, project, null);
        if (file == null) {
            return;
        }

        List<CodeViolation> violations = new ArrayList<>();
        File ioFile = new File(file.getPath());
        if (file.getName().contains("pmd")) {
            violations.addAll(new PmdXmlAdapter().parse(ioFile));
        } else {
            violations.addAll(new CheckstyleXmlAdapter().parse(ioFile));
        }
        ViolationCache.getInstance(project).setAll(violations);
        DaemonCodeAnalyzer.getInstance(project).restart();
        NotificationUtil.showInfo(project, RepairerBundle.message("notify.import.count", violations.size()));
    }
}
