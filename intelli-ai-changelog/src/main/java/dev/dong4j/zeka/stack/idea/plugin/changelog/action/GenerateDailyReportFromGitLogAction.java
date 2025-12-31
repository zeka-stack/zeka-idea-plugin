package dev.dong4j.zeka.stack.idea.plugin.changelog.action;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import javax.swing.Icon;

import dev.dong4j.zeka.stack.idea.plugin.changelog.service.ChangelogService;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIStreamResponseListener;
import icons.ChangelogIcons;

/**
 * 用于生成每日 Git 日志报告的操作类
 * <p>
 * 该类继承自 AbstractGitLogAction, 负责生成基于 Git 提交记录的每日报告内容, 主要功能包括设置图标, 文本资源键以及生成报告内容.
 * 适用于在集成开发环境中执行 Git 日志分析并生成每日报告的场景.
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email mailto:zeka.stack@gmail.com
 * @date 2025.11.30
 * @since 1.0.0
 */
public class GenerateDailyReportFromGitLogAction extends AbstractGitLogAction {

    /**
     * 获取图标对象
     * <p>
     * 返回一个预定义的图标对象, 用于表示每日变更日志.
     *
     * @return 图标对象
     */
    @NotNull
    protected Icon getIcon() {
        return ChangelogIcons.DAILY;
    }

    /**
     * 获取用于生成每日报告的文本键
     * <p>
     * 返回一个固定的文本键, 用于标识生成每日报告的 Git 日志相关操作
     *
     * @return 文本键
     */
    @Override
    protected @NotNull String getTextKey() {
        return "action.generate.daily.report.gitlog";
    }

    /**
     * 获取描述键用于生成每日报告的 Git 日志操作描述
     * <p>
     * 该方法返回一个描述键字符串, 用于在界面或日志中标识生成每日报告的 Git 日志操作.
     *
     * @return 描述键字符串
     */
    @Override
    protected @NotNull String getDescriptionKey() {
        return "action.generate.daily.report.gitlog.description";
    }

    /**
     * 获取生成每日报告进度标题的国际化键值
     * <p>
     * 该方法用于返回生成每日报告进度标题所对应的国际化资源键值, 用于支持多语言显示.
     *
     * @return 国际化键值字符串
     * @since 1.0
     */
    @Override
    protected @NotNull String getProgressTitleKey() {
        return "action.generate.daily.report.gitlog.progress.title";
    }

    /**
     * 获取进度文本的资源键
     * <p>
     * 返回用于显示进度信息的资源键字符串, 该键用于从资源文件中查找对应的进度文本.
     *
     * @return 进度文本的资源键
     */
    @Override
    protected @NotNull String getProgressTextKey() {
        return "action.generate.daily.report.gitlog.progress.text";
    }

    /**
     * 获取错误键用于生成每日报告的 Git 日志错误信息
     * <p>
     * 该方法用于返回一个预定义的错误键, 用于标识生成每日报告时出现的 Git 日志相关错误.
     *
     * @return 错误键字符串
     */
    @Override
    protected @NotNull String getErrorKey() {
        return "action.generate.daily.report.gitlog.error";
    }

    /**
     * 生成每日报告内容
     * <p>
     * 通过 {@link ChangelogService#generateDailyReport(java.util.List)} 方法, 使用给定的提交哈希列表生成报告字符串.
     *
     * @param service      用于生成报告的 {@link ChangelogService} 实例
     * @param commitHashes 提供的提交哈希列表
     * @return 生成的报告内容字符串
     * @throws Exception 生成过程中可能抛出的异常
     */
    @Override
    protected @NotNull String generateContent(@NotNull ChangelogService service,
                                              @NotNull List<String> commitHashes) throws Exception {
        return service.generateDailyReport(commitHashes);
    }

    @Override
    protected @NotNull String generateContentStream(@NotNull ChangelogService service,
                                                    @NotNull List<String> commitHashes,
                                                    @NotNull AIStreamResponseListener listener) throws Exception {
        return service.generateDailyReportStream(commitHashes, listener);
    }
}
