package dev.dong4j.zeka.stack.idea.plugin.common.whatsnew;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.jcef.JBCefApp;
import com.intellij.ui.jcef.JBCefBrowser;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefLifeSpanHandlerAdapter;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.beans.PropertyChangeListener;

import javax.swing.JComponent;
import javax.swing.JEditorPane;

/**
 * `WhatsNewHtmlEditor` 类是一个继承自 `UserDataHolderBase` 并实现了 `FileEditor` 接口的 HTML 编辑器.
 * <p> 该类用于显示和编辑特定的 HTML 文件. 它根据系统是否支持 CEF 浏览器来选择使用 CEF 浏览器或 JEditorPane 来渲染 HTML 内容.
 * <p> 主要功能包括加载指定的 HTML 文件并在用户界面中显示, 同时提供了文件编辑器的基本接口实现.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2025.12.31
 * @since 1.0.0
 */
public class WhatsNewHtmlEditor extends UserDataHolderBase implements FileEditor {
    /** UI 组件, 用于显示 HTML 内容 */
    private final JComponent component;
    /**
     * CEF 浏览器实例
     * <p> 用于加载和显示 HTML 内容
     *
     * @see JBCefBrowser
     */
    private final JBCefBrowser browser;
    /**
     * 当前编辑器关联的虚拟文件
     *
     * @see VirtualFile
     */
    private final VirtualFile file;

    /**
     * 构造一个 WhatsNewHtmlEditor 实例
     * <p> 根据提供的虚拟文件和 HTML 内容初始化编辑器组件, 支持 CEF 浏览器或 JEditorPane 作为显示组件
     *
     * @param file 虚拟文件对象, 表示编辑器关联的文件
     * @param html HTML 内容, 用于初始化编辑器显示
     */
    public WhatsNewHtmlEditor(@NotNull VirtualFile file, @NotNull String html) {
        this.file = file;
        if (JBCefApp.isSupported()) {
            browser = new JBCefBrowser();
            // 注册新窗口请求处理器，拦截所有新窗口请求并在外部浏览器中打开
            browser.getJBCefClient().addLifeSpanHandler(new CefLifeSpanHandlerAdapter() {
                @Override
                public boolean onBeforePopup(CefBrowser browser, CefFrame frame, String targetUrl, String targetFrameName) {
                    // 在外部浏览器中打开链接，避免在 JCEF 浏览器中打开新窗口导致页面被阻塞
                    BrowserUtil.browse(targetUrl);
                    // 返回 true 表示已处理，阻止在 JCEF 浏览器中打开新窗口
                    return true;
                }
            }, browser.getCefBrowser());
            browser.loadHTML(html);
            component = browser.getComponent();
        } else {
            browser = null;
            JEditorPane pane = new JEditorPane("text/html", html);
            pane.setEditable(false);
            pane.setOpaque(false);
            component = new JBScrollPane(pane);
        }
    }

    /**
     * 返回编辑器组件
     * <p> 此方法返回当前编辑器的主组件, 用于在 IDE 中显示.
     *
     * @return 编辑器组件
     */
    @Override
    public @NotNull JComponent getComponent() {
        return component;
    }

    /**
     * 返回此文件编辑器中首选的可聚焦组件
     * <p> 该方法返回用户界面中应该接收键盘焦点的组件, 用于交互操作
     *
     * @return 首选的可聚焦组件, 此处返回用于显示内容的组件
     */
    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return component;
    }

    /**
     * 获取编辑器的名称
     * <p> 返回该文件编辑器的显示名称, 用于在 IDE 中标识该编辑器.
     *
     * @return 编辑器名称, 不会为 null
     */
    @Override
    public @Nls @NotNull String getName() {
        return "IntelliAI What's New";
    }

    /**
     * 返回与此文件编辑器关联的虚拟文件
     *
     * @return 与此编辑器关联的虚拟文件对象
     */
    @Override
    public @NotNull VirtualFile getFile() {
        return file;
    }

    /**
     * 设置文件编辑器状态
     * <p> 此方法用于设置文件编辑器的状态, 目前不实现具体逻辑, 保持空操作.
     *
     * @param state 文件编辑器状态对象
     */
    @Override
    public void setState(@NotNull com.intellij.openapi.fileEditor.FileEditorState state) {
    }

    /**
     * 判断当前编辑器内容是否被修改
     * <p> 该方法始终返回 false, 表示此编辑器内容不可修改
     *
     * @return 始终返回 false
     */
    @Override
    public boolean isModified() {
        return false;
    }

    /**
     * 检查编辑器是否有效
     * <p> 此方法用于判断当前编辑器的状态是否有效. 在本实现中, 始终返回 true, 表示编辑器总是有效的.
     *
     * @return 布尔值, 表示编辑器是否有效. 始终返回 true.
     */
    @Override
    public boolean isValid() {
        return true;
    }

    /**
     * 当此文件编辑器被选中时调用
     * <p> 用于执行编辑器获得焦点或激活状态时的相关操作, 当前实现为空
     */
    @Override
    public void selectNotify() {
    }

    /**
     * 当编辑器被取消选中时调用
     * <p> 此方法在编辑器失去焦点或被用户取消选中时触发, 用于执行相应的清理或状态更新操作.
     *
     * @since hello.world
     */
    @Override
    public void deselectNotify() {
    }

    /**
     * 添加属性变更监听器
     * <p> 该方法用于注册一个属性变更监听器, 当编辑器的属性发生变化时通知监听器.</p>
     *
     * @param listener 要添加的属性变更监听器
     */
    @Override
    public void addPropertyChangeListener(@NotNull PropertyChangeListener listener) {
    }

    /**
     * 移除指定的属性变化监听器
     * <p> 从监听器列表中移除指定的属性变化监听器, 如果该监听器已注册则生效.
     *
     * @param listener 要移除的属性变化监听器
     */
    @Override
    public void removePropertyChangeListener(@NotNull PropertyChangeListener listener) {
    }

    /**
     * 释放资源
     * <p> 在文件编辑器不再被使用时调用此方法, 用于释放与浏览器相关的资源.
     *
     * @since hello.world
     */
    @Override
    public void dispose() {
        if (browser != null) {
            Disposer.dispose(browser);
        }
    }
}
