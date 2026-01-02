package dev.dong4j.zeka.stack.idea.plugin.common.whatsnew;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.util.ui.JBUI;

import java.awt.event.ActionEvent;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import dev.dong4j.zeka.stack.idea.plugin.common.util.AICommonBundle;

/**
 * 新功能对话框类
 * <p>用于展示应用程序的新功能和更新内容, 支持切换不同功能模块的更新日志, 并提供向前 / 向后浏览版本的功能.
 * <p>该对话框通过加载配置的更新提供者 (WhatsNewProvider) 来动态展示内容, 支持多标签页切换和版本导航.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2025.12.31
 * @since 1.0.0
 */
public class WhatsNewDialog extends DialogWrapper {
    /**
     * 共享的 WhatsNewDialog 实例
     * <p> 用于在项目中显示更新内容对话框. 如果共享对话框已存在且可见, 则在显示新对话框前先销毁旧的实例.
     */
    private static WhatsNewDialog sharedDialog;

    /**
     * 显示“有什么新功能”页面的面板组件
     *
     * @see WhatsNewPanel
     */
    private final WhatsNewPanel whatsNewPanel = new WhatsNewPanel();
    /** 用于切换到旧版本更新日志的操作按钮 */
    private final OlderAction olderAction = new OlderAction();
    /** 新版本切换操作按钮, 用于展示更新后的变更日志 */
    private final NewerAction newerAction = new NewerAction();

    /**
     * 所有提供 "What's New" 内容的提供者列表
     * <p> 通过 {@link WhatsNewProviderServiceHolder} 获取并过滤掉无页面的提供者 </p>
     *
     * @see WhatsNewProviderServiceHolder#getProviders()
     */
    private final List<WhatsNewProvider> providers;

    /**
     * 选项卡面板, 用于显示不同的“最新动态”页面.
     *
     * @see #createNorthPanel()
     */
    private JTabbedPane tabbedPane;

    /**
     * 构造一个新的 WhatsNewDialog 实例
     * <p> 初始化对话框并设置标题, 关闭按钮文本等基本属性
     *
     * @param project 当前项目实例, 用于上下文相关初始化
     */
    public WhatsNewDialog(Project project) {
        super(project, true);
        setModal(false);
        setTitle(AICommonBundle.message("whatsnew.title"));
        setCancelButtonText(AICommonBundle.message("whatsnew.dialog.close"));
        // 延迟获取提供者列表，避免在类初始化时获取服务
        this.providers = WhatsNewProviderServiceHolder.getProviders();
        init();
    }

    /**
     * 创建北侧面板, 用于显示不同提供商的更新内容标签页
     * <p> 如果提供商列表为空, 则返回 null. 否则创建一个 JTabbedPane, 为每个提供商添加一个标签页, 并默认选择第一个提供商.
     *
     * @return 北侧面板组件, 若无提供商则返回 null
     */
    @Override
    protected JComponent createNorthPanel() {
        if (providers.isEmpty()) {
            return null;
        }

        tabbedPane = new JTabbedPane();
        tabbedPane.addChangeListener(new ChangeListener() {
            /**
             * 当选项卡选择状态改变时调用此方法
             * <p> 根据当前选中的选项卡索引, 调用 selectProvider 方法进行相应的处理
             *
             * @param e 表示状态变化的 ChangeEvent 对象
             */
            @Override
            public void stateChanged(ChangeEvent e) {
                selectProvider(tabbedPane.getSelectedIndex());
            }
        });

        for (WhatsNewProvider provider : providers) {
            tabbedPane.addTab(provider.getDisplayName(), new JPanel());
        }

        selectProvider(0);
        return tabbedPane;
    }

    /**
     * 创建对话框的中心面板
     * <p> 如果不存在任何提供者, 则返回一个空面板; 否则返回用于显示更新内容的面板.
     *
     * @return 中心面板组件
     */
    @Override
    protected JComponent createCenterPanel() {
        if (providers.isEmpty()) {
            return new WhatsNewEmptyPanel();
        }
        return whatsNewPanel;
    }

    /**
     * 创建对话框的南侧面板组件
     * <p> 调用父类创建的南侧面板, 并为其设置边距
     *
     * @return 南侧面板组件
     */
    @Override
    protected JComponent createSouthPanel() {
        JComponent panel = super.createSouthPanel();
        if (panel != null) {
            panel.setBorder(JBUI.Borders.empty(8, 12));
        }
        return panel;
    }

    /**
     * 创建并返回对话框中的动作数组
     * <p> 该方法重写了父类的 {@code createActions} 方法, 返回一个包含 "更早版本" 动作,"更新版本" 动作和取消动作的数组.
     *
     * @return 包含 "更早版本" 动作,"更新版本" 动作和取消动作的数组
     */
    @Override
    protected Action[] createActions() {
        return new Action[]{olderAction, newerAction, getCancelAction()};
    }

    /**
     * 获取首选聚焦组件
     * <p> 返回对话框中首选的聚焦组件, 通常为取消按钮.
     *
     * @return 首选聚焦组件
     */
    @Override
    public JComponent getPreferredFocusedComponent() {
        return getButton(getCancelAction());
    }

    /**
     * 根据指定索引选择对应的提供者并更新界面
     * <p> 该方法会设置当前选中的提供者, 并调用更新组件的方法以刷新界面状态
     *
     * @param index 提供者在列表中的索引位置
     */
    private void selectProvider(int index) {
        if (index < 0 || index >= providers.size()) {
            return;
        }
        whatsNewPanel.setProvider(providers.get(index));
        updateComponents();
    }

    /**
     * 更新对话框中的组件状态
     * <p>根据当前面板内容更新标签页标题和导航按钮 (新版本 / 旧版本) 的状态及显示文本
     */
    private void updateComponents() {
        updateTabTitle();

        String currentVersion = whatsNewPanel.currentVersion();
        boolean hasNewer = whatsNewPanel.hasNewer();
        boolean hasOlder = whatsNewPanel.hasOlder();
        newerAction.setEnabled(hasNewer);
        olderAction.setEnabled(hasOlder);

        String newerVersion = hasNewer ? whatsNewPanel.newerVersion() : currentVersion;
        updateActionName(newerAction, AICommonBundle.message("whatsnew.dialog.newer"), newerVersion);

        String olderVersion = hasOlder ? whatsNewPanel.olderVersion() : currentVersion;
        updateActionName(olderAction, AICommonBundle.message("whatsnew.dialog.older"), olderVersion);
    }

    /**
     * 更新选项卡标题
     * <p> 根据当前选中的选项卡索引和对应的版本信息更新选项卡标题. 如果版本信息为空, 则仅显示提供者的显示名称.</p>
     *
     * @since 1.0
     */
    private void updateTabTitle() {
        if (tabbedPane == null) {
            return;
        }
        int index = tabbedPane.getSelectedIndex();
        if (index < 0 || index >= providers.size()) {
            return;
        }
        String version = whatsNewPanel.currentVersion();
        String baseTitle = providers.get(index).getDisplayName();
        if (version == null) {
            tabbedPane.setTitleAt(index, baseTitle);
            return;
        }

        tabbedPane.setTitleAt(index, baseTitle + " (" + version + ")");
    }

    /**
     * 更新动作按钮的名称, 根据版本信息动态显示
     * <p> 如果版本信息不为空, 则在基础名称后添加版本号, 用于显示更详细的信息
     *
     * @param action   要更新名称的抽象动作对象
     * @param baseName 基础名称, 用于显示动作的默认名称
     * @param version  版本信息, 如果非空则附加到基础名称后
     */
    private void updateActionName(AbstractAction action, String baseName, String version) {
        if (version == null) {
            action.putValue(Action.NAME, baseName);
        } else {
            action.putValue(Action.NAME, baseName + " (" + version + ")");
        }
    }

    /**
     * 在给定的项目中显示“有什么新功能”对话框.
     * <p> 如果当前已有对话框存在且可见, 则先关闭该对话框, 然后创建一个新的对话框并显示.
     *
     * @param project 要显示对话框的项目
     * @since 1.0
     */
    public static synchronized void showForProject(Project project) {
        if (sharedDialog != null && sharedDialog.isVisible()) {
            sharedDialog.dispose();
        }
        sharedDialog = new WhatsNewDialog(project);
        sharedDialog.show();
    }

    /**
     * 旧版本操作类
     * <p> 用于在“新特性”对话框中表示“旧版本”操作, 点击后可查看更早的版本更新日志.
     * <p> 该类继承自 AbstractAction, 通过禁用状态控制是否可点击, 并在触发时调用面板的 olderChangelog 方法.
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2025.12.31
     * @since 1.0.0
     */
    private class OlderAction extends AbstractAction {
        /**
         * 构造函数, 初始化 OlderAction 对象
         * <p> 调用父类构造函数并设置动作不可用状态
         *
         * @since 1.0
         */
        private OlderAction() {
            super(AICommonBundle.message("whatsnew.dialog.older"));
            setEnabled(false);
        }

        /**
         * 当操作事件被触发时调用, 用于加载上一版本的更新日志并更新组件状态
         * <p> 该方法会调用 {@code whatsNewPanel.olderChangelog()} 加载旧版更新日志, 并调用 {@code updateComponents()} 刷新界面组件
         *
         * @param e 发生的动作事件
         */
        @Override
        public void actionPerformed(ActionEvent e) {
            whatsNewPanel.olderChangelog();
            updateComponents();
        }
    }

    /**
     * 新版本操作类
     * <p> 用于处理用户点击“新版本”按钮时的逻辑, 触发更新日志展示和界面组件更新操作
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2025.10.24
     * @since 1.0.0
     */
    private class NewerAction extends AbstractAction {
        /**
         * 初始化 NewerAction 对象
         * <p> 设置动作的名称为 "whatsnew.dialog.newer" 对应的本地化消息, 并禁用该动作
         *
         */
        private NewerAction() {
            super(AICommonBundle.message("whatsnew.dialog.newer"));
            setEnabled(false);
        }

        /**
         * 处理动作事件的方法
         * <p> 当动作事件触发时, 调用 newerChangelog 方法更新变更日志, 并调用 updateComponents 方法更新组件状态
         *
         * @param e 动作事件对象
         */
        @Override
        public void actionPerformed(ActionEvent e) {
            whatsNewPanel.newerChangelog();
            updateComponents();
        }
    }

    /**
     * 提供获取最新功能页面的服务持有者类
     * <p> 该类提供获取最新功能页面提供者的列表功能. 其中, 过滤掉没有页面的提供者, 并按一定的规则进行排序.
     * 使用延迟加载方式获取服务, 避免在类初始化时获取服务实例.
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2025.12.31
     * @since 1.0.0
     */
    private static final class WhatsNewProviderServiceHolder {
        /**
         * 获取所有有效的 WhatsNewProvider 列表
         * <p> 通过 WhatsNewProviderService 获取所有 WhatsNewProvider, 并过滤掉没有页面的 provider, 然后按排序规则进行排序.
         * 使用延迟加载方式获取服务, 避免在类初始化时获取服务实例.
         *
         * @return 有效的 WhatsNewProvider 列表, 如果服务获取失败则返回空列表
         */
        private static List<WhatsNewProvider> getProviders() {
            try {
                WhatsNewProviderService service = com.intellij.openapi.application.ApplicationManager.getApplication()
                    .getService(WhatsNewProviderService.class);
                if (service == null) {
                    return List.of();
                }
                return service.getWhatsNewProviders().stream()
                    .filter(provider -> !provider.getPages().isEmpty())
                    .sorted(new WhatsNewProviderComparator())
                    .collect(Collectors.toList());
            } catch (Exception e) {
                // 如果获取服务失败（例如在类初始化时），返回空列表
                return List.of();
            }
        }
    }

    /**
     * 用于比较 WhatsNewProvider 实例的比较器
     * <p> 该比较器用于对 WhatsNewProvider 对象进行排序, 其中 InternalWhatsNewProvider 类型的对象会被排在最前面
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2025.12.31
     * @since 1.0.0
     */
    private static final class WhatsNewProviderComparator implements Comparator<WhatsNewProvider> {
        /**
         * 比较两个 WhatsNewProvider 对象的优先级
         * <p> 如果 p1 是 InternalWhatsNewProvider 类型, 则 p1 优先级更高, 返回 -1;<br>
         * 如果 p2 是 InternalWhatsNewProvider 类型, 则 p2 优先级更高, 返回 1;<br>
         * 否则返回 0, 表示两者优先级相同.
         *
         * @param p1 要比较的第一个 WhatsNewProvider 对象
         * @param p2 要比较的第二个 WhatsNewProvider 对象
         * @return 比较结果,-1 表示 p1 优先级更高,1 表示 p2 优先级更高,0 表示优先级相同
         */
        @Override
        public int compare(WhatsNewProvider p1, WhatsNewProvider p2) {
            if (p1 instanceof InternalWhatsNewProvider) {
                return -1;
            }
            if (p2 instanceof InternalWhatsNewProvider) {
                return 1;
            }
            return 0;
        }
    }
}
