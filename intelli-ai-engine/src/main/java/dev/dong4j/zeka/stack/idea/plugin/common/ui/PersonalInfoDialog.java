package dev.dong4j.zeka.stack.idea.plugin.common.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URL;
import java.awt.Dimension;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPanel;

import dev.dong4j.zeka.stack.idea.plugin.common.util.AICommonBundle;
import com.intellij.util.ui.JBUI;

/**
 * 个人信息对话框类
 * <p> 继承自 DialogWrapper, 用于显示用户的个人信息, 并提供确认操作.
 * <p> 该对话框包含一个中心面板, 用于展示用户信息, 并提供了确认按钮.
 * <p> 通过调用 `createCenterPanel` 方法创建中心面板, 使用 `loadImageIcon` 方法加载图片资源.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.02
 * @since 1.0.0
 */
public class PersonalInfoDialog extends DialogWrapper {
    /** 内容面板, 用于显示个人信息对话框中的界面组件. */
    private final JPanel content;

    /**
     * 构造函数, 初始化个人信息对话框
     * <p> 设置对话框的内容面板, 标题, 是否可调整大小, 并进行初始化
     *
     * @param project 当前项目实例, 不能为 null
     */
    public PersonalInfoDialog(@NotNull Project project) {
        super(project, true);
        this.content = PersonalInfoPanel.createContentOnly().getContent();
        setTitle(AICommonBundle.message("personal.info.dialog.title"));
        setResizable(false);
        init();
        Dimension preferred = content.getPreferredSize();
        if (preferred != null) {
            int width = Math.max(preferred.width, JBUI.scale(630));
            int height = Math.max(preferred.height, JBUI.scale(420));
            setSize(width, height);
        }
    }

    /**
     * 创建中心面板
     * <p> 返回对话框的中心面板, 该面板包含个人信息的内容
     *
     * @return 包含个人信息内容的 JComponent, 如果未设置内容则返回 null
     */
    @Override
    protected @Nullable JComponent createCenterPanel() {
        return content;
    }

    /**
     * 创建对话框的操作按钮
     * <p> 重写此方法以定义对话框中的操作按钮, 这里仅创建了一个确认按钮 (OK 按钮)
     *
     * @return 包含操作按钮的数组, 至少包含一个按钮
     */
    @Override
    protected @NotNull javax.swing.Action @NotNull [] createActions() {
        return new javax.swing.Action[] {getOKAction()};
    }

    /**
     * 加载指定资源路径的图像图标
     * <p> 尝试从类路径加载指定资源路径的图像文件, 并返回对应的 ImageIcon 对象. 如果加载失败或图像为空, 则返回 null.
     *
     * @param resourcePath 资源路径, 不能为 null
     * @return 加载成功的 ImageIcon 对象, 如果加载失败或图像为空则返回 null
     */
    @Nullable
    private ImageIcon loadImageIcon(@NotNull String resourcePath) {
        try {
            URL imageUrl = getClass().getResource(resourcePath);
            if (imageUrl != null) {
                java.awt.image.BufferedImage image = ImageIO.read(imageUrl);
                if (image != null) {
                    return new ImageIcon(image);
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
