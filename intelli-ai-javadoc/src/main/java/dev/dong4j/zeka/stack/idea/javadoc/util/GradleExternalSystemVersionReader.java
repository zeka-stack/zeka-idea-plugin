package dev.dong4j.zeka.stack.idea.javadoc.util;

import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ExternalProjectInfo;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager;
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsManager;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import lombok.extern.slf4j.Slf4j;

/**
 * 从 External System 读取 Gradle 工程版本.
 * <p>
 * 2026.3 起 {@code intellij.platform.externalSystem} 使用独立 classloader.
 * 这些类型如果留在 {@link ProjectVersionResolver} 的常量池中, 模块不可见时
 * 整个解析类会在链接阶段失败, pom.xml / build.gradle 兜底也无法执行.
 * 因此本类只能由调用方通过 {@code Class.forName} 加载.
 *
 * @author dong4j
 * @version 2026.3.1000
 * @since 2026.3.1000
 */
@Slf4j
public final class GradleExternalSystemVersionReader {
    private GradleExternalSystemVersionReader() {
    }

    /**
     * 读取已导入 Gradle 工程的版本号.
     *
     * @param project 当前项目
     * @return 版本号; External System 没有项目数据时返回 null
     */
    @Nullable
    public static String resolve(@NotNull Project project) {
        ExternalProjectsManager externalProjectsManager = ExternalProjectsManager.getInstance(project);
        if (externalProjectsManager == null) {
            log.debug("ExternalProjectsManager 不可用");
            return null;
        }

        ProjectDataManager projectDataManager = ProjectDataManager.getInstance();
        if (projectDataManager == null) {
            log.debug("ProjectDataManager 不可用");
            return null;
        }

        ProjectSystemId gradleSystemId = new ProjectSystemId("GRADLE");
        String externalProjectPath = ProjectVersionResolver.externalProjectPath(project);
        if (externalProjectPath == null || externalProjectPath.isEmpty()) {
            log.debug("外部项目路径为空");
            return null;
        }

        ExternalProjectInfo projectInfo = projectDataManager
            .getExternalProjectData(project, gradleSystemId, externalProjectPath);
        if (projectInfo == null) {
            log.debug("ExternalProjectInfo 为空");
            return null;
        }

        DataNode<ProjectData> projectDataNode = projectInfo.getExternalProjectStructure();
        if (projectDataNode == null) {
            log.debug("ExternalProjectStructure 为空");
            return null;
        }

        ProjectData projectData = projectDataNode.getData();
        if (projectData == null) {
            log.debug("ProjectData 为空");
            return null;
        }

        String version = projectData.getVersion();
        if (version == null || version.isEmpty()) {
            log.debug("ProjectData 未提供 version");
            return null;
        }
        return version;
    }
}
