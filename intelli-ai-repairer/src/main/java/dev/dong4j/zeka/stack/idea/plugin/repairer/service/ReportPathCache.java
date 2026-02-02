package dev.dong4j.zeka.stack.idea.plugin.repairer.service;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;

import java.util.Collections;
import java.util.List;

import lombok.Getter;

/**
 * Cache for last scanned report paths.
 */
@Getter
@Service(Service.Level.PROJECT)
public final class ReportPathCache {
    private volatile List<String> checkstylePaths = Collections.emptyList();
    private volatile List<String> pmdPaths = Collections.emptyList();

    public static ReportPathCache getInstance(Project project) {
        return project.getService(ReportPathCache.class);
    }

    public void update(List<String> checkstylePaths, List<String> pmdPaths) {
        this.checkstylePaths = checkstylePaths == null ? Collections.emptyList() : List.copyOf(checkstylePaths);
        this.pmdPaths = pmdPaths == null ? Collections.emptyList() : List.copyOf(pmdPaths);
    }
}
