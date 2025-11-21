package dev.dong4j.zeka.stack.idea.plugin.nacos.ui.toolwindow;

import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.BorderLayout;
import java.util.Objects;

import javax.swing.JComponent;
import javax.swing.JPanel;

import dev.dong4j.zeka.stack.idea.plugin.nacos.ui.components.JsonEditor;
import lombok.Getter;
import lombok.Setter;

/**
 * Tab 类型枚举
 */
enum TabKind {
    CONFIG,    // 配置编辑
    COMPARE    // 配置对比
}

/**
 * Nacos 配置标签页
 * 代表一个配置编辑标签页或对比标签页
 *
 * @author dong4j
 * @since 1.0.0
 */
public class Tab {
    /**
     * -- GETTER --
     * 获取标签页 ID
     */
    @Getter
    private final String id;
    /**
     * -- GETTER --
     * 获取标签页标题
     */
    @Getter
    private final String title;
    /**
     * -- GETTER --
     * 获取项目实例
     */
    @Getter
    private final Project project;
    /**
     * -- GETTER --
     * 获取内容面板
     */
    @Getter
    private final JPanel contentPanel;
    @Nullable
    private final JsonEditor editor;
    /**
     * -- GETTER --
     * 获取 Tab 类型
     */
    @Getter
    private final TabKind kind;
    /**
     * -- SETTER --
     * 设置命名空间
     *
     * @param namespace 命名空间
     */
    @Setter
    @Getter
    private String namespace;
    /**
     * -- GETTER --
     * 获取分组
     */
    @Setter
    @Getter
    private String group;
    /**
     * -- GETTER --
     * 获取数据 ID
     */
    @Setter
    @Getter
    private String dataId;
    /**
     * -- GETTER --
     * 检查是否已修改
     */
    @Setter
    @Getter
    private boolean modified = false;

    /**
     * 构造函数 - 配置编辑类型
     */
    public Tab(@NotNull String id, @NotNull String title, @NotNull Project project, @NotNull JsonEditor editor) {
        this.id = id;
        this.title = title;
        this.project = project;
        this.editor = editor;
        this.kind = TabKind.CONFIG;
        this.contentPanel = new JPanel(new BorderLayout());
        this.contentPanel.add(editor, BorderLayout.CENTER);
    }

    /**
     * 构造函数 - 对比类型
     */
    public Tab(@NotNull String id, @NotNull String title, @NotNull Project project, @NotNull JComponent content) {
        this.id = id;
        this.title = title;
        this.project = project;
        this.editor = null;
        this.kind = TabKind.COMPARE;
        this.contentPanel = new JPanel(new BorderLayout());
        this.contentPanel.add(content, BorderLayout.CENTER);
    }

    /**
     * 获取编辑器
     *
     * @return JsonEditor（仅配置编辑类型有值）
     */
    @Nullable
    public JsonEditor getEditor() {
        return editor;
    }

    /**
     * 是否为配置编辑类型
     *
     * @return 是否为配置编辑类型
     */
    public boolean isConfigTab() {
        return kind == TabKind.CONFIG;
    }

    /**
     * 是否为对比类型
     *
     * @return 是否为对比类型
     */
    public boolean isCompareTab() {
        return kind == TabKind.COMPARE;
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