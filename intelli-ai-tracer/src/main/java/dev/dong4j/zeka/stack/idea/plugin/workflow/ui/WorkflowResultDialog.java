// package dev.dong4j.zeka.stack.idea.plugin.workflow.ui;
//
// import com.intellij.ide.BrowserUtil;
// import com.intellij.openapi.ide.CopyPasteManager;
// import com.intellij.openapi.project.Project;
// import com.intellij.openapi.ui.DialogWrapper;
// import com.intellij.ui.components.JBScrollPane;
// import com.intellij.util.ui.JBUI;
// import com.intellij.util.ui.components.BorderLayoutPanel;
//
// import org.jetbrains.annotations.NotNull;
// import org.jetbrains.annotations.Nullable;
//
// import java.awt.BorderLayout;
// import java.awt.datatransfer.StringSelection;
// import java.awt.event.ActionEvent;
// import java.nio.charset.StandardCharsets;
// import java.util.Base64;
//
// import javax.swing.AbstractAction;
// import javax.swing.Action;
// import javax.swing.JComponent;
// import javax.swing.JEditorPane;
// import javax.swing.JPanel;
// import javax.swing.event.HyperlinkEvent;
// import javax.swing.event.HyperlinkListener;
//
// import dev.dong4j.zeka.stack.idea.plugin.workflow.util.WorkflowBundle;
//
// /**
//  * 工作流结果展示对话框
//  * <p>
//  * 支持 Markdown 渲染和复制功能
//  *
//  * @author dong4j
//  * @version 1.0.0
//  */
// public class WorkflowResultDialog extends DialogWrapper {
//
//     /** Markdown 原始文本 */
//     private final String markdownText;
//     /** HTML 渲染内容 */
//     private final String htmlContent;
//     /** HTML 渲染面板 */
//     private JEditorPane editorPane;
//
//     /**
//      * 创建工作流结果对话框
//      *
//      * @param project     项目对象
//      * @param markdownText Markdown 原始文本
//      */
//     public WorkflowResultDialog(@Nullable Project project, @NotNull String markdownText) {
//         super(project);
//         this.markdownText = markdownText;
//         this.htmlContent = convertMarkdownToHtml(markdownText);
//         setTitle(WorkflowBundle.message("notification.success.title"));
//         setModal(false);
//         init();
//     }
//
//     /**
//      * 创建中心面板
//      *
//      * @return 包含 Markdown 渲染面板的组件
//      */
//     @Override
//     protected @Nullable JComponent createCenterPanel() {
//         JPanel panel = new JPanel(new BorderLayout());
//         panel.setPreferredSize(JBUI.size(900, 700));
//
//         // 尝试使用 JCEF 渲染（支持 Mermaid）
//         JComponent browserComponent = createBrowserComponent();
//         if (browserComponent != null) {
//             panel.add(browserComponent, BorderLayout.CENTER);
//             return panel;
//         }
//
//         // 降级方案：使用 JEditorPane（不支持 Mermaid）
//         editorPane = new JEditorPane();
//         editorPane.setContentType("text/html");
//         editorPane.setText(htmlContent);
//         editorPane.setEditable(false);
//         editorPane.setBackground(javax.swing.UIManager.getColor("Panel.background"));
//
//         // 添加超链接支持
//         editorPane.addHyperlinkListener(new HyperlinkListener() {
//             @Override
//             public void hyperlinkUpdate(HyperlinkEvent e) {
//                 if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
//                     BrowserUtil.browse(e.getURL());
//                 }
//             }
//         });
//
//         JBScrollPane scrollPane = new JBScrollPane(editorPane);
//         scrollPane.setBorder(JBUI.Borders.empty(10));
//         panel.add(scrollPane, BorderLayout.CENTER);
//
//         return panel;
//     }
//
//     /**
//      * 创建浏览器组件（支持 Mermaid 渲染）
//      *
//      * @return 浏览器组件，如果 JCEF 不可用则返回 null
//      */
//     @Nullable
//     private JComponent createBrowserComponent() {
//         try {
//             // 尝试使用 IntelliJ Platform 的 JCEF API
//             // 可能的类名：com.intellij.ui.jcef.JBCefBrowser 或 org.jetbrains.cef.JBCefBrowser
//             Class<?> jbCefBrowserClass = null;
//             try {
//                 jbCefBrowserClass = Class.forName("com.intellij.ui.jcef.JBCefBrowser");
//             } catch (ClassNotFoundException e) {
//                 try {
//                     jbCefBrowserClass = Class.forName("org.jetbrains.cef.JBCefBrowser");
//                 } catch (ClassNotFoundException ex) {
//                     // JCEF 不可用
//                     return null;
//                 }
//             }
//
//             // 创建浏览器实例
//             Object browser = jbCefBrowserClass.getConstructor().newInstance();
//
//             // 创建包含 Mermaid.js 的 HTML
//             String htmlWithMermaid = createHtmlWithMermaid();
//
//             // 使用 data URL 加载 HTML
//             String dataUrl = "data:text/html;base64," +
//                 Base64.getEncoder().encodeToString(htmlWithMermaid.getBytes(StandardCharsets.UTF_8));
//
//             // 调用 loadURL 方法
//             jbCefBrowserClass.getMethod("loadURL", String.class).invoke(browser, dataUrl);
//
//             // 获取组件
//             JComponent component = (JComponent) jbCefBrowserClass.getMethod("getComponent").invoke(browser);
//             return component;
//         } catch (Exception e) {
//             // JCEF 不可用或出错，返回 null 使用降级方案
//             return null;
//         }
//     }
//
//     /**
//      * 创建操作按钮
//      *
//      * @return 操作按钮数组
//      */
//     @Override
//     protected Action[] createActions() {
//         // 复制按钮
//         Action copyAction = new AbstractAction(WorkflowBundle.message("dialog.copy.markdown")) {
//             @Override
//             public void actionPerformed(ActionEvent e) {
//                 copyMarkdownToClipboard();
//             }
//         };
//
//         // OK 按钮
//         Action okAction = getOKAction();
//         okAction.putValue(Action.NAME, WorkflowBundle.message("dialog.close"));
//
//         return new Action[]{copyAction, okAction};
//     }
//
//     /**
//      * 复制 Markdown 原始文本到剪贴板
//      */
//     private void copyMarkdownToClipboard() {
//         CopyPasteManager.getInstance().setContents(new StringSelection(markdownText));
//         // 显示提示（可选）
//         // NotificationUtil.showInfo(getProject(), WorkflowBundle.message("dialog.copy.success"));
//     }
//
//     /**
//      * 将 Markdown 转换为 HTML
//      * <p>
//      * 这是一个简化的实现，支持基本的 Markdown 语法
//      * 如果需要更完整的支持，可以使用第三方库如 CommonMark
//      *
//      * @param markdown Markdown 文本
//      * @return HTML 内容
//      */
//     @NotNull
//     private String convertMarkdownToHtml(@NotNull String markdown) {
//         StringBuilder html = new StringBuilder();
//         html.append("<html><head><style>");
//         html.append("body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
//         padding: 15px; line-height: 1.6; color: #333; }");
//         html.append("h1 { color: #2c3e50; border-bottom: 2px solid #3498db; padding-bottom: 10px; }");
//         html.append("h2 { color: #34495e; border-bottom: 1px solid #bdc3c7; padding-bottom: 5px; margin-top: 20px; }");
//         html.append("h3 { color: #7f8c8d; margin-top: 15px; }");
//         html.append("code { background-color: #f4f4f4; padding: 2px 6px; border-radius: 3px; font-family: 'Courier New', monospace;
//         font-size: 0.9em; }");
//         html.append("pre { background-color: #f8f8f8; border: 1px solid #ddd; border-radius: 4px; padding: 10px; overflow-x: auto; }");
//         html.append("pre code { background-color: transparent; padding: 0; }");
//         html.append("blockquote { border-left: 4px solid #3498db; margin: 0; padding-left: 15px; color: #7f8c8d; }");
//         html.append("ul, ol { margin: 10px 0; padding-left: 30px; }");
//         html.append("li { margin: 5px 0; }");
//         html.append("table { border-collapse: collapse; width: 100%; margin: 15px 0; }");
//         html.append("th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }");
//         html.append("th { background-color: #3498db; color: white; }");
//         html.append("a { color: #3498db; text-decoration: none; }");
//         html.append("a:hover { text-decoration: underline; }");
//         html.append("</style></head><body>");
//
//         // 简单的 Markdown 转 HTML 实现
//         String[] lines = markdown.split("\n");
//         boolean inCodeBlock = false;
//         boolean inMermaidBlock = false;
//         boolean inList = false;
//         boolean isOrderedList = false;
//         StringBuilder codeBlock = new StringBuilder();
//         String codeBlockLanguage = null;
//
//         for (String line : lines) {
//             if (line.trim().startsWith("```")) {
//                 // 结束列表（如果有）
//                 if (inList) {
//                     html.append(isOrderedList ? "</ol>" : "</ul>");
//                     inList = false;
//                 }
//
//                 if (inCodeBlock || inMermaidBlock) {
//                     // 结束代码块
//                     if (inMermaidBlock) {
//                         // Mermaid 图表使用特殊的 div 容器
//                         html.append("<div class=\"mermaid\">").append(codeBlock.toString()).append("</div>");
//                     } else {
//                         html.append("<pre><code>").append(escapeHtml(codeBlock.toString())).append("</code></pre>");
//                     }
//                     codeBlock.setLength(0);
//                     inCodeBlock = false;
//                     inMermaidBlock = false;
//                     codeBlockLanguage = null;
//                 } else {
//                     // 开始代码块
//                     String trimmed = line.trim();
//                     if (trimmed.length() > 3) {
//                         codeBlockLanguage = trimmed.substring(3).trim().toLowerCase();
//                         inMermaidBlock = "mermaid".equals(codeBlockLanguage);
//                     }
//                     inCodeBlock = true;
//                 }
//                 continue;
//             }
//
//             if (inCodeBlock || inMermaidBlock) {
//                 codeBlock.append(line).append("\n");
//                 continue;
//             }
//
//             // 处理标题
//             if (line.startsWith("# ")) {
//                 if (inList) {
//                     html.append(isOrderedList ? "</ol>" : "</ul>");
//                     inList = false;
//                 }
//                 html.append("<h1>").append(processInlineMarkdown(line.substring(2))).append("</h1>");
//             } else if (line.startsWith("## ")) {
//                 if (inList) {
//                     html.append(isOrderedList ? "</ol>" : "</ul>");
//                     inList = false;
//                 }
//                 html.append("<h2>").append(processInlineMarkdown(line.substring(3))).append("</h2>");
//             } else if (line.startsWith("### ")) {
//                 if (inList) {
//                     html.append(isOrderedList ? "</ol>" : "</ul>");
//                     inList = false;
//                 }
//                 html.append("<h3>").append(processInlineMarkdown(line.substring(4))).append("</h3>");
//             } else if (line.startsWith("#### ")) {
//                 if (inList) {
//                     html.append(isOrderedList ? "</ol>" : "</ul>");
//                     inList = false;
//                 }
//                 html.append("<h4>").append(processInlineMarkdown(line.substring(5))).append("</h4>");
//             } else if (line.trim().startsWith("- ") || line.trim().startsWith("* ")) {
//                 // 无序列表项
//                 if (inList && isOrderedList) {
//                     html.append("</ol>");
//                     inList = false;
//                 }
//                 if (!inList) {
//                     html.append("<ul>");
//                     inList = true;
//                     isOrderedList = false;
//                 }
//                 String item = line.trim().substring(2);
//                 html.append("<li>").append(processInlineMarkdown(item)).append("</li>");
//             } else if (line.trim().matches("^\\d+\\.\\s+.*")) {
//                 // 有序列表
//                 if (inList && !isOrderedList) {
//                     html.append("</ul>");
//                     inList = false;
//                 }
//                 if (!inList) {
//                     html.append("<ol>");
//                     inList = true;
//                     isOrderedList = true;
//                 }
//                 String item = line.trim().replaceFirst("^\\d+\\.\\s+", "");
//                 html.append("<li>").append(processInlineMarkdown(item)).append("</li>");
//             } else if (line.trim().isEmpty()) {
//                 // 空行，结束列表
//                 if (inList) {
//                     html.append(isOrderedList ? "</ol>" : "</ul>");
//                     inList = false;
//                 }
//                 html.append("<p></p>");
//             } else {
//                 // 普通段落
//                 if (inList) {
//                     html.append(isOrderedList ? "</ol>" : "</ul>");
//                     inList = false;
//                 }
//                 html.append("<p>").append(processInlineMarkdown(line)).append("</p>");
//             }
//         }
//
//         // 处理剩余的代码块
//         if ((inCodeBlock || inMermaidBlock) && codeBlock.length() > 0) {
//             if (inMermaidBlock) {
//                 html.append("<div class=\"mermaid\">").append(codeBlock.toString()).append("</div>");
//             } else {
//                 html.append("<pre><code>").append(escapeHtml(codeBlock.toString())).append("</code></pre>");
//             }
//         }
//
//         // 结束列表
//         if (inList) {
//             html.append(isOrderedList ? "</ol>" : "</ul>");
//         }
//
//         html.append("</body></html>");
//         return html.toString();
//     }
//
//     /**
//      * 处理行内 Markdown 语法
//      *
//      * @param text 文本
//      * @return 处理后的 HTML
//      */
//     @NotNull
//     private String processInlineMarkdown(@NotNull String text) {
//         // 处理粗体 **text**
//         text = text.replaceAll("\\*\\*(.+?)\\*\\*", "<strong>$1</strong>");
//         // 处理斜体 *text*
//         text = text.replaceAll("\\*(.+?)\\*", "<em>$1</em>");
//         // 处理行内代码 `code`
//         text = text.replaceAll("`([^`]+)`", "<code>$1</code>");
//         // 处理链接 [text](url)
//         text = text.replaceAll("\\[([^\\]]+)\\]\\(([^\\)]+)\\)", "<a href=\"$2\">$1</a>");
//         return escapeHtml(text);
//     }
//
//     /**
//      * 创建包含 Mermaid.js 的 HTML
//      *
//      * @return 包含 Mermaid.js 的完整 HTML
//      */
//     @NotNull
//     private String createHtmlWithMermaid() {
//         // 重新生成 HTML，这次包含 Mermaid 支持
//         StringBuilder html = new StringBuilder();
//         html.append("<!DOCTYPE html>");
//         html.append("<html><head>");
//         html.append("<meta charset=\"UTF-8\">");
//         html.append("<style>");
//         html.append("body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
//         padding: 15px; line-height: 1.6; color: #333; }");
//         html.append("h1 { color: #2c3e50; border-bottom: 2px solid #3498db; padding-bottom: 10px; }");
//         html.append("h2 { color: #34495e; border-bottom: 1px solid #bdc3c7; padding-bottom: 5px; margin-top: 20px; }");
//         html.append("h3 { color: #7f8c8d; margin-top: 15px; }");
//         html.append("code { background-color: #f4f4f4; padding: 2px 6px; border-radius: 3px; font-family: 'Courier New', monospace;
//         font-size: 0.9em; }");
//         html.append("pre { background-color: #f8f8f8; border: 1px solid #ddd; border-radius: 4px; padding: 10px; overflow-x: auto; }");
//         html.append("pre code { background-color: transparent; padding: 0; }");
//         html.append("blockquote { border-left: 4px solid #3498db; margin: 0; padding-left: 15px; color: #7f8c8d; }");
//         html.append("ul, ol { margin: 10px 0; padding-left: 30px; }");
//         html.append("li { margin: 5px 0; }");
//         html.append("table { border-collapse: collapse; width: 100%; margin: 15px 0; }");
//         html.append("th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }");
//         html.append("th { background-color: #3498db; color: white; }");
//         html.append("a { color: #3498db; text-decoration: none; }");
//         html.append("a:hover { text-decoration: underline; }");
//         html.append(".mermaid { margin: 20px 0; text-align: center; background-color: #f9f9f9; padding: 15px; border-radius: 4px; }");
//         html.append("</style>");
//         html.append("<script src=\"https://cdn.jsdelivr.net/npm/mermaid@10/dist/mermaid.min.js\"></script>");
//         html.append("<script>");
//         html.append("mermaid.initialize({ startOnLoad: true, theme: 'default' });");
//         html.append("</script>");
//         html.append("</head><body>");
//
//         // 提取 body 内容（去掉原有的 html/head/body 标签）
//         String bodyContent = htmlContent;
//         if (bodyContent.contains("<body>")) {
//             int bodyStart = bodyContent.indexOf("<body>") + 6;
//             int bodyEnd = bodyContent.indexOf("</body>");
//             if (bodyEnd > bodyStart) {
//                 bodyContent = bodyContent.substring(bodyStart, bodyEnd);
//             }
//         } else if (bodyContent.contains("<html>")) {
//             // 如果没有 body 标签，尝试提取 html 标签之间的内容
//             int htmlStart = bodyContent.indexOf("<html>");
//             if (htmlStart >= 0) {
//                 bodyContent = bodyContent.substring(htmlStart + 6);
//                 if (bodyContent.endsWith("</html>")) {
//                     bodyContent = bodyContent.substring(0, bodyContent.length() - 7);
//                 }
//             }
//         }
//
//         html.append(bodyContent);
//         html.append("</body></html>");
//         return html.toString();
//     }
//
//     /**
//      * 转义 HTML 特殊字符
//      *
//      * @param text 文本
//      * @return 转义后的文本
//      */
//     @NotNull
//     private String escapeHtml(@NotNull String text) {
//         return text.replace("&", "&amp;")
//                    .replace("<", "&lt;")
//                    .replace(">", "&gt;")
//                    .replace("\"", "&quot;")
//                    .replace("'", "&#39;");
//     }
// }
//
