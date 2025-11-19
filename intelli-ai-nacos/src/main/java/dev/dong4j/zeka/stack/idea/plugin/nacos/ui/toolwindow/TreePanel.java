package dev.dong4j.zeka.stack.idea.plugin.nacos.ui.toolwindow;

import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.tree.TreeUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.BorderLayout;
import java.util.Enumeration;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;

/**
 * Nacos 配置树面板
 * 用于展示 Nacos 配置的树形结构
 *
 * @author dong4j
 * @since 1.0.0
 */
public class TreePanel extends JPanel {
    private final Project project;
    private final Tree configTree;
    private final DefaultTreeModel treeModel;
    private final DefaultMutableTreeNode root;
    private final JTextField searchField;

    public TreePanel(@NotNull Project project) {
        this.project = project;
        this.root = new DefaultMutableTreeNode("Nacos Configurations");
        this.treeModel = new DefaultTreeModel(root);
        this.configTree = new Tree(treeModel);
        this.searchField = new JTextField();

        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        // 添加边框：内边距 + 可见边框
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UIManager.getColor("Separator.separatorColor")),
            JBUI.Borders.empty(5)
                                                    ));

        // 设置树的基本属性
        configTree.setRootVisible(true);
        configTree.setShowsRootHandles(true);

        // 创建滚动面板
        JScrollPane scrollPane = new JScrollPane(configTree);
        scrollPane.setBorder(JBUI.Borders.empty());

        // 创建搜索面板
        JPanel searchPanel = new JPanel(new BorderLayout());
        searchPanel.add(searchField, BorderLayout.CENTER);
        searchField.setToolTipText("Search configurations...");

        // 添加组件到主面板
        add(searchPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        // 添加搜索监听器
        setupSearchListener();
    }

    private void setupSearchListener() {
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                filterTree();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                filterTree();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                filterTree();
            }
        });
    }

    private void filterTree() {
        String searchText = searchField.getText().toLowerCase();
        filterNodes(root, searchText);
        TreeUtil.expandAll(configTree);
    }

    private boolean filterNodes(DefaultMutableTreeNode node, String searchText) {
        if (searchText.isEmpty()) {
            // 如果搜索文本为空，显示所有节点
            node.setUserObject(getOriginalUserObject(node));
            if (node.getChildCount() > 0) {
                Enumeration<TreeNode> children = node.children();
                while (children.hasMoreElements()) {
                    DefaultMutableTreeNode child = (DefaultMutableTreeNode) children.nextElement();
                    filterNodes(child, searchText);
                }
            }
            return true;
        }

        // 检查当前节点是否匹配
        String nodeText = node.getUserObject().toString().toLowerCase();
        boolean matches = nodeText.contains(searchText);

        // 递归检查子节点
        boolean hasMatchingChildren = false;
        if (node.getChildCount() > 0) {
            Enumeration<TreeNode> children = node.children();
            while (children.hasMoreElements()) {
                DefaultMutableTreeNode child = (DefaultMutableTreeNode) children.nextElement();
                if (filterNodes(child, searchText)) {
                    hasMatchingChildren = true;
                }
            }
        }

        // 如果当前节点匹配或有匹配的子节点，则显示该节点
        if (matches || hasMatchingChildren) {
            node.setUserObject(getOriginalUserObject(node));
            return true;
        } else {
            // 否则隐藏该节点
            return false;
        }
    }

    private Object getOriginalUserObject(DefaultMutableTreeNode node) {
        // 这里应该返回节点的原始用户对象
        // 为简化实现，直接返回当前对象
        return node.getUserObject();
    }

    /**
     * 更新树结构
     *
     * @param rootNode 新的根节点
     */
    public void updateTree(DefaultMutableTreeNode rootNode) {
        DefaultMutableTreeNode newRoot = rootNode != null ? rootNode : new DefaultMutableTreeNode("Nacos");
        treeModel.setRoot(newRoot);
        treeModel.reload();
        TreeUtil.expandAll(configTree);
    }

    /**
     * 获取配置树
     *
     * @return 配置树
     */
    public Tree getConfigTree() {
        return configTree;
    }

    /**
     * 获取树模型
     *
     * @return 树模型
     */
    public DefaultTreeModel getTreeModel() {
        return treeModel;
    }

    /**
     * 获取根节点
     *
     * @return 根节点
     */
    public DefaultMutableTreeNode getRoot() {
        return root;
    }

    /**
     * 获取当前选中的节点
     *
     * @return 选中节点
     */
    @Nullable
    public DefaultMutableTreeNode getSelectedNode() {
        Object component = configTree.getLastSelectedPathComponent();
        if (component instanceof DefaultMutableTreeNode node) {
            return node;
        }
        return null;
    }
}