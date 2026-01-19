package dev.dong4j.zeka.stack.idea.plugin.changelog.action;

import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Paths;
import java.util.List;

import javax.swing.Icon;

import dev.dong4j.zeka.stack.idea.plugin.changelog.service.ChangelogService;
import dev.dong4j.zeka.stack.idea.plugin.changelog.settings.SettingsState;
import dev.dong4j.zeka.stack.idea.plugin.changelog.util.ChangelogBundle;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIStreamResponseListener;
import icons.ChangelogIcons;

/**
 * 生成变更日志文件的操作类
 * <p> 继承自 {@code AbstractGitLogAction}, 用于在 IntelliJ IDEA 插件中实现根据 Git 提交记录生成或更新 CHANGELOG.md 文件的功能.
 * 该类主要负责在用户触发操作时, 判断项目中是否已存在 CHANGELOG.md 文件, 若不存在则生成新文件, 若存在则更新内容.
 * 本类不负责请求处理, 仅专注于变更日志文件的生成与更新逻辑, 避免与基础设施层耦合.
 * <p> 支持流式生成变更日志内容 (通过 {@code AIStreamResponseListener}), 并可将结果保存至文件系统.
 * <p> 在更新操作前会检查项目是否处于“Dumb”状态或用户禁用该功能, 若条件不满足则禁用操作项.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.15
 * @since 1.0.0
 */
public class GenerateChangelogFileAction extends AbstractGitLogAction {

    /**
     * 获取操作图标的实现方法
     * <p>
     * 该方法用于返回当前操作对应的图标资源, 此处返回的是变更日志相关的图标常量 {@code ChangelogIcons.CHANGELOG}.
     *
     * @return 图标对象, 类型为 {@code Icon}, 用于在 UI 中显示该操作的图标
     */
    @NotNull
    protected Icon getIcon() {
        return ChangelogIcons.CHANGELOG;
    }

    /**
     * 更新操作按钮的启用状态和显示文本
     * <p>
     * 根据当前项目状态, 是否处于哑模式 (DumbService) 以及配置项 generateChangelogFile 的值, 动态设置按钮的启用状态和显示文本.
     * 如果变更日志文件已存在, 则显示“更新变更日志文件”相关文本; 否则显示“生成变更日志文件”相关文本.
     *
     * @param e 操作事件对象, 包含当前操作上下文信息
     */
    @Override
    public void update(@NotNull com.intellij.openapi.actionSystem.AnActionEvent e) {
        super.update(e);
        Project project = e.getProject();
        if (project != null && DumbService.isDumb(project)) {
            e.getPresentation().setEnabled(false);
            return;
        }

        if (e.getPresentation().isEnabled() && !SettingsState.getInstance().generateChangelogFile) {
            e.getPresentation().setEnabled(false);
        }

        boolean exists = project != null && !project.isDisposed() && changelogFileExists(project);
        if (exists) {
            e.getPresentation().setText(ChangelogBundle.message("action.update.changelog.file.gitlog"));
            e.getPresentation().setDescription(ChangelogBundle.message("action.update.changelog.file.gitlog.description"));
        } else {
            e.getPresentation().setText(ChangelogBundle.message(getTextKey()));
            e.getPresentation().setDescription(ChangelogBundle.message(getDescriptionKey()));
        }
    }

    /**
     * 检查项目根目录下是否存在 CHANGELOG.md 文件
     * <p>
     * 通过获取项目基础路径, 并构造路径 <pre>{@code Paths.get(basePath, "CHANGELOG.md")}</pre>, 判断该文件是否存在.
     *
     * @param project 项目实例, 用于获取基础路径
     * @return 如果文件存在则返回 true, 否则返回 false
     */
    private static boolean changelogFileExists(@NotNull Project project) {
        String basePath = project.getBasePath();
        if (basePath == null || basePath.isBlank()) {
            return false;
        }
        return Paths.get(basePath, "CHANGELOG.md").toFile().exists();
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
     * 返回用于显示进度标题的国际化资源键字符串, 用于在生成变更日志过程中显示进度标题.
     *
     * @return 进度标题的资源键字符串
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
        return service.generateAndSaveChangelogFile(service.getProject(), commitHashes, getStatisticsUserAction());
    }

    /**
     * 流式生成变更日志内容
     * <p>
     * 该方法通过传入的变更日志服务和提交哈希列表, 调用流式生成接口生成日志内容, 并实时保存到文件中. 生成完成后返回完整内容.
     * <p>
     * 本方法适用于需要实时输出或 AI 流式响应的场景, 支持监听器回调.
     *
     * @param service      变更日志服务实例, 用于执行流式日志生成和文件保存
     * @param commitHashes 提交记录的哈希列表, 用于确定生成日志所包含的提交范围
     * @param listener     流式响应监听器, 用于接收中间生成内容或进度反馈
     * @throws Exception 生成或保存过程中发生异常时抛出
     */
    @Override
    protected void generateContentStream(@NotNull ChangelogService service,
                                                    @NotNull List<String> commitHashes,
                                                    @NotNull AIStreamResponseListener listener) throws Exception {
        // 先使用流式生成内容
        String content = service.generateChangelogStream(commitHashes, listener, getStatisticsUserAction());
        // 然后保存到文件
        service.saveChangelogToFile(service.getProject(), content);
    }
}
