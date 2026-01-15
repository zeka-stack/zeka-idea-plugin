package dev.dong4j.zeka.stack.idea.plugin.changelog.action;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import javax.swing.Icon;

import dev.dong4j.zeka.stack.idea.plugin.changelog.service.ChangelogService;
import dev.dong4j.zeka.stack.idea.plugin.changelog.util.ChangelogBundle;
import dev.dong4j.zeka.stack.idea.plugin.changelog.util.NotificationUtil;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIStreamResponseListener;
import icons.ChangelogIcons;

/**
 * 用于生成并保存 CHANGELOG.md 文件的 Action 类
 * <p>
 * 该类继承自 AbstractGitLogAction, 主要负责生成基于 Git 提交记录的变更日志内容
 * 并将其保存到项目根目录下的 CHANGELOG.md 文件中, 如果文件已存在则更新内容.
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email mailto:zeka.stack@gmail.com
 * @date 2026.01.15
 * @since 1.0.0
 */
public class GenerateChangelogFileAction extends AbstractGitLogAction {

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
        return "action.generate.changelog.file.gitlog";
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
        return "action.generate.changelog.file.gitlog.description";
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
        return "action.generate.changelog.file.gitlog.progress.title";
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
        return "action.generate.changelog.file.gitlog.progress.text";
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
        return "action.generate.changelog.file.gitlog.error";
    }

    /**
     * 生成变更日志内容
     * <p>
     * 使用指定的变更日志服务和提交哈希列表生成变更日志内容并保存到文件
     *
     * @param service      变更日志服务实例, 用于生成日志内容
     * @param commitHashes 提交哈希列表, 用于确定需要包含的提交记录
     * @return 生成的变更日志内容
     * @throws Exception 如果生成过程中发生错误
     */
    @Override
    protected @NotNull String generateContent(@NotNull ChangelogService service,
                                              @NotNull List<String> commitHashes) throws Exception {
        // 调用 generateAndSaveChangelogFile 方法，该方法会自动保存到文件
        return service.generateAndSaveChangelogFile(service.getProject(), commitHashes);
    }

    /**
     * 流式生成内容
     * <p>
     * 子类可重写此方法以启用 AI 流式输出. 默认实现会退化为一次性生成.
     *
     * @param service      ChangelogService 实例
     * @param commitHashes 提交记录 hash 列表
     * @param listener     流式监听器
     * @return 生成的内容
     * @throws Exception 生成过程中可能发生的异常
     */
    @Override
    protected @NotNull String generateContentStream(@NotNull ChangelogService service,
                                                    @NotNull List<String> commitHashes,
                                                    @NotNull AIStreamResponseListener listener) throws Exception {
        // 先使用流式生成内容
        String content = service.generateChangelogStream(commitHashes, listener);
        
        // 然后保存到文件
        service.saveChangelogToFile(service.getProject(), content);
        
        return content;
    }
}
