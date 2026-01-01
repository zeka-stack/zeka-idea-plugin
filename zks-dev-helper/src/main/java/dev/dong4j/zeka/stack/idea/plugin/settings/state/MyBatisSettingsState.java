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
 * My Batis Settings State
 *
 * @author dong4j
 * @version hello.world
 * @date 2026-01-02 03:01:31
 * @since hello.world
 */
@Data
@State(
    name = "ZksDevHelperMyBatisSettingsState",
    storages = @Storage("zeka.stack.dev.helper.mybatis.xml")
)
public class MyBatisSettingsState implements PersistentStateComponent<MyBatisSettingsState> {

    // ========== MyBatis 模块配置 ==========
    /** 是否启用 MyBatis 功能，默认启用 */
    private boolean enableMyBatis = true;

    // 后续可以在此添加更多 MyBatis 相关配置选项，例如：
    // private boolean enableAutoCompletion = true;
    // private String mapperScanPath = "";

    /**
     * 获取当前对象的 MyBatis 设置状态
     * <p>
     * 返回当前对象的 MyBatis 设置状态，该方法用于获取配置信息。
     *
     * @return 当前对象的 MyBatis 设置状态，可能为 null
     */
    @Nullable
    @Override
    public MyBatisSettingsState getState() {
        return this;
    }

    /**
     * 加载状态信息到当前对象
     * <p>
     * 通过复制传入的 MyBatisSettingsState 对象的状态到当前对象中，实现状态的加载。
     *
     * @param state 要加载状态的 MyBatisSettingsState 对象
     */
    @Override
    public void loadState(@NotNull MyBatisSettingsState state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    /**
     * 获取 MyBatisSettingsState 的单例实例
     * <p>
     * 通过 ApplicationManager 获取应用实例，并从中获取 MyBatisSettingsState 服务对象。
     *
     * @return MyBatisSettingsState 的单例实例
     */
    public static MyBatisSettingsState getInstance() {
        return ApplicationManager.getApplication().getService(MyBatisSettingsState.class);
    }
}

