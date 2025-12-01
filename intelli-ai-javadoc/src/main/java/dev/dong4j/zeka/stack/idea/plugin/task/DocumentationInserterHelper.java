package dev.dong4j.zeka.stack.idea.plugin.task;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDocCommentOwner;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.codeStyle.CodeStyleManager;

import org.jetbrains.annotations.NotNull;

import dev.dong4j.zeka.stack.idea.plugin.common.util.AIConsoleLoggerUtil;
import dev.dong4j.zeka.stack.idea.plugin.settings.SettingsState;
import dev.dong4j.zeka.stack.idea.plugin.util.JavaDocFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 文档插入辅助类
 * <p>
 * 负责将生成的 JavaDoc 文档插入到源代码中，包括删除旧注释、确定插入位置、
 * 格式化文档内容等操作。
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email mailto:zeka.stack@gmail.com
 * @date 2025.12.01
 * @since 1.0.0
 */
@Slf4j
@RequiredArgsConstructor
public class DocumentationInserterHelper {
    /** 项目对象 */
    @NotNull
    private final Project project;

    /** 设置配置 */
    @NotNull
    private final SettingsState settings;

    /**
     * 插入文档到代码中
     *
     * <p>将生成的文档注释插入到源代码的适当位置。
     * 如果元素已有注释，会先删除旧注释，再插入新注释。
     * 整个操作在 IntelliJ 的命令和写入操作上下文中执行。
     *
     * <p>插入流程：
     * <ol>
     *   <li>获取元素对应的文档对象</li>
     *   <li>删除已有的旧注释</li>
     *   <li>确定插入位置</li>
     *   <li>格式化并插入新注释</li>
     *   <li>应用代码格式化</li>
     * </ol>
     *
     * <p>线程模型：
     * <ul>
     *   <li>使用 invokeLater 调度到事件调度线程</li>
     *   <li>在命令上下文中执行</li>
     *   <li>在写入操作中执行</li>
     * </ul>
     *
     * @param task           文档生成任务
     * @param documentation  生成的文档内容
     * @param verboseLogging 是否启用详细日志
     */
    @SuppressWarnings("D")
    public void insertDocumentation(@NotNull DocumentationTask task,
                                    @NotNull String documentation,
                                    boolean verboseLogging) {
        ApplicationManager.getApplication().invokeLater(() -> {
            PsiElement element = task.getElement();
            Document document = FileDocumentManager.getInstance()
                .getDocument(element.getContainingFile().getVirtualFile());

            if (document == null) {
                return;
            }

            PsiDocumentManager.getInstance(project)
                .doPostponedOperationsAndUnblockDocument(document);

            CommandProcessor.getInstance().executeCommand(
                project,
                () -> ApplicationManager.getApplication().runWriteAction(() -> {
                    try {
                        // 1. 先删除旧注释（如果存在）
                        deleteOldDocComment(element, document, verboseLogging);

                        // 2. 提交删除操作
                        PsiDocumentManager.getInstance(project).commitDocument(document);

                        // 3. 获取插入位置（删除后需要重新获取）
                        int startPosition = getInsertPosition(element);
                        int lineNumber = document.getLineNumber(startPosition);
                        int lineStartPosition = document.getLineStartOffset(lineNumber);

                        // 4. 确保文档以 /** 开头
                        String javadoc = documentation.trim();
                        if (!javadoc.startsWith("/**")) {
                            javadoc = "/**\n" + javadoc;
                        }
                        if (!javadoc.endsWith("*/")) {
                            javadoc = javadoc + "\n */";
                        }

                        // 5. 格式化 JavaDoc 内容（根据配置进行格式化）
                        javadoc = formatJavaDocContent(javadoc);

                        // 6. 插入新 JavaDoc
                        document.insertString(lineStartPosition, javadoc + "\n");
                        PsiDocumentManager.getInstance(project).commitDocument(document);

                        // 7. 格式化插入的 JavaDoc
                        PsiFile psiFile = element.getContainingFile();
                        if (psiFile != null) {
                            int endPosition = lineStartPosition + javadoc.length() + 1;
                            CodeStyleManager.getInstance(project).reformatText(psiFile, lineStartPosition, endPosition);
                        }

                        // Console 日志：输出最终插入的 JavaDoc（仅详细日志模式）
                        AIConsoleLoggerUtil.printWithTimestamp(project, "=== 最终插入的 JavaDoc ===");
                        AIConsoleLoggerUtil.print(project, javadoc);
                        AIConsoleLoggerUtil.print(project, "");

                        // 输出可点击跳转的代码位置
                        VirtualFile virtualFile = element.getContainingFile().getVirtualFile();
                        if (virtualFile != null) {
                            String fileName = virtualFile.getName();
                            int line = lineNumber + 1; // 行号从 1 开始显示
                            String locationMessage = String.format("==>>: %s:%d", fileName, line);

                            // 使用可点击的超链接格式输出
                            AIConsoleLoggerUtil.printHyperlink(project, locationMessage, virtualFile, lineNumber);
                        }
                        AIConsoleLoggerUtil.print(project, "");

                    } catch (Exception e) {
                        log.info("插入文档失败", e);
                    }
                }),
                "Insert JavaDoc",
                "IntelliAI JavaDoc"
                                                         );
        });
    }

    /**
     * 删除元素的旧 JavaDoc 注释
     *
     * <p>删除元素已有的 JavaDoc 注释，为新注释腾出空间。
     * 同时删除注释前后的空白行，防止空行累积。
     *
     * <p>删除策略：
     * <ul>
     *   <li>删除注释本身</li>
     *   <li>删除注释后面的一个换行符（如果有）</li>
     *   <li>删除注释前面的所有空白行（防止累积）</li>
     * </ul>
     *
     * <p>安全措施：
     * <ul>
     *   <li>检查元素是否支持文档</li>
     *   <li>检查是否已有注释</li>
     *   <li>捕获异常防止中断操作</li>
     *   <li>边界检查防止越界</li>
     * </ul>
     *
     * @param element        目标元素
     * @param document       文档对象
     * @param verboseLogging 是否启用详细日志
     */
    @SuppressWarnings("D")
    private void deleteOldDocComment(@NotNull PsiElement element,
                                     @NotNull Document document,
                                     boolean verboseLogging) {
        if (!(element instanceof PsiDocCommentOwner)) {
            return;
        }

        com.intellij.psi.javadoc.PsiDocComment oldComment = ((PsiDocCommentOwner) element).getDocComment();
        if (oldComment == null) {
            return;
        }

        try {
            int startOffset = oldComment.getTextRange().getStartOffset();
            int endOffset = oldComment.getTextRange().getEndOffset();

            // 计算实际删除范围
            int deleteStart = startOffset;
            final int deleteEnd = getDeleteEnd(document, endOffset);

            // 2. 向前扩展：删除注释前面的所有空白行（包括空格、制表符）
            // 这是防止空行累积的关键！
            int lineStart = document.getLineStartOffset(document.getLineNumber(startOffset));
            while (deleteStart > lineStart) {
                char prevChar = document.getCharsSequence().charAt(deleteStart - 1);
                // 只删除空白字符（空格和制表符），但保留换行符
                if (prevChar == ' ' || prevChar == '\t') {
                    deleteStart--;
                } else {
                    break;
                }
            }

            // 如果注释前面只有空白字符，则从行首开始删除
            if (deleteStart == lineStart) {
                // 检查是否可以继续向前删除空行
                while (lineStart > 0) {
                    int prevLineEnd = lineStart - 1;
                    // 跳过换行符
                    if (document.getCharsSequence().charAt(prevLineEnd) == '\n') {
                        int prevLineStart = document.getLineStartOffset(document.getLineNumber(prevLineEnd));
                        // 检查前一行是否为空行（只包含空白字符）
                        boolean isEmptyLine = true;
                        for (int i = prevLineStart; i < prevLineEnd; i++) {
                            char c = document.getCharsSequence().charAt(i);
                            if (c != ' ' && c != '\t' && c != '\r') {
                                isEmptyLine = false;
                                break;
                            }
                        }

                        if (isEmptyLine) {
                            // 是空行，继续向前删除
                            deleteStart = prevLineStart;
                            lineStart = prevLineStart;
                        } else {
                            // 不是空行，停止向前扩展
                            break;
                        }
                    } else {
                        break;
                    }
                }
            }

            // 执行删除
            document.deleteString(deleteStart, deleteEnd);

            if (verboseLogging) {
                log.debug("删除旧注释: 从 {} 到 {} (原注释: {} 到 {})",
                          deleteStart, deleteEnd, startOffset, endOffset);
            }

        } catch (Exception e) {
            log.warn("删除旧注释失败", e);
        }
    }

    /**
     * 计算删除操作的结束位置
     * <p>
     * 根据给定的文档对象和结束偏移量，计算删除操作的实际结束位置。该方法会处理换行符，包括Windows风格的\r\n换行符。
     *
     * @param document  文档对象，用于获取文本内容和长度
     * @param endOffset 初始的结束偏移量
     * @return 调整后的删除结束位置
     */
    private static int getDeleteEnd(@NotNull Document document, int endOffset) {
        int deleteEnd = endOffset;

        // 1. 向后扩展：删除注释后面的一个换行符（如果有）
        if (deleteEnd < document.getTextLength()) {
            char nextChar = document.getCharsSequence().charAt(deleteEnd);
            if (nextChar == '\n') {
                deleteEnd++;
            } else if (nextChar == '\r' && deleteEnd + 1 < document.getTextLength()) {
                // 处理 Windows 风格的换行符 \r\n
                if (document.getCharsSequence().charAt(deleteEnd + 1) == '\n') {
                    deleteEnd += 2;
                } else {
                    deleteEnd++;
                }
            }
        }
        return deleteEnd;
    }

    /**
     * 获取文档插入位置
     *
     * <p>确定新文档注释应该插入的位置。
     * 通常插入在元素修饰符列表之前，确保注释位置正确。
     *
     * <p>位置规则：
     * <ul>
     *   <li>PsiMethod：方法修饰符列表之前</li>
     *   <li>PsiClass：类修饰符列表之前</li>
     *   <li>PsiField：字段修饰符列表之前</li>
     *   <li>其他：元素起始位置</li>
     * </ul>
     *
     * @param element PSI 元素
     * @return 文档插入位置的偏移量
     */
    private int getInsertPosition(@NotNull PsiElement element) {
        if (element instanceof PsiMethod) {
            return ((PsiMethod) element).getModifierList().getTextRange().getStartOffset();
        } else if (element instanceof PsiClass) {
            return ((PsiClass) element).getModifierList().getTextRange().getStartOffset();
        } else if (element instanceof PsiField) {
            return ((PsiField) element).getModifierList().getTextRange().getStartOffset();
        }
        return element.getTextRange().getStartOffset();
    }

    /**
     * 格式化 JavaDoc 内容
     *
     * <p>对 JavaDoc 注释进行格式化处理，根据用户配置决定是否执行各项格式化操作：
     * <ul>
     *   <li>在中英文之间添加空格（如果配置启用）</li>
     *   <li>将中文标点符号替换为英文标点符号（如果配置启用）</li>
     * </ul>
     *
     * @param javadoc 原始 JavaDoc 文本
     * @return 格式化后的 JavaDoc 文本
     */
    @NotNull
    private String formatJavaDocContent(@NotNull String javadoc) {
        if (javadoc.isEmpty()) {
            return javadoc;
        }

        return JavaDocFormatter.format(
            javadoc,
            settings.addSpaceBetweenChineseAndEnglish,
            settings.replaceChinesePunctuation
                                      );
    }
}

