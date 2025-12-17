package dev.dong4j.zeka.stack.idea.plugin.service;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.PsiDocCommentOwner;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.util.PsiTreeUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.psi.KtClassOrObject;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.KtNamedFunction;
import org.jetbrains.kotlin.psi.KtProperty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import dev.dong4j.zeka.stack.idea.plugin.PluginContents;
import dev.dong4j.zeka.stack.idea.plugin.settings.SettingsState;
import lombok.extern.slf4j.Slf4j;

/**
 * Javadoc 删除服务类
 * <p>
 * 提供统一的 Javadoc/KDoc 注释删除功能，支持单个元素和批量删除。
 * 支持 Java 和 Kotlin 语言的文档注释删除。
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @since 2.6.0
 */
@Slf4j
public class JavadocDeletionService {

    /**
     * 删除指定元素的 Javadoc/KDoc 注释
     * <p>
     * 检查配置是否允许删除，如果允许则删除元素的文档注释。
     * 使用 WriteCommandAction 确保线程安全。
     *
     * @param project 项目对象
     * @param element 要删除注释的元素
     * @return 如果成功删除返回 true，否则返回 false
     */
    public boolean deleteJavadoc(@NotNull Project project, @NotNull PsiElement element) {
        // 检查配置是否允许删除
        SettingsState settings = SettingsState.getInstance();
        if (!settings.allowDeleteJavadoc) {
            log.info("删除 Javadoc 功能未启用");
            return false;
        }

        PsiElement comment = getDocComment(element);
        if (comment == null) {
            log.debug("元素没有文档注释，无需删除");
            return false;
        }

        try {
            WriteCommandAction.runWriteCommandAction(project, () -> {
                comment.delete();
            });
            log.info("成功删除元素的文档注释");
            return true;
        } catch (Exception e) {
            log.warn("删除文档注释失败", e);
            return false;
        }
    }

    /**
     * 批量删除文件或目录中所有元素的 Javadoc/KDoc 注释
     * <p>
     * 遍历文件中的所有支持文档注释的元素，删除它们的注释。
     *
     * @param project 项目对象
     * @param psiFile 要处理的文件
     * @return 删除的注释数量
     */
    public int deleteJavadocFromFile(@NotNull Project project, @NotNull PsiFile psiFile) {
        // 检查配置是否允许删除
        SettingsState settings = SettingsState.getInstance();
        if (!settings.allowDeleteJavadoc) {
            log.info("删除 Javadoc 功能未启用");
            return 0;
        }

        // 检查文件类型
        if (!(psiFile instanceof PsiJavaFile) && !(psiFile instanceof KtFile)) {
            log.debug("不支持的文件类型: {}", psiFile.getClass().getName());
            return 0;
        }

        // 检查是否支持 Kotlin
        if (psiFile instanceof KtFile) {
            if (!settings.isLanguageSupported(PluginContents.KOTLIN)) {
                log.debug("Kotlin 语言支持未启用");
                return 0;
            }
        }

        // 收集所有需要删除注释的元素
        List<PsiElement> elementsWithComments = ReadAction.compute(() -> {
            List<PsiElement> elements = new ArrayList<>();
            collectElementsWithComments(psiFile, elements);
            return elements;
        });

        if (elementsWithComments.isEmpty()) {
            log.debug("文件中没有需要删除的文档注释");
            return 0;
        }

        // 批量删除
        int deletedCount = WriteCommandAction.runWriteCommandAction(project, (Computable<Integer>) () -> {
            int count = 0;
            for (PsiElement element : elementsWithComments) {
                PsiElement comment = getDocComment(element);
                if (comment != null) {
                    try {
                        comment.delete();
                        count++;
                    } catch (Exception e) {
                        log.warn("删除元素文档注释失败", e);
                    }
                }
            }
            return count;
        });

        log.info("从文件 {} 中删除了 {} 个文档注释", psiFile.getName(), deletedCount);
        return deletedCount;
    }

    /**
     * 收集文件中所有有文档注释的元素
     *
     * @param file    文件对象
     * @param results 结果列表
     */
    private void collectElementsWithComments(@NotNull PsiFile file, @NotNull List<PsiElement> results) {
        // Java 文件
        if (file instanceof PsiJavaFile) {
            Collection<PsiDocCommentOwner> owners = PsiTreeUtil.findChildrenOfType(file, PsiDocCommentOwner.class);
            for (PsiDocCommentOwner owner : owners) {
                if (owner.getDocComment() != null) {
                    results.add(owner);
                }
            }
        }
        // Kotlin 文件
        else if (file instanceof KtFile) {
            collectKotlinElementsWithComments((KtFile) file, results);
        }
    }

    /**
     * 收集 Kotlin 文件中有 KDoc 注释的元素
     *
     * @param file    Kotlin 文件
     * @param results 结果列表
     */
    private void collectKotlinElementsWithComments(@NotNull KtFile file, @NotNull List<PsiElement> results) {
        file.acceptChildren(new org.jetbrains.kotlin.psi.KtTreeVisitorVoid() {
            @Override
            public void visitClassOrObject(@NotNull KtClassOrObject klass) {
                if (klass.getDocComment() != null) {
                    results.add(klass);
                }
                super.visitClassOrObject(klass);
            }

            @Override
            public void visitNamedFunction(@NotNull KtNamedFunction function) {
                if (function.getDocComment() != null) {
                    results.add(function);
                }
                super.visitNamedFunction(function);
            }

            @Override
            public void visitProperty(@NotNull KtProperty property) {
                if (property.getDocComment() != null) {
                    results.add(property);
                }
                super.visitProperty(property);
            }
        });
    }

    /**
     * 获取元素的文档注释
     *
     * @param element 元素
     * @return 文档注释，如果没有则返回 null
     */
    @Nullable
    public static PsiElement getDocComment(@NotNull PsiElement element) {
        // Java 元素
        if (element instanceof PsiDocCommentOwner) {
            return ((PsiDocCommentOwner) element).getDocComment();
        }
        // Kotlin 元素
        if (element instanceof KtClassOrObject) {
            return ((KtClassOrObject) element).getDocComment();
        }
        if (element instanceof KtNamedFunction) {
            return ((KtNamedFunction) element).getDocComment();
        }
        if (element instanceof KtProperty) {
            return ((KtProperty) element).getDocComment();
        }
        return null;
    }

    /**
     * 检查元素是否有文档注释
     *
     * @param element 元素
     * @return 如果有文档注释返回 true
     */
    public static boolean hasDocComment(@NotNull PsiElement element) {
        return getDocComment(element) != null;
    }
}

