package dev.dong4j.zeka.stack.idea.plugin.changelog.action;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import javax.swing.Icon;

import dev.dong4j.zeka.stack.idea.plugin.changelog.service.ChangelogService;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIStreamResponseListener;
import icons.ChangelogIcons;

/**
 * 用于从 Git diff 生成变更日志的操作类
 * <p>
 * 该类继承自 AbstractGitLogAction, 负责处理基于 code diff 生成变更日志的逻辑, 包括获取项目上下文, 验证日志数据, 检查选中的提交记录, 并显示相关信息通知.
 * 与 {@link GenerateChangelogFromGitLogAction} 类似, 但针对 code diff 而非 Git 提交记录.
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email mailto:zeka.stack@gmail.com
 * @date 2025.11.30
 * @since 1.0.0
 */
public class GenerateChangelogFromGitDiffAction extends AbstractGitLogAction {

    /**
     * 获取提交操作的文本键
     * <p>
     * 返回用于提交操作的国际化文本键, 用于获取对应的文本资源
     *
     * @return 提交操作的文本键
     */
    @Override
    protected @NotNull String getTextKey() {
        return "action.generate.changelog.diff";
    }

    /**
     * 获取提交操作的描述键
     * <p>
     * 返回用于标识提交操作描述的键值, 通常用于国际化资源文件中查找对应的描述文本.
     *
     * @return 提交操作的描述键
     */
    @Override
    protected @NotNull String getDescriptionKey() {
        return "action.generate.changelog.diff.description";
    }

    /**
     * 获取图标
     * <p>
     * 返回指定的图标对象, 用于表示当前节点的图标.
     *
     * @return 图标对象
     */
    @Override
    protected @NotNull Icon getIcon() {
        return ChangelogIcons.DIFF;
    }

    /**
     * 获取进度标题的资源键
     *
     * @return 进度标题的资源键
     */
    @Override
    protected @NotNull String getProgressTitleKey() {
        return "action.generate.changelog.diff.progress.title";
    }

    /**
     * 获取进度文本键
     *
     * @return 进度文本键
     */
    @Override
    protected @NotNull String getProgressTextKey() {
        return "action.generate.changelog.diff.progress.text";
    }

    /**
     * 获取错误键
     *
     * @return 错误键
     */
    @Override
    protected @NotNull String getErrorKey() {
        return "action.generate.changelog.diff.error";
    }

    /**
     * 生成变更日志内容（基于 code diff）
     *
     * @param service      变更日志服务实例
     * @param commitHashes 提交哈希列表
     * @return 生成的变更日志内容
     * @throws Exception 如果生成过程中发生错误
     */
    @Override
    protected @NotNull String generateContent(@NotNull ChangelogService service,
                                              @NotNull List<String> commitHashes) throws Exception {
        return service.generateChangelogFromDiff(commitHashes);
    }

    /**
     * 生成变更日志内容流 (基于 code diff)
     * <p> 调用变更日志服务生成基于 code diff 的变更日志内容流, 并通过监听器反馈进度信息
     *
     * @param service      变更日志服务实例, 不能为 null
     * @param commitHashes 提交哈希列表, 不能为 null
     * @param listener     AI 流响应监听器, 用于接收生成过程中的反馈信息, 不能为 null
     * @return 生成的变更日志内容
     * @throws Exception 如果生成过程中发生错误
     */
    @Override
    protected @NotNull String generateContentStream(@NotNull ChangelogService service,
                                                    @NotNull List<String> commitHashes,
                                                    @NotNull AIStreamResponseListener listener) throws Exception {
        return service.generateChangelogFromDiffStream(commitHashes, listener);
    }

}
