package dev.dong4j.zeka.stack.idea.plugin.action;

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.codeInsight.intention.preview.IntentionPreviewUtils;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Iconable;
import com.intellij.psi.PsiDocCommentOwner;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.util.IncorrectOperationException;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.psi.KtFile;

import javax.swing.Icon;

import dev.dong4j.zeka.stack.idea.plugin.settings.SettingsState;
import dev.dong4j.zeka.stack.idea.plugin.util.JavaDocBundle;
import dev.dong4j.zeka.stack.idea.plugin.util.NotificationUtil;
import dev.dong4j.zeka.stack.idea.plugin.util.PsiElementLocator;
import icons.AIJicons;
import lombok.extern.slf4j.Slf4j;

/**
 * 生成 JavaDoc 意图动作类
 * <p>
 * 该类实现了 IDEA 插件中的意图动作功能, 用于为 Java 代码元素生成 JavaDoc 注释.
 * 继承自 PsiElementBaseIntentionAction 并实现 Iconable 接口, 提供在 IDE 中通过意图操作快速生成 JavaDoc 的功能.
 * 该动作会在编辑器中选中的代码元素上生成标准的 JavaDoc 注释, 支持类, 方法, 字段等 Java 元素的文档注释生成.
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email mailto:zeka.stack@gmail.com
 * @date 2025.11.30
 * @since 1.0.0
 */
@Slf4j
public class GenerateJavaDocIntentionAction extends PsiElementBaseIntentionAction implements Iconable {
    /** 用于执行 JavaDoc 生成操作的基础动作对象 */
    private final AbstractGenerateJavaDocAction baseAction = new AbstractGenerateJavaDocAction() {
        /**
         * 处理用户操作事件
         * <p>
         * 当用户执行某个操作时触发此方法, 用于处理相应的动作事件.
         *
         * @param e 操作事件对象, 包含与动作相关的信息
         * @throws NullPointerException 如果传入的事件对象为 null(但通过 @NotNull 注解已确保不会发生)
         */
        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {

        }

        /**
         * 当定位失败时通知用户没有任务位置
         * <p>
         * 该方法在定位失败时被调用, 通过通知工具提示用户当前项目没有任务位置信息
         *
         * @param project 项目对象, 用于标识当前处理的项目
         */
        @Override
        protected void onLocateFailed(@NotNull Project project) {
            NotificationUtil.notifyNoTask(project, JavaDocBundle.message("notification.no.task.location"));
        }
    };

    /**
     * 获取生成 JavaDoc 的文本内容
     * <p>
     * 返回用于生成 JavaDoc 的提示文本, 通常用于界面展示或提示信息
     *
     * @return 生成 JavaDoc 的文本内容
     */
    @NotNull
    @Override
    public String getText() {
        return JavaDocBundle.message("action.generate.javadoc");
    }

    /**
     * 获取插件的家族名称
     * <p>
     * 返回插件的家族名称, 用于标识插件所属的家族或分类
     *
     * @return 插件家族名称
     */
    @NotNull
    @Override
    public String getFamilyName() {
        return JavaDocBundle.message("plugin.name");
    }

    /**
     * 获取图标
     * <p>
     * 根据指定的图标标志返回对应的图标对象.
     *
     * @param flags 图标标志, 用于指定图标样式或变体
     * @return 对应的图标对象
     */
    @Override
    public Icon getIcon(@Iconable.IconFlags int flags) {
        return AIJicons.AIJ_16;
    }

    /**
     * 判断当前意图操作是否可用.
     * <p>
     * 该方法首先排除预览元素和非 Java 文件. 随后根据编辑器光标位置定位到的元素,
     * 判断其是否属于 {@link PsiDocCommentOwner}(即可以拥有 Javadoc 注释的元素). 最后根据
     * {@link SettingsState#getInstance()} 中 {@code overrideExisting} 配置决定是否允许覆盖已有注释.
     *
     * @param project 当前项目
     * @param editor  当前编辑器实例
     * @param element 当前光标所在的 PSI 元素
     * @return 若满足上述所有条件且 (若不覆盖已有注释时) 目标元素未包含 Javadoc, 则返回 {@code true};
     *     否则返回 {@code false}
     */
    @SuppressWarnings("D")
    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement element) {
        // 如果处于预览模式，则直接返回 false，避免在预览阶段执行会产生副作用的操作
        if (IntentionPreviewUtils.isPreviewElement(element)) {
            return false;
        }

        PsiFile file = element.getContainingFile();

        // 1. 必须是 Java 或 Kotlin 文件
        if (!(file instanceof PsiJavaFile) && !(file instanceof KtFile)) {
            return false;
        }

        // 检查是否支持 Kotlin
        if (file instanceof KtFile) {
            SettingsState settings = SettingsState.getInstance();
            if (!settings.isLanguageSupported("kotlin")) {
                return false;
            }
        }

        // 2. 智能定位元素
        PsiElementLocator.LocateResult locateResult = PsiElementLocator.locateElementAtOffset(file, editor.getCaretModel().getOffset());

        if (locateResult == null) {
            return false;
        }

        // 3. 如果是整个文件，不在 Intention 中显示（避免重复）
        if (locateResult.type() == PsiElementLocator.LocateType.FILE) {
            return false;
        }

        // 4. 检查元素是否为拥有文档注释的元素（如类、方法、字段等）
        PsiElement locatedElement = locateResult.element();

        // 检查 Java 元素的文档注释
        if (locatedElement instanceof PsiDocCommentOwner docOwner) {
            // 5. 获取配置：是否允许覆盖现有文档
            SettingsState settings = SettingsState.getInstance();
            boolean overrideExisting = settings.overrideExisting;

            // 6. 根据 overrideExisting 设置决定是否显示
            if (overrideExisting) {
                // 允许覆盖：无论是否已有 JavaDoc 都显示
                return true;
            } else {
                // 不允许覆盖：只在没有 JavaDoc 时显示
                return docOwner.getDocComment() == null;
            }
        }

        // 检查 Kotlin 元素的文档注释
        if (file instanceof KtFile) {
            // 使用 PsiElementLocator 的 hasJavaDoc 方法检查（已支持 Kotlin）
            boolean hasDoc = PsiElementLocator.hasJavaDoc(locatedElement);
            SettingsState settings = SettingsState.getInstance();
            boolean overrideExisting = settings.overrideExisting;

            if (overrideExisting) {
                return true;
            } else {
                return !hasDoc;
            }
        }

        return false;
    }

    /**
     * 执行指定的意图操作
     * <p>
     * 该方法用于处理意图操作, 若当前元素为预览元素则直接返回. 否则调用基础操作处理方法, 并传递包含文件信息.
     *
     * @param project 项目对象
     * @param editor  编辑器对象
     * @param element 要处理的 PsiElement 对象
     * @throws IncorrectOperationException 如果操作不正确时抛出
     */
    @Override
    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement element)
        throws IncorrectOperationException {

        // 如果处于预览模式，则直接返回，不执行任何会产生副作用的操作, 只有在真实执行意图操作时才执行完整的处理流程
        if (IntentionPreviewUtils.isPreviewElement(element)) {
            return;
        }

        // 使用基类的统一逻辑处理
        baseAction.process(project, editor, element.getContainingFile(), true);
    }

}

