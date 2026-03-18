package dev.dong4j.zeka.stack.idea.plugin.repairer.fix;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Rule-based helper for deciding formatter-first fixes and applying formatting.
 */
public final class RuleFixSupport {

    private static final Set<String> FORMATTER_FIRST_RULES = Set.of(
        "WhitespaceAround",
        "WhitespaceAfter",
        "NoWhitespaceAfter",
        "NoWhitespaceBefore",
        "ParenPad",
        "MethodParamPad",
        "TypecastParenPad",
        "OperatorWrap",
        "LeftCurly",
        "RightCurly",
        "Indentation"
                                                                   );

    /**
     * 私有构造函数, 防止实例化.
     * <p> 此类为工具类, 不提供实例化方式.
     */
    private RuleFixSupport() {
    }

    /**
     * Whether this rule should be handled by formatter before using AI.
     *
     * @param ruleId raw rule id
     * @return true if formatter-first
     */
    public static boolean shouldFormatFirst(@Nullable String ruleId) {
        String normalized = normalizeRuleId(ruleId);
        return !normalized.isEmpty() && FORMATTER_FIRST_RULES.contains(normalized);
    }

    /**
     * Normalize rule id from source class / check id to canonical rule name.
     *
     * @param ruleId raw rule id
     * @return normalized rule name without package and trailing "Check"
     */
    @NotNull
    public static String normalizeRuleId(@Nullable String ruleId) {
        if (ruleId == null || ruleId.isBlank()) {
            return "";
        }
        String normalized = ruleId.trim();
        int lastDot = normalized.lastIndexOf('.');
        if (lastDot >= 0 && lastDot + 1 < normalized.length()) {
            normalized = normalized.substring(lastDot + 1);
        }
        if (normalized.endsWith("Check") && normalized.length() > "Check".length()) {
            normalized = normalized.substring(0, normalized.length() - "Check".length());
        }
        return normalized;
    }

    /**
     * Format a single file and optimize imports.
     *
     * @param project project
     * @param file    file
     */
    public static void formatFile(@NotNull Project project, @NotNull PsiFile file) {
        formatFiles(project, Set.of(file));
    }

    /**
     * Format files and optimize imports once before AI stage.
     *
     * @param project project
     * @param files   files
     */
    public static void formatFiles(@NotNull Project project, @NotNull Collection<PsiFile> files) {
        if (files.isEmpty()) {
            return;
        }
        Set<PsiFile> uniqueFiles = new LinkedHashSet<>(files);
        WriteCommandAction.runWriteCommandAction(project, () -> {
            CodeStyleManager codeStyleManager = CodeStyleManager.getInstance(project);
            JavaCodeStyleManager javaCodeStyleManager = JavaCodeStyleManager.getInstance(project);
            for (PsiFile file : uniqueFiles) {
                if (file == null || !file.isValid() || !file.isWritable()) {
                    continue;
                }
                codeStyleManager.reformat(file);
                javaCodeStyleManager.optimizeImports(file);
            }
            PsiDocumentManager.getInstance(project).commitAllDocuments();
        });
    }
}
