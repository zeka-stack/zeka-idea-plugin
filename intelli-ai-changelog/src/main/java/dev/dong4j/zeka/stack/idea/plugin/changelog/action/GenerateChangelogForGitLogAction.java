package dev.dong4j.zeka.stack.idea.plugin.changelog.action;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import javax.swing.Icon;

import dev.dong4j.zeka.stack.idea.plugin.changelog.service.ChangelogService;
import icons.ChangelogIcons;

/**
 * 用于从 Git 日志生成变更日志的 Action 类
 * <p>
 * 该类继承自 AbstractGitLogAction, 主要负责生成基于 Git 提交记录的变更日志内容, 适用于集成到 IDE 或构建工具中, 用于自动化生成项目变更记录.
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email mailto:zeka.stack@gmail.com
 * @date 2025.11.30
 * @since 1.0.0
 */
public class GenerateChangelogForGitLogAction extends AbstractGitLogAction {

    @NotNull
    protected Icon getIcon() {
        return ChangelogIcons.LOGS;
    }

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

