package dev.dong4j.zeka.stack.idea.plugin.nacos.ui.toolwindow;

import com.intellij.openapi.project.Project;
import com.intellij.ui.tabs.TabInfo;
import com.intellij.ui.tabs.TabsListener;
import com.intellij.ui.tabs.impl.JBTabsImpl;
import com.intellij.util.ui.JBUI;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

import lombok.Getter;

/**
 * Nacos 标签页栏
 * 管理多个配置编辑标签页
 *
 * @author dong4j
 * @since 1.0.0
 */
public class TabBar extends JPanel {
    private final Project project;
    /**
     * -- GETTER --
     * 获取 JBTabsImpl 实例
     */
    @Getter
    private final JBTabsImpl tabs;
    private final List<Tab> tabList;

    public TabBar(@NotNull Project project) {
        this.project = project;
        this.tabs = new JBTabsImpl(project);
        this.tabList = new ArrayList<>();

        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        setBorder(JBUI.Borders.empty(5));

        // 添加标签页监听器
        tabs.addListener(new TabsListener() {
            @Override
            public void selectionChanged(TabInfo oldSelection, TabInfo newSelection) {
                // 标签页选择改变时的处理
            }

            @Override
            public void tabRemoved(TabInfo tabInfo) {
                // 标签页被移除时的处理
                Tab tab = getTabById(tabInfo.getText());
                if (tab != null) {
                    tabList.remove(tab);
                }
            }
        });

        add(tabs.getComponent(), BorderLayout.CENTER);
    }

    /**
     * 添加新标签页
     *
     * @param tab 标签页
     */
    public void addTab(@NotNull Tab tab) {
        if (getTabById(tab.getId()) != null) {
            // 如果标签页已存在，切换到该标签页
            selectTab(tab.getId());
            return;
        }

        TabInfo tabInfo = new TabInfo(tab.getContentPanel());
        tabInfo.setText(tab.getTitle());
        tabInfo.setObject(tab);

        tabs.addTab(tabInfo);
        tabList.add(tab);
    }

    /**
     * 关闭标签页
     *
     * @param tabId 标签页 ID
     */
    public void closeTab(@NotNull String tabId) {
        Tab tab = getTabById(tabId);
        if (tab != null) {
            TabInfo tabInfo = findTabInfo(tab);
            if (tabInfo != null) {
                tabs.removeTab(tabInfo);
                tabList.remove(tab);
            }
        }
    }

    /**
     * 选择标签页
     *
     * @param tabId 标签页 ID
     */
    public void selectTab(@NotNull String tabId) {
        Tab tab = getTabById(tabId);
        if (tab != null) {
            TabInfo tabInfo = findTabInfo(tab);
            if (tabInfo != null) {
                tabs.select(tabInfo, true);
            }
        }
    }

    /**
     * 更新标签页标题
     *
     * @param tabId 标签页 ID
     * @param title 新标题
     */
    public void updateTabTitle(@NotNull String tabId, @NotNull String title) {
        Tab tab = getTabById(tabId);
        if (tab != null) {
            TabInfo tabInfo = findTabInfo(tab);
            if (tabInfo != null) {
                tabInfo.setText(title);
            }
        }
    }

    /**
     * 获取标签页
     *
     * @param tabId 标签页 ID
     * @return 标签页，如果不存在则返回 null
     */
    @Nullable
    public Tab getTabById(@NotNull String tabId) {
        return tabList.stream()
            .filter(tab -> tab.getId().equals(tabId))
            .findFirst()
            .orElse(null);
    }

    /**
     * 获取所有标签页
     *
     * @return 标签页列表
     */
    public List<Tab> getAllTabs() {
        return new ArrayList<>(tabList);
    }

    /**
     * 获取当前选中的标签页
     *
     * @return 当前选中的标签页，如果没有则返回 null
     */
    @Nullable
    public Tab getSelectedTab() {
        TabInfo selectedInfo = tabs.getSelectedInfo();
        if (selectedInfo != null) {
            return (Tab) selectedInfo.getObject();
        }
        return null;
    }

    /**
     * 检查标签页是否存在
     *
     * @param tabId 标签页 ID
     * @return 是否存在
     */
    public boolean containsTab(@NotNull String tabId) {
        return getTabById(tabId) != null;
    }

    /**
     * 获取标签页数量
     *
     * @return 标签页数量
     */
    public int getTabCount() {
        return tabList.size();
    }

    /**
     * 关闭所有标签页
     */
    public void closeAllTabs() {
        List<Tab> tabsToClose = new ArrayList<>(tabList);
        for (Tab tab : tabsToClose) {
            closeTab(tab.getId());
        }
    }

    /**
     * 根据 Tab 对象查找 TabInfo
     *
     * @param tab Tab 对象
     * @return TabInfo 对象，如果不存在则返回 null
     */
    @Nullable
    private TabInfo findTabInfo(@NotNull Tab tab) {
        for (TabInfo tabInfo : tabs.getTabs()) {
            if (tabInfo.getObject() == tab) {
                return tabInfo;
            }
        }
        return null;
    }

}