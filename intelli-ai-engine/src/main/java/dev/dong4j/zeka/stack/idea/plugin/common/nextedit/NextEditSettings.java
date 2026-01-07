package dev.dong4j.zeka.stack.idea.plugin.common.nextedit;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;

import org.jetbrains.annotations.NotNull;

/**
 * NextEditSettings 类
 * <p> 用于存储和管理 NextEdit 插件的配置设置, 包括是否启用功能以及去抖时间间隔等参数.
 * <p> 该类实现了 PersistentStateComponent 接口, 支持将配置持久化到 XML 文件中, 并在插件启动时加载配置.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.05
 * @since 1.0.0
 */
@State(
    name = "IntelliAINextEditSettings",
    storages = @Storage("zeka.stack.intelliai.engine.xml")
)
public final class NextEditSettings implements PersistentStateComponent<NextEditSettings> {
    /**
     * 是否启用功能
     * <p> 默认值为 false, 表示功能处于关闭状态.</p>
     */
    public boolean enabled = false;
    /** 延迟触发的时间间隔, 单位为毫秒 */
    public long debounceMs = 300;

    /**
     * 获取当前应用的 NextEditSettings 实例
     * <p> 通过应用管理器获取并返回 NextEditSettings 服务的单例实例
     *
     * @return NextEditSettings 的单例实例
     */
    public static NextEditSettings getInstance() {
        return ApplicationManager.getApplication().getService(NextEditSettings.class);
    }

    /**
     * 获取当前设置状态
     * <p> 返回当前 NextEditSettings 实例的副本, 用于持久化存储
     *
     * @return 当前设置状态的不可变副本
     */
    @Override
    public @NotNull NextEditSettings getState() {
        return this;
    }

    /**
     * 加载状态数据
     * <p> 将指定的状态对象复制到当前实例中, 用于恢复组件的持久化状态
     * <p> 该方法由 IntelliJ 平台调用, 用于从 XML 配置中加载组件状态
     *
     * @param state 要加载的状态对象, 不能为 null
     */
    @Override
    public void loadState(@NotNull NextEditSettings state) {
        XmlSerializerUtil.copyBean(state, this);
    }
}
