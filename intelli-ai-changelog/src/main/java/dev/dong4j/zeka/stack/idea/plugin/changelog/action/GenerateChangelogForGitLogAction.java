package dev.dong4j.zeka.stack.idea.plugin.changelog.action;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import dev.dong4j.zeka.stack.idea.plugin.changelog.service.ChangelogService;

/**
 * Git Log 工具窗口中生成 Changelog 的 Action
 * <p>
 * 在 Git Log 工具窗口中，用户可以选择多条提交记录，然后右键选择此 Action 生成 Changelog
 */
public class GenerateChangelogForGitLogAction extends AbstractGitLogAction {

    /**
     * 获取用于生成提交日志的文本键
     * <p>
     * 返回一个预定义的文本键, 用于国际化处理, 通常用于生成提交日志的描述信息.
     *
     * @return 文本键字符串
     */
    @Override
    protected @NotNull String getTextKey() {
        return "action.generate.changelog.gitlog";
    }

    /**
     * 获取描述键用于生成更改日志的描述信息
     * <p>
     * 该方法返回一个描述键字符串, 用于在生成更改日志时获取对应的描述信息.
     *
     * @return 描述键字符串
     */
    @Override
    protected @NotNull String getDescriptionKey() {
        return "action.generate.changelog.gitlog.description";
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
        return "action.generate.changelog.gitlog.progress.title";
    }

    /**
     * 获取进度文本键
     * <p>
     * 返回用于显示进度信息的国际化文本键, 用于获取对应的进度描述文本.
     *
     * @return 进度文本键
     */
    @Override
    protected @NotNull String getProgressTextKey() {
        return "action.generate.changelog.gitlog.progress.text";
    }

    /**
     * 获取错误键
     * <p>
     * 该方法返回用于标识 Git 日志错误的错误键字符串, 供错误处理机制使用.
     *
     * @return 错误键字符串
     */
    @Override
    protected @NotNull String getErrorKey() {
        return "action.generate.changelog.gitlog.error";
    }

    /**
     * 生成变更日志内容
     * <p>
     * 使用指定的变更日志服务和提交哈希列表生成变更日志内容
     *
     * @param service      变更日志服务实例, 用于生成日志内容
     * @param commitHashes 提交哈希列表, 用于确定需要包含的提交记录
     * @return 生成的变更日志内容
     * @throws Exception 如果生成过程中发生错误
     */
    @Override
    protected @NotNull String generateContent(@NotNull ChangelogService service,
                                              @NotNull List<String> commitHashes) throws Exception {
        return service.generateChangelog(commitHashes);
    }
}

