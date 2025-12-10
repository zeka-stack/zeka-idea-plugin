package dev.dong4j.zeka.stack.idea.plugin.settings.ui;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;

import org.jetbrains.annotations.NotNull;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;

import dev.dong4j.zeka.stack.idea.plugin.settings.CustomJavadocTag;
import dev.dong4j.zeka.stack.idea.plugin.settings.SettingsState;
import dev.dong4j.zeka.stack.idea.plugin.util.JavadocBundle;

/**
 * 自定义 Javadoc 标签面板
 * <p>
 * 提供自定义 Javadoc 标签的配置界面，允许用户添加、删除、编辑自定义标签。
 * 包括标签名称和默认值的配置，支持标签的显示/隐藏控制。
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @since 1.4.0
 */
public class CustomJavaDocTagsPanel {

    /** 显示自定义 Javadoc 标签的复选框 */
    private JBCheckBox showCustomJavaDocTagsCheckBox;

    /** 自定义 Javadoc 标签列表表格 */
    private JBTable customJavaDocTagsTable;

    /** 自定义 Javadoc 标签列表面板（包含表格和工具栏） */
    private JPanel customJavaDocTagsPanel;

    /** 自定义 Javadoc 标签提示标签 */
    private JBLabel customTagsHintLabel;

    /** 自定义 Javadoc 标签列表表格模型 */
    private CustomJavaDocTagsTableModel customJavaDocTagsTableModel;

    /** 主面板 */
    private JPanel panel;

    /**
     * 构造函数
     */
    public CustomJavaDocTagsPanel() {
        createUI();
        setupListeners();
    }

    /**
     * 创建 UI
     */
    private void createUI() {
        // 创建自定义 Javadoc 标签组件
        showCustomJavaDocTagsCheckBox = new JBCheckBox(JavadocBundle.message("settings.custom.javadoc.tags"));
        customJavaDocTagsTableModel = new CustomJavaDocTagsTableModel();
        customJavaDocTagsTable = new JBTable(customJavaDocTagsTableModel);
        customJavaDocTagsTable.setPreferredScrollableViewportSize(new Dimension(500, 100));

        // 创建自定义 Javadoc 标签提示标签
        customTagsHintLabel = new JBLabel(JavadocBundle.message("settings.custom.javadoc.tags.hint"));
        customTagsHintLabel.setFont(customTagsHintLabel.getFont().deriveFont(customTagsHintLabel.getFont().getSize() - 1f));
        customTagsHintLabel.setForeground(javax.swing.UIManager.getColor("Label.disabledForeground"));
        customTagsHintLabel.setBorder(JBUI.Borders.emptyLeft(22)); // 与复选框对齐

        // 创建带工具栏的面板
        ToolbarDecorator tagsDecorator = ToolbarDecorator.createDecorator(customJavaDocTagsTable)
            .setAddAction(button -> addCustomJavaDocTag())
            .setRemoveAction(button -> {
                int selectedRow = customJavaDocTagsTable.getSelectedRow();
                if (selectedRow >= 0) {
                    removeCustomJavaDocTag(selectedRow);
                }
            })
            .addExtraAction(new AnAction(JavadocBundle.message("settings.custom.javadoc.tags.clear.all"),
                                         JavadocBundle.message("settings.custom.javadoc.tags.clear.all.description"),
                                         com.intellij.icons.AllIcons.Actions.GC) {
                /**
                 * 处理动作事件, 清除所有自定义的 Javadoc 标签
                 * <p>
                 * 该方法用于响应动作事件, 执行清除自定义 Javadoc 标签的操作
                 *
                 * @param e 动作事件对象
                 */
                @Override
                public void actionPerformed(@NotNull AnActionEvent e) {
                    clearAllCustomJavaDocTags();
                }

                /**
                 * 根据表格数据状态更新操作按钮的启用状态
                 * <p>
                 * 检查自定义 Javadoc 标签表格中是否有数据行, 若有则启用按钮, 否则禁用
                 *
                 * @param e 动作事件对象, 包含操作相关的上下文信息
                 */
                @Override
                public void update(@NotNull AnActionEvent e) {
                    // 根据表格状态启用/禁用按钮
                    boolean hasData = customJavaDocTagsTableModel.getRowCount() > 0;
                    e.getPresentation().setEnabled(hasData);
                }

                /**
                 * 获取动作更新线程
                 * <p>
                 * 返回用于更新动作的线程, 该线程为事件调度线程 (EDT)
                 *
                 * @return 动作更新线程
                 */
                @Override
                public @NotNull ActionUpdateThread getActionUpdateThread() {
                    // 需要访问 Swing 组件（表格模型），必须在 EDT 中执行
                    return ActionUpdateThread.EDT;
                }
            });

        customJavaDocTagsPanel = tagsDecorator.createPanel();

        // 构建主面板
        panel = new JPanel(new BorderLayout());
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.add(showCustomJavaDocTagsCheckBox, BorderLayout.NORTH);
        contentPanel.add(customTagsHintLabel, BorderLayout.CENTER);
        contentPanel.add(customJavaDocTagsPanel, BorderLayout.SOUTH);
        panel.add(contentPanel, BorderLayout.CENTER);

        // 初始可见性
        customJavaDocTagsPanel.setVisible(false);
        customTagsHintLabel.setVisible(false);
    }

    /**
     * 设置监听器
     */
    private void setupListeners() {
        showCustomJavaDocTagsCheckBox.addActionListener(e -> {
            boolean selected = showCustomJavaDocTagsCheckBox.isSelected();
            customJavaDocTagsPanel.setVisible(selected);
            if (customTagsHintLabel != null) {
                customTagsHintLabel.setVisible(selected);
            }
        });
    }

    /**
     * 获取主面板
     *
     * @return 主面板组件
     */
    @NotNull
    public JPanel getPanel() {
        return panel;
    }

    /**
     * 获取设置
     *
     * @param settings 设置对象，将读取的值填充到此对象中
     */
    public void getSettings(@NotNull SettingsState settings) {
        // 获取标签列表（已经是 List<CustomJavaDocTag>）
        settings.customJavadocTags = new ArrayList<>(customJavaDocTagsTableModel.getData());
        settings.showCustomJavaDocTags = showCustomJavaDocTagsCheckBox.isSelected();
    }

    /**
     * 加载设置
     *
     * @param settings 设置对象
     */
    public void loadSettings(@NotNull SettingsState settings) {
        // 设置标签列表（已经是 List<CustomJavaDocTag>）
        if (settings.customJavadocTags != null) {
            customJavaDocTagsTableModel.setData(new ArrayList<>(settings.customJavadocTags));
        } else {
            customJavaDocTagsTableModel.setData(new ArrayList<>());
        }
        showCustomJavaDocTagsCheckBox.setSelected(settings.showCustomJavaDocTags);
        customJavaDocTagsPanel.setVisible(settings.showCustomJavaDocTags);
        // 提示语也随复选框状态显示/隐藏
        if (customTagsHintLabel != null) {
            customTagsHintLabel.setVisible(settings.showCustomJavaDocTags);
        }
    }

    /**
     * 获取主面板所在的顶级窗口
     * <p>
     * 使用 Swing 工具类查找主面板的顶级窗口祖先
     *
     * @return 主面板所在的顶级窗口, 若未找到则返回 null
     */
    private java.awt.Window getParentWindow() {
        return SwingUtilities.getWindowAncestor(panel);
    }

    /**
     * 添加自定义 Javadoc 标签
     * <p>
     * 该方法弹出输入框提示用户输入自定义标签名称和默认值, 随后对输入进行合法性校验
     * (使用 {@link SettingsState#isValidTagName(String)}), 并检查当前标签列表中是否已存在相同名称 (不区分大小写).<br>
     * 若输入合法且标签不存在, 则将该标签添加到 {@link CustomJavaDocTagsTableModel} 中;<br>
     * 否则根据不同情况弹出相应的错误或警告对话框.
     */
    private void addCustomJavaDocTag() {
        // 创建输入对话框
        JPanel panel = new JPanel();
        panel.setLayout(new java.awt.GridLayout(2, 2, 5, 5));

        JBLabel tagNameLabel = new JBLabel(JavadocBundle.message("settings.custom.javadoc.tags.column.name") + ":");
        JBTextField tagNameField = new JBTextField();
        JBLabel defaultValueLabel = new JBLabel(JavadocBundle.message("settings.custom.javadoc.tags.column.default.value") + ":");
        JBTextField defaultValueField = new JBTextField();

        panel.add(tagNameLabel);
        panel.add(tagNameField);
        panel.add(defaultValueLabel);
        panel.add(defaultValueField);

        int result = JOptionPane.showConfirmDialog(
            getParentWindow(),
            panel,
            JavadocBundle.message("settings.custom.javadoc.tags.add.title"),
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.QUESTION_MESSAGE
                                                  );

        if (result == JOptionPane.OK_OPTION) {
            String tagName = tagNameField.getText().trim();
            String defaultValue = defaultValueField.getText().trim();

            if (tagName.isEmpty()) {
                JOptionPane.showMessageDialog(
                    getParentWindow(),
                    JavadocBundle.message("settings.custom.javadoc.tags.invalid.name", tagName),
                    JavadocBundle.message("settings.error.title"),
                    JOptionPane.ERROR_MESSAGE
                                             );
                return;
            }

            // 验证标签名称
            if (!SettingsState.isValidTagName(tagName)) {
                JOptionPane.showMessageDialog(
                    getParentWindow(),
                    JavadocBundle.message("settings.custom.javadoc.tags.invalid.name", tagName),
                    JavadocBundle.message("settings.error.title"),
                    JOptionPane.ERROR_MESSAGE
                                             );
                return;
            }

            // 检查是否已存在
            List<CustomJavadocTag> currentTags = customJavaDocTagsTableModel.getData();
            String tagNameLower = tagName.toLowerCase();
            if (currentTags.stream().anyMatch(t -> t.tagName.toLowerCase().equals(tagNameLower))) {
                JOptionPane.showMessageDialog(
                    getParentWindow(),
                    JavadocBundle.message("settings.custom.javadoc.tags.already.exists", tagName),
                    JavadocBundle.message("settings.error.title"),
                    JOptionPane.WARNING_MESSAGE
                                             );
                return;
            }

            // 添加到表格
            customJavaDocTagsTableModel.addTag(
                new CustomJavadocTag(tagName, defaultValue)
                                              );
        }
    }

    /**
     * 删除自定义 Javadoc 标签
     */
    private void removeCustomJavaDocTag(int selectedRow) {
        if (selectedRow < 0 || selectedRow >= customJavaDocTagsTableModel.getRowCount()) {
            return;
        }

        CustomJavadocTag tag = customJavaDocTagsTableModel.getData().get(selectedRow);
        String tagName = tag.tagName;

        int result = JOptionPane.showConfirmDialog(
            getParentWindow(),
            JavadocBundle.message("settings.custom.javadoc.tags.delete.confirm", tagName),
            JavadocBundle.message("settings.custom.javadoc.tags.delete.title"),
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
                                                  );

        if (result == JOptionPane.YES_OPTION) {
            customJavaDocTagsTableModel.removeRow(selectedRow);
        }
    }

    /**
     * 清空所有自定义 Javadoc 标签
     */
    private void clearAllCustomJavaDocTags() {
        if (customJavaDocTagsTableModel.getRowCount() == 0) {
            return;
        }

        int result = JOptionPane.showConfirmDialog(
            getParentWindow(),
            JavadocBundle.message("settings.custom.javadoc.tags.clear.confirm",
                                  customJavaDocTagsTableModel.getRowCount()),
            JavadocBundle.message("settings.custom.javadoc.tags.clear.title"),
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
                                                  );

        if (result == JOptionPane.YES_OPTION) {
            customJavaDocTagsTableModel.clearAll();
        }
    }

    /**
     * 自定义 Javadoc 标签表格模型
     * <p>
     * 该模型用于管理自定义 Javadoc 标签的数据, 继承自 AbstractTableModel,
     * 提供了对自定义标签的增删改查操作, 支持表格界面的数据展示和编辑功能.
     * 主要用于 Javadoc 设置界面中自定义标签的管理, 包括标签名称和默认值的配置.
     *
     * @author zeka.stack.team
     * @version 1.0.0
     * @email "mailto:zeka.stack@gmail.com"
     * @date 2025.11.30
     * @since 1.0.0
     */
    private static class CustomJavaDocTagsTableModel extends AbstractTableModel {
        /**
         * 列名数组, 用于显示自定义 Javadoc 标签的设置界面
         * <p>
         * 数组中的元素通过 JavaDocBundle 获取国际化字符串
         */
        private final String[] columnNames = {
            JavadocBundle.message("settings.custom.javadoc.tags.column.name"),
            JavadocBundle.message("settings.custom.javadoc.tags.column.default.value")
        };
        /** 数据列表 */
        private final List<CustomJavadocTag> data;

        /**
         * 构造函数, 初始化 CustomJavaDocTagsTableModel 实例
         * <p>
         * 创建一个空的表格模型, 用于展示 Javadoc 标签信息
         *
         * @since 2.0.0
         */
        public CustomJavaDocTagsTableModel() {
            this.data = new ArrayList<>();
        }

        /**
         * 设置新的数据列表并触发表格数据变更事件
         * <p>
         * 清除当前数据列表, 若传入的 newData 不为 null, 则将新数据添加到当前数据列表中, 并触发表格数据变更事件.
         *
         * @param newData 要设置的新数据列表
         * @since 2.0.0
         */
        public void setData(List<CustomJavadocTag> newData) {
            this.data.clear();
            if (newData != null) {
                this.data.addAll(newData);
            }
            fireTableDataChanged();
        }

        /**
         * 获取数据列表
         * <p>
         * 返回数据的副本列表
         *
         * @return 数据列表
         */
        public List<CustomJavadocTag> getData() {
            return new ArrayList<>(data);
        }

        /**
         * 添加一个标签到数据集合中, 并通知表格数据已更新
         * <p>
         * 该方法将指定的标签添加到内部数据集合, 并触发表格行插入事件以更新界面.
         *
         * @param tag 要添加的标签
         */
        public void addTag(CustomJavadocTag tag) {
            data.add(tag);
            fireTableRowsInserted(data.size() - 1, data.size() - 1);
        }

        /**
         * 删除指定行的数据并通知表格视图更新.
         *
         * <p> 该方法首先检查传入的行索引是否在合法范围内 (0 ≤ row < data.size()). 若合法, 则从内部数据集合中移除对应行, 并通过 {@code fireTableRowsDeleted} 通知表格模型行已被删除, 从而触发视图刷新
         * .</p>
         *
         * @param row 要删除的行索引, 基于 0 的索引
         */
        public void removeRow(int row) {
            if (row >= 0 && row < data.size()) {
                data.remove(row);
                fireTableRowsDeleted(row, row);
            }
        }

        /**
         * 清除所有数据并通知表格数据已删除
         * <p>
         * 该方法会清除数据集合中的所有元素, 并触发表格数据删除的事件通知.
         *
         * @since 2.0.0
         */
        public void clearAll() {
            int size = data.size();
            if (size > 0) {
                data.clear();
                fireTableRowsDeleted(0, size - 1);
            }
        }

        /**
         * 获取数据行数
         * <p>
         * 返回数据集合中的元素数量
         *
         * @return 数据行数
         */
        @Override
        public int getRowCount() {
            return data.size();
        }

        /**
         * 获取列的数量
         * <p>
         * 返回当前列名数组中的列数, 即表格的列数.
         *
         * @return 列的数量
         */
        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        /**
         * 根据列索引获取列名称
         * <p>
         * 通过指定的列索引从列名称数组中获取对应的列名称
         *
         * @param column 列索引
         * @return 对应的列名称
         */
        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }

        /**
         * 获取表格中指定行和列的单元格值
         * <p>
         * 根据行索引和列索引返回对应的数据值, 若行索引超出范围或数据为空, 则返回空字符串
         *
         * @param rowIndex    行索引
         * @param columnIndex 列索引
         * @return 表格单元格的值
         */
        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (rowIndex >= 0 && rowIndex < data.size()) {
                CustomJavadocTag tag = data.get(rowIndex);
                if (columnIndex == 0) {
                    return tag.tagName;
                } else if (columnIndex == 1) {
                    return tag.defaultValue;
                }
            }
            return "";
        }

        /**
         * 判断指定单元格是否可编辑
         * <p>
         * 该方法用于确定表格中指定行和列的单元格是否允许用户进行编辑操作.
         *
         * @param rowIndex    表格中的行索引
         * @param columnIndex 表格中的列索引
         * @return 如果单元格可编辑则返回 true, 否则返回 false
         */
        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return true; // 允许编辑标签名称和默认值
        }

        /**
         * 设置表格指定单元格的值.
         * <p>
         * 仅当行索引在有效范围内且传入值不为 {@code null} 时才会进行处理.
         * 对于标签名称列，会进行合法性校验和重复检查。
         * 对于默认值列，直接更新值。
         *
         * @param aValue      要设置的新值, 通常为 {@link String}, 但方法接受任何对象并调用 {@link Object#toString()}.
         * @param rowIndex    行索引, 必须在 0 与 {@link #data} 的大小之间.
         * @param columnIndex 列索引, 用于通知表格更新.
         */
        @SuppressWarnings("D")
        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            if (rowIndex >= 0 && rowIndex < data.size() && aValue != null) {
                CustomJavadocTag tag = data.get(rowIndex);

                if (columnIndex == 0) {
                    // 编辑标签名称
                    String newTagName = aValue.toString().trim();

                    // 验证标签名称
                    if (!SettingsState.isValidTagName(newTagName)) {
                        // 可以显示错误提示，这里简单处理为不更新
                        return;
                    }

                    // 检查是否与其他标签重复
                    String newTagNameLower = newTagName.toLowerCase();
                    for (int i = 0; i < data.size(); i++) {
                        if (i != rowIndex && data.get(i).tagName.toLowerCase().equals(newTagNameLower)) {
                            // 重复标签，不更新
                            return;
                        }
                    }

                    tag.tagName = newTagName;
                    fireTableCellUpdated(rowIndex, columnIndex);
                } else if (columnIndex == 1) {
                    // 编辑默认值
                    tag.defaultValue = aValue.toString();
                    fireTableCellUpdated(rowIndex, columnIndex);
                }
            }
        }
    }
}

