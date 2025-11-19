package dev.dong4j.zeka.stack.idea.plugin.nacos.ui.toolwindow;

import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

import java.awt.BorderLayout;
import java.util.Objects;

import javax.swing.JPanel;

import dev.dong4j.zeka.stack.idea.plugin.nacos.ui.components.JsonEditor;

/**
 * Nacos 配置标签页
 * 代表一个配置编辑标签页
 *
 * @author dong4j
 * @since 1.0.0
 */
public class Tab {
    private final String id;
    private final String title;
    private final Project project;
    private final JPanel contentPanel;
    private final JsonEditor editor;
    private String namespace;
    private String group;
    private String dataId;
    private boolean modified = false;

    public Tab(@NotNull String id, @NotNull String title, @NotNull Project project, @NotNull JsonEditor editor) {
        this.id = id;
        this.title = title;
        this.project = project;
        this.editor = editor;
        this.contentPanel = new JPanel(new BorderLayout());
        this.contentPanel.add(editor, BorderLayout.CENTER);
    }

    /**
     * 获取标签页 ID
     *
     * @return 标签页 ID
     */
    public String getId() {
        return id;
    }

    /**
     * 获取标签页标题
     *
     * @return 标签页标题
     */
    public String getTitle() {
        return title;
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
     * 获取内容面板
     *
     * @return 内容面板
     */
    public JPanel getContentPanel() {
        return contentPanel;
    }

    /**
     * 获取编辑器
     *
     * @return JsonEditor
     */
    public JsonEditor getEditor() {
        return editor;
    }

    /**
     * 获取命名空间
     *
     * @return 命名空间
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * 设置命名空间
     *
     * @param namespace 命名空间
     */
    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    /**
     * 获取分组
     *
     * @return 分组
     */
    public String getGroup() {
        return group;
    }

    /**
     * 设置分组
     *
     * @param group 分组
     */
    public void setGroup(String group) {
        this.group = group;
    }

    /**
     * 获取数据 ID
     *
     * @return 数据 ID
     */
    public String getDataId() {
        return dataId;
    }

    /**
     * 设置数据 ID
     *
     * @param dataId 数据 ID
     */
    public void setDataId(String dataId) {
        this.dataId = dataId;
    }

    /**
     * 检查是否已修改
     *
     * @return 是否已修改
     */
    public boolean isModified() {
        return modified;
    }

    /**
     * 设置修改状态
     *
     * @param modified 修改状态
     */
    public void setModified(boolean modified) {
        this.modified = modified;
    }

    /**
     * 更新标签页标题
     *
     * @param newTitle 新标题
     */
    public void updateTitle(String newTitle) {
        // 标题更新逻辑将在 TabBar 中处理
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Tab tab = (Tab) o;
        return Objects.equals(id, tab.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Tab{" +
               "id='" + id + '\'' +
               ", title='" + title + '\'' +
               ", namespace='" + namespace + '\'' +
               ", group='" + group + '\'' +
               ", dataId='" + dataId + '\'' +
               '}';
    }
}