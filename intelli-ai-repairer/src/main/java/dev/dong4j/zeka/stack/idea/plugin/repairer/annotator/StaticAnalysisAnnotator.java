package dev.dong4j.zeka.stack.idea.plugin.repairer.annotator;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.ExternalAnnotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiFile;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import dev.dong4j.zeka.stack.idea.plugin.repairer.ai.AIViolationIntention;
import dev.dong4j.zeka.stack.idea.plugin.repairer.service.ViolationCache;
import dev.dong4j.zeka.stack.idea.plugin.repairer.violation.CodeViolation;

/**
 * 静态分析注解器
 * <p> 用于在代码中进行静态分析, 并将发现的代码违规问题以注解的形式标注出来.
 * 该类继承自 ExternalAnnotator, 实现了对项目中所有代码违规项的收集和应用功能.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.20
 * @since 1.0.0
 */
public class StaticAnalysisAnnotator extends ExternalAnnotator<Project, List<CodeViolation>> {
    /**
     * 收集与指定 PSI 文件相关的项目信息
     * <p> 此方法返回与给定 PSI 文件相关的项目对象. 如果文件为空, 则返回 null.
     *
     * @param file 要收集信息的 PSI 文件
     * @return 与文件相关的项目对象, 如果文件为空则返回 null
     */
    @Override
    public @Nullable Project collectInformation(@NotNull PsiFile file) {
        return file.getProject();
    }

    /**
     * 执行注解逻辑, 从缓存中检索当前项目的所有代码违规记录
     * <p> 该方法基于传入的项目获取违规缓存实例, 并返回缓存中所有的代码违规数据
     *
     * @param project 当前项目对象, 用于获取 ViolationCache 实例
     * @return 包含所有代码违规的列表, 如果项目对象为 null 则返回 null
     */
    @Override
    public @Nullable List<CodeViolation> doAnnotate(Project project) {
        if (project == null) {
            return null;
        }
        return ViolationCache.getInstance(project).getAll();
    }

    /**
     * 将静态分析结果应用到指定的文件中, 用于编辑器高亮和提示
     * <p> 遍历所有代码违规项, 并对匹配当前文件路径的违规项进行标注, 显示警告信息并附加修复意图
     *
     * @param file       要应用标注的源码文件
     * @param violations 从外部收集的代码违规列表
     * @param holder     用于创建和添加注解的容器对象
     */
    @Override
    public void apply(@NotNull PsiFile file,
                      List<CodeViolation> violations,
                      @NotNull AnnotationHolder holder) {
        if (violations == null || violations.isEmpty()) {
            return;
        }
        String filePath = file.getVirtualFile() != null ? file.getVirtualFile().getPath() : null;
        if (filePath == null) {
            return;
        }
        for (CodeViolation v : violations) {
            if (!filePath.equals(v.filePath)) {
                continue;
            }
            TextRange range = computeRange(file, v);
            if (range == null) {
                continue;
            }
            holder.newAnnotation(HighlightSeverity.WARNING, v.message)
                .range(range)
                .withFix(new AIViolationIntention(v))
                .create();
        }
    }

    /**
     * 根据代码违规信息计算文本范围
     * <p> 根据 CodeViolation 中的行号信息, 计算该违规在源文件中对应的文本范围
     *
     * @param file PSI 文件对象, 用于获取文档和计算偏移量
     * @param v    代码违规对象, 包含违规的起始和结束行号
     * @return 表示违规文本位置的 TextRange 对象, 如果行号无效则返回 null
     */
    private TextRange computeRange(PsiFile file, CodeViolation v) {
        if (v.startLine <= 0 || v.startLine > file.getViewProvider().getDocument().getLineCount()) {
            return null;
        }
        int startLine = v.startLine - 1;
        int startOffset = file.getViewProvider().getDocument().getLineStartOffset(startLine);
        int endLine = v.endLine > 0 ? v.endLine - 1 : startLine;
        int endOffset = file.getViewProvider().getDocument().getLineEndOffset(endLine);
        return new TextRange(startOffset, endOffset);
    }
}
