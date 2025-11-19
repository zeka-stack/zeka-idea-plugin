package dev.dong4j.zeka.stack.idea.plugin.nacos.ui.toolwindow;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.JBSplitter;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.ui.JBUI;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.BorderLayout;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.tree.DefaultMutableTreeNode;

import dev.dong4j.zeka.stack.idea.plugin.nacos.client.NacosClient;
import dev.dong4j.zeka.stack.idea.plugin.nacos.client.NacosClientUtils;
import dev.dong4j.zeka.stack.idea.plugin.nacos.client.model.ConfigInfo;
import dev.dong4j.zeka.stack.idea.plugin.nacos.client.model.ConfigInfoWrapper;
import dev.dong4j.zeka.stack.idea.plugin.nacos.client.model.Namespace;
import dev.dong4j.zeka.stack.idea.plugin.nacos.entity.ConfigFile;
import dev.dong4j.zeka.stack.idea.plugin.nacos.service.CompareConfigService;
import dev.dong4j.zeka.stack.idea.plugin.nacos.settings.SettingsState;
import dev.dong4j.zeka.stack.idea.plugin.nacos.ui.components.JsonEditor;
import dev.dong4j.zeka.stack.idea.plugin.nacos.util.NacosBundle;
import dev.dong4j.zeka.stack.idea.plugin.nacos.util.NotificationUtil;

/**
 * Nacos 工具窗口主面板
 * 整合所有 UI 组件的主容器
 *
 * @author dong4j
 * @since 1.0.0
 */
public class NacosToolWindow {
    private static final Key<NacosToolWindow> TOOL_WINDOW_KEY = Key.create("dev.dong4j.zeka.stack.idea.plugin.nacos.toolwindow");

    private final Project project;
    private ToolWindow toolWindow;
    private final JPanel mainPanel;
    private final ToolBarPanel toolBarPanel;
    private final TreePanel treePanel;
    private final TabBar tabBar;
    private final ConfigOperationPanel configOperationPanel;
    private final JBSplitter splitter;
    private int newTabCounter = 1;

    public NacosToolWindow(@NotNull Project project) {
        this.project = project;
        this.toolBarPanel = new ToolBarPanel(project);
        this.treePanel = new TreePanel(project);
        this.tabBar = new TabBar(project);
        this.configOperationPanel = new ConfigOperationPanel(project, this);
        this.splitter = new JBSplitter(false, 0.3f);

        this.mainPanel = new JPanel(new BorderLayout());

        initialize();
    }

    private void initialize() {
        // 设置主面板边框
        mainPanel.setBorder(JBUI.Borders.empty(5));

        // 设置分割面板（使用 JBSplitter 提供更好的拖动体验）
        splitter.setFirstComponent(treePanel);
        JPanel rightPanel = new JPanel(new BorderLayout());
        // 为右侧面板添加边框
        rightPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UIManager.getColor("Separator.separatorColor")),
            JBUI.Borders.empty(5)
                                                               ));
        rightPanel.add(configOperationPanel, BorderLayout.NORTH);
        rightPanel.add(tabBar, BorderLayout.CENTER);
        splitter.setSecondComponent(rightPanel);
        splitter.setDividerWidth(5); // 设置分割线宽度，方便拖动

        // 添加组件到主面板
        mainPanel.add(toolBarPanel, BorderLayout.NORTH);
        mainPanel.add(splitter, BorderLayout.CENTER);

        installTreeInteractions();
        toolBarPanel.bindActions(this);
        refresh();
    }

    private void installTreeInteractions() {
        treePanel.getConfigTree().addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
                    var node = treePanel.getSelectedNode();
                    if (node != null) {
                        Object userObject = node.getUserObject();
                        ConfigInfoWrapper wrapper = null;
                        if (userObject instanceof TreeNodeData treeNodeData) {
                            wrapper = treeNodeData.wrapper();
                        } else if (userObject instanceof ConfigInfoWrapper) {
                            wrapper = (ConfigInfoWrapper) userObject;
                        }
                        if (wrapper != null) {
                            handleConfigSelection(wrapper);
                        }
                    }
                }
            }
        });
    }

    private void handleConfigSelection(@NotNull ConfigInfoWrapper wrapper) {
        ConfigFile configFile = new ConfigFile();
        configFile.setNamespace(wrapper.getTenant());
        configFile.setGroup(wrapper.getGroup());
        configFile.setDataId(wrapper.getDataId());
        configFile.setType(wrapper.getType());
        pullAndOpenConfig(configFile);
    }

    /**
     * 获取主面板
     *
     * @return 主面板
     */
    public JPanel getMainPanel() {
        return mainPanel;
    }

    /**
     * 获取工具窗口
     *
     * @return 工具窗口
     */
    @Nullable
    public ToolWindow getToolWindow() {
        return toolWindow;
    }

    /**
     * 设置工具窗口
     *
     * @param toolWindow 工具窗口
     */
    public void setToolWindow(@NotNull ToolWindow toolWindow) {
        this.toolWindow = toolWindow;
        project.putUserData(TOOL_WINDOW_KEY, this);
    }

    /**
     * 获取项目实例
     *
     * @return 项目实例
     */
    public Project getProject() {
        return project;
    }

    /**
     * 获取工具栏面板
     *
     * @return 工具栏面板
     */
    public ToolBarPanel getToolBarPanel() {
        return toolBarPanel;
    }

    /**
     * 获取树面板
     *
     * @return 树面板
     */
    public TreePanel getTreePanel() {
        return treePanel;
    }

    /**
     * 获取标签页栏
     *
     * @return 标签页栏
     */
    public TabBar getTabBar() {
        return tabBar;
    }

    /**
     * 刷新工具窗口内容
     */
    public void refresh() {
        configOperationPanel.updateStatus(NacosBundle.message("status.refreshing"));
        CompletableFuture
            .supplyAsync(this::loadTreeData, AppExecutorUtil.getAppExecutorService())
            .whenComplete((root, throwable) -> {
                if (throwable != null) {
                    Throwable cause = throwable.getCause() != null ? throwable.getCause() : throwable;
                    showNotification(cause.getMessage(), "error");
                    configOperationPanel.updateStatus(cause.getMessage());
                    return;
                }
                ApplicationManager.getApplication().invokeLater(() -> {
                    treePanel.updateTree(root);
                    configOperationPanel.updateStatus(NacosBundle.message("status.ready"));
                });
            });
    }

    private DefaultMutableTreeNode loadTreeData() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(NacosBundle.message("ui.config.tree.title"));
        try {
            NacosClient client = NacosClientUtils.getDefaultClient();
            if (client == null) {
                throw new IllegalStateException(NacosBundle.message("error.nacos.not.configured"));
            }
            client.login();
            SettingsState settings = SettingsState.getInstance();
            settings.isAuthed = true;
            settings.globalAdmin = client.isGlobalAdmin();

            List<Namespace> namespaces = client.getNamespaces();
            for (Namespace namespace : namespaces) {
                String namespaceId = namespace.getNamespaceId();
                String namespaceLabel = StringUtil.isNotEmpty(namespace.getNamespaceName())
                                        ? namespace.getNamespaceName() + " (" + namespaceId + ")"
                                        : namespaceId;
                DefaultMutableTreeNode namespaceNode = new DefaultMutableTreeNode(namespaceLabel);
                List<ConfigInfoWrapper> configs = client.listAllConfigs(namespaceId);
                Map<String, List<ConfigInfoWrapper>> groupMap = configs.stream()
                    .collect(Collectors.groupingBy(ConfigInfo::getGroup, Collectors.toList()));

                for (Map.Entry<String, List<ConfigInfoWrapper>> entry : groupMap.entrySet()) {
                    DefaultMutableTreeNode groupNode = new DefaultMutableTreeNode(entry.getKey());
                    for (ConfigInfoWrapper wrapper : entry.getValue()) {
                        // 显示 dataId 而不是序列化对象
                        String dataId = wrapper.getDataId();
                        DefaultMutableTreeNode dataIdNode = new DefaultMutableTreeNode(dataId);
                        // 将 wrapper 对象存储在节点中，以便后续使用
                        dataIdNode.setUserObject(new TreeNodeData(dataId, wrapper));
                        groupNode.add(dataIdNode);
                    }
                    namespaceNode.add(groupNode);
                }
                root.add(namespaceNode);
            }

            List<String> namespaceNames = namespaces.stream()
                .map(Namespace::getNamespaceId)
                .filter(StringUtil::isNotEmpty)
                .toList();
            ApplicationManager.getApplication().invokeLater(() -> configOperationPanel.updateNamespaces(namespaceNames));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return root;
    }

    /**
     * 显示通知消息
     *
     * @param message 消息内容
     * @param type    消息类型 (info, warning, error)
     */
    public void showNotification(String message, String type) {
        switch (type) {
            case "warning" -> NotificationUtil.showWarning(project, message);
            case "error" -> NotificationUtil.showError(project, message);
            default -> NotificationUtil.showInfo(project, message);
        }
    }

    /**
     * 打开或创建配置标签页
     */
    public void openConfigTab(@NotNull ConfigFile configFile, boolean resetModified) {
        String tabId = configFile.getUniqueId();
        Tab existing = tabBar.getTabById(tabId);
        if (existing == null) {
            JsonEditor editor = new JsonEditor(project);
            Tab newTab = new Tab(tabId, buildTabTitle(configFile), project, editor);
            attachModificationListener(newTab);
            tabBar.addTab(newTab);
            existing = newTab;
        }
        JsonEditor editor = existing.getEditor();
        editor.setFileType(toLanguageId(configFile.getType()));
        editor.setContent(configFile.getContent());
        if (resetModified) {
            editor.markClean();
            existing.setModified(false);
            tabBar.updateTabTitle(existing.getId(), buildTabTitle(existing));
        }
        existing.setNamespace(configFile.getNamespace());
        existing.setGroup(configFile.getGroup());
        existing.setDataId(configFile.getDataId());
        configOperationPanel.setNamespace(configFile.getNamespace());
        configOperationPanel.setGroup(configFile.getGroup());
        configOperationPanel.setDataId(configFile.getDataId());
        tabBar.selectTab(existing.getId());
    }

    private void attachModificationListener(@NotNull Tab tab) {
        tab.getEditor().getEditorTextField().addDocumentListener(new com.intellij.openapi.editor.event.DocumentListener() {
            @Override
            public void documentChanged(@NotNull com.intellij.openapi.editor.event.DocumentEvent event) {
                boolean modified = tab.getEditor().isModified();
                if (tab.isModified() != modified) {
                    tab.setModified(modified);
                    tabBar.updateTabTitle(tab.getId(), buildTabTitle(tab));
                }
            }
        });
    }

    private String buildTabTitle(@NotNull ConfigFile configFile) {
        return buildDisplayTitle(configFile.getDataId(), configFile.getGroup());
    }

    private String buildTabTitle(@NotNull Tab tab) {
        String display = buildDisplayTitle(tab.getDataId(), tab.getGroup());
        return tab.isModified() ? "*" + display : display;
    }

    private String buildDisplayTitle(String dataId, String group) {
        String safeDataId = StringUtil.isNotEmpty(dataId) ? dataId : NacosBundle.message("ui.config.editor.title");
        String safeGroup = StringUtil.isNotEmpty(group) ? group : "DEFAULT_GROUP";
        return safeDataId + " (" + safeGroup + ")";
    }

    public void pullAndOpenConfig(@NotNull ConfigFile configInfo) {
        configOperationPanel.updateStatus(NacosBundle.message("status.pull.running"));
        CompletableFuture
            .supplyAsync(() -> {
                try {
                    NacosClient client = NacosClientUtils.getDefaultClient();
                    if (client == null) {
                        throw new IllegalStateException(NacosBundle.message("error.nacos.not.configured"));
                    }
                    String content = client.getConfig(configInfo.getNamespace(), configInfo.getGroup(), configInfo.getDataId());
                    configInfo.setContent(content);
                    if (configInfo.getType() == null) {
                        configInfo.setType(SettingsState.getInstance().type.toLowerCase());
                    }
                    return configInfo;
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }, AppExecutorUtil.getAppExecutorService())
            .whenComplete((cfg, throwable) -> {
                ApplicationManager.getApplication().invokeLater(() -> {
                    if (throwable != null) {
                        showNotification(throwable.getMessage(), "error");
                        configOperationPanel.updateStatus(throwable.getMessage());
                        return;
                    }
                    openConfigTab(cfg, true);
                    configOperationPanel.updateStatus(NacosBundle.message("success.config.pulled"));
                });
            });
    }

    public void pushCurrentTab() {
        Tab tab = tabBar.getSelectedTab();
        if (tab == null) {
            showNotification(NacosBundle.message("error.no.file"), "warning");
            return;
        }
        ConfigFile configFile = new ConfigFile();
        configFile.setNamespace(configOperationPanel.getNamespace());
        configFile.setGroup(configOperationPanel.getGroup());
        configFile.setDataId(configOperationPanel.getDataId());
        configFile.setContent(tab.getEditor().getContent());
        configFile.setType(tab.getEditor().getFileType().toLowerCase());

        CompletableFuture
            .supplyAsync(() -> {
                try {
                    NacosClient client = NacosClientUtils.getDefaultClient();
                    if (client == null) {
                        throw new IllegalStateException(NacosBundle.message("error.nacos.not.configured"));
                    }
                    boolean success = client.publishConfig(
                        configFile.getNamespace(),
                        configFile.getGroup(),
                        configFile.getDataId(),
                        configFile.getContent(),
                        configFile.getType()
                                                          );
                    if (!success) {
                        throw new IllegalStateException("Publish failed");
                    }
                    return true;
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }, AppExecutorUtil.getAppExecutorService())
            .whenComplete((unused, throwable) -> ApplicationManager.getApplication().invokeLater(() -> {
                if (throwable != null) {
                    showNotification(throwable.getMessage(), "error");
                    return;
                }
                tab.getEditor().markClean();
                tab.setModified(false);
                tabBar.updateTabTitle(tab.getId(), buildTabTitle(tab));
            }));
    }

    public void compareWithRemote() {
        Tab tab = tabBar.getSelectedTab();
        if (tab == null) {
            showNotification(NacosBundle.message("error.no.file"), "warning");
            return;
        }
        String namespace = configOperationPanel.getNamespace();
        String group = configOperationPanel.getGroup();
        String dataId = configOperationPanel.getDataId();
        if (namespace == null || group == null || dataId == null) {
            showNotification(NacosBundle.message("error.nacos.not.configured"), "warning");
            return;
        }
        CompletableFuture
            .supplyAsync(() -> {
                try {
                    NacosClient client = NacosClientUtils.getDefaultClient();
                    if (client == null) {
                        throw new IllegalStateException(NacosBundle.message("error.nacos.not.configured"));
                    }
                    return client.getConfig(namespace, group, dataId);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }, AppExecutorUtil.getAppExecutorService())
            .whenComplete((remoteContent, throwable) -> ApplicationManager.getApplication().invokeLater(() -> {
                if (throwable != null) {
                    showNotification(throwable.getMessage(), "error");
                    return;
                }
                CompareConfigService.getInstance(project).compareConfigurations(
                    project,
                    tab.getEditor().getContent(),
                    remoteContent != null ? remoteContent : "",
                    dataId
                                                                               );
            }));
    }

    public static @Nullable NacosToolWindow getInstance(@NotNull Project project) {
        return project.getUserData(TOOL_WINDOW_KEY);
    }

    public void createEmptyTab() {
        ConfigFile configFile = new ConfigFile();
        configFile.setNamespace(StringUtil.notNullize(configOperationPanel.getNamespace(), "public"));
        configFile.setGroup(StringUtil.notNullize(configOperationPanel.getGroup(), "DEFAULT_GROUP"));
        configFile.setDataId("new-config-" + (newTabCounter++) + ".yaml");
        configFile.setType(StringUtil.notNullize(SettingsState.getInstance().type, "yaml").toLowerCase());
        configFile.setContent("");
        openConfigTab(configFile, true);
        Tab tab = tabBar.getTabById(configFile.getUniqueId());
        if (tab != null) {
            tab.getEditor().markClean();
        }
    }

    public void closeCurrentTab() {
        Tab tab = tabBar.getSelectedTab();
        if (tab == null) {
            showNotification(NacosBundle.message("error.no.file"), "warning");
            return;
        }
        if (tab.isModified()) {
            int exitCode = Messages.showYesNoDialog(
                project,
                NacosBundle.message("dialog.close.modified.message", tab.getDataId()),
                NacosBundle.message("dialog.close.modified.title"),
                Messages.getYesButton(),
                Messages.getNoButton(),
                Messages.getWarningIcon()
                                                   );
            if (exitCode != Messages.YES) {
                return;
            }
        }
        tabBar.closeTab(tab.getId());
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

    /**
         * 树节点数据包装类
         * 用于在树节点中存储 dataId 和 ConfigInfoWrapper
         */
        private record TreeNodeData(String dataId, ConfigInfoWrapper wrapper) {

        @Override
            public String toString() {
                return dataId;
            }
        }
}