package dev.dong4j.zeka.stack.idea.plugin.nacos.ui.toolwindow;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;

import org.jetbrains.annotations.NotNull;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;

import dev.dong4j.zeka.stack.idea.plugin.nacos.client.NacosClient;
import dev.dong4j.zeka.stack.idea.plugin.nacos.client.NacosClientUtils;
import dev.dong4j.zeka.stack.idea.plugin.nacos.entity.ConfigFile;
import dev.dong4j.zeka.stack.idea.plugin.nacos.util.NacosBundle;
import dev.dong4j.zeka.stack.idea.plugin.nacos.util.NotificationUtil;

/**
 * 配置操作面板
 * 提供配置的基本操作功能（Pull, Push, Compare）
 *
 * @author dong4j
 * @since 1.0.0
 */
public class ConfigOperationPanel extends JPanel {
    private final Project project;
    private final NacosToolWindow nacosToolWindow;
    private final JComboBox<String> namespaceComboBox;
    private final JComboBox<String> groupComboBox;
    private final JBTextField dataIdTextField;
    private final JButton pullButton;
    private final JButton pushButton;
    private final JButton compareButton;
    private final JBLabel statusLabel;

    public ConfigOperationPanel(@NotNull Project project, @NotNull NacosToolWindow nacosToolWindow) {
        this.project = project;
        this.nacosToolWindow = nacosToolWindow;
        this.namespaceComboBox = new ComboBox<>();
        this.groupComboBox = new ComboBox<>();
        this.dataIdTextField = new JBTextField();
        this.pullButton = new JButton(NacosBundle.message("button.pull"));
        this.pushButton = new JButton(NacosBundle.message("button.push"));
        this.compareButton = new JButton(NacosBundle.message("button.compare"));
        this.statusLabel = new JBLabel(NacosBundle.message("status.ready"));

        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        setBorder(JBUI.Borders.empty(10));

        // 创建输入面板
        JPanel inputPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent(new JBLabel("Namespace:"), namespaceComboBox)
            .addLabeledComponent(new JBLabel("Group:"), groupComboBox)
            .addLabeledComponent(new JBLabel("Data ID:"), dataIdTextField)
            .getPanel();

        // 创建按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonPanel.add(pullButton);
        buttonPanel.add(pushButton);
        buttonPanel.add(compareButton);

        // 创建状态面板
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusPanel.add(statusLabel);

        // 创建主操作面板
        JPanel operationPanel = new JPanel(new BorderLayout());
        operationPanel.add(inputPanel, BorderLayout.NORTH);
        operationPanel.add(buttonPanel, BorderLayout.CENTER);
        operationPanel.add(statusPanel, BorderLayout.SOUTH);

        add(operationPanel, BorderLayout.CENTER);

        // 设置组件属性
        setupComponentProperties();

        // 添加事件监听器
        setupEventListeners();
    }

    private void setupComponentProperties() {
        dataIdTextField.getEmptyText().setText("Enter data ID...");
        namespaceComboBox.setToolTipText("Select namespace");
        groupComboBox.setToolTipText("Select group");
        dataIdTextField.setToolTipText("Enter data ID");

        // 设置按钮工具提示
        pullButton.setToolTipText("Pull configuration from Nacos server");
        pushButton.setToolTipText("Push configuration to Nacos server");
        compareButton.setToolTipText("Compare local and remote configurations");
    }

    private void setupEventListeners() {
        pullButton.addActionListener(e -> pullConfiguration());
        pushButton.addActionListener(e -> pushConfiguration());
        compareButton.addActionListener(e -> compareConfiguration());

        // 添加命名空间选择监听器
        namespaceComboBox.addActionListener(e -> onNamespaceSelected());

        // 添加分组选择监听器
        groupComboBox.addActionListener(e -> onGroupSelected());
    }

    private void onNamespaceSelected() {
        String selectedNamespace = (String) namespaceComboBox.getSelectedItem();
        if (selectedNamespace != null) {
            loadGroupsForNamespace(selectedNamespace);
        }
    }

    private void onGroupSelected() {
        String selectedGroup = (String) groupComboBox.getSelectedItem();
        if (selectedGroup != null) {
            loadDataIdsForGroup(selectedGroup);
        }
    }

    private void pullConfiguration() {
        if (StringUtil.isEmpty(getNamespace()) || StringUtil.isEmpty(getGroup()) || StringUtil.isEmpty(getDataId())) {
            NotificationUtil.showWarning(project, NacosBundle.message("error.nacos.not.configured"));
            return;
        }
        ConfigFile configFile = new ConfigFile();
        configFile.setNamespace(getNamespace());
        configFile.setGroup(getGroup());
        configFile.setDataId(getDataId());
        nacosToolWindow.pullAndOpenConfig(configFile);
    }

    private void pushConfiguration() {
        nacosToolWindow.pushCurrentTab();
    }

    private void compareConfiguration() {
        nacosToolWindow.compareWithRemote();
    }

    /**
     * 更新命名空间列表
     *
     * @param namespaces 命名空间列表
     */
    public void updateNamespaces(List<String> namespaces) {
        namespaceComboBox.removeAllItems();
        namespaces.forEach(namespaceComboBox::addItem);
        if (namespaceComboBox.getItemCount() > 0) {
            namespaceComboBox.setSelectedIndex(0);
        }
    }

    /**
     * 更新分组列表
     *
     * @param groups 分组列表
     */
    public void updateGroups(List<String> groups) {
        groupComboBox.removeAllItems();
        groups.forEach(groupComboBox::addItem);
        if (groupComboBox.getItemCount() > 0) {
            groupComboBox.setSelectedIndex(0);
        }
    }

    /**
     * 更新 Data ID 列表
     *
     * @param dataIds Data ID 列表
     */
    public void updateDataIds(List<String> dataIds) {
        if (dataIds.isEmpty()) {
            return;
        }
        dataIdTextField.putClientProperty("nacos.dataIds", dataIds);
        dataIdTextField.getEmptyText().setText(dataIds.get(0));
    }

    /**
     * 加载指定命名空间的分组列表
     *
     * @param namespace 命名空间
     */
    private void loadGroupsForNamespace(String namespace) {
        updateStatus(NacosBundle.message("status.loading.groups"));
        runAsync(() -> {
            NacosClient client = NacosClientUtils.getDefaultClient();
            if (client == null) {
                throw new IllegalStateException(NacosBundle.message("error.nacos.not.configured"));
            }
            return client.listGroups(namespace);
        }, groups -> {
            updateGroups(groups);
            updateStatus(NacosBundle.message("status.ready"));
        });
    }

    /**
     * 加载指定分组的 Data ID 列表
     *
     * @param group 分组
     */
    private void loadDataIdsForGroup(String group) {
        if (StringUtil.isEmpty(group) || StringUtil.isEmpty(getNamespace())) {
            return;
        }
        updateStatus(NacosBundle.message("status.loading.dataids"));
        runAsync(() -> {
            NacosClient client = NacosClientUtils.getDefaultClient();
            if (client == null) {
                throw new IllegalStateException(NacosBundle.message("error.nacos.not.configured"));
            }
            return client.listDataIds(getNamespace(), group);
        }, dataIds -> {
            updateDataIds(dataIds);
            updateStatus(NacosBundle.message("status.ready"));
        });
    }

    /**
     * 更新状态标签
     *
     * @param status 状态文本
     */
    public void updateStatus(String status) {
        ApplicationManager.getApplication().invokeLater(() -> statusLabel.setText(status));
    }

    /**
     * 获取命名空间
     *
     * @return 命名空间
     */
    public String getNamespace() {
        return (String) namespaceComboBox.getSelectedItem();
    }

    /**
     * 获取分组
     *
     * @return 分组
     */
    public String getGroup() {
        return (String) groupComboBox.getSelectedItem();
    }

    /**
     * 获取 Data ID
     *
     * @return Data ID
     */
    public String getDataId() {
        return dataIdTextField.getText();
    }

    /**
     * 设置命名空间
     *
     * @param namespace 命名空间
     */
    public void setNamespace(String namespace) {
        namespaceComboBox.setSelectedItem(namespace);
    }

    /**
     * 设置分组
     *
     * @param group 分组
     */
    public void setGroup(String group) {
        groupComboBox.setSelectedItem(group);
    }

    /**
     * 设置 Data ID
     *
     * @param dataId Data ID
     */
    public void setDataId(String dataId) {
        dataIdTextField.setText(dataId);
    }

    private void runAsync(@NotNull java.util.concurrent.Callable<List<String>> supplier,
                          @NotNull java.util.function.Consumer<List<String>> success) {
        CompletableFuture
            .supplyAsync(() -> {
                try {
                    return supplier.call();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, AppExecutorUtil.getAppExecutorService())
            .whenComplete((result, throwable) -> {
                if (throwable != null) {
                    NotificationUtil.showError(project, throwable.getMessage());
                    updateStatus(throwable.getMessage());
                    return;
                }
                success.accept(result != null ? result : Collections.emptyList());
            });
    }
}