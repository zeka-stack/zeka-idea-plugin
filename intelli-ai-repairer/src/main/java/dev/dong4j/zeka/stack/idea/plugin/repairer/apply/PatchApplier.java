package dev.dong4j.zeka.stack.idea.plugin.repairer.apply;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;

import dev.dong4j.zeka.stack.idea.plugin.common.util.NotificationUtil;
import dev.dong4j.zeka.stack.idea.plugin.repairer.util.RepairerBundle;

/**
 * 补丁应用器工具类
 * <p> 提供对 PSI 文件的文本补丁应用功能, 用于在代码修复过程中安全地替换指定的文本内容.
 * <p> 该类采用单例模式, 通过私有构造函数确保工具类的使用方式, 主要通过静态方法提供补丁应用服务.
 * <p> 在应用补丁时, 会首先验证当前文本是否与预期原始文本匹配, 如果验证失败则拒绝执行替换操作,
 * 确保代码修改的安全性和准确性.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.20
 * @since x.x.x
 */
public final class PatchApplier {
    /**
     * 私有构造函数, 禁止外部实例化该类
     * <p> 该类为工具类, 仅提供静态方法用于应用补丁, 不允许直接创建实例
     */
    private PatchApplier() {
    }

    /**
     * 应用代码修复到指定的文件范围
     * <p> 该方法用于将指定的替换内容应用到项目中的特定文件的指定范围. 首先检查当前内容是否与原始内容一致, 若不一致则提示警告并返回. 若一致, 则执行替换操作, 并提交文档更改.
     *
     * @param project     项目实例, 用于获取文档管理器和执行写入操作
     * @param file        需要修改的 PsiFile 实例
     * @param range       文档中的 TextRange 范围, 表示需要替换的区域
     * @param original    原始内容, 用于校验当前内容是否与之匹配
     * @param replacement 替换后的内容
     */
    public static void apply(Project project, PsiFile file, TextRange range, String original, String replacement) {
        Document document = PsiDocumentManager.getInstance(project).getDocument(file);
        if (document == null) {
            return;
        }
        String current = document.getText(range);
        if (!current.equals(original)) {
            NotificationUtil.showWarning(project, RepairerBundle.message("error.code.changed"));
            return;
        }
        WriteCommandAction.runWriteCommandAction(project, () -> {
            document.replaceString(range.getStartOffset(), range.getEndOffset(), replacement);
            PsiDocumentManager.getInstance(project).commitDocument(document);
        });
    }
}
