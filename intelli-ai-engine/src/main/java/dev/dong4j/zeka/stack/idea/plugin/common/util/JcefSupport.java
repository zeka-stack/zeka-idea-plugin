package dev.dong4j.zeka.stack.idea.plugin.common.util;

/**
 * JCEF 可用性探测
 * <p>
 * 2026.2 起 JCEF 从平台核心拆到 {@code com.intellij.modules.jcef} bundled plugin.
 * 直接调用 {@code JBCefApp.isSupported()} 会在 classloader 看不到 JCEF 时抛出
 * {@link NoClassDefFoundError}, 进而拖垮 Engine 启动 (聊天面板 / What's New).
 * 因此这里只用反射探测类, 任何链路错误都视为不支持并走降级 UI.
 * </p>
 *
 * @author dong4j
 * @version 2026.2.1004
 * @since 2026.2.1004
 */
public final class JcefSupport {

    private static final String JB_CEF_APP_CLASS = "com.intellij.ui.jcef.JBCefApp";

    private JcefSupport() {
        // 工具类, 禁止实例化
    }

    /**
     * 当前 IDE 是否可以创建 JCEF 浏览器
     * <p>
     * 必须对调用方保持无异常: JCEF 插件未安装、被禁用、或 natives 缺失时都返回 false.
     * </p>
     *
     * @return true 表示可以安全使用 {@code JBCefBrowser}; false 应走 Swing 降级
     */
    public static boolean isSupported() {
        try {
            Class<?> jbCefApp = Class.forName(JB_CEF_APP_CLASS);
            Object supported = jbCefApp.getMethod("isSupported").invoke(null);
            return Boolean.TRUE.equals(supported);
        } catch (ClassNotFoundException | LinkageError ignored) {
            return false;
        } catch (Throwable ignored) {
            // InvocationTargetException / 原生库加载失败等, 一律降级而不是炸掉工具窗口
            return false;
        }
    }
}
