package dev.dong4j.zeka.stack.idea.plugin.common.statistics;

import com.intellij.ide.AppLifecycleListener;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import kotlin.Unit;
import kotlin.coroutines.Continuation;
import lombok.extern.slf4j.Slf4j;

/**
 * <p> 描述: 统计服务启动器.</p>
 *
 * @author dong4j
 * @version 1.4.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2025.01.05
 */
@Slf4j
@Service
public final class StatisticsServiceInitializer {
    /**
     * 统计服务的单例实现实例
     * <p> 用于延迟初始化并保证线程安全的访问
     */
    private static StatisticsServiceImpl serviceInstance;

    /**
     * 私有构造函数, 防止外部实例化
     * <p> 该类为静态服务初始化器, 仅允许通过静态方法获取服务实例
     */
    private StatisticsServiceInitializer() {
    }

    /**
     * 获取服务实例
     *
     * @return 服务实例对象
     */
    public static StatisticsService getService() {
        if (serviceInstance == null) {
            synchronized (StatisticsServiceInitializer.class) {
                if (serviceInstance == null) {
                    serviceInstance = new StatisticsServiceImpl();
                }
            }
        }
        return serviceInstance;
    }

    /** 启动统计服务 */
    public static void startService() {
        StatisticsService service = getService();
        service.start();
        log.debug("Statistics service started");
    }

    /** 停止统计服务 */
    public static void stopService() {
        if (serviceInstance != null) {
            serviceInstance.stop();
            log.debug("Statistics service stopped");
        }
    }

    /**
     * 项目启动时执行的统计活动
     *
     * @param project      项目对象
     * @param continuation 续航对象, 用于异步回调
     * @return Unit 实例, 表示操作完成
     */
    public static class StatisticsStartupActivity implements ProjectActivity {

        /**
         * 执行项目统计启动活动
         * <p> 根据项目设置判断是否启用统计功能, 若启用则启动统计服务
         * <p> 该方法作为项目启动时的活动处理器, 用于初始化统计服务
         *
         * @param project      当前项目对象, 不能为空
         * @param continuation 续航回调对象, 用于异步操作完成后的回调, 不能为空
         * @return 返回 Unit.INSTANCE, 表示操作已完成, 可能为 null
         */
        @Override
        public @Nullable Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
            // 获取当前项目的名称，用于统计
            StatisticsSettings settings = StatisticsSettings.getInstance();
            if (settings.isEnableStatistics()) {
                startService();
            }
            return Unit.INSTANCE;
        }
    }

    /** 应用关闭监听 */
    public static class ApplicationShutdownListener implements AppLifecycleListener {
        /**
         * 应用关闭时的回调处理
         * <p> 当应用即将关闭时, 执行服务停止操作, 确保资源正确释放
         *
         */
        @Override
        public void appClosing() {
            stopService();
        }
    }
}
