package dev.dong4j.zeka.stack.idea.plugin.nacos.ui.toolwindow;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;

import org.jetbrains.annotations.NotNull;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;

/**
 * 配置操作面板
 * 提供配置的基本操作功能（Pull, Push, Compare）
 *
 * @author dong4j
 * @since 1.0.0
 */
public class ConfigOperationPanel extends JPanel {
    private final Project project;
    private final JComboBox<String> namespaceComboBox;
    private final JComboBox<String> groupComboBox;
    private final JBTextField dataIdTextField;
    private final JButton pullButton;
    private final JButton pushButton;
    private final JButton compareButton;
    private final JBLabel statusLabel;

    public ConfigOperationPanel(@NotNull Project project) {
        this.project = project;
        this.namespaceComboBox = new ComboBox<>();
        this.groupComboBox = new ComboBox<>();
        this.dataIdTextField = new JBTextField();
        this.pullButton = new JButton("Pull");
        this.pushButton = new JButton("Push");
        this.compareButton = new JButton("Compare");
        this.statusLabel = new JBLabel("Ready");

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
        // TODO: 根据选择的命名空间加载对应的分组列表
        String selectedNamespace = (String) namespaceComboBox.getSelectedItem();
        if (selectedNamespace != null) {
            loadGroupsForNamespace(selectedNamespace);
        }
    }

    private void onGroupSelected() {
        // TODO: 根据选择的分组加载对应的 Data ID 列表
        String selectedGroup = (String) groupComboBox.getSelectedItem();
        if (selectedGroup != null) {
            loadDataIdsForGroup(selectedGroup);
        }
    }

    private void pullConfiguration() {
        // TODO: 实现拉取配置逻辑
        updateStatus("Pulling configuration...");
    }

    private void pushConfiguration() {
        // TODO: 实现推送配置逻辑
        updateStatus("Pushing configuration...");
    }

    private void compareConfiguration() {
        // TODO: 实现对比配置逻辑
        updateStatus("Comparing configurations...");
    }

    /**
     * 更新命名空间列表
     *
     * @param namespaces 命名空间列表
     */
    public void updateNamespaces(List<String> namespaces) {
        namespaceComboBox.removeAllItems();
        for (String namespace : namespaces) {
            namespaceComboBox.addItem(namespace);
        }
    }

    /**
     * 更新分组列表
     *
     * @param groups 分组列表
     */
    public void updateGroups(List<String> groups) {
        groupComboBox.removeAllItems();
        for (String group : groups) {
            groupComboBox.addItem(group);
        }
    }

    /**
     * 更新 Data ID 列表
     *
     * @param dataIds Data ID 列表
     */
    public void updateDataIds(List<String> dataIds) {
        // Data ID 通常是手动输入的，这里可以提供自动完成功能
    }

    /**
     * 加载指定命名空间的分组列表
     *
     * @param namespace 命名空间
     */
    private void loadGroupsForNamespace(String namespace) {
        // TODO: 实现加载分组逻辑
    }

    /**
     * 加载指定分组的 Data ID 列表
     *
     * @param group 分组
     */
    private void loadDataIdsForGroup(String group) {
        // TODO: 实现加载 Data ID 逻辑
    }

    /**
     * 更新状态标签
     *
     * @param status 状态文本
     */
    public void updateStatus(String status) {
        statusLabel.setText(status);
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
}