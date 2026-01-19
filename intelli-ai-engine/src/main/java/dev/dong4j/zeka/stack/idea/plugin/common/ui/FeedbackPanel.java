package dev.dong4j.zeka.stack.idea.plugin.common.ui;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.ide.BrowserUtil;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import dev.dong4j.zeka.stack.idea.plugin.common.ui.component.SpacedJBLabel;
import dev.dong4j.zeka.stack.idea.plugin.common.util.AICommonBundle;
import dev.dong4j.zeka.stack.idea.plugin.common.util.RequestSigner;
import dev.dong4j.zeka.stack.idea.plugin.kit.SiteContents;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * 反馈面板
 * <p>
 * 用于收集用户反馈并提交到反馈服务器。
 * 自动收集插件信息、IDEA 版本和系统信息。
 *
 * @author dong4j
 * @version 1.0.0
 * @date 2025.12.07
 * @since 2.2.0
 */
@Slf4j
@SuppressWarnings("all")
public class FeedbackPanel {
    /**
     * 反馈 API 的 URL 地址
     * <p> todo-dong4j 需要让插件传过来
     *
     * @see #sendHttpRequest(Map)
     */
    private static final String DISCUSSIONS_API_URL = SiteContents.DISCUSSIONS_API_URL;
    // private static final String FEEDBACK_API_URL = "http://127.0.0.1:8080/api/feedback/discussion";
    /** GitHub Discussions 链接 */
    private static final String GITHUB_DISCUSSIONS_URL = SiteContents.GITHUB_DISCUSSIONS_URL;
    /** GitHub Discussions API 路径, 用于提交反馈问题 */
    private static final String DISCUSSIONS_PATH = "/api/plugin/feedback/discussion";
    /** 请求超时时间, 单位为秒, 值为 10 */
    private static final int REQUEST_TIMEOUT_SECONDS = 10;
    /** 链接颜色（主题感知的蓝色） */
    private static final Color LINK_COLOR = new JBColor(new Color(74, 144, 226), new Color(100, 149, 237));

    /** 面板内容 */
    @Getter
    private JPanel content;

    /** 项目对象，可以为 null（应用级设置时） */
    @Nullable
    private final Project project;

    /** 插件 ID（用于获取插件信息） */
    private final String pluginId;

    /** 插件名称 */
    private final String pluginName;

    /** 签名密钥 */
    private final String secret;

    /** 表单组件 */
    private JBTextField titleField;
    /** 反馈内容输入区域, 支持多行文本输入, 自动换行并限制行数 */
    private JBTextArea contentArea;
    /** 反馈类型下拉框, 用于选择反馈类别 (如 bug, 功能建议, 问题咨询等) */
    private JComboBox<String> typeComboBox;
    /** GitHub 用户名输入框, 用于收集用户 GitHub 账号以便关联反馈内容 */
    private JBTextField githubUsernameField;
    /** 提交按钮, 用于提交用户反馈表单 */
    private JButton submitButton;
    /** 状态标签, 用于显示反馈提交结果或提示信息 */
    private JBLabel statusLabel;

    /** 主内容面板 */
    private JPanel mainPanel;

    /**
     * 构造函数
     *
     * @param project    项目对象，可以为 null（应用级设置时）
     * @param pluginId   插件 ID
     * @param pluginName 插件名称
     * @param secret     签名密钥
     */
    public FeedbackPanel(@Nullable Project project, @NotNull String pluginId, @NotNull String pluginName, @NotNull String secret) {
        this.project = project;
        this.pluginId = pluginId;
        this.pluginName = pluginName;
        this.secret = secret;
        createFeedbackPanel();
    }

    /**
     * 创建反馈面板
     */
    private void createFeedbackPanel() {
        // 创建表单组件
        createFormComponents();

        // 创建可折叠容器
        content = createCollapsiblePanel();
    }

    /**
     * 创建表单组件
     */
    private void createFormComponents() {
        // 第一行：反馈类型、讨论类别、GitHub 用户名
        JPanel firstRowPanel = createFirstRowPanel();

        // 标题字段
        titleField = new JBTextField();
        titleField.setToolTipText(AICommonBundle.message("settings.feedback.title.hint"));

        // 内容区域（参考 PromptTemplatesPanel 的实现）
        contentArea = new JBTextArea();
        contentArea.setRows(8);
        contentArea.setLineWrap(true);
        contentArea.setWrapStyleWord(true);
        contentArea.setToolTipText(AICommonBundle.message("settings.feedback.content.hint"));
        JBScrollPane contentScrollPane = new JBScrollPane(contentArea);
        contentScrollPane.setVerticalScrollBarPolicy(javax.swing.JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        contentScrollPane.setHorizontalScrollBarPolicy(javax.swing.JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        // 使用复合边框保留默认边框并添加间距
        javax.swing.border.Border defaultBorder = contentScrollPane.getBorder();
        if (defaultBorder != null) {
            contentScrollPane.setBorder(BorderFactory.createCompoundBorder(defaultBorder, JBUI.Borders.empty(10)));
        } else {
            contentScrollPane.setBorder(JBUI.Borders.empty(10));
        }

        // 状态标签（使用 JBLabel 配合 HTML 支持可点击链接）
        statusLabel = new JBLabel("", javax.swing.SwingConstants.LEFT);
        statusLabel.setVerticalAlignment(javax.swing.SwingConstants.CENTER);
        // 设置标签不换行，确保链接文本在一行显示
        statusLabel.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, statusLabel.getPreferredSize().height));
        // 初始状态显示 Discussions 超链接
        showDiscussionsLink();

        // 按钮面板（右对齐）
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setOpaque(false);

        submitButton = new JButton(AICommonBundle.message("settings.feedback.submit"));
        submitButton.addActionListener(e -> submitFeedback());
        buttonPanel.add(submitButton);

        JButton clearButton = new JButton(AICommonBundle.message("settings.feedback.clear"));
        clearButton.addActionListener(e -> clearForm());
        buttonPanel.add(clearButton);

        // 按钮和状态面板（使用 BorderLayout：左侧显示状态，右侧显示按钮）
        JPanel buttonAndStatusPanel = new JPanel(new BorderLayout(10, 0));
        buttonAndStatusPanel.setOpaque(false);
        buttonAndStatusPanel.add(statusLabel, BorderLayout.WEST);
        buttonAndStatusPanel.add(buttonPanel, BorderLayout.EAST);

        // 使用 FormBuilder 构建表单
        mainPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent(new SpacedJBLabel(AICommonBundle.message("settings.feedback.type")), firstRowPanel)
            .addLabeledComponent(new SpacedJBLabel(AICommonBundle.message("settings.feedback.title.label")), titleField)
            .addLabeledComponent(new SpacedJBLabel(AICommonBundle.message("settings.feedback.content")), contentScrollPane)
            .addLabeledComponent(new SpacedJBLabel(""), buttonAndStatusPanel)  // 状态标签和按钮在同一行，状态标签在第二列
            .getPanel();
    }

    /**
     * 创建第一行面板（反馈类型下拉框、讨论类别、GitHub 用户名）
     * 这个面板会作为 FormBuilder 的 addLabeledComponent 的右侧内容
     * 第一个标签"反馈类型"会由 FormBuilder 添加到标签列，确保对齐
     */
    private JPanel createFirstRowPanel() {
        // 使用 BorderLayout 让 GitHub 用户名输入框填充剩余空间
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setOpaque(false);

        // 左侧面板：反馈类型下拉框、讨论类别标签和下拉框、GitHub 用户名标签
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        leftPanel.setOpaque(false);

        typeComboBox = new JComboBox<>(new String[] {
            AICommonBundle.message("settings.feedback.type.bug"),
            AICommonBundle.message("settings.feedback.type.feature"),
            AICommonBundle.message("settings.feedback.type.question"),
            AICommonBundle.message("settings.feedback.type.other")
        });
        leftPanel.add(typeComboBox);

        SpacedJBLabel githubLabel = new SpacedJBLabel(AICommonBundle.message("settings.feedback.github.username"));
        leftPanel.add(githubLabel);

        // GitHub 用户名输入框放在 CENTER，填充剩余空间
        githubUsernameField = new JBTextField();
        panel.add(leftPanel, BorderLayout.WEST);
        panel.add(githubUsernameField, BorderLayout.CENTER);

        return panel;
    }

    /**
     * 创建可折叠面板
     *
     * @return 可折叠面板
     */
    @NotNull
    private JPanel createCollapsiblePanel() {
        JPanel content = new JPanel();
        content.setLayout(new BorderLayout());

        // 创建标题文本
        final String titleText = AICommonBundle.message("settings.feedback.title");

        // 创建标题面板（不带边框，因为边框在容器上）
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBorder(JBUI.Borders.empty(5));
        titlePanel.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        titlePanel.setOpaque(false);

        // 默认折叠：隐藏内容面板
        mainPanel.setVisible(false);

        // 使用包装面板确保内容左对齐（使用 BorderLayout.NORTH 而不是 CENTER）
        JPanel contentWrapper = new JPanel(new BorderLayout());
        contentWrapper.add(mainPanel, BorderLayout.NORTH);
        contentWrapper.setOpaque(false);

        // 将标题栏和内容面板添加到主面板
        content.add(titlePanel, BorderLayout.NORTH);
        content.add(contentWrapper, BorderLayout.CENTER);

        // 为容器设置 TitledBorder（边框会包围整个区域）
        TitledBorder containerBorder = BorderFactory.createTitledBorder("▶ " + titleText);
        configureTitledBorder(containerBorder);
        content.setBorder(BorderFactory.createCompoundBorder(
            containerBorder,
            JBUI.Borders.empty(5)
                                                            ));
        content.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        content.setOpaque(true);
        content.setBackground(UIUtil.getPanelBackground());

        // 为容器添加点击事件（整个容器都可以点击）
        content.addMouseListener(new MouseAdapter() {
            /**
             * 处理鼠标点击事件, 用于切换面板的可见性并更新标题栏图标
             * <p> 点击时切换 mainPanel 的可见状态, 根据当前状态显示不同的箭头图标 (▼ 或 ▶), 并重新设置边框和重绘内容区域 </p>
             *
             * @param e 鼠标点击事件对象
             */
            @Override
            public void mouseClicked(MouseEvent e) {
                boolean isVisible = mainPanel.isVisible();
                mainPanel.setVisible(!isVisible);

                // 更新容器的 TitledBorder（因为边框在容器上）
                String arrow = !isVisible ? "▼ " : "▶ ";
                TitledBorder containerBorder = BorderFactory.createTitledBorder(arrow + titleText);
                configureTitledBorder(containerBorder);
                content.setBorder(BorderFactory.createCompoundBorder(
                    containerBorder,
                    JBUI.Borders.empty(5)
                                                                    ));

                content.revalidate();
                content.repaint();
            }
        });

        return content;
    }

    /**
     * 提交反馈
     */
    private void submitFeedback() {
        // 验证必填字段
        String title = titleField.getText().trim();
        String contentText = contentArea.getText().trim();

        if (title.isEmpty()) {
            showStatus(AICommonBundle.message("settings.feedback.title.required"), true);
            return;
        }

        if (contentText.isEmpty()) {
            showStatus(AICommonBundle.message("settings.feedback.content.required"), true);
            return;
        }

        // 禁用提交按钮
        submitButton.setEnabled(false);
        showStatus(AICommonBundle.message("settings.feedback.submitting"), false);

        // 在后台线程执行 HTTP 请求
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                // 构建请求体
                Map<String, Object> requestBody = buildRequestBody(title, contentText);

                // 发送 HTTP 请求
                String response = sendHttpRequest(requestBody);

                // 解析响应
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> responseMap = mapper.readValue(response, Map.class);

                // 新的响应结构：外层 success 表示请求是否成功，data.success 表示业务是否成功
                boolean requestSuccess = Boolean.TRUE.equals(responseMap.get("success"));
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) responseMap.get("data");

                // 在 UI 线程更新状态
                ApplicationManager.getApplication().executeOnPooledThread(() -> {
                    submitButton.setEnabled(true);
                    if (requestSuccess && data != null) {
                        // 检查业务是否成功
                        boolean businessSuccess = Boolean.TRUE.equals(data.get("success"));
                        if (businessSuccess) {
                            clearForm();
                            // 解析响应中的 discussion URL
                            @SuppressWarnings("unchecked")
                            Map<String, Object> discussion = (Map<String, Object>) data.get("discussion");
                            if (discussion != null) {
                                String url = (String) discussion.get("url");
                                if (url != null && !url.isEmpty()) {
                                    // 显示可点击的链接
                                    showStatusWithLink(url);
                                } else {
                                    showStatus(AICommonBundle.message("settings.feedback.success"), false);
                                }
                            } else {
                                // 如果没有 discussion，使用 data 中的 message
                                String message = (String) data.getOrDefault("message", AICommonBundle.message("settings.feedback.success"));
                                showStatus(message, false);
                            }
                        } else {
                            // 业务失败，显示错误信息
                            String error = (String) data.getOrDefault("error", "");
                            String message = (String) data.getOrDefault("message", "");
                            log.debug("提交反馈失败: {}", error.isEmpty() ? message : error);
                            showStatus(AICommonBundle.message("settings.feedback.error"), true);
                        }
                    } else {
                        // 请求失败或响应格式不正确
                        String message = (String) responseMap.getOrDefault("message", "");
                        log.debug("提交反馈失败: {}", message.isEmpty() ? "未知错误" : message);
                        showStatus(AICommonBundle.message("settings.feedback.error"), true);
                    }
                });
            } catch (Exception e) {
                log.debug("提交反馈失败: {}", e.getMessage());
                ApplicationManager.getApplication().executeOnPooledThread(() -> {
                    submitButton.setEnabled(true);
                    showStatus(AICommonBundle.message("settings.feedback.error"), true);
                    // 显示错误通知
                    Project targetProject = project != null ? project : ProjectManager.getInstance().getDefaultProject();
                    if (targetProject != null && !targetProject.isDisposed()) {
                        dev.dong4j.zeka.stack.idea.plugin.common.util.NotificationUtil.showError(
                            targetProject, AICommonBundle.message("settings.feedback.error"));
                    }
                });
            }
        });
    }

    /**
     * 构建请求体
     *
     * @param title       标题
     * @param contentText 内容
     * @return 请求体 Map
     */
    @NotNull
    private Map<String, Object> buildRequestBody(@NotNull String title, @NotNull String contentText) {
        Map<String, Object> requestBody = new HashMap<>();

        // 基本信息
        requestBody.put("title", title);
        requestBody.put("content", contentText);

        // 反馈类型
        String typeValue = getTypeValue(typeComboBox.getSelectedIndex());
        requestBody.put("type", typeValue);

        // 讨论类别（固定为 general）
        requestBody.put("category", "GENERAL");

        // 用户信息
        Map<String, Object> userInfo = new HashMap<>();
        String githubUsername = githubUsernameField.getText().trim();
        if (!githubUsername.isEmpty()) {
            userInfo.put("githubUsername", githubUsername);
        }

        // 自动收集的信息
        // 插件名称和版本（使用构造函数传入的值，确保每个插件使用各自的名称和版本）
        userInfo.put("pluginName", pluginName);
        String pluginVersion = getPluginVersion();
        if (pluginVersion != null) {
            userInfo.put("pluginVersion", pluginVersion);
        }
        userInfo.put("ideaVersion", getIdeaVersion());
        userInfo.put("os", getOperatingSystem());

        requestBody.put("userInfo", userInfo);

        // 元数据
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("timestamp", System.currentTimeMillis());
        requestBody.put("metadata", metadata);

        return requestBody;
    }

    /**
     * 发送 HTTP 请求
     *
     * @param requestBody 请求体
     * @return 响应字符串
     * @throws IOException          IO 异常
     * @throws InterruptedException 中断异常
     */
    @NotNull
    private String sendHttpRequest(@NotNull Map<String, Object> requestBody) throws IOException, InterruptedException {
        ObjectMapper mapper = new ObjectMapper();
        String jsonBody = mapper.writeValueAsString(requestBody);

        // 将 JSON 字符串转换为字节数组（用于签名）
        byte[] bodyBytes = jsonBody.getBytes(StandardCharsets.UTF_8);

        String pathWithQuery = DISCUSSIONS_PATH;
        URI uri = URI.create(DISCUSSIONS_API_URL);
        if (uri.getQuery() != null && !uri.getQuery().isEmpty()) {
            pathWithQuery += "?" + uri.getQuery();
        }

        // 生成签名头（使用插件 ID 作为客户端 ID）
        RequestSigner.SignedHeaders signedHeaders;
        try {
            signedHeaders = RequestSigner.sign(pluginId, secret, "POST", pathWithQuery, bodyBytes);
        } catch (Exception e) {
            log.debug("生成请求签名失败", e);
            throw new IOException("生成请求签名失败: " + e.getMessage(), e);
        }

        HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(REQUEST_TIMEOUT_SECONDS))
            .build();

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
            .uri(uri)
            .header("Content-Type", "application/json")
            .header("X-Client-Id", signedHeaders.clientId())
            .header("X-Timestamp", signedHeaders.timestamp())
            .header("X-Nonce", signedHeaders.nonce())
            .header("X-Body-SHA256", signedHeaders.bodySha256())
            .header("X-Signature", signedHeaders.signature())
            .POST(HttpRequest.BodyPublishers.ofByteArray(bodyBytes))
            .timeout(Duration.ofSeconds(REQUEST_TIMEOUT_SECONDS));

        HttpRequest request = requestBuilder.build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    /**
     * 获取反馈类型值
     *
     * @param index 索引
     * @return 类型值
     */
    @NotNull
    private String getTypeValue(int index) {
        switch (index) {
            case 0:
                return "BUG";
            case 1:
                return "FEATURE";
            case 2:
                return "QUESTION";
            case 3:
                return "OTHER";
            default:
                return "OTHER";
        }
    }


    /**
     * 获取插件版本
     *
     * @return 插件版本
     */
    @Nullable
    private String getPluginVersion() {
        try {
            PluginId pluginIdObj = PluginId.getId(pluginId);
            IdeaPluginDescriptor pluginDescriptor = PluginManagerCore.getPlugin(pluginIdObj);
            if (pluginDescriptor != null) {
                return pluginDescriptor.getVersion();
            }
        } catch (Exception e) {
            log.debug("获取插件版本失败", e);
        }
        return null;
    }

    /**
     * 获取 IDEA 版本
     *
     * @return IDEA 版本
     */
    @NotNull
    private String getIdeaVersion() {
        try {
            ApplicationInfo info = ApplicationInfo.getInstance();
            return info.getFullVersion();
        } catch (Exception e) {
            log.debug("获取 IDEA 版本失败", e);
            return "未知";
        }
    }

    /**
     * 获取操作系统信息
     *
     * @return 操作系统信息
     */
    @NotNull
    private String getOperatingSystem() {
        if (SystemInfo.isWindows) {
            return "Windows " + SystemInfo.getOsNameAndVersion();
        } else if (SystemInfo.isMac) {
            return "macOS " + SystemInfo.getOsNameAndVersion();
        } else if (SystemInfo.isLinux) {
            return "Linux " + SystemInfo.getOsNameAndVersion();
        } else {
            return SystemInfo.getOsNameAndVersion();
        }
    }

    /**
     * 格式化元数据为文本
     * <p>
     * 在前面留 2 个空行，然后使用 --- 分割元数据
     *
     * @return 格式化后的元数据文本
     */
    @NotNull
    private String formatMetadata() {
        StringBuilder sb = new StringBuilder();

        // 前面留 2 个空行
        sb.append("\n\n\n");

        // 使用 --- 分割元数据
        sb.append("---\n");

        // 插件信息
        sb.append("Plugin: ").append(pluginName);
        String pluginVersion = getPluginVersion();
        if (pluginVersion != null) {
            sb.append(" (").append(pluginVersion).append(")");
        }
        sb.append("\n");

        // IDEA 版本
        sb.append("IDEA Version: ").append(getIdeaVersion()).append("\n");

        // 操作系统
        sb.append("OS: ").append(getOperatingSystem()).append("\n");

        return sb.toString();
    }

    /**
     * 显示状态信息
     *
     * @param message 消息
     * @param isError 是否为错误
     */
    private void showStatus(@NotNull String message, boolean isError) {
        statusLabel.setText(message);
        statusLabel.setForeground(isError ? JBColor.RED : UIUtil.getLabelForeground());
        // 移除所有鼠标监听器
        removeAllMouseListeners();
        statusLabel.setCursor(java.awt.Cursor.getDefaultCursor());
    }

    /**
     * 显示带链接的状态信息
     * <p>
     * 使用 HTML 格式创建蓝色可点击链接，参考 IntelliAgentPanel 和 PersonalInfoPanel 的实现
     *
     * @param url 链接地址
     */
    private void showStatusWithLink(@NotNull String url) {
        // 使用国际化文本作为链接显示文本
        String linkDisplayText = AICommonBundle.message("settings.feedback.view.discussion");
        showLinkStatus(url, linkDisplayText);
    }

    /**
     * 清空表单
     */
    private void clearForm() {
        titleField.setText("");
        contentArea.setText("");
        typeComboBox.setSelectedIndex(0);
        githubUsernameField.setText("");
        // 恢复显示 Discussions 链接
        showDiscussionsLink();
    }

    /**
     * 显示 Discussions 超链接
     * <p>
     * 在状态标签中显示 Discussions 超链接，作为初始状态或清空表单后的状态
     */
    private void showDiscussionsLink() {
        showLinkStatus(GITHUB_DISCUSSIONS_URL, "\uD83D\uDCACDiscussions");
    }

    /**
     * 显示带链接的状态信息（通用方法）
     * <p>
     * 使用 HTML 格式创建蓝色可点击链接，参考 IntelliAgentPanel 和 PersonalInfoPanel 的实现
     *
     * @param url             链接地址
     * @param linkDisplayText 链接显示文本
     */
    private void showLinkStatus(@NotNull String url, @NotNull String linkDisplayText) {
        // 使用 HTML 格式化链接样式，使用主题感知的蓝色
        // 添加 white-space: nowrap 防止换行
        String linkText = String.format(
            "<html><div style='white-space: nowrap;'><a href='%s' style='color: rgb(%d,%d,%d); text-decoration: underline;" +
            "'>%s</a></div></html>",
            url,
            LINK_COLOR.getRed(),
            LINK_COLOR.getGreen(),
            LINK_COLOR.getBlue(),
            linkDisplayText
                                       );
        statusLabel.setText(linkText);

        // 移除旧的鼠标监听器
        removeAllMouseListeners();

        // 添加点击事件来打开浏览器
        statusLabel.addMouseListener(createLinkMouseAdapter(url));
        statusLabel.setForeground(LINK_COLOR);
    }

    /**
     * 移除状态标签的所有鼠标监听器
     */
    private void removeAllMouseListeners() {
        for (java.awt.event.MouseListener listener : statusLabel.getMouseListeners()) {
            statusLabel.removeMouseListener(listener);
        }
    }

    /**
     * 创建链接鼠标适配器
     * <p>
     * 用于处理链接的点击、鼠标进入和移出事件
     *
     * @param url 链接地址
     * @return 鼠标适配器
     */
    @NotNull
    private MouseAdapter createLinkMouseAdapter(@NotNull String url) {
        return new MouseAdapter() {
            /**
             * 处理鼠标点击事件, 打开指定 URL
             * <p> 当用户点击时, 调用 {@code BrowserUtil.browse(url)} 方法在默认浏览器中打开指定 URL</p>
             *
             * @param e 鼠标点击事件对象
             */
            @Override
            public void mouseClicked(MouseEvent e) {
                BrowserUtil.browse(url);
            }

            /**
             * 当鼠标进入组件时, 将状态标签的光标设置为手型光标
             * <p> 此方法用于在鼠标悬停于组件上时, 通过设置光标样式提示用户可点击操作
             *
             * @param e 鼠标事件对象, 包含鼠标进入位置等信息
             */
            @Override
            public void mouseEntered(MouseEvent e) {
                statusLabel.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
            }

            /**
             * 鼠标移出时将状态标签的光标恢复为默认光标
             * <p> 当鼠标从组件上移出时, 将状态标签的光标设置为系统默认光标, 以提供一致的用户体验
             *
             * @param e 鼠标事件对象, 包含鼠标位置和状态信息
             */
            @Override
            public void mouseExited(MouseEvent e) {
                statusLabel.setCursor(java.awt.Cursor.getDefaultCursor());
            }
        };
    }

    /**
     * 配置 TitledBorder 的字体和颜色
     *
     * @param titledBorder 要配置的 TitledBorder
     */
    private void configureTitledBorder(@NotNull TitledBorder titledBorder) {
        titledBorder.setTitleFont(javax.swing.UIManager.getFont("Label.font"));
        Color titleColor = UIUtil.getLabelForeground();
        titledBorder.setTitleColor(titleColor);
    }
}
