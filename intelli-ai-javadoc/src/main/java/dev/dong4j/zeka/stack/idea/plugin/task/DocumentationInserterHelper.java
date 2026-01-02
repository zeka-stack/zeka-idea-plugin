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
import com.intellij.psi.PsiType;
import com.intellij.psi.codeStyle.CodeStyleManager;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.psi.KtClassOrObject;
import org.jetbrains.kotlin.psi.KtNamedFunction;
import org.jetbrains.kotlin.psi.KtProperty;

import java.util.Objects;

import dev.dong4j.zeka.stack.idea.plugin.PluginContents;
import dev.dong4j.zeka.stack.idea.plugin.common.util.AIConsoleLoggerUtil;
import dev.dong4j.zeka.stack.idea.plugin.kit.MessageFormatter;
import dev.dong4j.zeka.stack.idea.plugin.settings.SettingsState;
import dev.dong4j.zeka.stack.idea.plugin.util.JavadocSingleLineFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 文档插入辅助类
 * <p>
 * 负责将生成的 Javadoc 文档插入到源代码中，包括删除旧注释、确定插入位置、
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

            PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(document);

            CommandProcessor.getInstance().executeCommand(
                project,
                () -> ApplicationManager.getApplication().runWriteAction(() -> {
                    try {
                        // 1. 清理 javadoc（去掉多余代码/Markdown 包裹，并做合法性校验）
                        String javadoc = cleanJavadoc(documentation.trim());
                        if (javadoc.isEmpty()) {
                            log.info("清理后的 Javadoc 为空，插入文档失败");
                            return;
                        }

                        // 1.1 按方法签名清理多余的 @param/@throws 标签
                        javadoc = cleanParamAndThrowsTags(javadoc, element);

                        // 2. 先删除旧注释（如果存在）
                        deleteOldDocComment(element, verboseLogging);

                        // 3. 提交删除操作
                        PsiDocumentManager.getInstance(project).commitDocument(document);

                        // 3. 获取插入位置（删除后需要重新获取）
                        int startPosition = getInsertPosition(element);
                        int lineNumber = document.getLineNumber(startPosition);
                        int lineStartPosition = document.getLineStartOffset(lineNumber);

                        // 4. 格式化 Javadoc 内容（根据配置进行格式化）
                        javadoc = formatJavaDocContent(javadoc);

                        // 5. 插入新 Javadoc
                        document.insertString(lineStartPosition, javadoc + "\n");
                        PsiDocumentManager.getInstance(project).commitDocument(document);

                        // 7. 格式化插入的 Javadoc
                        PsiFile psiFile = element.getContainingFile();
                        if (psiFile != null) {
                            int endPosition = lineStartPosition + javadoc.length() + 1;
                            CodeStyleManager.getInstance(project).reformatText(psiFile, lineStartPosition, endPosition);
                        }

                        // 8. 提交格式化后的文档变更
                        PsiDocumentManager.getInstance(project).commitDocument(document);

                        // 9. 后处理：如果是单行注释，压缩为单行格式（如果配置启用）
                        // 注意：需要在格式化后重新获取元素，因为格式化可能改变了 PSI 结构
                        // 使用原始元素进行查找，因为格式化不会改变元素本身
                        if (settings.compressSingleLineJavaDoc && element instanceof PsiDocCommentOwner) {
                            JavadocSingleLineFormatter.compressSingleLineJavaDoc(element, document);
                            // 提交压缩后的变更
                            PsiDocumentManager.getInstance(project).commitDocument(document);
                        }

                        // Console 日志：输出最终插入的 Javadoc（仅详细日志模式）
                        AIConsoleLoggerUtil.printWithTimestamp(project, "=== 最终插入的 Javadoc ===");
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
                "Insert Javadoc",
                PluginContents.PLUGIN_NAME
                                                         );
        });
    }

    /**
     * 清理 Javadoc 内容
     * <p>
     * 该方法用于过滤 Javadoc 中的 Markdown 代码块内容, 并进一步清理代码注释后的多余内容.
     *
     * @param javadoc 需要清理的原始 Javadoc 字符串
     * @return 清理后的 Javadoc 字符串
     */
    private String cleanJavadoc(String javadoc) {
        // 过滤注释后的代码部分
        final String filterMarkdownCodeBlocksContent = filterMarkdownCodeBlocks(javadoc);
        javadoc = filterCodeAfterComment(filterMarkdownCodeBlocksContent);

        // 确保文档是合法的 javadoc
        if (!javadoc.startsWith("/**") || !javadoc.endsWith("*/")) {
            return "";
        }
        return javadoc;
    }

    /**
     * 删除元素的旧 Javadoc/KDoc 注释
     *
     * <p>使用 IntelliJ Platform PSI API 直接删除元素已有的文档注释
     * （Java 的 Javadoc 或 Kotlin 的 KDoc），为新注释腾出空间。
     *
     * <p>支持的元素类型：
     * <ul>
     *   <li>Java 元素：通过 PsiDocCommentOwner 接口获取 Javadoc</li>
     *   <li>Kotlin 元素：KtClassOrObject、KtNamedFunction、KtProperty 的 KDoc</li>
     * </ul>
     *
     * <p>实现方式：
     * <ul>
     *   <li>使用 PSI 元素的 delete() 方法直接删除注释</li>
     *   <li>IntelliJ Platform 会自动处理 PSI 树的更新和文档同步</li>
     *   <li>比手动操作 Document 更安全可靠</li>
     * </ul>
     *
     * <p>安全措施：
     * <ul>
     *   <li>检查元素是否支持文档</li>
     *   <li>检查是否已有注释</li>
     *   <li>捕获异常防止中断操作</li>
     * </ul>
     *
     * @param element        目标元素
     * @param verboseLogging 是否启用详细日志
     */
    private void deleteOldDocComment(@NotNull PsiElement element,
                                     boolean verboseLogging) {
        final PsiElement oldComment = getOldComment(element);

        // 如果没有注释，直接返回
        if (oldComment == null) {
            return;
        }

        try {
            if (verboseLogging) {
                log.debug("删除旧注释: {} (使用 PSI API)", oldComment.getTextRange());
            }

            // 使用 IntelliJ Platform PSI API 直接删除注释
            // 这会自动更新 PSI 树和文档，比手动操作 Document 更可靠
            oldComment.delete();

        } catch (Exception e) {
            log.warn("删除旧注释失败", e);
        }
    }

    /**
     * 获取指定元素的旧文档注释
     * <p>
     * 根据元素的类型获取对应的文档注释. 支持 PsiDocCommentOwner 类型以及 Kotlin 中的类, 对象, 函数和属性.
     *
     * @param element 要获取文档注释的元素, 不能为 null
     * @return 元素的文档注释, 如果元素不支持文档注释或没有文档注释则返回 null
     */
    @Nullable
    private static PsiElement getOldComment(@NotNull PsiElement element) {
        PsiElement oldComment = null;

        // 处理 Java 元素的 Javadoc
        if (element instanceof PsiDocCommentOwner) {
            oldComment = ((PsiDocCommentOwner) element).getDocComment();
        }
        // 处理 Kotlin 元素的 KDoc
        else if (element instanceof KtClassOrObject) {
            oldComment = ((KtClassOrObject) element).getDocComment();
        } else if (element instanceof KtNamedFunction) {
            oldComment = ((KtNamedFunction) element).getDocComment();
        } else if (element instanceof KtProperty) {
            oldComment = ((KtProperty) element).getDocComment();
        }
        return oldComment;
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
        if (element instanceof PsiMethod method) {
            return method.getModifierList().getTextRange().getStartOffset();
        } else if (element instanceof PsiClass clazz) {
            return Objects.requireNonNull(clazz.getModifierList()).getTextRange().getStartOffset();
        } else if (element instanceof PsiField field) {
            return Objects.requireNonNull(field.getModifierList()).getTextRange().getStartOffset();
        }
        return element.getTextRange().getStartOffset();
    }

    /**
     * 格式化 Javadoc 内容
     *
     * <p>对 Javadoc 注释进行格式化处理，根据用户配置决定是否执行各项格式化操作：
     * <ul>
     *   <li>在中英文之间添加空格（如果配置启用）</li>
     *   <li>将中文标点符号替换为英文标点符号（如果配置启用）</li>
     * </ul>
     *
     * @param javadoc 原始 Javadoc 文本
     * @return 格式化后的 Javadoc 文本
     */
    @NotNull
    private String formatJavaDocContent(@NotNull String javadoc) {
        if (javadoc.isEmpty()) {
            return javadoc;
        }

        return MessageFormatter.format(
            javadoc,
            settings.addSpaceBetweenChineseAndEnglish,
            settings.replaceChinesePunctuation
                                      );
    }

    /**
     * 过滤 Markdown 代码块标签
     * <p>
     * 去除内容中可能存在的 Markdown 代码块标记，如开头的 ```java、```javascript 等
     * 以及结尾的 ```。即使提示词要求不返回代码块标签，LLM 有时仍会返回。
     *
     * @param content 需要过滤的原始内容
     * @return 过滤后的内容，已去除 Markdown 代码块标签
     */
    private String filterMarkdownCodeBlocks(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }

        String result = content.trim();

        // 去除开头的代码块标记（如 ```java、```javascript、``` 等）
        // 匹配模式：``` 后面可能跟语言标识符，然后是换行符
        if (result.startsWith("```")) {
            int firstNewlineIndex = result.indexOf('\n');
            if (firstNewlineIndex != -1) {
                // 找到第一个换行符，去除从开头到换行符的部分（包括换行符）
                result = result.substring(firstNewlineIndex + 1);
            } else {
                // 如果没有换行符，说明只有 ```，直接去除开头的 ```
                result = result.replaceFirst("^```+\\s*", "");
            }
        }

        // 去除结尾的代码块标记 ```
        result = result.trim();
        if (result.endsWith("```")) {
            // 去除结尾的 ```，可能包含多个反引号，并去除前面的空白字符
            result = result.replaceAll("\\s*```+$", "").trim();
        }

        return result;
    }

    /**
     * 过滤注释后的代码部分
     * <p>
     * 即使提示词要求只返回注释，部分模型仍会返回原始代码。
     * 该方法从 {@code /**} 开始，到第一个 {@code /} 结束，直接截取注释部分。
     * <p>
     * 处理示例：
     * <ul>
     * <li>输入：/** comment /\nSerializable getId(); → 输出：/** comment /</li>
     * <li>输入：/** comment /\npublic void method() {} → 输出：/** comment /</li>
     * </ul>
     *
     * @param content 需要过滤的内容
     * @return 过滤后的内容，只包含从 /** 到第一个 / 之间的注释部分
     */
    private String filterCodeAfterComment(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }

        // 查找 Javadoc 注释块的开始位置
        int commentStartIndex = content.indexOf("/**");
        if (commentStartIndex == -1) {
            // 如果没有找到注释块开始标记，直接返回原内容
            return content;
        }

        // 从注释块开始位置查找结束位置
        String fromCommentStart = content.substring(commentStartIndex);
        int commentEndIndex = fromCommentStart.indexOf("*/");
        if (commentEndIndex == -1) {
            // 如果没有找到注释块结束标记，直接返回原内容
            return content;
        }

        // 截取从 /** 到第一个 */ 之间的内容（包括 */）
        int endPos = commentStartIndex + commentEndIndex + 2;
        return content.substring(commentStartIndex, endPos).trim();
    }

    /**
     * 根据目标元素的签名清理多余的 @param/@throws/@exception/@return 标签
     * <p>
     * 即便在提示词中已经声明"没有参数/异常/返回值时不要添加标签"，部分模型仍然会错误地添加这些标签，
     * 因此需要在插入前做一次基于 PSI 的安全过滤。
     * <p>
     * 处理规则：
     * <ul>
     *   <li>方法：根据实际签名清理多余的标签</li>
     *   <li>字段：移除所有 @param、@throws、@exception、@return 标签（字段不应该有这些标签）</li>
     *   <li>类：不做处理</li>
     * </ul>
     *
     * @param javadoc 原始 Javadoc 文本（已通过 {@link #cleanJavadoc(String)} 处理）
     * @param element 目标 PSI 元素（方法/函数/字段/其他）
     * @return 清理后的 Javadoc 文本
     */
    @NotNull
    private String cleanParamAndThrowsTags(@NotNull String javadoc, @NotNull PsiElement element) {
        // 字段不应该有任何参数/异常/返回值标签，全部移除
        if (element instanceof PsiField || element instanceof KtProperty) {
            String[] lines = javadoc.split("\n");
            StringBuilder sb = new StringBuilder(javadoc.length());

            for (String line : lines) {
                String trim = line.trim();
                // 移除字段上不应该存在的标签
                if (trim.contains("@param") || trim.contains("@throws") ||
                    trim.contains("@exception") || trim.contains("@return")) {
                    continue;
                }
                sb.append(line).append("\n");
            }

            String cleaned = sb.toString().trim();
            // 保底：如果全部被删空，就返回原始 javadoc，避免插入空注释
            return cleaned.isEmpty() ? javadoc : cleaned;
        }

        // 方法处理：根据实际签名清理多余的标签
        boolean hasParams;
        boolean hasThrows = false;
        boolean hasReturnValue; // 默认有返回值

        if (element instanceof PsiMethod method) {
            hasParams = method.getParameterList().getParametersCount() > 0;
            hasThrows = method.getThrowsList().getReferencedTypes().length > 0;
            // 检查返回类型是否为 void
            PsiType returnType = method.getReturnType();
            hasReturnValue = returnType != null && !returnType.getCanonicalText().equals("void");
        } else if (element instanceof KtNamedFunction function) {
            // Kotlin 函数：根据参数列表判断是否保留 @param
            hasParams = !function.getValueParameters().isEmpty();
            // Kotlin 中异常声明较少使用，这里暂不根据签名处理 @throws，只在 Java 方法中严格校验
            // 对于 Kotlin 函数，暂时不处理 @return 标签（因为判断 Unit 比较复杂）
            // 默认认为有返回值，避免误删
            hasReturnValue = true;
        } else {
            // 其他元素（类等）不做额外处理
            return javadoc;
        }

        // 拆分 Javadoc 为行
        String[] lines = javadoc.split("\n");
        StringBuilder sb = new StringBuilder(javadoc.length());

        for (String line : lines) {
            String trim = line.trim();

            // 如果方法没有参数，则移除所有 @param 行
            if (!hasParams && (trim.contains("@param") || trim.contains("@param "))) {
                continue;
            }
            // 如果方法没有 throws，则移除 @throws/@exception 行（仅针对 Java 方法）
            if (!hasThrows && element instanceof PsiMethod &&
                (trim.contains("@throws") || trim.contains("@exception"))) {
                continue;
            }
            // 如果方法没有返回值（void），则移除 @return 行（仅针对 Java 方法）
            if (!hasReturnValue && element instanceof PsiMethod &&
                (trim.contains("@return") || trim.contains("@return "))) {
                continue;
            }

            sb.append(line).append("\n");
        }

        String cleaned = sb.toString().trim();
        // 保底：如果全部被删空，就返回原始 javadoc，避免插入空注释
        return cleaned.isEmpty() ? javadoc : cleaned;
    }
}

