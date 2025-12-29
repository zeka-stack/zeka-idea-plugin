package dev.dong4j.zeka.stack.idea.plugin.common.update;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderSettings;
import dev.dong4j.zeka.stack.idea.plugin.common.util.ApplicationUtil;
import kotlin.Unit;
import kotlin.coroutines.Continuation;

/**
 * 更新启动活动类
 * 该类负责在项目启动时进行一次性的更新检查, 并定期重复检查以确保插件的最新性.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2025.12.27
 * @since 1.0.0
 */
public class UpdateStartupActivity implements ProjectActivity {
    /**
     * 记录插件更新检查活动的日志
     *
     * @see Logger
     */
    private static final Logger LOG = Logger.getInstance(UpdateStartupActivity.class);
    /**
     * 表示插件更新检查是否已经运行过的标志
     * <p>
     * 使用 AtomicBoolean 来确保线程安全, 避免在多线程环境中出现竞态条件.
     */
    private final AtomicBoolean hasRun = new AtomicBoolean(false);

    /**
     * 在 IDE 启动时执行插件更新检查
     * <p> 该方法在 IDE 启动时被调用, 用于检查插件更新. 如果自动更新检查功能被禁用, 则跳过此步骤.
     * 如果尚未进行过更新检查且当前不是单元测试模式, 则将更新检查任务加入队列, 并安排重复的更新检查.
     *
     * @param project      当前项目对象
     * @param continuation 异步操作的延续对象
     * @return 如果是默认项目或自动更新检查已禁用, 返回 Unit.INSTANCE
     * @since 1.0.0
     */
    @Override
    @RequiresBackgroundThread
    public @Nullable Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        if (project.isDefault()) {
            return Unit.INSTANCE;
        }
        // 检查是否启用自动更新
        AIProviderSettings settings = AIProviderSettings.getInstance();
        if (!settings.lastUpdateCheck) {
            LOG.info("自动更新检查已禁用，跳过启动时的更新检查");
            return Unit.INSTANCE;
        }

        // 只在第一次运行时检查更新
        if (hasRun.compareAndSet(false, true) && !ApplicationManager.getApplication().isUnitTestMode()) {
            queueUpdateCheck(project);
        }

        // 每 24 小时检查一次更新
        scheduleRepeatedUpdateCheck();

        return Unit.INSTANCE;
    }

    /**
     * 安排重复的更新检查
     * <p> 使用定时任务每 24 小时检查一次插件更新
     * <p>
     * 该方法会在后台线程中执行, 确保在项目可用时进行更新检查.
     */
    private void scheduleRepeatedUpdateCheck() {
        AppExecutorUtil.getAppScheduledExecutorService().scheduleWithFixedDelay(() -> {
            Project project = ApplicationUtil.findCurrentProject();
            if (project != null) {
                queueUpdateCheck(project);
            }
        }, 24L, 24L, TimeUnit.HOURS);
    }

    /**
     * 将更新检查任务加入队列
     * <p> 创建一个 PluginUpdater 的 CheckUpdatesTask 并将其加入队列进行执行
     *
     * @param project 项目对象
     */
    private void queueUpdateCheck(@NotNull Project project) {
        new PluginUpdater.CheckUpdatesTask(project).queue();
    }
}

