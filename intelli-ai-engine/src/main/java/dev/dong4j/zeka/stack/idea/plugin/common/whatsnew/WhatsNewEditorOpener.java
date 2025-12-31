package dev.dong4j.zeka.stack.idea.plugin.common.whatsnew;

import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.util.ResourceUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;

/**
 * 用于打开 "What's New" 页面的编辑器操作类
 * <p> 该类负责加载并打开 IntelliJ IDEA 的 "What's New" 页面, 支持从资源中读取 HTML 内容并显示在编辑器中.
 * 提供了打开文件和加载 HTML 内容的功能, 适用于 IDE 启动时展示新特性说明.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2025.12.31
 * @since 1.0.0
 */
public final class WhatsNewEditorOpener {
    /** 用于标识 What's New 编辑器 HTML 内容的用户数据键 */
    static final Key<String> HTML_KEY = Key.create("intelliai.whatsnew.html");
    /** 标题键, 用于存储 What's New 界面的标题信息 */
    static final Key<String> TITLE_KEY = Key.create("intelliai.whatsnew.title");

    /** 资源文件路径, 用于定位 Whats New 页面的 HTML 文件 */
    private static final String RESOURCE_PATH = "/whatsnew/";
    /**
     * 用于存储资源文件的名称
     *
     * @see #RESOURCE_PATH
     */
    private static final String RESOURCE_FILE = "index.html";
    /**
     * 默认标题文本
     * <p>
     * 用于在打开 "What's New" 编辑器时显示的默认标题.
     */
    private static final String DEFAULT_TITLE = "IntelliAI What's New";

    /**
     * 私有构造函数, 防止外部实例化
     * <p> 该构造函数为私有, 确保类不能被外部直接创建实例
     */
    private WhatsNewEditorOpener() {
    }

    /**
     * 打开“IntelliAI What's New”编辑器
     * <p> 加载 HTML 内容并在项目中打开一个虚拟文件进行查看. 如果已有一个打开的文件包含相同的内容, 则直接打开该文件.
     *
     * @param project 项目对象
     * @since hello.world
     */
    public static void open(@NotNull Project project) {
        String html = loadHtml();
        if (html == null || html.isBlank()) {
            return;
        }

        FileEditorManager editorManager = FileEditorManager.getInstance(project);
        for (var file : editorManager.getOpenFiles()) {
            if (file.getUserData(HTML_KEY) != null) {
                editorManager.openFile(file, true);
                return;
            }
        }

        LightVirtualFile file = new LightVirtualFile(DEFAULT_TITLE, html);
        file.putUserData(HTML_KEY, html);
        file.putUserData(TITLE_KEY, DEFAULT_TITLE);
        file.setWritable(false);
        editorManager.openFile(file, true);
    }

    /**
     * 从资源路径加载 HTML 内容
     * <p> 尝试从指定的资源路径读取 HTML 文件内容, 如果读取失败或文件为空则返回 null
     *
     * @return HTML 内容字符串, 如果读取失败或文件为空则返回 null
     */
    @Nullable
    public static String loadHtml() {
        try (InputStream stream = ResourceUtil.getResourceAsStream(WhatsNewEditorOpener.class.getClassLoader(),
            RESOURCE_PATH, RESOURCE_FILE)) {
            if (stream == null) {
                return null;
            }
            return ResourceUtil.loadText(stream);
        } catch (IOException ex) {
            return null;
        }
    }
}
