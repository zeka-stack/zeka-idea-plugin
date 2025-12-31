package dev.dong4j.zeka.stack.idea.plugin.changelog.ui;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.ContentFactory;

import org.jetbrains.annotations.NotNull;

import javax.swing.JPanel;

/**
 * Changelog Tool Window Factory
 *
 * @author dong4j
 * @version hello.world
 * @date 2025-12-31 17:19:21
 * @since hello.world
 */
public class ChangelogToolWindowFactory implements ToolWindowFactory, DumbAware {

    /**
     * 创建工具窗口的内容
     * <p> 此方法用于初始化工具窗口的内容. 由于工具窗口内容由服务动态创建, 因此这里仅创建一个空的占位内容, 并将其固定在右侧下方区域.
     *
     * @param project    当前项目
     * @param toolWindow 工具窗口实例
     */
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        // 工具窗口内容由服务动态创建，这里仅创建一个空的占位内容
        JPanel panel = new JPanel();
        toolWindow.getContentManager().addContent(
            ContentFactory.getInstance().createContent(panel, "", false)
                                                 );
        // 将工具窗口固定在右侧下方区域
        setRightBottomDock(toolWindow);
    }

    /**
     * 设置工具窗口的右下角布局
     * <p> 尝试调用不同的版本方法以实现将右侧区域拆分为上下两块的效果. 优先使用带有 Runnable 参数的 setSplitMode 方法,
     * 如果失败, 则尝试调用不带参数的 setSplitMode 方法.
     *
     * @param toolWindow 工具窗口实例
     */
    private void setRightBottomDock(@NotNull ToolWindow toolWindow) {
        // 兼容不同版本的 API，优先使用 split mode 将右侧区域拆分为上下两块
        boolean applied = invokeBooleanMethod(toolWindow, "setSplitMode", true, Runnable.class);
        if (!applied) {
            invokeBooleanMethod(toolWindow, "setSplitMode", true);
        }
    }

    /**
     * 尝试调用指定的布尔方法并设置其参数
     * <p> 该方法通过反射机制调用工具窗口对象上的指定布尔方法, 并传递相应的参数. 如果方法调用成功, 则返回 true; 否则返回 false.
     *
     * @param toolWindow 工具窗口对象
     * @param methodName 要调用的方法名
     * @param value      布尔参数值
     * @param extraTypes 额外的参数类型数组
     * @return 如果方法调用成功则返回 true, 否则返回 false
     */
    private boolean invokeBooleanMethod(@NotNull ToolWindow toolWindow,
                                        @NotNull String methodName,
                                        boolean value,
                                        Class<?>... extraTypes) {
        try {
            Class<?>[] paramTypes = new Class<?>[1 + extraTypes.length];
            paramTypes[0] = boolean.class;
            System.arraycopy(extraTypes, 0, paramTypes, 1, extraTypes.length);
            Object[] params = new Object[1 + extraTypes.length];
            params[0] = value;
            for (int i = 0; i < extraTypes.length; i++) {
                params[i + 1] = null;
            }
            toolWindow.getClass().getMethod(methodName, paramTypes).invoke(toolWindow, params);
            return true;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }
}
