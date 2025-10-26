package dev.dong4j.zeka.stack.idea.plugin.task;

import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.javadoc.PsiDocComment;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import dev.dong4j.zeka.stack.idea.plugin.MyBasePlatformTestCase;
import dev.dong4j.zeka.stack.idea.plugin.ai.AIServiceException;
import dev.dong4j.zeka.stack.idea.plugin.ai.ValidationResult;
import dev.dong4j.zeka.stack.idea.plugin.ai.provider.AIServiceProvider;
import dev.dong4j.zeka.stack.idea.plugin.settings.SettingsState;

/**
 * TaskExecutor 集成测试
 * <p>
 * 本测试类展示了如何测试 JavaDoc 替换逻辑，包括：
 * <ul>
 *   <li>使用 IntelliJ Platform Test Framework</li>
 *   <li>创建和操作 PSI 元素</li>
 *   <li>模拟 AI 返回结果</li>
 *   <li>测试文档的插入和替换</li>
 *   <li>验证 PSI 树的变化</li>
 * </ul>
 * <p>
 * 这是一个完整的集成测试示例，展示了 IntelliJ SDK 的核心 API 使用方法。
 *
 * @author Cursor AI Assistant
 * @version 1.0
 */
public class TaskExecutorIntegrationTest extends MyBasePlatformTestCase {

    private TaskExecutor taskExecutor;
    private ProgressIndicator mockIndicator;
    private SettingsState settings;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // 初始化设置
        settings = SettingsState.getInstance();
        settings.skipExisting = false; // 允许替换已有注释
        settings.maxRetries = 1; // 减少重试次数以加快测试

        // 创建 Mock ProgressIndicator
        mockIndicator = new MockProgressIndicator();

        // 创建 TaskExecutor 并注入 Mock AI Service
        taskExecutor = new TaskExecutor(getProject(), mockIndicator);

        // 使用反射注入 Mock AI Service
        injectMockAIService(taskExecutor);
    }

    /**
     * 测试为没有 JavaDoc 的方法插入注释
     * <p>
     * 展示了完整的测试流程：
     * 1. 创建测试文件和 PSI 元素
     * 2. 收集任务
     * 3. 执行任务（模拟 AI 返回）
     * 4. 验证结果
     */
    public void testInsertJavaDocForMethodWithoutExistingComment() {
        // 1. 创建测试 Java 文件
        String originalCode = """
            package com.example;
            
            public class TestClass {
                public String getUserName(int userId) {
                    return "user_" + userId;
                }
            }
            """;

        PsiJavaFile file = createJavaFile("TestClass.java", originalCode);

        // 2. 获取方法元素
        PsiMethod method = runReadAction(() -> {
            PsiClass[] classes = file.getClasses();
            assertNotNull("Should have classes", classes);
            assertTrue("Should have at least one class", classes.length > 0);

            PsiMethod[] methods = classes[0].getMethods();
            assertNotNull("Should have methods", methods);
            assertTrue("Should have at least one method", methods.length > 0);

            return methods[0];
        });

        // 3. 验证方法没有 JavaDoc
        Boolean hasDocBefore = runReadAction(() -> method.getDocComment() != null);
        assertFalse("Method should not have JavaDoc initially", hasDocBefore);

        // 4. 创建文档生成任务
        String methodCode = runReadAction(() -> method.getText());
        DocumentationTask task = new DocumentationTask(
            method,
            methodCode,
            DocumentationTask.TaskType.METHOD,
            file.getVirtualFile().getPath()
        );

        // 5. 执行任务（AI 将返回 mock 的 JavaDoc）
        taskExecutor.processTasks(List.of(task));

        // 6. 等待异步操作完成
        waitForPendingWrites();

        // 7. 验证结果
        String updatedContent = getFileText(file);
        System.out.println("Updated file content:");
        System.out.println(updatedContent);

        // 验证 JavaDoc 被插入
        assertTrue("File should contain JavaDoc opening",
                   updatedContent.contains("/**"));
        assertTrue("File should contain JavaDoc closing",
                   updatedContent.contains("*/"));
        assertTrue("File should contain method description",
                   updatedContent.contains("根据用户ID获取用户名称") ||
                   updatedContent.contains("Mock JavaDoc"));

        // 验证任务状态
        assertEquals("Task should be completed",
                     DocumentationTask.TaskStatus.COMPLETED,
                     task.getStatus());
        assertNotNull("Task should have result", task.getResult());
    }

    /**
     * 测试替换已有的 JavaDoc 注释
     * <p>
     * 展示了 JavaDoc 替换的完整流程：
     * 1. 创建带有旧注释的文件
     * 2. 执行替换
     * 3. 验证旧注释被删除
     * 4. 验证新注释被插入
     */
    public void testReplaceExistingJavaDoc() {
        // 1. 创建带有旧 JavaDoc 的文件
        String originalCode = """
            package com.example;
            
            public class TestClass {
                /**
                 * Old JavaDoc comment
                 */
                public void oldMethod() {
                    System.out.println("Hello");
                }
            }
            """;

        PsiJavaFile file = createJavaFile("TestClass.java", originalCode);

        // 2. 获取方法
        PsiMethod method = runReadAction(() -> {
            PsiClass[] classes = file.getClasses();
            return classes[0].getMethods()[0];
        });

        // 3. 验证方法有旧的 JavaDoc
        Boolean hasOldDoc = runReadAction(() -> {
            PsiDocComment docComment = method.getDocComment();
            return docComment != null &&
                   docComment.getText().contains("Old JavaDoc comment");
        });
        assertTrue("Method should have old JavaDoc", hasOldDoc);

        // 4. 创建任务并执行
        String methodCode = runReadAction(() -> method.getText());
        DocumentationTask task = new DocumentationTask(
            method,
            methodCode,
            DocumentationTask.TaskType.METHOD,
            file.getVirtualFile().getPath()
        );

        taskExecutor.processTasks(List.of(task));
        waitForPendingWrites();

        // 5. 验证旧注释被替换
        String updatedContent = getFileText(file);
        System.out.println("Content after replacement:");
        System.out.println(updatedContent);

        // 旧注释应该被删除
        assertFalse("Old comment should be removed",
                    updatedContent.contains("Old JavaDoc comment"));

        // 新注释应该被插入
        assertTrue("New comment should be inserted",
                   updatedContent.contains("Mock JavaDoc") ||
                   updatedContent.contains("打印 Hello"));
    }

    /**
     * 测试为类添加 JavaDoc
     * <p>
     * 展示如何为类添加文档注释
     */
    public void testInsertJavaDocForClass() {
        String originalCode = """
            package com.example;
            
            public class UserService {
                private String name;
            
                public void save() {
                    // save logic
                }
            }
            """;

        PsiJavaFile file = createJavaFile("UserService.java", originalCode);

        // 获取类元素
        PsiClass psiClass = runReadAction(() -> file.getClasses()[0]);

        // 创建任务
        String classCode = runReadAction(() -> psiClass.getText());
        DocumentationTask task = new DocumentationTask(
            psiClass,
            classCode,
            DocumentationTask.TaskType.CLASS,
            file.getVirtualFile().getPath()
        );

        // 执行
        taskExecutor.processTasks(List.of(task));
        waitForPendingWrites();

        // 验证
        String updatedContent = getFileText(file);
        assertTrue("Should have JavaDoc for class",
                   updatedContent.contains("/**") &&
                   updatedContent.indexOf("/**") < updatedContent.indexOf("public class"));
    }

    /**
     * 测试为字段添加 JavaDoc
     * <p>
     * 展示如何为字段添加文档注释
     */
    public void testInsertJavaDocForField() {
        String originalCode = """
            package com.example;
            
            public class User {
                private String username;
                private int age;
            }
            """;

        PsiJavaFile file = createJavaFile("User.java", originalCode);

        // 获取字段
        PsiField field = runReadAction(() -> {
            PsiClass[] classes = file.getClasses();
            PsiField[] fields = classes[0].getFields();
            return fields[0]; // username 字段
        });

        // 创建任务
        String fieldCode = runReadAction(() -> field.getText());
        DocumentationTask task = new DocumentationTask(
            field,
            fieldCode,
            DocumentationTask.TaskType.FIELD,
            file.getVirtualFile().getPath()
        );

        // 执行
        taskExecutor.processTasks(List.of(task));
        waitForPendingWrites();

        // 验证
        String updatedContent = getFileText(file);
        assertTrue("Should have JavaDoc for field",
                   updatedContent.contains("/**") &&
                   updatedContent.contains("username"));
    }

    /**
     * 测试批量处理多个任务
     * <p>
     * 展示如何批量处理多个文档生成任务
     */
    public void testProcessMultipleTasks() {
        String originalCode = """
            package com.example;
            
            public class Calculator {
                public int add(int a, int b) {
                    return a + b;
                }
            
                public int subtract(int a, int b) {
                    return a - b;
                }
            }
            """;

        PsiJavaFile file = createJavaFile("Calculator.java", originalCode);

        // 获取所有方法
        List<PsiMethod> methods = runReadAction(() -> {
            PsiClass[] classes = file.getClasses();
            return Arrays.asList(classes[0].getMethods());
        });

        // 为每个方法创建任务
        List<DocumentationTask> tasks = methods.stream()
            .map(method -> {
                String code = runReadAction(() -> method.getText());
                return new DocumentationTask(
                    method,
                    code,
                    DocumentationTask.TaskType.METHOD,
                    file.getVirtualFile().getPath()
                );
            })
            .collect(Collectors.toList());

        // 批量执行
        taskExecutor.processTasks(tasks);
        waitForPendingWrites();

        // 验证所有方法都有 JavaDoc
        String updatedContent = getFileText(file);

        // 应该有两个 JavaDoc 块
        int javadocCount = countOccurrences(updatedContent, "/**");
        assertTrue("Should have JavaDoc for both methods",
                   javadocCount >= 2);

        // 验证统计信息
        TaskExecutor.TaskStatistics stats = taskExecutor.getStatistics();
        assertTrue("Should have completed tasks",
                   stats.completed() > 0);
        System.out.println("Statistics: " + stats);
    }

    /**
     * 测试跳过已有文档的功能
     */
    public void testSkipExistingDocumentation() {
        // 启用跳过功能
        settings.skipExisting = true;

        String originalCode = """
            package com.example;
            
            public class Test {
                /**
                 * Existing documentation
                 */
                public void method1() {}
            
                public void method2() {}
            }
            """;

        PsiJavaFile file = createJavaFile("Test.java", originalCode);

        // 获取两个方法
        List<PsiMethod> methods = runReadAction(() -> {
            return Arrays.asList(file.getClasses()[0].getMethods());
        });

        // 创建任务
        List<DocumentationTask> tasks = methods.stream()
            .map(method -> {
                String code = runReadAction(() -> method.getText());
                return new DocumentationTask(
                    method,
                    code,
                    DocumentationTask.TaskType.METHOD,
                    file.getVirtualFile().getPath()
                );
            })
            .collect(Collectors.toList());

        // 执行
        taskExecutor.processTasks(tasks);
        waitForPendingWrites();

        // 验证统计
        TaskExecutor.TaskStatistics stats = taskExecutor.getStatistics();
        assertEquals("Should skip one task", 1, stats.skipped());
        assertEquals("Should complete one task", 1, stats.completed());
    }

    // ==================== 辅助方法 ====================

    /**
     * 注入 Mock AI Service
     * <p>
     * 使用反射替换 TaskExecutor 中的 AI Service，
     * 这样我们可以控制 AI 的返回结果，而不需要真实的 API 调用。
     */
    private void injectMockAIService(TaskExecutor executor) {
        try {
            Field field = TaskExecutor.class.getDeclaredField("aiService");
            field.setAccessible(true);
            field.set(executor, new MockAIServiceProvider());
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject mock AI service", e);
        }
    }

    /**
     * 等待所有异步写操作完成
     * <p>
     * IntelliJ Platform 的很多操作是异步的，
     * 在测试中需要等待这些操作完成才能验证结果。
     */
    private void waitForPendingWrites() {
        // 等待所有异步任务完成
        PsiDocumentManager.getInstance(getProject()).commitAllDocuments();

        // 等待 EDT（Event Dispatch Thread）上的任务
        com.intellij.util.ui.UIUtil.dispatchAllInvocationEvents();

        // 稍微等待一下确保所有操作完成
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 计算字符串中子串出现的次数
     */
    private int countOccurrences(String text, String substring) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(substring, index)) != -1) {
            count++;
            index += substring.length();
        }
        return count;
    }

    // ==================== Mock 类 ====================

    /**
     * Mock AI Service Provider
     * <p>
     * 模拟 AI 服务，返回预定义的 JavaDoc 注释。
     * 这样测试不依赖真实的 AI API，更快更稳定。
     */
    private static class MockAIServiceProvider implements AIServiceProvider {

        @NotNull
        @Override
        public String generateDocumentation(@NotNull String code,
                                            @NotNull DocumentationTask.TaskType type,
                                            @NotNull String language) throws AIServiceException {
            // 根据代码内容返回不同的 Mock JavaDoc
            if (code.contains("getUserName")) {
                return """
                    /**
                     * 根据用户ID获取用户名称
                     * 
                     * @param userId 用户ID
                     * @return 用户名称
                     */
                    """;
            } else if (code.contains("oldMethod")) {
                return """
                    /**
                     * 新的方法文档
                     * <p>
                     * 打印 Hello 消息到控制台
                     */
                    """;
            } else if (code.contains("UserService")) {
                return """
                    /**
                     * 用户服务类
                     * <p>
                     * 提供用户相关的业务逻辑处理
                     */
                    """;
            } else if (code.contains("username")) {
                return "/** 用户名 */";
            } else if (code.contains("add")) {
                return """
                    /**
                     * 计算两个数的和
                     * 
                     * @param a 第一个数
                     * @param b 第二个数
                     * @return 两数之和
                     */
                    """;
            } else if (code.contains("subtract")) {
                return """
                    /**
                     * 计算两个数的差
                     * 
                     * @param a 被减数
                     * @param b 减数
                     * @return 两数之差
                     */
                    """;
            } else {
                return """
                    /**
                     * Mock JavaDoc for testing
                     */
                    """;
            }
        }

        @NotNull
        @Override
        public ValidationResult validateConfiguration() {
            return ValidationResult.success("for test");
        }

        @NotNull
        @Override
        public String getProviderId() {
            return "mock";
        }

        @NotNull
        @Override
        public String getProviderName() {
            return "Mock Provider";
        }

        @NotNull
        @Override
        public List<String> getSupportedModels() {
            return List.of("mock-model");
        }

        @NotNull
        @Override
        public String getDefaultModel() {
            return "mock-model";
        }

        @NotNull
        @Override
        public String getDefaultBaseUrl() {
            return "http://localhost:8080";
        }

        @Override
        public boolean requiresApiKey() {
            return false;
        }

        @NotNull
        @Override
        public List<String> getAvailableModels() {
            return List.of("mock-model-1", "mock-model-2");
        }
    }

    /**
     * Mock Progress Indicator
     * <p>
     * 模拟进度指示器，记录进度信息但不实际显示 UI。
     */
    private static class MockProgressIndicator implements ProgressIndicator {
        private boolean canceled = false;
        private double fraction = 0.0;
        private String text = "";
        private String text2 = "";

        @Override
        public void start() {}

        @Override
        public void stop() {}

        @Override
        public boolean isRunning() {
            return true;
        }

        @Override
        public void cancel() {
            canceled = true;
        }

        @Override
        public boolean isCanceled() {
            return canceled;
        }

        @Override
        public void setText(String text) {
            this.text = text;
            System.out.println("Progress: " + text);
        }

        @Override
        public String getText() {
            return text;
        }

        @Override
        public void setText2(String text) {
            this.text2 = text;
            System.out.println("Progress2: " + text);
        }

        @Override
        public String getText2() {
            return text2;
        }

        @Override
        public double getFraction() {
            return fraction;
        }

        @Override
        public void setFraction(double fraction) {
            this.fraction = fraction;
        }

        @Override
        public void pushState() {}

        @Override
        public void popState() {}

        @Override
        public boolean isModal() {
            return false;
        }

        @NotNull
        @Override
        public ModalityState getModalityState() {
            return com.intellij.openapi.application.ModalityState.NON_MODAL;
        }

        @Override
        public void setModalityProgress(ProgressIndicator modalityProgress) {}

        @Override
        public boolean isIndeterminate() {
            return false;
        }

        @Override
        public void setIndeterminate(boolean indeterminate) {}

        @Override
        public void checkCanceled() throws com.intellij.openapi.progress.ProcessCanceledException {
            if (canceled) {
                throw new com.intellij.openapi.progress.ProcessCanceledException();
            }
        }

        @Override
        public boolean isPopupWasShown() {
            return false;
        }

        @Override
        public boolean isShowing() {
            return false;
        }
    }
}

