package dev.dong4j.zeka.stack.idea.plugin.common.ai;

import org.jetbrains.annotations.Nullable;

import java.net.HttpURLConnection;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 流取消令牌类
 * <p> 用于在流操作中控制取消状态, 支持绑定 HTTP 连接并在取消时自动断开连接.
 * <p> 该类通过原子操作确保线程安全, 适用于需要在异步或长时间运行的流操作中提供取消机制的场景.
 * <p> 使用示例:
 * <pre>{@code
 * StreamCancellationToken token = new StreamCancellationToken();
 * token.bindConnection(httpConnection);
 * // ... 执行流操作 ...
 * token.cancel(); // 取消操作并断开连接
 * }</pre>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.08
 * @since 1.0.0
 */
public final class StreamCancellationToken {
    /** 流式请求是否已被取消的标志位, 用于控制 SSE 连接的断开行为 */
    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    /** 绑定的 HTTP 连接引用, 用于在取消时断开连接 */
    private final AtomicReference<HttpURLConnection> connectionRef = new AtomicReference<>();

    /**
     * 检查是否已取消流式请求
     * <p> 用于判断当前流式请求是否已被外部请求取消, 若已取消则返回 true, 否则返回 false
     *
     * @return true 表示请求已被取消,false 表示请求尚未取消
     */
    public boolean isCancelled() {
        return cancelled.get();
    }

    /**
     * 取消流式请求
     * <p> 当调用此方法时, 若请求尚未被取消, 则标记为已取消状态, 并断开关联的 HTTP 连接以终止 SSE 输出.
     * <p> 若连接已存在且未被取消, 则调用 {@link HttpURLConnection#disconnect()} 断开连接.
     *
     */
    public void cancel() {
        if (cancelled.compareAndSet(false, true)) {
            HttpURLConnection connection = connectionRef.get();
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * 绑定 HTTP 连接对象
     * <p> 将指定的 HttpURLConnection 对象绑定到当前取消令牌, 以便在令牌被取消时自动断开连接.
     * 如果连接对象为 null, 则直接返回, 不进行任何操作.
     * 如果在绑定时令牌已标记为取消状态, 则立即断开连接.
     *
     * @param connection 要绑定的 HttpURLConnection 对象, 可以为 null
     */
    public void bindConnection(@Nullable HttpURLConnection connection) {
        if (connection == null) {
            return;
        }
        connectionRef.set(connection);
        if (cancelled.get()) {
            connection.disconnect();
        }
    }
}
