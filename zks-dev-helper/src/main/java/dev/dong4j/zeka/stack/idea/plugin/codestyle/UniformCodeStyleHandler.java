package dev.dong4j.zeka.stack.idea.plugin.codestyle;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;

import dev.dong4j.zeka.stack.idea.plugin.settings.state.CodeStyleSettingsState;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import lombok.extern.slf4j.Slf4j;

/**
 * 代码样式处理器
 * <p>
 * 该处理器用于在项目启动时执行代码样式配置任务。根据配置决定是否启用代码样式设置，若启用则提供统一的代码风格方案，并记录相关使用统计信息。
 * 这是 ZKS Dev Helper 插件中代码样式模块的核心组件。
 *
 * @author dong4j
 * @version 1.0.0
 * @date 2025.10.25
 * @since 1.0.0
 */
@Slf4j
public class UniformCodeStyleHandler implements ProjectActivity {
    /**
     * 表示插件更新检查是否已经运行过的标志
     * <p>
     * 使用 AtomicBoolean 来确保线程安全, 避免在多线程环境中出现竞态条件.
     */
    private final AtomicBoolean hasRun = new AtomicBoolean(false);

    @Override
    public @Nullable Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        try {
            // 只在第一次运行时检查更新
            if (hasRun.compareAndSet(false, true) && !ApplicationManager.getApplication().isUnitTestMode()) {
                CodeStyleSettingsState settings = CodeStyleSettingsState.getInstance();
                if (!settings.isEnableCodeStyle()) {
                    log.info("Code style disabled, skipping configuration");
                    return Unit.INSTANCE;
                }

                // 检查自动更新配置
                CodeStyleSettingsState.CodeStyleUpdateSettings updateSettings =
                    settings.getCodeStyleUpdateSettings();
                if (updateSettings != null && updateSettings.isAutoUpdate() &&
                    updateSettings.getDownloadUrl() != null && !updateSettings.getDownloadUrl().trim().isEmpty()) {
                    // 后台检查并更新
                    ApplicationManager.getApplication().executeOnPooledThread(() -> {
                        try {
                            ProgressIndicator indicator = new EmptyProgressIndicator();
                            CodeStyleDownloadManager.checkAndUpdate(
                                project,
                                updateSettings.getDownloadUrl().trim(), // baseUrl
                                indicator,
                                null // 启动时不需要显示进度
                                                                   );
                        } catch (Exception e) {
                            log.error("Failed to check and update code style", e);
                        }
                    });
                }

                // 提供统一代码风格方案（会优先使用本地下载的文件）
                UniformCodeStyleSchemeProvider.provideUniformCodeStyleScheme(project);

                // 报告使用统计
                // StatisticsUtil.reportCodeStyleUsage();
            }
        } catch (Exception e) {
            log.error("Failed to configure uniform code style for project: {}", project.getName(), e);
        }
        return Unit.INSTANCE;
    }
}
