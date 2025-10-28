package dev.dong4j.zeka.stack.idea.plugin.task;

import com.intellij.openapi.progress.ProgressIndicator;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import dev.dong4j.zeka.stack.idea.plugin.MyBasePlatformTestCase;
import dev.dong4j.zeka.stack.idea.plugin.ai.provider.AIServiceProvider;
import dev.dong4j.zeka.stack.idea.plugin.settings.SettingsState;

/**
 * TaskExecutor ProviderStatistics 弹出框 UI 测试
 * <p>
 * 用于直接显示统计信息弹出框，方便查看和调整 HTML 样式
 *
 * @author Cursor AI Assistant
 * @version 1.0
 * @date 2025.01.17
 */
public class TaskExecutorProviderStatisticsUITest extends MyBasePlatformTestCase {

    /** 任务执行器 */
    private TaskExecutor taskExecutor;
    /** 模拟进度指示器 */
    private ProgressIndicator mockIndicator;

    /**
     * 设置测试环境
     * <p>
     * 初始化必要的模拟对象和任务执行器
     *
     * @throws Exception 初始化异常
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();

        SettingsState settings = SettingsState.getInstance();

        // 创建 Mock ProgressIndicator
        mockIndicator = new MockProgressIndicator();

        // 创建 TaskExecutor
        taskExecutor = new TaskExecutor(getProject(), mockIndicator);

        // 注入 Mock AI Service
        injectMockAIService(taskExecutor);
    }

    /**
     * 测试显示统计信息弹出框
     * <p>
     * 创建模拟的统计数据，然后使用反射调用私有方法显示弹出框
     */
    public void testShowProviderStatisticsDialog() throws Exception {
        // 创建模拟的 ProviderStatistics 数据
        Map<String, TaskExecutor.ProviderStatistics> providerStats = createMockStatistics();

        // 使用反射调用私有方法
        Method method = TaskExecutor.class.getDeclaredMethod(
            "showProviderStatistics",
            Map.class);
        method.setAccessible(true);

        method.invoke(taskExecutor, providerStats);
        // 确保在 EDT 线程中运行
        com.intellij.util.ui.UIUtil.invokeAndWaitIfNeeded(
            (java.lang.Runnable) () -> {
                try {
                    method.invoke(taskExecutor, providerStats);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

        // 等待用户查看对话框
        System.out.println("对话框已显示，请查看样式效果...");
        System.out.println("关闭对话框后，测试将完成");
    }

    /**
     * 创建模拟统计数据
     * <p>
     * 创建多个提供商的模拟统计数据，用于展示不同的样式效果
     *
     * @return ProviderStatistics Map
     */
    private Map<String, TaskExecutor.ProviderStatistics> createMockStatistics() {
        Map<String, TaskExecutor.ProviderStatistics> stats = new HashMap<>();

        // 创建第一个提供商统计（较多任务）
        TaskExecutor.ProviderStatistics provider1 = new TaskExecutor.ProviderStatistics("QianWen 千问");
        provider1.incrementCompleted();
        provider1.incrementCompleted();
        provider1.incrementCompleted();
        provider1.incrementCompleted();
        provider1.incrementCompleted();
        provider1.incrementFailed();
        provider1.finish();
        stats.put("QianWen 千问", provider1);

        // 创建第二个提供商统计
        TaskExecutor.ProviderStatistics provider2 = new TaskExecutor.ProviderStatistics("Ollama (本地模型)");
        provider2.incrementCompleted();
        provider2.incrementCompleted();
        provider2.incrementSkipped();
        provider2.finish();
        stats.put("Ollama (本地模型)", provider2);

        // 创建第三个提供商统计（失败较多的场景）
        TaskExecutor.ProviderStatistics provider3 = new TaskExecutor.ProviderStatistics("Custom Provider");
        provider3.incrementCompleted();
        provider3.incrementFailed();
        provider3.incrementFailed();
        provider3.incrementSkipped();
        provider3.incrementSkipped();
        provider3.finish();
        stats.put("Custom Provider", provider3);

        return stats;
    }

    /**
     * 注入 Mock AI Service
     * <p>
     * 使用反射将 Mock AI Service 注入到 TaskExecutor 中
     *
     * @param executor TaskExecutor 实例
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
     * 模拟 AI 服务提供者
     */
    private static class MockAIServiceProvider implements AIServiceProvider {

        @NotNull
        @Override
        public String generateDocumentation(@NotNull String code,
                                            @NotNull DocumentationTask.TaskType type,
                                            @NotNull String language) {
            return "/**\n * Mock JavaDoc\n */";
        }

        @NotNull
        @Override
        public dev.dong4j.zeka.stack.idea.plugin.ai.ValidationResult validateConfiguration() {
            return dev.dong4j.zeka.stack.idea.plugin.ai.ValidationResult.success("for test");
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
        public java.util.List<String> getSupportedModels() {
            return java.util.List.of("mock-model");
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
        public java.util.List<String> getAvailableModels() {
            return java.util.List.of("mock-model-1", "mock-model-2");
        }
    }

    /**
     * 模拟进度指示器
     */
    private static class MockProgressIndicator implements ProgressIndicator {
        private boolean canceled = false;
        private double fraction = 0.0;
        private String text = "";

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
        }

        @Override
        public String getText() {
            return text;
        }

        @Override
        public void setText2(String text) {}

        @Override
        public String getText2() {
            return "";
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
        public com.intellij.openapi.application.ModalityState getModalityState() {
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

