package dev.dong4j.zeka.stack.idea.plugin.repairer.checkstyle;

import com.intellij.codeInspection.LocalQuickFix;

import org.infernus.idea.checkstyle.checker.CheckstyleQuickFixProvider;
import org.infernus.idea.checkstyle.checker.Problem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * AI 检查样式快速修复提供程序
 * <p> 实现 CheckstyleQuickFixProvider 接口, 提供针对特定问题的本地快速修复功能.
 * 该类主要用于根据问题信息生成相应的修复建议, 以便开发者可以快速解决代码中的问题.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.20
 * @since 1.0.0
 */
public class AICheckstyleQuickFixProvider implements CheckstyleQuickFixProvider {

    /**
     * 获取用于修复 Checkstyle 违规问题的快速修复方案
     * <p> 根据传入的 Problem 对象判断是否可修复, 若可修复则返回对应的快速修复方案数组
     *
     * @param problem 包含违规信息的对象, 用于判断是否可修复
     * @return 修复方案数组, 若不可修复则返回 null
     */
    @Override
    @Nullable
    public LocalQuickFix[] getQuickFixes(@NotNull Problem problem) {
        if (!isFixable(problem)) {
            return null;
        }
        return new LocalQuickFix[] {new AICheckstyleFix(problem)};
    }

    /**
     * 判断给定 {@link Problem} 是否可修复.
     *
     * <p> 若问题的源文件名, 错误信息和目标位置均不为 {@code null}, 则认为该问题可被 AI 自动修复, 否则认为不可修复.</p>
     *
     * @param problem 当前检查到的错误信息
     * @return 若问题的源文件名, 错误信息和目标位置均不为 {@code null}, 则返回 {@code true}; 否则返回 {@code false}.
     */
    private boolean isFixable(@NotNull Problem problem) {
        return problem.sourceName() != null && problem.message() != null && problem.target() != null;
    }
}
