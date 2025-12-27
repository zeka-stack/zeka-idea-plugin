package dev.dong4j.zeka.stack.idea.plugin.common.update;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.updateSettings.impl.PluginDownloader;
import com.intellij.openapi.updateSettings.impl.UpdateChecker;
import com.intellij.openapi.util.BuildNumber;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;

/**
 * 更新检查器类
 * <p> 用于查找可用的插件更新. 通过反射机制调用内部的更新检查方法, 获取所有启用的插件下载器.
 * 在调用过程中, 可能会因为不支持的 IntelliJ 版本, 权限问题或其他异常导致无法获取更新信息.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2025.12.27
 * @since 1.0.0
 */
final class UpdaterChecker {
    /**
     * 记录更新检查过程中的日志信息
     *
     * @see Logger
     */
    private static final Logger LOG = Logger.getInstance(UpdaterChecker.class);

    /**
     * 查找可用的插件更新
     * <p> 使用反射调用 IntelliJ Platform 的内部 API 来检查插件更新. 此方法会根据给定的进度指示器执行更新检查, 并返回可用的插件更新列表.
     * 如果在检查过程中出现异常, 则记录警告日志并返回空列表.
     *
     * @param indicator 进度指示器, 用于显示检查进度
     * @return 可用的插件更新列表, 如果检查失败则返回空列表
     */
    @NotNull
    static Collection<PluginDownloader> findAvailableUpdates(@NotNull ProgressIndicator indicator) {
        try {
            // 使用反射调用 UpdateChecker.getInternalPluginUpdates 方法
            Method getInternalPluginUpdatesMethod = UpdateChecker.class.getMethod(
                "getInternalPluginUpdates",
                BuildNumber.class,
                ProgressIndicator.class
                                                                                 );
            Object internalPluginResults = getInternalPluginUpdatesMethod.invoke(null, null, indicator);

            // 调用 getPluginUpdates 方法
            Method getPluginUpdatesMethod = internalPluginResults.getClass().getMethod("getPluginUpdates");
            Object pluginUpdates = getPluginUpdatesMethod.invoke(internalPluginResults);

            // 调用 getAllEnabled 方法获取所有启用的插件更新
            Method getAllEnabledMethod = pluginUpdates.getClass().getMethod("getAllEnabled");
            Object allEnabled = getAllEnabledMethod.invoke(pluginUpdates);

            // 转换为 Collection<PluginDownloader>
            @SuppressWarnings("unchecked")
            Collection<PluginDownloader> result = allEnabled == null
                                                  ? Collections.emptyList()
                                                  : (Collection<PluginDownloader>) allEnabled;

            return result;
        } catch (NoSuchMethodException e) {
            LOG.warn("无法找到更新检查方法，可能是不支持的 IntelliJ 版本", e);
            return Collections.emptyList();
        } catch (IllegalAccessException e) {
            LOG.warn("无法访问更新检查方法，可能是权限问题", e);
            return Collections.emptyList();
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause != null) {
                LOG.warn("调用更新检查方法时发生异常", cause);
            } else {
                LOG.warn("调用更新检查方法时发生异常", e);
            }
            return Collections.emptyList();
        } catch (ClassCastException e) {
            LOG.warn("返回类型转换失败，可能是不支持的 IntelliJ 版本", e);
            return Collections.emptyList();
        } catch (Exception e) {
            LOG.warn("检查插件更新时发生未知异常", e);
            return Collections.emptyList();
        }
    }
}
