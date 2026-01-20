package dev.dong4j.zeka.stack.idea.plugin.repairer.service;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;

import java.util.Collections;
import java.util.List;

import dev.dong4j.zeka.stack.idea.plugin.repairer.violation.CodeViolation;

/**
 * 项目级违规模型缓存.
 */
@Service(Service.Level.PROJECT)
public final class ViolationCache {
    private volatile List<CodeViolation> violations = Collections.emptyList();

    public static ViolationCache getInstance(Project project) {
        return project.getService(ViolationCache.class);
    }

    public List<CodeViolation> getAll() {
        return violations;
    }

    public void setAll(List<CodeViolation> violations) {
        this.violations = violations == null ? Collections.emptyList() : List.copyOf(violations);
    }
}
