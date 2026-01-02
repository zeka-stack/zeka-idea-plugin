package dev.dong4j.zeka.stack.idea.plugin.kit;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsContexts;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 设置工具类
 * <p> 提供打开设置对话框的功能, 支持通过不同类型的参数来选择特定的设置项
 * <p> 可以传入项目, 配置类, 配置名称或显示名称来打开相应的设置界面
 * showSettingsDialog 与 editConfigurable 的区别是:
 * showSettingsDialog 显示完成的设置界面, 而 editConfigurable 只会显示某个插件的设置界面, 没有左边的菜单
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.02
 * @see ShowSettingsUtil
 * @since 1.0.0
 */
public class SettingsUtil {

    /**
     * 打开指定类型的设置对话框
     * <p> 根据项目和配置类类型打开对应的设置对话框
     *
     * @param project  项目对象, 可以为 null
     * @param toSelect 配置类类型, 不能为 null
     */
    public static <T extends Configurable> void openSettings(@Nullable Project project, @NotNull Class<T> toSelect) {
        ShowSettingsUtil.getInstance().showSettingsDialog(project, toSelect);
    }

    /**
     * 打开设置对话框
     * <p> 根据项目和设置名称打开对应的设置对话框
     *
     * @param project     项目对象, 可以为 null
     * @param displayName 设置显示名称, 模糊匹配, 不能为空,
     *                    可以使用 plugin.xml 中 applicationConfigurable.displayName 属性的值 或者 XxxConfigurable#getDisplayName()
     */
    public static void openSettings(@Nullable Project project, @NlsContexts.ConfigurableName @NotNull String displayName) {
        ShowSettingsUtil.getInstance().showSettingsDialog(project, displayName);
    }

    /**
     * 打开设置对话框, 使用指定的显示名称
     * <p> 此方法会调用 {@code ShowSettingsUtil} 的静态方法来编辑配置项,
     * 并传入一个显示名称作为参数.
     *
     * @param displayName 显示名称, 精确匹配, 必须是 plugin.xml 中 applicationConfigurable.displayName 属性的值
     *                    不能使用 XxxConfigurable#getDisplayName()
     */
    public static void openSettings(@NlsContexts.ConfigurableName @NotNull String displayName) {
        // 打开 IntelliAI Engine 全局设置页面（应用级配置）
        // 使用 null 作为 parent 参数表示打开应用级（全局）配置，而不是项目级配置
        ShowSettingsUtil.getInstance().editConfigurable(null, displayName);
    }

}


