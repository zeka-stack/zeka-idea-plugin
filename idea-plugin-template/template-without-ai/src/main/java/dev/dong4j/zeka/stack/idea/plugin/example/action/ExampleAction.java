package dev.dong4j.zeka.stack.idea.plugin.example.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;

import org.jetbrains.annotations.NotNull;

import dev.dong4j.zeka.stack.idea.plugin.example.util.ExampleBundle;
import dev.dong4j.zeka.stack.idea.plugin.example.util.NotificationUtil;
import icons.ExampleIcons;

/**
 * 示例动作类
 * <p> 继承自 AnAction, 提供一个具体的动作实现, 主要用于在 IntelliJ IDEA 插件中执行特定的操作.
 * <p> 该动作会在项目和文件可用时启用, 并在执行时显示相关信息通知.
 * <p> 具体功能包括:
 * <ul>
 * <li> 初始化动作标题和描述 </li>
 * <li> 在项目和文件存在的情况下启用动作 </li>
 * <li> 执行动作时显示成功消息, 提示操作已执行的文件名 </li>
 * <li> 在项目或文件不存在时显示错误信息 </li>
 * </ul>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.02
 * @since 1.0.0
 */
public class ExampleAction extends AnAction {

    /**
     * 构造函数, 用于初始化 ExampleAction 实例
     * <p> 通过调用父类构造函数设置 Action 的标题, 描述和图标
     *
     */
    public ExampleAction() {
        super(
            ExampleBundle.message("action.example.title"),
            ExampleBundle.message("action.example.description"),
            ExampleIcons.EXAMPLE_16
        );
    }

    /**
     * 执行 Action 的操作逻辑
     * <p> 当用户在编辑器中右键点击文件并选择此 Action 时触发该方法.
     * 方法会检查当前项目和文件是否存在, 如果存在则显示操作成功的通知信息.
     *
     * @param e AnActionEvent 事件对象, 包含触发 Action 的上下文信息
     */
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);

        if (project == null) {
            NotificationUtil.showError(project, ExampleBundle.message("error.no.project"));
            return;
        }

        if (psiFile == null) {
            NotificationUtil.showError(project, ExampleBundle.message("error.no.file"));
            return;
        }

        // 执行示例操作
        String fileName = psiFile.getName();
        NotificationUtil.showInfo(project, ExampleBundle.message("success.action.executed", fileName));
    }

    /**
     * 更新操作按钮的可用状态
     * <p> 根据当前项目和文件的存在性, 设置该操作按钮是否可用
     *
     * @param e 事件对象, 包含当前的项目和文件信息
     */
    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);

        // 只有在有项目和文件时才启用
        e.getPresentation().setEnabled(project != null && psiFile != null);
    }
}

