package dev.dong4j.zeka.stack.idea.plugin.common.chat;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.event.SelectionEvent;
import com.intellij.openapi.editor.event.SelectionListener;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.jcef.JBCefApp;
import com.intellij.ui.jcef.JBCefBrowser;
import com.intellij.ui.jcef.JBCefBrowserBase;
import com.intellij.ui.jcef.JBCefJSQuery;
import com.intellij.util.Alarm;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderSettings;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderSettingsListener;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefLifeSpanHandlerAdapter;
import org.cef.handler.CefLoadHandlerAdapter;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/**
 * AI 聊天面板
 * <p> 负责在 IntelliJ IDEA 中集成并显示 AI 聊天界面, 基于 JCEF 技术实现浏览器组件的嵌入与管理.
 * <p> 该类处理前端页面的加载,Java 与 JavaScript 的双向通信,IDE 编辑器事件的监听 (如文本选择变化)
 * 以及 AI 提供者配置的同步更新. 如果当前环境不支持 JCEF, 将显示提示信息面板.
 * <p> 实现了 {@code Disposable} 接口, 用于在面板销毁时释放相关资源.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.29
 * @since 1.0.0
 */
final class AIChatPanel implements Disposable {
    /** 当前项目实例, 用于获取项目级服务和配置 */
    private final Project project;
    /** UI 组件, 包含聊天界面的可视化内容 */
    private final JComponent component;
    /** JCEF 浏览器组件, 用于显示和处理富文本内容及交互 */
    private final JBCefBrowser browser;
    /** 处理聊天消息的处理器 */
    private final AIChatMessageHandler messageHandler;

    /** 文件编辑器事件监听, 用于更新选区信息 */
    private FileEditorManagerListener editorListener;
    /** 用于监听编辑器文本选择变更并更新聊天面板中的选区信息 */
    private SelectionListener selectionListener;
    /** AI 提供商设置变更监听器 */
    private AIProviderSettingsListener providerSettingsListener;
    /** 选择内容更新防抖定时器 */
    private Alarm selectionUpdateAlarm;
    /** 最后一次选择的 payload 数据, 用于避免重复发送相同内容 */
    private String lastSelectionPayload;

    /**
     * 构造 AIChatPanel 实例
     * <p> 初始化聊天面板, 根据环境是否支持 JCEF 创建相应的 UI 组件.
     * 如果支持 JCEF, 将初始化浏览器, 消息处理器, 监听器并加载 HTML 页面;
     * 否则显示不支持提示信息.
     *
     * @param project 当前项目实例, 不能为空
     */
    AIChatPanel(@NotNull Project project) {
        this.project = project;
        if (!JBCefApp.isSupported()) {
            this.browser = null;
            this.messageHandler = null;
            this.editorListener = null;
            this.selectionListener = null;
            this.providerSettingsListener = null;
            JBPanel<?> panel = new JBPanel<>(new BorderLayout());
            panel.add(new JBLabel("当前环境不支持 JCEF，无法显示聊天界面。"), BorderLayout.CENTER);
            this.component = panel;
            return;
        }

        this.browser = new JBCefBrowser();
        this.messageHandler = new AIChatMessageHandler(project, browser);
        this.selectionUpdateAlarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD, this);

        browser.getJBCefClient().addLifeSpanHandler(new CefLifeSpanHandlerAdapter() {
            /**
             * 在弹出窗口前进行拦截处理
             * <p> 当浏览器即将打开新窗口时, 通过 {@code BrowserUtil.browse(targetUrl)} 打开指定 URL, 并返回 true 表示阻止默认弹窗行为
             *
             * @param cefBrowser      触发弹窗的浏览器实例
             * @param frame           触发弹窗的帧对象
             * @param targetUrl       目标 URL 地址
             * @param targetFrameName 目标帧名称
             * @return 始终返回 true, 表示阻止浏览器默认弹窗行为
             */
            @Override
            public boolean onBeforePopup(CefBrowser cefBrowser, CefFrame frame, String targetUrl, String targetFrameName) {
                BrowserUtil.browse(targetUrl);
                return true;
            }
        }, browser.getCefBrowser());

        JBCefBrowserBase browserBase = browser;
        JBCefJSQuery jsQuery = JBCefJSQuery.create(browserBase);
        jsQuery.addHandler((msg) -> {
            messageHandler.handleMessage(msg);
            return new JBCefJSQuery.Response("ok");
        });

        browser.getJBCefClient().addLoadHandler(new CefLoadHandlerAdapter() {
            /**
             * 在页面加载完成时调用
             * <p> 当主框架加载完成时, 向页面注入 JavaScript 代码以建立与 Java 的通信渠道,
             * 并异步通知前端已准备就绪
             *
             * @param cefBrowser     浏览器实例
             * @param frame          加载完成的框架
             * @param httpStatusCode HTTP 状态码
             */
            @Override
            public void onLoadEnd(CefBrowser cefBrowser, CefFrame frame, int httpStatusCode) {
                if (!frame.isMain()) {
                    return;
                }
                String injection = "window.sendToJava = function(msg) { " + jsQuery.inject("msg") + " };";
                cefBrowser.executeJavaScript(injection, cefBrowser.getURL(), 0);
                ApplicationManager.getApplication().invokeLater(messageHandler::onFrontendReadyInjected);
            }
        }, browser.getCefBrowser());

        AIChatHtmlLoader htmlLoader = new AIChatHtmlLoader();
        browser.loadHTML(htmlLoader.loadHtml());
        this.component = browser.getComponent();

        editorListener = new FileEditorManagerListener() {
            /**
             * 当文件编辑器选择发生变化时触发
             * <p> 该方法在文件编辑器选择发生改变时被调用, 用于安排更新选择相关的操作.
             *
             * @param event 文件编辑器管理器事件对象, 包含与选择变化相关的信息
             */
            @Override
            public void selectionChanged(@NotNull FileEditorManagerEvent event) {
                scheduleSelectionUpdate();
            }
        };
        selectionListener = new SelectionListener() {
            /**
             * 处理选择事件变更
             * <p> 当选择事件发生时, 调度选择更新操作
             *
             * @param e 选择事件对象, 非空
             */
            @Override
            public void selectionChanged(@NotNull SelectionEvent e) {
                scheduleSelectionUpdate();
            }
        };
        providerSettingsListener = settings -> ApplicationManager.getApplication().invokeLater(messageHandler::sendProviders);

        project.getMessageBus().connect(this).subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, editorListener);
        EditorFactory.getInstance().getEventMulticaster().addSelectionListener(selectionListener, this);
        AIProviderSettings.getInstance().addListener(providerSettingsListener);

        Disposer.register(this, () -> AIProviderSettings.getInstance().removeListener(providerSettingsListener));
    }

    /**
     * 调度选择更新操作
     * <p> 当选择更新警报可用时, 取消所有现有请求并添加新的延迟请求.
     * 在延迟执行时, 构建选择负载并与上一次负载进行比较, 如果不同则发送选择信息.
     *
     * <p> 该方法用于在编辑器选择发生变化时, 延迟更新聊天面板中的选择信息,
     * 避免频繁更新以提高性能.
     */
    private void scheduleSelectionUpdate() {
        if (selectionUpdateAlarm == null || selectionUpdateAlarm.isDisposed()) {
            return;
        }
        selectionUpdateAlarm.cancelAllRequests();
        selectionUpdateAlarm.addRequest(() -> {
            String payload = messageHandler.buildSelectionPayload();
            if (payload != null && payload.equals(lastSelectionPayload)) {
                return;
            }
            lastSelectionPayload = payload;
            messageHandler.sendSelectionInfo();
        }, 200);
    }

    /**
     * 释放当前组件所占用的资源
     * <p> 执行清理操作, 移除所有监听器并取消挂起的任务
     */
    @Override
    public void dispose() {
        // listeners are disposed via Disposer registration
    }

    /**
     * 获取聊天面板的组件
     * <p> 返回用于显示聊天界面的 Swing 组件
     *
     * @return 不可为空的 JComponent 对象, 表示聊天面板的 UI 组件
     */
    @NotNull
    JComponent getComponent() {
        return component;
    }
}
