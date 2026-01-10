package dev.dong4j.zeka.stack.idea.plugin.common.agent;

import com.intellij.ide.AppLifecycleListener;
import com.intellij.ide.util.RunOnceUtil;
import com.intellij.openapi.application.ApplicationActivationListener;
import com.intellij.openapi.wm.IdeFrame;

import org.jetbrains.annotations.NotNull;

import java.awt.Window;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

/**
 * 插件生命周期监听器类
 * <p> 实现了 `AppLifecycleListener` 和 `ApplicationActivationListener` 接口, 用于监听应用程序的生命周期事件, 并在特定事件发生时执行相应的初始化或清理逻辑.
 * <p> 具体功能包括:
 * - 在应用启动时执行初始化逻辑
 * - 在欢迎屏幕显示时执行初始化逻辑
 * - 在项目框架关闭时执行清理逻辑
 * - 在项目打开失败时执行清理逻辑
 * - 在应用关闭时执行清理逻辑
 * - 在应用即将关闭时执行清理逻辑
 * - 监听应用程序的激活和去激活事件
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2025.12.25
 * @since 1.0.0
 */
@Slf4j
public final class PluginLifecycleListener implements AppLifecycleListener, ApplicationActivationListener {
    /**
     * 在应用程序框架创建时调用
     * <p> 当应用程序框架创建时, 执行一次初始化逻辑, 记录日志信息
     *
     * @param commandLineArgs 命令行参数列表
     * @since hello.world
     */
    @Override
    public void appFrameCreated(@NotNull List<String> commandLineArgs) {
        // 是应用第一次启动时执行, 后续启动时不执行(也就是只执行一次, 通过 id 进行唯一约束)
        RunOnceUtil.runOnceForApp("plugin-lifecycle-listener", () -> {
            // 在应用启动时执行的初始化逻辑
            log.debug("应用启动时执行的初始化逻辑");
        });
    }

    /**
     * 在欢迎屏幕显示时执行的初始化逻辑
     * <p> 此方法在欢迎屏幕显示时被调用, 确保初始化逻辑只执行一次. 具体逻辑包括记录日志信息.
     *
     * @since hello.world
     */
    @Override
    public void welcomeScreenDisplayed() {
        RunOnceUtil.runOnceForApp("plugin-lifecycle-listener", () -> {
            // 在欢迎屏幕显示时执行的初始化逻辑
            log.debug("在欢迎屏幕显示时执行的初始化逻辑");
        });
    }

    /**
     * 在项目框架关闭时执行的清理逻辑
     * <p> 确保在项目框架关闭时只执行一次特定的清理操作, 避免重复执行.
     *
     * @since hello.world
     */
    @Override
    public void projectFrameClosed() {
        RunOnceUtil.runOnceForApp("plugin-lifecycle-listener", () -> {
            // 在项目框架关闭时执行的清理逻辑
            log.debug("在项目框架关闭时执行的清理逻辑");
        });
    }

    /**
     * 在项目打开失败时执行的清理逻辑
     * <p> 当项目打开失败时, 执行一次清理逻辑, 记录日志信息
     *
     * @since hello.world
     */
    @Override
    public void projectOpenFailed() {
        RunOnceUtil.runOnceForApp("plugin-lifecycle-listener", () -> {
            // 在项目打开失败时执行的清理逻辑
            log.debug("在项目打开失败时执行的清理逻辑");
        });
    }

    /**
     * 在应用关闭时执行的清理逻辑
     * <p> 确保在应用关闭时执行一次清理操作, 避免重复执行.
     *
     * @since hello.world
     */
    @Override
    public void appClosing() {
        RunOnceUtil.runOnceForApp("plugin-lifecycle-listener", () -> {
            // 在应用关闭时执行的清理逻辑
            log.debug("在应用关闭时执行的清理逻辑");
        });
    }

    /**
     * 在应用即将关闭时执行的清理逻辑
     * <p> 此方法会在应用即将关闭时被调用, 用于执行必要的清理工作.
     *
     * @param isRestart 表示应用是否将重新启动
     */
    @Override
    public void appWillBeClosed(boolean isRestart) {
        RunOnceUtil.runOnceForApp("plugin-lifecycle-listener", () -> {
            // 在应用即将关闭时执行的清理逻辑
            log.debug("在应用即将关闭时执行的清理逻辑");
        });
    }

    /**
     * 应用程序从失去焦点到获得焦点时触发
     * <p> 当应用程序被激活时, 检查是否有打开的项目. 如果有, 则在后台线程中进行以下操作:
     * 1. 暂停 1 秒钟
     * 2. 获取 IntelliAI Agent 的设置
     * 3. 如果设置了自动启动且对应的 jar 文件存在, 则启动 IntelliAI Agent
     * 4. 如果设置了自动更新, 则启动更新检查器
     *
     * @param ideFrame 激活的应用程序框架
     */
    @Override
    public void applicationActivated(@NotNull IdeFrame ideFrame) {
    }

    /**
     * 应用程序失活时触发
     * <p> 当应用程序失去焦点时调用此方法. 可以在此方法中执行与应用程序失活相关的清理或逻辑.
     *
     * @param ideFrame 失活的应用程序框架
     * @since hello.world
     */
    @Override
    public void applicationDeactivated(@NotNull IdeFrame ideFrame) {
    }

    /**
     * 在应用程序延迟去激活时触发
     * <p> 此方法在应用程序窗口失去焦点时调用, 通常用于执行与窗口状态相关的清理或准备操作.
     *
     * @param ideFrame 失去焦点的应用程序窗口
     * @since hello.world
     */
    @Override
    public void delayedApplicationDeactivated(@NotNull Window ideFrame) {
    }

}
