package dev.dong4j.zeka.stack.idea.plugin.changelog.action;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import dev.dong4j.zeka.stack.idea.plugin.changelog.service.ChangelogService;

/**
 * Git Log 工具窗口中生成工作周报的 Action
 */
public class GenerateWeeklyReportForGitLogAction extends AbstractGitLogAction {

    /**
     * 获取用于生成周报的 Git 日志操作的文本键
     * <p>
     * 返回一个固定字符串, 用于标识生成周报时涉及 Git 日志的操作
     *
     * @return 固定的文本键 "action.generate.weekly.report.gitlog"
     */
    @Override
    protected @NotNull String getTextKey() {
        return "action.generate.weekly.report.gitlog";
    }

    /**
     * 获取描述键用于生成周报的 Git 日志操作描述
     * <p>
     * 返回一个固定的描述键字符串, 用于国际化显示操作描述信息
     *
     * @return 描述键字符串
     */
    @Override
    protected @NotNull String getDescriptionKey() {
        return "action.generate.weekly.report.gitlog.description";
    }

    /**
     * 获取进度标题的资源键
     * <p>
     * 返回用于显示进度标题的国际化资源键字符串
     *
     * @return 进度标题的资源键
     */
    @Override
    protected @NotNull String getProgressTitleKey() {
        return "action.generate.weekly.report.gitlog.progress.title";
    }

    /**
     * 获取进度文本键
     * <p>
     * 返回用于显示进度信息的文本键, 通常用于国际化资源加载.
     *
     * @return 进度文本键
     */
    @Override
    protected @NotNull String getProgressTextKey() {
        return "action.generate.weekly.report.gitlog.progress.text";
    }

    /**
     * 重写父类方法, 获取错误键
     * <p>
     * 返回用于标识生成周报时 Git 日志错误的错误键
     *
     * @return 错误键
     * @since 1.0
     */
    @Override
    protected @NotNull String getErrorKey() {
        return "action.generate.weekly.report.gitlog.error";
    }

    /**
     * 生成每周报告内容
     * <p>
     * 该方法通过 {@link ChangelogService#generateWeeklyReport(java.util.List)} 生成
     * 指定提交哈希列表对应的每周报告字符串.
     *
     * @param service      用于生成报告的 {@link ChangelogService} 实例
     * @param commitHashes 提交哈希列表, 报告将基于这些提交生成
     * @return 生成的报告内容字符串
     * @throws Exception 生成过程中可能抛出的任何异常
     */
    @Override
    protected @NotNull String generateContent(@NotNull ChangelogService service,
                                              @NotNull List<String> commitHashes) throws Exception {
        return service.generateWeeklyReport(commitHashes);
    }
}

