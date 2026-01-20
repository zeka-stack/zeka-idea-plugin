package dev.dong4j.zeka.stack.idea.plugin.repairer.maven;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import dev.dong4j.zeka.stack.idea.plugin.common.util.NotificationUtil;
import dev.dong4j.zeka.stack.idea.plugin.repairer.adapter.CheckstyleXmlAdapter;
import dev.dong4j.zeka.stack.idea.plugin.repairer.adapter.PmdXmlAdapter;
import dev.dong4j.zeka.stack.idea.plugin.repairer.service.ViolationCache;
import dev.dong4j.zeka.stack.idea.plugin.repairer.util.RepairerBundle;
import dev.dong4j.zeka.stack.idea.plugin.repairer.violation.CodeViolation;

/**
 * 监听 Maven 执行并加载静态分析报告.
 */
public final class MavenReportListener {
    private MavenReportListener() {
    }

    public static void register(@NotNull Project project) {
        try {
            Class<?> managerClass = Class.forName("org.jetbrains.idea.maven.execution.MavenExecutionManager");
            Class<?> listenerClass = Class.forName("org.jetbrains.idea.maven.execution.MavenExecutionListener");
            Object manager = managerClass.getMethod("getInstance", Project.class).invoke(null, project);
            Object listener = Proxy.newProxyInstance(
                listenerClass.getClassLoader(),
                new Class<?>[] {listenerClass},
                (proxy, method, args) -> {
                    if ("processTerminated".equals(method.getName()) && args != null && args.length == 1) {
                        handleTerminated(project, args[0]);
                    }
                    return null;
                }
                                                    );
            Method addListener = managerClass.getMethod("addExecutionListener", listenerClass, Project.class);
            addListener.invoke(manager, listener, project);
        } catch (Throwable ignored) {
        }
    }

    private static void handleTerminated(Project project, Object result) {
        if (!isSuccessful(result)) {
            return;
        }
        if (!containsAnalysisGoal(result)) {
            return;
        }
        List<CodeViolation> violations = new ArrayList<>();
        violations.addAll(loadCheckstyle(project));
        violations.addAll(loadPmd(project));
        ViolationCache.getInstance(project).setAll(violations);
        DaemonCodeAnalyzer.getInstance(project).restart();
        NotificationUtil.showInfo(project, RepairerBundle.message("notify.report.refreshed", violations.size()));
    }

    private static boolean isSuccessful(Object result) {
        try {
            Method method = result.getClass().getMethod("isSuccessful");
            Object value = method.invoke(result);
            return value instanceof Boolean && (Boolean) value;
        } catch (Throwable e) {
            return false;
        }
    }

    private static boolean containsAnalysisGoal(Object result) {
        try {
            Method method = result.getClass().getMethod("getGoals");
            Object value = method.invoke(result);
            if (!(value instanceof List<?> goals)) {
                return false;
            }
            return goals.stream().anyMatch(goal -> {
                String lower = String.valueOf(goal).toLowerCase(Locale.ROOT);
                return lower.contains("checkstyle")
                       || lower.contains("pmd")
                       || lower.endsWith("verify");
            });
        } catch (Throwable e) {
            return false;
        }
    }

    private static List<CodeViolation> loadCheckstyle(Project project) {
        File report = new File(project.getBasePath(), "target/checkstyle/checkstyle.xml");
        if (!report.exists()) {
            return List.of();
        }
        return new CheckstyleXmlAdapter().parse(report);
    }

    private static List<CodeViolation> loadPmd(Project project) {
        File report = new File(project.getBasePath(), "target/pmd/pmd.xml");
        if (!report.exists()) {
            return List.of();
        }
        return new PmdXmlAdapter().parse(report);
    }
}
