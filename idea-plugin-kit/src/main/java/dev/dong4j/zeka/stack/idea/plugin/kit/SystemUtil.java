package dev.dong4j.zeka.stack.idea.plugin.kit;

import com.intellij.openapi.util.SystemInfo;

import org.jetbrains.annotations.NotNull;

/**
 * 系统信息工具类
 * <p> 集中封装 IntelliJ Platform 系统信息 API, 避免业务插件直接依赖可能被移除的兼容性方法.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.05.21
 * @since 1.0.0
 */
public final class SystemUtil {

    private SystemUtil() {
    }

    /**
     * 获取当前操作系统名称和版本
     * <p> 不使用 {@code SystemInfo.getOsNameAndVersion()}, 因为该方法已被 IntelliJ Platform 标记为未来移除.
     * 这里仅基于稳定公开常量拼接, 保持反馈上报里的系统信息语义不变.
     *
     * @return 操作系统名称和版本, 例如 "Windows 10.0", "macOS 15.0", "Linux 6.6.0"
     */
    @NotNull
    public static String getOsNameAndVersion() {
        String version = SystemInfo.OS_VERSION == null || SystemInfo.OS_VERSION.isBlank()
                         ? "unknown"
                         : SystemInfo.OS_VERSION;
        if (SystemInfo.isWindows) {
            return "Windows " + version;
        }
        if (SystemInfo.isMac) {
            return "macOS " + version;
        }
        if (SystemInfo.isLinux) {
            return "Linux " + version;
        }
        String name = SystemInfo.OS_NAME == null || SystemInfo.OS_NAME.isBlank()
                      ? "Unknown OS"
                      : SystemInfo.OS_NAME;
        return name + " " + version;
    }
}
