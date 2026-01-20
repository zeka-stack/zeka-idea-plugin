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
 * MavenReportListener
 * <p> 用于监听 Maven 构建过程, 并在构建终止时处理报告结果. 主要功能包括检测构建是否成功, 判断是否包含代码分析目标 (如 Checkstyle,PMD), 加载并解析相关报告文件, 收集代码违规信息, 并更新到缓存中, 最后重启
 * DaemonCodeAnalyzer 并通知用户.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.20
 * @since 1.0.0
 */
public final class MavenReportListener {
    /**
     * 私有构造方法
     * <p> 用于防止外部实例化当前工具类, 该类仅包含静态方法 </p>
     *
     * @since 1.0.0
     */
    private MavenReportListener() {
    }

    /**
     * 注册 Maven 执行监听器, 用于在构建完成后加载静态分析报告
     * <p> 该方法通过动态代理创建一个 MavenExecutionListener 实例, 并将其注册到 MavenExecutionManager 中.
     * 当 Maven 构建过程结束时 (processTerminated 被调用), 会触发 {@link #handleTerminated(Project, Object)} 方法,
     * 从而加载 Checkstyle 和 PMD 的分析结果并刷新代码检查器.
     *
     * @param project IntelliJ IDEA 的项目实例
     */
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

    /**
     * 处理 Maven 构建终止事件, 加载静态分析报告并更新违规缓存
     * <p> 当 Maven 构建终止且包含静态分析目标时, 加载 Checkstyle 和 PMD 报告, 收集代码违规信息, 并更新项目违规缓存及重启分析器.
     *
     * @param project 项目对象, 用于获取基础路径和通知工具
     * @param result  构建结果对象, 用于判断构建是否成功以及是否包含分析目标
     */
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

    /**
     * 判断给定的结果对象是否表示成功状态
     * <p> 通过反射调用结果对象的 `isSuccessful` 方法来确定执行结果是否成功.
     * 如果结果对象没有 `isSuccessful` 方法, 则认为结果不成功.
     *
     * @param result 结果对象
     * @return 如果结果表示成功状态则返回 true, 否则返回 false
     */
    private static boolean isSuccessful(Object result) {
        try {
            Method method = result.getClass().getMethod("isSuccessful");
            Object value = method.invoke(result);
            return value instanceof Boolean && (Boolean) value;
        } catch (Throwable e) {
            return false;
        }
    }

    /**
     * 检查 Maven 执行结果是否包含分析目标
     * <p> 该方法尝试从执行结果对象中获取目标列表, 并检查其中是否包含特定的分析目标 (如 checkstyle,pmd 或 verify).
     *
     * @param result Maven 执行结果对象
     * @return 如果结果包含分析目标, 则返回 true; 否则返回 false
     */
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

    /**
     * 加载 Checkstyle 静态分析报告
     * <p> 从项目的 target/checkstyle/checkstyle.xml 文件中解析 Checkstyle 报告, 并返回包含代码违规的信息列表.
     * 如果文件不存在, 则返回一个空列表.
     *
     * @param project 当前项目对象
     * @return 包含代码违规信息的列表, 如果文件不存在则返回空列表
     */
    private static List<CodeViolation> loadCheckstyle(Project project) {
        File report = new File(project.getBasePath(), "target/checkstyle/checkstyle.xml");
        if (!report.exists()) {
            return List.of();
        }
        return new CheckstyleXmlAdapter().parse(report);
    }

    /**
     * 加载 PMD 静态分析报告文件并解析为代码违规列表
     * <p> 根据项目路径查找 PMD 报告文件 (target/pmd/pmd.xml), 如果文件存在则使用 PmdXmlAdapter 解析为 CodeViolation 列表, 否则返回空列表.
     *
     * @param project 项目对象, 用于获取基础路径
     * @return 包含代码违规信息的列表, 若文件不存在则返回空列表
     */
    private static List<CodeViolation> loadPmd(Project project) {
        File report = new File(project.getBasePath(), "target/pmd/pmd.xml");
        if (!report.exists()) {
            return List.of();
        }
        return new PmdXmlAdapter().parse(report);
    }
}
