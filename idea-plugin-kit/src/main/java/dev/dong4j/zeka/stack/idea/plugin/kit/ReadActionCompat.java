package dev.dong4j.zeka.stack.idea.plugin.kit;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.util.ThrowableComputable;

import org.jetbrains.annotations.NotNull;

/**
 * 可取消读操作兼容封装
 * <p>
 * 2026.1 起 {@code ReadAction.compute()} / {@code runReadAction()} 被标记弃用:
 * 后台线程里不可取消的读锁会堵住写操作并造成 UI 卡顿.
 * 本类在已有读权限或 EDT 上保持同步读; 在后台线程改走
 * {@code ReadAction.nonBlocking().executeSynchronously()}, 这样 2024.2 仍能编译, 2026.2 运行时也不再占用不可取消读锁.
 * </p>
 * <p>
 * 使用 {@link ThrowableComputable} 而不是 {@code Computable}, 以兼容原先 {@code ReadAction.compute} 可向外抛出受检异常的重载.
 * </p>
 *
 * @author dong4j
 * @version 2026.2.1001
 * @since 2026.2.1001
 */
public final class ReadActionCompat {

    private ReadActionCompat() {
        // 工具类, 禁止实例化
    }

    /**
     * 在读操作中计算结果
     *
     * @param computable 读操作逻辑
     * @param <T>        返回值类型
     * @param <E>        受检异常类型
     * @return 计算结果
     * @throws E 读操作抛出的受检异常
     */
    public static <T, E extends Throwable> T compute(@NotNull ThrowableComputable<T, E> computable) throws E {
        Application application = ApplicationManager.getApplication();
        if (application.isReadAccessAllowed()) {
            return computable.compute();
        }
        if (application.isDispatchThread()) {
            return application.runReadAction(computable);
        }
        try {
            return ReadAction.nonBlocking(() -> {
                try {
                    return computable.compute();
                } catch (RuntimeException | Error e) {
                    throw e;
                } catch (Throwable e) {
                    throw new WrappedException(e);
                }
            }).executeSynchronously();
        } catch (WrappedException e) {
            @SuppressWarnings("unchecked")
            E cause = (E) e.getCause();
            throw cause;
        }
    }

    /**
     * 在读操作中执行无返回值逻辑
     *
     * @param runnable 读操作逻辑
     */
    public static void run(@NotNull Runnable runnable) {
        compute(() -> {
            runnable.run();
            return null;
        });
    }

    /**
     * 把后台 nonBlocking 不允许声明的受检异常带回调用方
     */
    private static final class WrappedException extends RuntimeException {
        private WrappedException(@NotNull Throwable cause) {
            super(cause);
        }
    }
}
