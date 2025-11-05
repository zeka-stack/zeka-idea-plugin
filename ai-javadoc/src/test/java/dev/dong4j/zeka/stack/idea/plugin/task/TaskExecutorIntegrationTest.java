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
 * TaskExecutor 集成测试类
 * <p>
 * 本测试类用于验证 TaskExecutor 在不同场景下的行为，包括为方法、类、字段添加或替换 JavaDoc 注释，以及批量处理多个任务。
 * 测试覆盖了 JavaDoc 插入、替换、跳过已有注释、模拟 AI 服务返回结果等核心功能。
 *
 * @author Cursor AI Assistant
 * @version 1.0
 * @date 2025.04.05
 * @since 1.0
 */
public class TaskExecutorIntegrationTest extends MyBasePlatformTestCase {

    /** 任务执行器，用于执行定时任务或异步任务 */
    private TaskExecutor taskExecutor;
    /** 模拟的进度指示器，用于测试或展示进度状态 */
    private ProgressIndicator mockIndicator;
    /** 设置状态信息 */
    private SettingsState settings;

    /**
     * 初始化测试环境设置
     * <p>
     * 该方法用于设置测试所需的各项配置和模拟对象，包括加载设置状态、创建模拟进度指示器、
     * 初始化任务执行器并注入模拟的AI服务。
     *
     * @throws Exception 如果初始化过程中发生异常
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // 初始化设置
        settings = SettingsState.getInstance();
        settings.overrideExisting = true; // 允许替换已有注释
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
     * 该方法用于演示如何为一个未包含 JavaDoc 的方法生成并插入注释。
     * 测试流程包括创建测试文件、获取方法元素、验证原始状态、生成注释任务、执行任务、等待完成以及验证最终结果。
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
     * 1. 创建带有旧 JavaDoc 的文件
     * 2. 获取目标方法
     * 3. 验证旧注释的存在
     * 4. 执行 JavaDoc 替换任务
     * 5. 验证旧注释被删除，新注释被插入
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
     * 测试为类添加 JavaDoc 注释
     * <p>
     * 该方法用于演示如何为类生成并插入 JavaDoc 注释，展示注释的创建和插入过程。
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
     * 为字段添加 JavaDoc 注释
     * <p>
     * 该方法用于演示如何为类中的字段生成 JavaDoc 注释，包括注释内容的插入和验证。
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
        String fieldCode = runReadAction(field::getText);
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
     * 用于演示如何批量处理多个文档生成任务，包括获取代码内容、创建任务、执行任务以及验证结果。
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
     * <p>
     * 该方法用于验证在启用跳过已有文档功能的情况下，文档生成任务是否能够正确跳过已存在的文档注释，并对未注释的方法进行处理。
     */
    public void testSkipExistingDocumentation() {
        // 覆盖已有注释
        settings.overrideExisting = false;

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
                String code = runReadAction(method::getText);
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
     * 向 TaskExecutor 注入模拟的 AI 服务
     * <p>
     * 通过反射获取 TaskExecutor 类中的 aiService 字段，
     * 并将其替换为模拟的 AI 服务实现，以便在测试中控制 AI 的行为，
     * 而无需依赖真实的服务调用。
     *
     * @param executor 要注入模拟 AI 服务的 TaskExecutor 实例
     * @throws RuntimeException 如果反射操作失败或设置字段时发生异常
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
     * 该方法用于确保 IntelliJ Platform 中所有异步写入操作已经完成，
     * 以便在测试中正确验证结果。主要包括三个步骤：
     * 1. 提交所有文档的更改
     * 2. 分发所有事件调度线程（EDT）上的任务
     * 3. 短暂休眠以确保所有操作彻底完成
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
     * <p>
     * 该方法用于统计指定字符串中某个子串出现的总次数，包括重叠的情况。
     *
     * @param text      要搜索的原始字符串
     * @param substring 要查找的子串
     * @return 子串在字符串中出现的次数
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
     * 模拟 AI 服务提供者
     * <p>
     * 用于模拟 AI 服务，返回预定义的 JavaDoc 注释，以避免依赖真实 AI API，提升测试效率和稳定性。
     * 该类实现了 AIServiceProvider 接口，提供生成 JavaDoc、验证配置、获取服务信息等基础功能。
     *
     * @author 作者
     * @version 1.0.0
     * @date 2025.10.24
     * @since 1.0.0
     */
    private static class MockAIServiceProvider implements AIServiceProvider {

        /**
         * 生成指定代码的模拟 JavaDoc 文档
         * <p>
         * 根据传入的代码内容，返回对应的模拟 JavaDoc 注释。支持多种代码片段识别，如方法名、类名、关键字等。
         *
         * @param code     代码内容，用于判断生成哪种 JavaDoc
         * @param type     任务类型，用于区分不同文档生成场景
         * @param language 语言类型，用于指定文档语言（如中文、英文）
         * @return 生成的 JavaDoc 注释字符串
         * @throws AIServiceException 如果生成文档过程中发生错误
         */
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

        /**
         * 验证配置信息并返回验证结果
         * <p>
         * 该方法用于验证配置信息是否符合要求，返回验证结果对象。
         *
         * @return 验证结果对象，表示验证是否成功
         */
        @NotNull
        @Override
        public ValidationResult validateConfiguration(String apiKey) {
            return ValidationResult.success("for test");
        }

        /**
         * 获取服务提供方ID
         * <p>
         * 返回一个固定的模拟服务提供方ID，用于测试或演示场景
         *
         * @return 服务提供方ID
         */
        @NotNull
        @Override
        public String getProviderId() {
            return "mock";
        }

        /**
         * 获取当前数据提供者的名称
         * <p>
         * 返回一个固定的模拟数据提供者名称，用于标识当前使用的数据源
         *
         * @return 数据提供者名称
         */
        @NotNull
        @Override
        public String getProviderName() {
            return "Mock Provider";
        }

        /**
         * 获取当前支持的模型列表
         * <p>
         * 返回一个包含当前系统支持模型名称的列表，用于展示或配置用途。
         *
         * @return 支持的模型名称列表
         */
        @NotNull
        @Override
        public List<String> getSupportedModels() {
            return List.of("mock-model");
        }

        /**
         * 获取默认模型名称
         * <p>
         * 返回系统默认的模型名称，用于初始化或默认场景下的模型选择
         *
         * @return 默认模型名称
         */
        @NotNull
        @Override
        public String getDefaultModel() {
            return "mock-model";
        }

        /**
         * 返回默认的基URL
         * <p>
         * 该方法用于获取系统默认使用的基URL，通常用于构建API请求路径。
         *
         * @return 默认的基URL，值为 "<a href="http://localhost:8080">...</a>"
         */
        @NotNull
        @Override
        public String getDefaultBaseUrl() {
            return "http://localhost:8080";
        }

        /**
         * 判断该接口是否需要API密钥
         * <p>
         * 该方法返回false，表示当前接口不需要API密钥进行访问
         *
         * @return 始终返回false，表示不需要API密钥
         */
        @Override
        public boolean requiresApiKey() {
            return false;
        }

        /**
         * 获取可用的模型列表
         * <p>
         * 返回系统中当前可用的模型名称列表，用于展示或选择
         *
         * @return 可用模型名称列表
         */
        @NotNull
        @Override
        public List<String> getAvailableModels(String apiKey) {
            return List.of("mock-model-1", "mock-model-2");
        }
    }

    /**
     * 模拟进度指示器类
     * <p>
     * 用于在不实际显示 UI 的情况下模拟进度指示器的行为，记录进度信息如取消状态、进度分数和文本内容。
     * 该类实现了 ProgressIndicator 接口，主要用于测试或非图形界面环境下的进度控制。
     *
     * @author 作者信息未提供
     * @version 1.0.0
     * @date 2025.10.24
     * @since 1.0.0
     */
    private static class MockProgressIndicator implements ProgressIndicator {
        /** 取消标志，表示操作是否已被取消 */
        private boolean canceled = false;
        /** 分数值，表示某个比例或占比，默认初始化为 0.0 */
        private double fraction = 0.0;
        /** 文本内容字段，用于存储或展示用户输入的文本信息 */
        private String text = "";
        /** 文本内容字段，用于存储或处理特定文本信息 */
        private String text2 = "";

        /**
         * 启动组件或服务
         * <p>
         * 用于初始化并启动组件或服务的相关逻辑
         */
        @Override
        public void start() {}

        /**
         * 停止当前组件或服务的运行
         * <p>
         * 该方法用于执行停止操作，释放资源或中断正在进行的任务
         */
        @Override
        public void stop() {}

        /**
         * 判断当前任务是否正在运行
         * <p>
         * 返回一个布尔值，表示任务是否处于运行状态
         *
         * @return 如果任务正在运行，返回 true；否则返回 false
         */
        @Override
        public boolean isRunning() {
            return true;
        }

        /**
         * 取消当前操作，标记为已取消状态
         * <p>
         * 该方法用于取消当前正在进行的操作，并将取消状态设置为 true
         *
         * @since 1.0
         */
        @Override
        public void cancel() {
            canceled = true;
        }

        /**
         * 判断当前操作是否已取消
         * <p>
         * 返回内部状态变量 canceled 的值，用于判断当前操作是否已被取消
         *
         * @return 如果操作已取消，返回 true；否则返回 false
         */
        @Override
        public boolean isCanceled() {
            return canceled;
        }

        /**
         * 设置文本内容并打印进度信息
         * <p>
         * 该方法用于设置文本内容，并将文本内容作为进度信息打印到控制台。
         *
         * @param text 要设置的文本内容
         */
        @Override
        public void setText(String text) {
            this.text = text;
            System.out.println("Progress: " + text);
        }

        /**
         * 获取当前对象的文本内容
         * <p>
         * 返回该对象内部存储的文本字符串值
         *
         * @return 当前对象的文本内容
         */
        @Override
        public String getText() {
            return text;
        }

        /**
         * 设置文本内容并打印进度信息
         * <p>
         * 该方法用于设置文本内容，并打印当前进度信息。
         *
         * @param text 要设置的文本内容
         */
        @Override
        public void setText2(String text) {
            this.text2 = text;
            System.out.println("Progress2: " + text);
        }

        /**
         * 获取文本内容2
         * <p>
         * 返回对象中存储的文本内容2字段的值。
         *
         * @return 文本内容2
         */
        @Override
        public String getText2() {
            return text2;
        }

        /**
         * 获取当前对象的分数值
         * <p>
         * 返回该对象内部存储的分数值
         *
         * @return 当前对象的分数值
         */
        @Override
        public double getFraction() {
            return fraction;
        }

        /**
         * 设置分数值
         * <p>
         * 用于设置当前对象的分数属性值
         *
         * @param fraction 分数值
         */
        @Override
        public void setFraction(double fraction) {
            this.fraction = fraction;
        }

        /**
         * 执行状态推送操作
         * <p>
         * 该方法用于推送当前状态，具体实现由子类覆盖
         */
        @Override
        public void pushState() {}

        /**
         * 弹出当前状态
         * <p>
         * 该方法用于移除或弹出当前应用或系统中的状态信息，通常用于状态管理或回退操作。
         */
        @Override
        public void popState() {}

        /**
         * 判断当前组件是否为模态组件
         * <p>
         * 该方法用于确定当前组件是否具有模态特性，返回布尔值表示是否为模态组件
         *
         * @return true 表示是模态组件，false 表示不是模态组件
         */
        @Override
        public boolean isModal() {
            return false;
        }

        /**
         * 获取当前的模态状态
         * <p>
         * 返回非模态状态，表示该操作不依赖于任何模态对话框或用户交互。
         *
         * @return 当前模态状态，类型为 ModalityState
         */
        @NotNull
        @Override
        public ModalityState getModalityState() {
            return com.intellij.openapi.application.ModalityState.NON_MODAL;
        }

        /**
         * 设置模态进度指示器
         * <p>
         * 用于设置模态操作的进度指示器，通常用于显示任务执行进度
         *
         * @param modalityProgress 模态进度指示器对象
         */
        @Override
        public void setModalityProgress(ProgressIndicator modalityProgress) {}

        /**
         * 判断当前状态是否为不确定状态
         * <p>
         * 该方法用于判断当前对象的状态是否处于不确定状态，默认返回 false
         *
         * @return 如果当前状态是不确定状态，返回 true；否则返回 false
         */
        @Override
        public boolean isIndeterminate() {
            return false;
        }

        /**
         * 设置复选框是否为不确定状态
         * <p>
         * 该方法用于设置复选框的不确定状态，通常用于表示部分选中状态。
         *
         * @param indeterminate 是否为不确定状态
         */
        @Override
        public void setIndeterminate(boolean indeterminate) {}

        /**
         * 检查操作是否被取消
         * <p>
         * 如果取消标志为 true，则抛出 ProcessCanceledException 异常，表示操作已取消。
         *
         * @throws com.intellij.openapi.progress.ProcessCanceledException 当操作被取消时抛出
         * @since 1.0
         */
        @Override
        public void checkCanceled() throws com.intellij.openapi.progress.ProcessCanceledException {
            if (canceled) {
                throw new com.intellij.openapi.progress.ProcessCanceledException();
            }
        }

        /**
         * 判断弹窗是否已展示
         * <p>
         * 返回弹窗是否已经展示的状态
         *
         * @return 如果弹窗已展示返回 true，否则返回 false
         */
        @Override
        public boolean isPopupWasShown() {
            return false;
        }

        /**
         * 判断当前视图是否正在显示
         * <p>
         * 该方法用于检查当前视图是否处于显示状态
         *
         * @return 如果视图正在显示则返回 true，否则返回 false
         */
        @Override
        public boolean isShowing() {
            return false;
        }
    }
}

