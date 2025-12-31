package dev.dong4j.zeka.stack.idea.plugin.common.whatsnew;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorPolicy;
import com.intellij.openapi.fileEditor.FileEditorProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.vfs.VirtualFile;

import org.jetbrains.annotations.NotNull;

/**
 * 提供 HTML 编辑器的实现类
 * <p> 用于在 IntelliJ 平台中为特定文件类型提供自定义的 HTML 编辑器, 隐藏默认编辑器并展示自定义内容.
 * 该类实现了 FileEditorProvider 接口, 并标记为 DumbAware 以支持在索引未就绪时运行.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2025.12.31
 * @since 1.0.0
 */
public class WhatsNewHtmlEditorProvider implements FileEditorProvider, DumbAware {
    /** 编辑器类型标识, 用于唯一标识 "IntelliAIWhatsNewHtml" 编辑器 */
    private static final String EDITOR_TYPE_ID = "IntelliAIWhatsNewHtml";

    /**
     * 判断是否接受指定的文件作为编辑器处理对象
     * <p> 检查文件是否包含指定的 HTML 键值数据, 若包含则接受该文件
     *
     * @param project 项目对象
     * @param file    要检查的文件对象
     * @return 如果文件包含 HTML 键值数据则返回 true, 否则返回 false
     */
    @Override
    public boolean accept(@NotNull Project project, @NotNull VirtualFile file) {
        return file.getUserData(WhatsNewEditorOpener.HTML_KEY) != null;
    }

    /**
     * 创建文件编辑器实例, 用于显示 HTML 内容
     * <p> 根据指定的文件和 HTML 内容创建一个文件编辑器, 如果 HTML 内容为空则使用默认 HTML 结构
     *
     * @param project 项目对象, 用于获取编辑器相关上下文
     * @param file    要编辑的文件对象, 用于获取 HTML 内容
     * @return 返回一个文件编辑器实例
     */
    @Override
    public @NotNull FileEditor createEditor(@NotNull Project project, @NotNull VirtualFile file) {
        String html = file.getUserData(WhatsNewEditorOpener.HTML_KEY);
        if (html == null) {
            html = "<html><body></body></html>";
        }
        return new WhatsNewHtmlEditor(file, html);
    }

    /**
     * 获取编辑器的类型标识
     * <p> 返回当前编辑器的唯一标识符
     *
     * @return 编辑器的类型标识
     */
    @Override
    public @NotNull String getEditorTypeId() {
        return EDITOR_TYPE_ID;
    }

    /**
     * 获取文件编辑器策略
     * <p> 返回隐藏默认编辑器的策略, 表示该编辑器不会显示在默认的编辑器列表中
     *
     * @return 文件编辑器策略, 类型为隐藏默认编辑器
     */
    @Override
    public @NotNull FileEditorPolicy getPolicy() {
        return FileEditorPolicy.HIDE_DEFAULT_EDITOR;
    }
}
