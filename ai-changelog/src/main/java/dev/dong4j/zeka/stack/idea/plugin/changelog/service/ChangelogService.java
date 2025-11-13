package dev.dong4j.zeka.stack.idea.plugin.changelog.service;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/**
 * 示例服务类
 * 使用 @Service 注解，IntelliJ 会自动管理其生命周期
 */
@Service(Service.Level.PROJECT)
public final class ChangelogService {

    private final Project project;

    public ChangelogService(@NotNull Project project) {
        this.project = project;
    }

    /**
     * 获取服务实例
     */
    public static ChangelogService getInstance(@NotNull Project project) {
        return project.getService(ChangelogService.class);
    }

    /**
     * 处理文本内容
     */
    public String processText(@NotNull String text, int offset) {
        // 示例处理逻辑
        return String.format("Processed text at offset %d, length: %d", offset, text.length());
    }

    /**
     * 处理文件
     */
    public String processFile(@NotNull PsiFile psiFile) {
        // 示例处理逻辑
        return String.format("Processed file: %s, size: %d bytes", 
                psiFile.getName(), psiFile.getText().length());
    }

    /**
     * 处理选中的文件
     */
    public String processSelection(@NotNull VirtualFile[] files) {
        // 示例处理逻辑
        return String.format("Processed %d files: %s", 
                files.length, Arrays.toString(Arrays.stream(files)
                        .map(VirtualFile::getName)
                        .toArray(String[]::new)));
    }

    /**
     * 处理 Intention Action
     */
    public String processIntention(@NotNull PsiFile psiFile) {
        // 示例处理逻辑
        return String.format("Intention processed for file: %s", psiFile.getName());
    }

    /**
     * 获取项目信息
     */
    public String getProjectInfo() {
        return String.format("Project: %s, Base Path: %s", 
                project.getName(), project.getBasePath());
    }
}
