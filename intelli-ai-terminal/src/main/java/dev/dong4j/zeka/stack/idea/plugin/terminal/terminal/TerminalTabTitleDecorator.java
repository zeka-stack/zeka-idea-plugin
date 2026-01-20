package dev.dong4j.zeka.stack.idea.plugin.terminal.terminal;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.ex.ToolWindowManagerListener;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import com.intellij.ui.content.ContentManagerEvent;
import com.intellij.ui.content.ContentManagerListener;

import org.jetbrains.annotations.NotNull;

import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import dev.dong4j.zeka.stack.idea.plugin.terminal.settings.SettingsState;
import kotlin.Unit;
import kotlin.coroutines.Continuation;

/**
 * 终端标签标题装饰器实现类
 * <p> 用于在 IntelliJ IDEA 终端工具窗口的标签页标题前自动添加 AI 标识符号 (🤖), 以区分由 AI 功能生成或管理的终端会话.
 * 该类通过监听终端内容管理器事件, 在内容添加时自动装饰标题, 并支持动态安装监听器, 避免重复装饰.
 * 适用于插件开发中需要对终端标签进行语义标记的场景, 如 AI 辅助终端, 智能会话管理等.
 * <p> 装饰逻辑包括: 检查是否已装饰, 获取标题显示名, 添加前缀符号, 设置用户数据标记, 移除监听器以避免内存泄漏.
 * <p> 通过 ProjectActivity 接口集成到 IntelliJ 插件系统, 支持在项目启动时自动注册监听器.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.21
 * @since 1.0.0
 */
public final class TerminalTabTitleDecorator implements ProjectActivity {

    /** AI 功能标识符, 用于在终端标签标题后追加 AI 标识符号 */
    private static final String AI_MARK = "\uD83E\uDD16 ";
    /** 终端标签装饰器监听器安装状态标识键, 用于判断是否已在工具窗口中安装标题装饰监听器. */
    private static final Map<ContentManager, Boolean> INSTALLED_LISTENERS = createWeakMap();
    /** 终端标签装饰状态标识键, 用于避免重复追加 AI 标识 */
    private static final Key<Boolean> DECORATED_KEY =
        Key.create("dev.dong4j.zeka.stack.idea.plugin.terminal.tab.title.decorated");

    /**
     * 创建一个弱引用映射 (WeakHashMap)
     * <p> 返回一个新的 WeakHashMap 实例, 该映射使用弱引用作为键, 当键不再被其他对象引用时, 其对应的条目会被自动移除.</p>
     * <p> 此方法适用于需要避免内存泄漏的场景, 例如缓存或监听器注册表.</p>
     *
     * @return 新创建的 WeakHashMap 实例, 键和值均支持非空类型
     * @since 1.0
     */
    public static <K, V> @NotNull Map<@NotNull K, V> createWeakMap() {
        return new WeakHashMap<>();
    }

    /**
     * 安装终端标签内容管理器监听器
     * <p> 当终端 AI 功能启用时, 为终端标签添加 AI 标识, 并监听内容添加事件以动态装饰新内容.</p>
     * <p> 若监听器已安装或终端 AI 功能未启用, 则直接返回.</p>
     *
     * @param project  当前项目对象
     * @param terminal 终端工具窗口实例
     */
    private static void installListener(@NotNull Project project, @NotNull ToolWindow terminal) {
        ContentManager contentManager = terminal.getContentManager();
        if (Boolean.TRUE.equals(INSTALLED_LISTENERS.get(contentManager))) {
            return;
        }
        INSTALLED_LISTENERS.put(contentManager, Boolean.TRUE);
        contentManager.addContentManagerListener(new ContentManagerListener() {
            /**
             * 处理内容添加事件
             * <p> 当内容被添加到内容管理器时触发, 若终端 AI 功能未启用则直接返回, 否则对内容进行装饰处理
             *
             * @param event 内容管理器事件对象, 非空
             */
            @Override
            public void contentAdded(@NotNull ContentManagerEvent event) {
                if (!SettingsState.getInstance().enableTerminalAI) {
                    return;
                }
                decorateContent(event.getContent());
            }
        });
    }

    /**
     * 为终端内容标签添加 AI 功能标识装饰
     * <p> 当内容未被装饰过时, 为其添加 AI 标识符号, 并监听属性变更事件, 一旦成功装饰则移除监听器.</p>
     * <p> 装饰过程包括: 检查是否已装饰, 提取显示名称, 格式化并设置装饰后的内容名称.</p>
     *
     * @param content 终端内容对象, 用于设置装饰后的标题名称
     * @since 1.0
     */
    private static void decorateContent(@NotNull Content content) {
        if (Boolean.TRUE.equals(content.getUserData(DECORATED_KEY))) {
            return;
        }
        PropertyChangeListener[] holder = new PropertyChangeListener[1];
        PropertyChangeListener listener = evt -> {
            if (tryDecorate(content)) {
                content.removePropertyChangeListener(holder[0]);
            }
        };
        holder[0] = listener;
        content.addPropertyChangeListener(listener);
        tryDecorate(content);
    }

    /**
     * 尝试为终端内容标签添加 AI 功能标识
     * <p> 检查内容是否已装饰过, 若未装饰则提取显示名称或标签名称, 若名称不为空且未以 AI 标识结尾, 则在名称后追加 AI 标识并更新显示名称和标签名称, 最后标记为已装饰.</p>
     * <p> 若内容已装饰或名称为空, 则直接返回 true.</p>
     *
     * @param content 终端内容对象, 用于获取显示名称, 标签名称并设置装饰状态
     * @return 如果装饰成功或内容已装饰, 则返回 true; 否则返回 false
     */
    private static boolean tryDecorate(@NotNull Content content) {
        if (Boolean.TRUE.equals(content.getUserData(DECORATED_KEY))) {
            return true;
        }
        String displayName = content.getDisplayName();
        String tabName = content.getTabName();
        String baseName = firstNonBlank(displayName, tabName);
        if (baseName == null) {
            return false;
        }
        if (baseName.startsWith(AI_MARK)) {
            content.putUserData(DECORATED_KEY, Boolean.TRUE);
            return true;
        }
        String decorated = AI_MARK + baseName;
        content.setDisplayName(decorated);
        content.setTabName(decorated);
        content.putUserData(DECORATED_KEY, Boolean.TRUE);
        return true;
    }

    /**
     * 获取第一个非空 (非空白) 的字符串
     * <p>依次检查两个字符串参数, 返回第一个非空且非空白的字符串; 若两者均为空或空白, 则返回 null.</p>
     *
     * @param first  第一个字符串, 可能为 null
     * @param second 第二个字符串, 可能为 null
     * @return 第一个非空非空白的字符串, 若无则返回 null
     */
    private static String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        if (second != null && !second.isBlank()) {
            return second;
        }
        return null;
    }

    /**
     * 执行终端 AI 功能的初始化逻辑
     * <p> 当终端 AI 功能启用时, 为终端标签添加 AI 标识, 并监听工具窗口注册事件以动态添加装饰.</p>
     * <p> 若终端 AI 功能未启用, 则直接返回空结果.</p>
     *
     * @param project      当前项目对象
     * @param continuation 继续执行的回调对象, 用于异步操作
     * @return 执行结果, 始终返回 Unit.INSTANCE
     * @since 1.0
     */
    @Override
    public @NotNull Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        if (!SettingsState.getInstance().enableTerminalAI) {
            return Unit.INSTANCE;
        }
        ToolWindowManager manager = ToolWindowManager.getInstance(project);
        ToolWindow terminal = manager.getToolWindow("Terminal");
        if (terminal != null) {
            installListener(project, terminal);
        }
        project.getMessageBus().connect().subscribe(ToolWindowManagerListener.TOPIC, new ToolWindowManagerListener() {
            /**
             * 监听工具窗口注册事件, 当终端工具窗口注册完成后安装监听器
             * <p> 当工具窗口 ID 列表中包含终端窗口 ID 时, 获取该工具窗口并安装监听器
             *
             * @param ids               已注册的工具窗口 ID 列表
             * @param toolWindowManager 工具窗口管理器实例
             */
            @Override
            public void toolWindowsRegistered(@NotNull List<String> ids, @NotNull ToolWindowManager toolWindowManager) {
                if (!ids.contains("Terminal")) {
                    return;
                }
                ToolWindow tw = toolWindowManager.getToolWindow("Terminal");
                if (tw != null) {
                    installListener(project, tw);
                }
            }
        });
        return Unit.INSTANCE;
    }
}
