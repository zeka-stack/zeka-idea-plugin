package dev.dong4j.zeka.stack.idea.plugin.codestyle;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;

import org.jetbrains.annotations.NotNull;

import dev.dong4j.zeka.stack.idea.plugin.settings.UniformFormatSettingsState;
import dev.dong4j.zeka.stack.idea.plugin.util.StatisticsUtil;
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
public class UniformCodeStyleHandler implements StartupActivity {
    /**
     * 执行项目级别的代码样式配置任务
     * <p>
     * 该方法用于根据项目配置，执行代码样式的设置操作。如果代码样式功能被禁用，则直接返回。否则，提供统一代码风格方案，并记录使用统计信息。
     *
     * @param project 项目对象，用于获取项目相关信息和执行配置操作
     */
    @Override
    public void runActivity(@NotNull Project project) {
        try {
            UniformFormatSettingsState settings = UniformFormatSettingsState.getInstance();
            if (!settings.isEnableCodeStyle()) {
                log.info("Code style disabled, skipping configuration");
                return;
            }

            // 提供统一代码风格方案
            UniformCodeStyleSchemeProvider.provideUniformCodeStyleScheme(project);

            // 报告使用统计
            StatisticsUtil.reportCodeStyleUsage();

        } catch (Exception e) {
            log.error("Failed to configure uniform code style for project: {}", project.getName(), e);
        }
    }

}
