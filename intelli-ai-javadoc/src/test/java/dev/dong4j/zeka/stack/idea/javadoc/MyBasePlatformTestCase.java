package dev.dong4j.zeka.stack.idea.javadoc;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;

import org.jetbrains.annotations.NotNull;

/**
 * IntelliJ Platform 测试基类
 * <p>
 * 提供用于 IntelliJ Platform SDK 测试的通用工具方法和配置，简化 PSI 文件操作和测试流程。
 * <p>
 * 该类封装了 PSI 文件的创建、读写操作、文档与 PSI 的同步以及断言方法，适用于基于 IntelliJ 平台的插件开发测试场景。
 * <p>
 * 主要功能包括：
 * <ul>
 *   <li>创建测试用的 PSI Java 文件</li>
 *   <li>执行 PSI 读操作（Read Action）</li>
 *   <li>执行 PSI 写操作（Write Action）</li>
 *   <li>获取 PSI 文件对应的 Document</li>
 *   <li>提交 Document 变更到 PSI 树</li>
 *   <li>获取 PSI 文件的文本内容</li>
 *   <li>断言文件是否包含或不包含指定文本</li>
 *   <li>打印文件内容（用于调试）</li>
 * </ul>
 *
 * @author Cursor AI Assistant
 * @version 1.0
 * @date 2025.10.24
 * @since 1.0
 */
public abstract class MyBasePlatformTestCase extends com.intellij.testFramework.fixtures.BasePlatformTestCase {
    /**
     * 创建 Java 测试文件
     * <p>
     * 通过指定文件名和内容生成一个 Java 文件，并返回对应的 PSI Java 文件对象。
     *
     * @param fileName 文件名
     * @param content  文件内容
     * @return PSI Java 文件
     * @throws IllegalStateException 如果生成的文件不是 Java 文件
     */
    protected PsiJavaFile createJavaFile(@NotNull String fileName, @NotNull String content) {
        // 使用 fixture 创建文件 - 这是 IntelliJ 测试框架提供的便捷方法
        // fixture 是测试框架提供的核心工具，包含了项目、编辑器、PSI 等所有测试需要的组件
        PsiFile file = myFixture.configureByText(fileName, content);

        if (!(file instanceof PsiJavaFile)) {
            throw new IllegalStateException("Created file is not a Java file");
        }

        return (PsiJavaFile) file;
    }

    /**
     * 在读操作中执行代码
     * <p>
     * 确保在读取 PSI 数据时，所有操作都在读取动作中执行，以保证 PSI 树的线程安全性和一致性。
     * <p>
     * 该方法通过 IntelliJ Platform 的 ApplicationManager 执行读操作，防止在读取过程中发生 PSI 树的修改。
     *
     * @param computation 要执行的计算逻辑，实现 Computable 接口
     * @param <T>         计算结果的类型
     * @return 计算结果
     */
    protected <T> T runReadAction(@NotNull Computable<T> computation) {
        return ApplicationManager.getApplication().runReadAction(computation);
    }

    /**
     * 在写操作中执行代码
     * <p>
     * 用于在 IntelliJ Platform 的写操作（Write Action）中执行指定的可运行操作。
     * 所有对 PSI（Program Structure Interface）的修改必须在写操作中进行，以确保 PSI 树的线程安全和一致性。
     * <p>
     * 该方法会自动将传入的 Runnable 操作封装在写操作中执行，确保操作期间 PSI 树被正确锁定，防止并发修改。
     *
     * @param runnable 要执行的操作，必须是一个非空的 Runnable 对象
     */
    protected void runWriteAction(@NotNull Runnable runnable) {
        WriteCommandAction.runWriteCommandAction(getProject(), runnable);
    }

    /**
     * 获取文件对应的 Document
     * <p>
     * Document 是 IntelliJ 中文本文件的编辑器表示。
     * PSI 是代码的语义树表示。
     * 两者需要保持同步。
     *
     * @param file PSI 文件
     * @return Document 对象
     */
    protected Document getDocument(@NotNull PsiFile file) {
        return FileDocumentManager.getInstance().getDocument(file.getVirtualFile());
    }

    /**
     * 提交 Document 变更到 PSI
     * <p>
     * 当修改 Document 后，需要调用此方法同步到 PSI 树。
     * 这是 IntelliJ Platform 的重要概念：Document 和 PSI 的同步。
     *
     * @param file PSI 文件
     */
    protected void commitDocument(@NotNull PsiFile file) {
        Document document = getDocument(file);
        PsiDocumentManager.getInstance(getProject()).commitDocument(document);
    }

    /**
     * 获取文件的文本内容
     * <p>
     * 通过指定的 PSI 文件获取其文本内容，使用读写操作确保线程安全。
     *
     * @param file PSI 文件
     * @return 文件文本内容
     */
    protected String getFileText(@NotNull PsiFile file) {
        return runReadAction(file::getText);
    }

    /**
     * 断言指定的 PSI 文件包含指定的文本内容
     * <p>
     * 该方法会读取 PSI 文件的内容，并验证其是否包含预期的文本。若不包含，则抛出断言失败异常。
     *
     * @param file         要检查的 PSI 文件对象
     * @param expectedText 预期文件中应包含的文本内容
     */
    protected void assertFileContains(@NotNull PsiFile file, @NotNull String expectedText) {
        String fileText = getFileText(file);
        assertTrue("File should contain: " + expectedText,
                   fileText.contains(expectedText));
    }

    /**
     * 断言指定的文件不包含指定的文本内容
     * <p>
     * 该方法用于验证文件内容中不包含预期之外的文本，若包含则抛出断言失败异常
     *
     * @param file           要检查的 PSI 文件对象
     * @param unexpectedText 不应该出现在文件中的文本内容
     */
    protected void assertFileNotContains(@NotNull PsiFile file, @NotNull String unexpectedText) {
        String fileText = getFileText(file);
        assertFalse("File should not contain: " + unexpectedText,
                    fileText.contains(unexpectedText));
    }

    /**
     * 打印文件内容（用于调试）
     * <p>
     * 该方法用于将指定 PSI 文件的内容输出到控制台，便于调试时查看文件内容。
     *
     * @param file PSI 文件对象
     */
    protected void printFileContent(@NotNull PsiFile file) {
        String content = getFileText(file);
        System.out.println("=== File Content ===");
        System.out.println(content);
        System.out.println("=== End of Content ===");
    }
}

