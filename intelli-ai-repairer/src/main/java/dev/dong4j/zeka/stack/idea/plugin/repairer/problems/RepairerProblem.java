package dev.dong4j.zeka.stack.idea.plugin.repairer.problems;

import com.intellij.analysis.problemsView.FileProblem;
import com.intellij.analysis.problemsView.ProblemsProvider;
import com.intellij.codeInsight.multiverse.CodeInsightContext;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.vfs.VirtualFile;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

import dev.dong4j.zeka.stack.idea.plugin.repairer.violation.CodeViolation;

/**
 * Problems view entry for a single Repairer violation.
 *
 * @param provider  提供问题的提供者
 *                  <p> 用于获取与当前问题相关的提供者信息
 * @param violation 违规信息对象
 *                  <p> 表示代码中检测到的违规记录, 包含规则 ID, 严重程度, 消息等信息
 * @param file      文件对象
 *                  <p> 表示与当前问题相关的虚拟文件
 */
public record RepairerProblem(ProblemsProvider provider, CodeViolation violation, VirtualFile file) implements FileProblem {
    /**
     * 初始化一个 Repairer 问题视图条目
     * <p> 用于封装单个修复者违规问题的视图入口, 包含问题提供者, 违规信息和文件引用
     *
     * @param provider  问题提供者, 非空
     * @param violation 违规信息, 非空
     * @param file      关联的虚拟文件, 非空
     */
    public RepairerProblem(@NotNull ProblemsProvider provider,
                           @NotNull CodeViolation violation,
                           @NotNull VirtualFile file) {
        this.provider = provider;
        this.violation = violation;
        this.file = file;
    }

    /**
     * 获取与该问题相关的代码违规信息
     * <p> 返回表示此问题的代码违规对象
     *
     * @return 不可为空的 {@code CodeViolation} 对象, 表示该问题的具体违规信息
     */
    @Override
    public @NotNull CodeViolation violation() {
        return violation;
    }

    /**
     * 获取问题提供者
     * <p> 返回当前问题实例所关联的 ProblemsProvider 对象, 用于后续问题的处理和展示
     *
     * @return 问题提供者对象, 非空
     */
    @Override
    public @NotNull ProblemsProvider provider() {
        return provider;
    }

    /**
     * 获取问题描述文本
     * <p> 根据违反规则的标识符返回相应的文本描述, 若规则标识符为空或空白, 则返回默认文本 "Rule"
     *
     * @return 问题描述文本
     */
    @Override
    public @NotNull String getText() {
        return violation.ruleId == null || violation.ruleId.isBlank() ? "Rule" : violation.ruleId;
    }

    /**
     * 获取问题的分组信息
     * <p> 返回当前违规项关联的工具名称作为分组标识, 若无工具信息则返回 null
     *
     * @return 工具名称作为分组标识, 若无工具信息则返回 null
     */
    @Override
    public @Nullable String getGroup() {
        return violation.tool;
    }

    /**
     * 获取与该问题关联的代码洞察上下文组
     * <p> 此方法返回一个表示代码洞察上下文组的对象, 但在此实现中始终返回 null.
     *
     * @return 与该问题关联的代码洞察上下文组, 若不存在则返回 null
     */
    @Override
    public @Nullable CodeInsightContext getContextGroup() {
        return null;
    }

    /**
     * 获取问题描述信息
     * <p> 根据违规信息中的 message 字段返回描述内容, 若 message 为 null 则返回空字符串
     *
     * @return 问题描述字符串, 若 message 为 null 则返回空字符串
     */
    @Override
    public @NotNull String getDescription() {
        return violation.message == null ? "" : violation.message;
    }

    /**
     * 获取问题的图标
     * <p> 根据问题的严重程度返回相应的图标. 严重程度小于等于 1 时返回错误图标, 等于 2 时返回警告图标, 大于等于 4 时返回信息图标, 其余情况返回警告图标.
     *
     * @return 问题的图标, 可能为 null
     */
    @Override
    public @Nullable Icon getIcon() {
        int severity = violation.severity;
        if (severity <= 1) {
            return AllIcons.General.Error;
        }
        if (severity == 2) {
            return AllIcons.General.Warning;
        }
        if (severity >= 4) {
            return AllIcons.General.Information;
        }
        return AllIcons.General.Warning;
    }

    /**
     * 获取问题关联的虚拟文件
     * <p> 返回当前问题实例所关联的虚拟文件对象, 用于定位问题所在文件
     *
     * @return 虚拟文件对象, 非空
     */
    @Override
    public @NotNull VirtualFile file() {
        return file;
    }

    /**
     * 获取问题所在的行号, 返回值为 0 基准.
     * <p> 根据 {@code violation.startLine} 计算行号, 若 {@code violation.startLine} 小于等于 0, 则返回 0;
     * 否则返回 {@code violation.startLine - 1}, 并确保不小于 0.
     *
     * @return 行号 (从 0 开始)
     */
    @Override
    public int getLine() {
        int line = violation.startLine > 0 ? violation.startLine - 1 : 0;
        return Math.max(0, line);
    }

    /**
     * 获取违规信息在文件中的列位置 (从 0 开始)
     * <p> 根据违规起始列号计算实际列索引, 若起始列号小于等于 0, 则返回 0</p>
     *
     * @return 列位置, 确保最小值为 0
     */
    @Override
    public int getColumn() {
        int column = violation.startColumn > 0 ? violation.startColumn - 1 : 0;
        return Math.max(0, column);
    }
}
