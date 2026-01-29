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
 * IntelliAI Chat 面板
 */
final class AIChatPanel implements Disposable {
    private final Project project;
    private final JComponent component;
    private final JBCefBrowser browser;
    private final AIChatMessageHandler messageHandler;

    private FileEditorManagerListener editorListener;
    private SelectionListener selectionListener;
    private AIProviderSettingsListener providerSettingsListener;
    private Alarm selectionUpdateAlarm;
    private String lastSelectionPayload;

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
            @Override
            public void selectionChanged(@NotNull FileEditorManagerEvent event) {
                scheduleSelectionUpdate();
            }
        };
        selectionListener = new SelectionListener() {
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

    @Override
    public void dispose() {
        // listeners are disposed via Disposer registration
    }

    @NotNull
    JComponent getComponent() {
        return component;
    }
}
