package dev.dong4j.zeka.stack.idea.plugin.settings.state;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import lombok.Data;

/**
 * ZKS Dev Helper 主设置状态类
 * <p>
 * 用于保存和管理 ZKS Dev Helper 插件的主配置状态，包括全局配置选项等。
 * 该类实现了持久化状态接口，支持将配置状态存储到 XML 文件中，并在需要时加载配置。
 * 该类位于 {@code settings.state} 包中。
 * <p>
 * 目录结构说明：
 * <ul>
 *   <li>{@code settings.configurable} - 配置类（Configurable 接口实现）</li>
 *   <li>{@code settings.ui} - UI 面板类（Panel 类）</li>
 *   <li>{@code settings.state} - 状态类（PersistentStateComponent 实现）</li>
 * </ul>
 *
 * @author dong4j
 * @version 1.0.0
 * @date 2026.01.02
 * @see dev.dong4j.zeka.stack.idea.plugin.settings.configurable.ZksDevHelperMainConfigurable
 * @see dev.dong4j.zeka.stack.idea.plugin.settings.ui.ZksDevHelperMainPanel
 * @since 1.0.0
 */

/**
 * ZKS Dev Helper 主设置状态类
 * <p>
 * 用于保存和管理 ZKS Dev Helper 插件的主配置状态，包括全局配置选项等。
 * 该类实现了持久化状态接口，支持将配置状态存储到 XML 文件中，并在需要时加载配置。
 * 该类位于 {@code settings.state} 包中。
 * <p>
 * <b>@State 注解说明：</b>
 * <ul>
 *   <li><b>name</b> - 状态的唯一标识符，用于在 IntelliJ 平台内部标识该状态组件。
 *       建议使用简短、描述性的名称，确保唯一性。此名称用于日志记录和服务注册。</li>
 *   <li><b>storages</b> - 指定存储文件路径，配置将保存在 IDE 配置目录下的指定 XML 文件中。</li>
 * </ul>
 * <p>
 * 目录结构说明：
 * <ul>
 *   <li>{@code settings.configurable} - 配置类（Configurable 接口实现）</li>
 *   <li>{@code settings.ui} - UI 面板类（Panel 类）</li>
 *   <li>{@code settings.state} - 状态类（PersistentStateComponent 实现）</li>
 * </ul>
 *
 * @author dong4j
 * @version 1.0.0
 * @date 2026.01.02
 * @since 1.0.0
 * @see dev.dong4j.zeka.stack.idea.plugin.settings.configurable.ZksDevHelperMainConfigurable
 * @see dev.dong4j.zeka.stack.idea.plugin.settings.ui.ZksDevHelperMainPanel
 */
@Data
@State(
    name = "ZksDevHelperMainSettingsState",
    storages = @Storage("zeka.stack.dev.helper.main.xml")
)
public class ZksDevHelperMainState implements PersistentStateComponent<ZksDevHelperMainState> {

    /**
     * 获取当前对象的状态
     * <p>
     * 返回当前对象的设置状态，该方法用于获取配置信息。
     *
     * @return 当前对象的设置状态，可能为 null
     */
    @Nullable
    @Override
    public ZksDevHelperMainState getState() {
        return this;
    }

    /**
     * 加载状态信息到当前对象
     * <p>
     * 通过复制传入的 ZksDevHelperMainState 对象的状态到当前对象中，实现状态的加载。
     *
     * @param state 要加载状态的 ZksDevHelperMainState 对象
     */
    @Override
    public void loadState(@NotNull ZksDevHelperMainState state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    /**
     * 获取 ZksDevHelperMainState 的单例实例
     * <p>
     * 通过 ApplicationManager 获取应用实例，并从中获取 ZksDevHelperMainState 服务对象。
     *
     * @return ZksDevHelperMainState 的单例实例
     */
    public static ZksDevHelperMainState getInstance() {
        return ApplicationManager.getApplication().getService(ZksDevHelperMainState.class);
    }
}

