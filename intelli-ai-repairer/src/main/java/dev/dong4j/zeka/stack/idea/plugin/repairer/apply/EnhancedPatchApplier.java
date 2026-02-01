package dev.dong4j.zeka.stack.idea.plugin.repairer.apply;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiFile;
import dev.dong4j.zeka.stack.idea.plugin.common.util.NotificationUtil;
import dev.dong4j.zeka.stack.idea.plugin.repairer.util.RepairerBundle;

/**
 * 增强的补丁应用器工具类
 * <p> 提供多种策略的代码修复应用功能，包括基于 AST 的修复、基于精确范围的修复和基于行的修复。
 * <p> 在应用补丁时，会尝试多种修复策略，并对修复结果进行预处理和验证，确保修复的准确性和可靠性。
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.30
 * @since 1.0.0
 */
public final class EnhancedPatchApplier {

    /**
     * 私有构造函数，禁止外部实例化该类
     * <p> 该类为工具类，仅提供静态方法用于应用补丁，不允许直接创建实例
     */
    private EnhancedPatchApplier() {
    }

    /**
     * 应用增强的代码修复
     * <p> 尝试多种修复策略，确保修复的准确性和可靠性。
     *
     * @param project     项目实例，用于获取文档管理器和执行写入操作
     * @param file        需要修改的 PsiFile 实例
     * @param element     需要修改的 PsiElement 实例
     * @param range       文档中的 TextRange 范围，表示需要替换的区域
     * @param original    原始内容，用于校验当前内容是否与之匹配
     * @param replacement 替换后的内容
     */
    public static void apply(Project project, PsiFile file, PsiElement element, TextRange range, String original, String replacement) {
        // 1. 尝试基于 AST 的修复
        if (tryASTBasedFix(project, file, element, original, replacement)) {
            return;
        }

        // 2. 尝试基于精确范围的修复
        if (tryExactRangeFix(project, file, range, original, replacement)) {
            return;
        }

        // 3. 尝试基于行的修复
        tryLineBasedFix(project, file, range, original, replacement);
    }

    /**
     * 尝试基于 AST 的修复
     * <p> 使用 PSI 元素替换的方式进行修复，确保修复结果与原始代码结构兼容。
     *
     * @param project     项目实例
     * @param file        PsiFile 实例
     * @param element     PsiElement 实例
     * @param original    原始代码
     * @param replacement 修复后的代码
     * @return 是否修复成功
     */
    private static boolean tryASTBasedFix(Project project, PsiFile file, PsiElement element, String original, String replacement) {
        try {
            // 1. 预处理修复结果
            String processedReplacement = preprocessReplacement(replacement, original);

            // 2. 验证修复结果
            if (!validateReplacement(processedReplacement, original)) {
                return false;
            }

            // 3. 解析修复代码为 PSI 元素
            PsiElementFactory factory = JavaPsiFacade.getElementFactory(project);
            PsiElement fixedElement = parseElement(factory, processedReplacement, element);

            // 4. 验证 PSI 元素
            if (fixedElement == null) {
                return false;
            }

            // 5. 应用修复
            WriteCommandAction.runWriteCommandAction(project, () -> {
                element.replace(fixedElement);
            });

            return true;
        } catch (Exception e) {
            // 记录异常但不抛出，继续尝试其他策略
            return false;
        }
    }

    /**
     * 尝试基于精确范围的修复
     * <p> 使用文档替换的方式进行修复，确保修复范围的精确性。
     *
     * @param project     项目实例
     * @param file        PsiFile 实例
     * @param range       TextRange 范围
     * @param original    原始代码
     * @param replacement 修复后的代码
     * @return 是否修复成功
     */
    private static boolean tryExactRangeFix(Project project, PsiFile file, TextRange range, String original, String replacement) {
        try {
            Document document = PsiDocumentManager.getInstance(project).getDocument(file);
            if (document == null) {
                return false;
            }

            String current = document.getText(range);
            if (!current.equals(original)) {
                return false;
            }

            // 1. 预处理修复结果
            String processedReplacement = preprocessReplacement(replacement, original);

            // 2. 验证修复结果
            if (!validateReplacement(processedReplacement, original)) {
                return false;
            }

            // 3. 应用修复
            WriteCommandAction.runWriteCommandAction(project, () -> {
                document.replaceString(range.getStartOffset(), range.getEndOffset(), processedReplacement);
                PsiDocumentManager.getInstance(project).commitDocument(document);
            });

            return true;
        } catch (Exception e) {
            // 记录异常但不抛出，继续尝试其他策略
            return false;
        }
    }

    /**
     * 尝试基于行的修复
     * <p> 基于行的修复，确保修复的最小影响范围。
     *
     * @param project     项目实例
     * @param file        PsiFile 实例
     * @param range       TextRange 范围
     * @param original    原始代码
     * @param replacement 修复后的代码
     */
    private static void tryLineBasedFix(Project project, PsiFile file, TextRange range, String original, String replacement) {
        try {
            Document document = PsiDocumentManager.getInstance(project).getDocument(file);
            if (document == null) {
                return;
            }

            // 1. 计算行范围
            int startLine = document.getLineNumber(range.getStartOffset());
            int endLine = document.getLineNumber(range.getEndOffset());

            // 2. 获取行文本
            String startLineText = document.getText(new TextRange(document.getLineStartOffset(startLine), document.getLineEndOffset(startLine)));
            String endLineText = document.getText(new TextRange(document.getLineStartOffset(endLine), document.getLineEndOffset(endLine)));

            // 3. 预处理修复结果
            String processedReplacement = preprocessReplacement(replacement, original);

            // 4. 保持缩进
            String indentation = getIndentation(startLineText);
            if (!indentation.isEmpty()) {
                processedReplacement = indentWith(processedReplacement, indentation);
            }

            // 5. 应用修复
            final String finalReplacement = processedReplacement;
            WriteCommandAction.runWriteCommandAction(project, () -> {
                document.replaceString(
                    document.getLineStartOffset(startLine),
                    document.getLineEndOffset(endLine),
                    finalReplacement
                );
                PsiDocumentManager.getInstance(project).commitDocument(document);
            });
        } catch (Exception e) {
            // 记录异常并显示警告
            NotificationUtil.showWarning(project, RepairerBundle.message("error.fix.failed"));
        }
    }

    /**
     * 预处理修复结果
     * <p> 去除多余的空白行和缩进，保持原始缩进。
     *
     * @param replacement 修复后的代码
     * @param original    原始代码
     * @return 预处理后的修复结果
     */
    private static String preprocessReplacement(String replacement, String original) {
        // 1. 去除多余的空白行和缩进
        replacement = replacement.trim();

        // 2. 保持原始缩进
        String indentation = getIndentation(original);
        if (!indentation.isEmpty()) {
            replacement = indentWith(replacement, indentation);
        }

        // 3. 确保修复结果符合代码风格
        // 这里可以添加更多的预处理逻辑

        return replacement;
    }

    /**
     * 验证修复结果
     * <p> 检查修复结果是否为空，是否与原始代码相同。
     *
     * @param replacement 修复后的代码
     * @param original    原始代码
     * @return 是否验证通过
     */
    private static boolean validateReplacement(String replacement, String original) {
        // 1. 检查修复结果是否为空
        if (replacement == null || replacement.isEmpty()) {
            return false;
        }

        // 2. 检查修复结果是否与原始代码相同
        if (replacement.equals(original)) {
            return false;
        }

        // 3. 检查修复结果是否包含有效的代码
        // 这里可以添加更多的验证逻辑

        return true;
    }

    /**
     * 解析代码片段为 PSI 元素
     * <p> 根据代码类型选择合适的解析方法。
     *
     * @param factory     PsiElementFactory 实例
     * @param code        代码片段
     * @param contextElement 上下文元素
     * @return 解析后的 PSI 元素
     */
    private static PsiElement parseElement(PsiElementFactory factory, String code, PsiElement contextElement) {
        try {
            // 尝试解析为语句
            return factory.createStatementFromText(code + ";", contextElement);
        } catch (Exception e1) {
            try {
                // 尝试解析为表达式
                return factory.createExpressionFromText(code, contextElement);
            } catch (Exception e2) {
                try {
                    // 尝试解析为代码块
                    return factory.createCodeBlockFromText("{" + code + "}", contextElement);
                } catch (Exception e3) {
                    // 解析失败
                    return null;
                }
            }
        }
    }

    /**
     * 获取原始代码的缩进
     * <p> 提取代码开头的空白字符作为缩进。
     *
     * @param code 代码片段
     * @return 缩进字符串
     */
    private static String getIndentation(String code) {
        int firstNonWhitespace = code.indexOf(code.trim().charAt(0));
        return code.substring(0, firstNonWhitespace);
    }

    /**
     * 为代码添加缩进
     * <p> 为代码的每一行添加指定的缩进。
     *
     * @param code        代码片段
     * @param indentation 缩进字符串
     * @return 缩进后的代码
     */
    private static String indentWith(String code, String indentation) {
        String[] lines = code.split("\\n");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) {
                sb.append("\n");
            }
            sb.append(indentation).append(lines[i]);
        }
        return sb.toString();
    }
}
