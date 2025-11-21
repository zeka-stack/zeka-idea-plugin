package dev.dong4j.zeka.stack.idea.plugin.nacos.ui.components;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.ui.JBUI;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.BorderLayout;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import javax.swing.JButton;
import javax.swing.JPanel;

import dev.dong4j.zeka.stack.idea.plugin.nacos.client.NacosClient;
import dev.dong4j.zeka.stack.idea.plugin.nacos.client.NacosClientUtils;
import dev.dong4j.zeka.stack.idea.plugin.nacos.client.model.ConfigHistoryItem;
import dev.dong4j.zeka.stack.idea.plugin.nacos.util.NacosBundle;

/**
 * 配置对比面板
 * 支持 2 面板（本地 vs 云端）和 3 面板（本地 vs 云端 vs 历史版本）对比
 *
 * @author dong4j
 * @since 1.0.0
 */
public class ConfigComparePanel extends JPanel {
    private static final Logger LOG = Logger.getInstance(ConfigComparePanel.class);

    private final Project project;
    private final boolean includeHistory;
    private final String fileType;
    private final String namespace;
    private final String group;
    private final String dataId;

    // 编辑器面板
    private JsonEditor localEditor;
    private JsonEditor remoteEditor;
    private JsonEditor historyEditor;

    // 布局组件
    private JBSplitter mainSplitter;
    private JBSplitter rightSplitter; // 用于 3 面板模式

    // 本地文件相关
    @Nullable
    private VirtualFile localFile;
    private final String originalLocalContent;

    // 修改状态监听
    @Nullable
    private Consumer<Boolean> modificationListener;

    // 历史版本相关
    @Nullable
    private ComboBox<ConfigHistoryItem> historyVersionComboBox;
    @Nullable
    private List<ConfigHistoryItem> historyItems;

    /**
     * 构造函数
     *
     * @param project        项目实例
     * @param fileType       文件类型
     * @param namespace      命名空间
     * @param group          分组
     * @param dataId         数据 ID
     * @param localContent   本地配置内容
     * @param remoteContent  远程配置内容
     * @param localFilePath  本地文件路径（可选）
     * @param includeHistory 是否包含历史版本面板
     */
    public ConfigComparePanel(@NotNull Project project,
                              @NotNull String fileType,
                              @Nullable String namespace,
                              @Nullable String group,
                              @NotNull String dataId,
                              @NotNull String localContent,
                              @NotNull String remoteContent,
                              @Nullable Path localFilePath,
                              boolean includeHistory) {
        this.project = project;
        this.fileType = fileType;
        this.namespace = namespace;
        this.group = group;
        this.dataId = dataId;
        this.includeHistory = includeHistory;
        this.originalLocalContent = localContent;

        // 尝试获取本地文件
        if (localFilePath != null) {
            localFile = LocalFileSystem.getInstance().findFileByPath(localFilePath.toString());
        }

        initialize(localContent, remoteContent);
    }

    private void initialize(@NotNull String localContent, @NotNull String remoteContent) {
        setLayout(new BorderLayout());
        setBorder(JBUI.Borders.empty(5));

        // 创建编辑器
        localEditor = createEditor(fileType, false);
        remoteEditor = createEditor(fileType, true);
        if (includeHistory) {
            historyEditor = createEditor(fileType, true);
        }

        // 设置内容
        localEditor.setContent(localContent);
        localEditor.markClean();
        remoteEditor.setContent(remoteContent);

        // 如果是历史版本面板，设置初始提示
        if (includeHistory && historyEditor != null) {
            historyEditor.setContent(NacosBundle.message("compare.history.loading"));
        }

        // 创建布局
        createLayout();

        // 添加修改监听
        attachModificationListener();
    }

    private JsonEditor createEditor(@NotNull String fileType, boolean readOnly) {
        JsonEditor editor = new JsonEditor(project);
        editor.setFileType(toLanguageId(fileType));
        editor.setReadOnly(readOnly);
        return editor;
    }

    private String toLanguageId(String type) {
        String normalized = StringUtil.notNullize(type, "yaml").toLowerCase();
        return switch (normalized) {
            case "yaml", "yml" -> "YAML";
            case "json" -> "JSON";
            case "xml" -> "XML";
            case "properties" -> "Properties";
            case "html" -> "HTML";
            default -> "TEXT";
        };
    }

    private void createLayout() {
        if (includeHistory) {
            // 3 面板模式
            createThreePanelLayout();
        } else {
            // 2 面板模式
            createTwoPanelLayout();
        }
    }

    private void createTwoPanelLayout() {
        mainSplitter = new JBSplitter(false, 0.5f);
        mainSplitter.setFirstComponent(createLocalPanel());
        mainSplitter.setSecondComponent(createRemotePanel());
        mainSplitter.setDividerWidth(5);

        add(mainSplitter, BorderLayout.CENTER);
    }

    private void createThreePanelLayout() {
        mainSplitter = new JBSplitter(false, 0.33f);
        mainSplitter.setFirstComponent(createLocalPanel());

        rightSplitter = new JBSplitter(false, 0.5f);
        rightSplitter.setFirstComponent(createRemotePanel());
        rightSplitter.setSecondComponent(createHistoryPanel());
        rightSplitter.setDividerWidth(5);

        mainSplitter.setSecondComponent(rightSplitter);
        mainSplitter.setDividerWidth(5);

        add(mainSplitter, BorderLayout.CENTER);
    }

    private JPanel createLocalPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(JBUI.Borders.empty(5));

        // 标题栏
        JPanel header = new JPanel(new BorderLayout());
        JBLabel title = new JBLabel(NacosBundle.message("compare.panel.local"));
        header.add(title, BorderLayout.WEST);

        // 保存按钮
        if (localFile != null) {
            JButton saveButton = new JButton(NacosBundle.message("compare.toolbar.saveLocal"));
            saveButton.addActionListener(e -> saveLocalFile());
            header.add(saveButton, BorderLayout.EAST);
        }

        panel.add(header, BorderLayout.NORTH);
        panel.add(localEditor, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createRemotePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(JBUI.Borders.empty(5));

        JBLabel title = new JBLabel(NacosBundle.message("compare.panel.remote"));
        panel.add(title, BorderLayout.NORTH);
        panel.add(remoteEditor, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createHistoryPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(JBUI.Borders.empty(5));

        // 标题栏
        JPanel header = new JPanel(new BorderLayout());
        JBLabel title = new JBLabel(NacosBundle.message("compare.panel.history"));
        header.add(title, BorderLayout.WEST);

        // 版本选择下拉框
        if (includeHistory) {
            historyVersionComboBox = new ComboBox<>();
            historyVersionComboBox.setRenderer((list, value, index, isSelected, cellHasFocus) -> {
                if (value == null) {
                    return new JBLabel(NacosBundle.message("compare.history.select.version"));
                }
                String displayText = formatHistoryVersion(value);
                return new JBLabel(displayText);
            });
            historyVersionComboBox.addActionListener(e -> {
                ConfigHistoryItem selected = (ConfigHistoryItem) historyVersionComboBox.getSelectedItem();
                if (selected != null) {
                    loadHistoryVersion(selected);
                }
            });
            header.add(historyVersionComboBox, BorderLayout.EAST);
        }

        panel.add(header, BorderLayout.NORTH);

        if (historyEditor != null) {
            panel.add(historyEditor, BorderLayout.CENTER);
        }

        return panel;
    }

    private String formatHistoryVersion(@NotNull ConfigHistoryItem item) {
        String time = item.getLastModifiedTime() != null ? item.getLastModifiedTime() : "";
        String id = item.getId() != null ? item.getId() : "";
        return NacosBundle.message("compare.history.version.format", time, id);
    }

    /**
     * 加载历史版本列表
     */
    @SuppressWarnings("D")
    public void loadHistoryVersions() {
        if (!includeHistory) {
            return;
        }

        CompletableFuture
            .supplyAsync(() -> {
                try {
                    NacosClient client = NacosClientUtils.getDefaultClient();
                    if (client == null) {
                        throw new IllegalStateException("Nacos client not configured");
                    }
                    return client.getConfigHistoryList(namespace, group, dataId, 1, 100);
                } catch (Exception e) {
                    LOG.error("Failed to load history versions", e);
                    throw new RuntimeException(e);
                }
            }, AppExecutorUtil.getAppExecutorService())
            .whenComplete((items, throwable) -> {
                ApplicationManager.getApplication().invokeLater(() -> {
                    if (throwable != null) {
                        LOG.error("Failed to load history versions", throwable);
                        if (historyVersionComboBox != null) {
                            historyVersionComboBox.removeAllItems();
                        }
                        if (historyEditor != null) {
                            historyEditor.setContent(NacosBundle.message("compare.error.history.load", throwable.getMessage()));
                        }
                        return;
                    }

                    historyItems = items;
                    if (historyVersionComboBox != null) {
                        historyVersionComboBox.removeAllItems();
                        if (items != null && !items.isEmpty()) {
                            for (ConfigHistoryItem item : items) {
                                historyVersionComboBox.addItem(item);
                            }
                            // 默认选中第一个（最新的）
                            historyVersionComboBox.setSelectedIndex(0);
                        } else {
                            if (historyEditor != null) {
                                historyEditor.setContent(NacosBundle.message("compare.history.empty"));
                            }
                        }
                    }
                });
            });
    }

    /**
     * 加载指定历史版本的内容
     *
     * @param item 历史版本项
     */
    @SuppressWarnings("D")
    private void loadHistoryVersion(@NotNull ConfigHistoryItem item) {
        if (historyEditor == null) {
            return;
        }

        // 显示加载中
        historyEditor.setContent(NacosBundle.message("compare.history.loading"));

        CompletableFuture
            .supplyAsync(() -> {
                try {
                    NacosClient client = NacosClientUtils.getDefaultClient();
                    if (client == null) {
                        throw new IllegalStateException("Nacos client not configured");
                    }
                    long nid = Long.parseLong(item.getId());
                    ConfigHistoryItem historyItem = client.getConfigHistory(namespace, group, dataId, nid);
                    return historyItem.getContent();
                } catch (Exception e) {
                    LOG.error("Failed to load history version", e);
                    throw new RuntimeException(e);
                }
            }, AppExecutorUtil.getAppExecutorService())
            .whenComplete((content, throwable) -> {
                ApplicationManager.getApplication().invokeLater(() -> {
                    if (throwable != null) {
                        LOG.error("Failed to load history version", throwable);
                        if (historyEditor != null) {
                            historyEditor.setContent(NacosBundle.message("compare.error.history.load", throwable.getMessage()));
                        }
                        return;
                    }

                    if (historyEditor != null && content != null) {
                        historyEditor.setContent(content);
                    }
                });
            });
    }

    private void attachModificationListener() {
        localEditor.getEditorTextField().getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void documentChanged(@NotNull DocumentEvent event) {
                boolean modified = localEditor.isModified();
                if (modificationListener != null) {
                    modificationListener.accept(modified);
                }
            }
        });
    }

    /**
     * 保存本地文件
     */
    private void saveLocalFile() {
        if (localFile == null) {
            LOG.warn("Local file is null, cannot save");
            return;
        }

        String content = localEditor.getContent();
        ApplicationManager.getApplication().invokeLater(() -> {
            ApplicationManager.getApplication().runWriteAction(() -> {
                try {
                    FileDocumentManager documentManager = FileDocumentManager.getInstance();
                    com.intellij.openapi.editor.Document document = documentManager.getDocument(localFile);
                    if (document != null) {
                        WriteCommandAction.writeCommandAction(project).run(() -> {
                            document.setText(content);
                            documentManager.saveDocument(document);
                        });
                        localEditor.markClean();
                        if (modificationListener != null) {
                            modificationListener.accept(false);
                        }
                    }
                } catch (Exception e) {
                    LOG.error("Failed to save local file", e);
                }
            });
        });
    }

    /**
     * 设置修改状态监听器
     *
     * @param listener 监听器
     */
    public void setModificationListener(@Nullable Consumer<Boolean> listener) {
        this.modificationListener = listener;
    }

    /**
     * 检查本地内容是否已修改
     *
     * @return 是否已修改
     */
    public boolean isLocalModified() {
        return localEditor.isModified();
    }

    /**
     * 获取本地编辑器内容
     *
     * @return 本地内容
     */
    public String getLocalContent() {
        return localEditor.getContent();
    }

    /**
     * 设置历史版本内容
     *
     * @param content 历史版本内容
     */
    public void setHistoryContent(@NotNull String content) {
        if (historyEditor != null) {
            historyEditor.setContent(content);
        }
    }

    /**
     * 获取历史版本编辑器
     *
     * @return 历史版本编辑器（3面板模式）或 null
     */
    @Nullable
    public JsonEditor getHistoryEditor() {
        return historyEditor;
    }
}

