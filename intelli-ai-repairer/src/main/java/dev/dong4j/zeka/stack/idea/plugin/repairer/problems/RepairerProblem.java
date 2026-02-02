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
 * 修复器问题实现类
 * <p> 实现了 {@link FileProblem} 接口, 用于表示代码分析工具检测到的具体违规问题.
 * 该类封装了违规代码的详细信息, 包括问题提供者, 代码违规对象以及目标文件, 并提供了获取问题描述, 严重程度图标, 文件位置等信息的实现.
 *
 * @param provider  问题提供者, 用于获取问题列表和上下文信息
 * @param violation 违规问题的具体内容, 包含规则 ID, 消息, 严重等级及位置信息
 * @param file      关联的虚拟文件对象, 表示问题所在的源文件
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.02.02
 * @since 1.0.0
 */
public record RepairerProblem(ProblemsProvider provider, CodeViolation violation, VirtualFile file) implements FileProblem {
    /**
     * 构造函数, 用于初始化修复器问题对象
     * <p> 创建一个包含问题提供者, 违规信息和关联文件的修复器问题实例
     *
     * @param provider  问题提供者, 用于获取问题列表和上下文信息
     * @param violation 违规问题的具体内容, 包含规则 ID, 消息, 严重等级及位置信息
     * @param file      关联的虚拟文件对象, 表示问题所在的源文件
     */
    public RepairerProblem(@NotNull ProblemsProvider provider,
                           @NotNull CodeViolation violation,
                           @NotNull VirtualFile file) {
        this.provider = provider;
        this.violation = violation;
        this.file = file;
    }

    /**
     * 获取当前问题对应的代码违规信息
     *
     * @return 代码违规信息
     */
    @Override
    public @NotNull CodeViolation violation() {
        return violation;
    }

    /**
     * 获取问题提供者
     * <p> 返回当前问题对应的提供者对象
     *
     * @return 问题提供者对象
     */
    @Override
    public @NotNull ProblemsProvider provider() {
        return provider;
    }

    /**
     * 获取问题的文本描述
     * <p> 根据违规信息组合消息内容、位置和规则信息, 便于在问题面板中直接展示定位信息
     *
     * @return 格式化后的问题文本
     */
    @Override
    public @NotNull String getText() {
        String message = safeText(violation.message, "");
        String rule = safeText(violation.ruleId, "Rule");
        String line = formatLocationValue(violation.startLine);
        String column = formatLocationValue(violation.startColumn);
        if (message.isBlank()) {
            message = rule;
        }
        return String.format("%s (%s:%s) [%s]", message, line, column, rule);
    }

    /**
     * 获取问题所属的组别
     * <p> 返回与该问题相关联的工具名称, 若未定义则返回 null.
     *
     * @return 问题所属的组别名称, 若不存在则返回 null
     */
    @Override
    public @Nullable String getGroup() {
        return violation.tool;
    }

    /**
     * 获取上下文分组.
     * <p> 此实现始终返回 {@code null}, 表示该问题不属于任何 {@link CodeInsightContext} 分组.
     *
     * @return {@code null}
     */
    @Override
    public @Nullable CodeInsightContext getContextGroup() {
        return null;
    }

    /**
     * 获取描述信息
     * <p> 返回 {@link CodeViolation} 对象中的 {@code message} 字段; 若 {@code violation.message} 为 {@code null}, 则返回空字符串.
     *
     * @return 消息文本; 若为空则返回空字符串
     */
    @Override
    public @NotNull String getDescription() {
        return violation.message == null ? "" : violation.message;
    }

    /**
     * 获取与违规严重程度对应的图标
     * <p> 根据 {@code violation.severity} 的值返回对应的图标:
     * <ul>
     *   <li> 小于等于 1: 错误图标 </li>
     *   <li> 等于 2: 警告图标 </li>
     *   <li> 大于等于 4: 信息图标 </li>
     *   <li> 其他情况: 警告图标 </li>
     * </ul>
     *
     * @return 表示严重程度的图标
     */
    @Override
    public @NotNull Icon getIcon() {
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
     * 获取关联的虚拟文件
     *
     * @return 关联的 {@link VirtualFile} 对象
     */
    @Override
    public @NotNull VirtualFile file() {
        return file;
    }

    /**
     * 获取问题所在行号 (从 0 开始)
     * <p> 根据违规信息中的起始行号计算并返回对应的行索引, 若起始行号小于等于 0 则返回 0
     *
     * @return 问题所在行号 (0 起始索引)
     */
    @Override
    public int getLine() {
        return violation.startLine > 0 ? violation.startLine - 1 : 0;
    }

    /**
     * 获取违规信息在文件中的列位置 (从 0 开始)
     * <p> 根据违规起始列号计算实际显示列位置, 若起始列号小于等于 0, 则返回 0
     *
     * @return 列位置 (从 0 开始的索引)
     */
    @Override
    public int getColumn() {
        return violation.startColumn > 0 ? violation.startColumn - 1 : 0;
    }

    /**
     * 安全获取文本内容
     * <p> 如果输入的字符串为 null 或空白, 则返回备用值, 否则返回去除前后空格的字符串
     *
     * @param value    输入的字符串, 可能为 null 或空白
     * @param fallback 备用值, 当输入字符串无效时返回
     * @return 处理后的字符串, 如果输入无效则返回备用值
     */
    private static String safeText(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    /**
     * 格式化位置值
     * <p> 将给定的整数值转换为字符串表示, 如果值小于等于 0, 则返回字符串 "0"
     *
     * @param value 需要格式化的整数值
     * @return 格式化后的字符串, 如果输入值大于 0 则返回其字符串形式, 否则返回 "0"
     */
    private static String formatLocationValue(int value) {
        return value > 0 ? Integer.toString(value) : "0";
    }

    /**
     * 获取关联的虚拟文件
     *
     * @return 关联的 {@link VirtualFile} 对象
     */
    @Override
    public @NotNull VirtualFile getFile() {
        return file;
    }

    /**
     * 获取当前问题对应的提供者对象
     * <p> 返回当前问题对应的提供者对象, 用于获取问题列表和上下文信息
     *
     * @return 问题提供者对象
     */
    @Override
    public @NotNull ProblemsProvider getProvider() {
        return provider;
    }
}
