package dev.dong4j.zeka.stack.idea.plugin.changelog.ai;

import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIStreamResponseListener;
import dev.dong4j.zeka.stack.idea.plugin.common.util.AIConsoleLoggerUtil;

/**
 * Changelog AI Stream Response Listener
 *
 * @author dong4j
 * @version hello.world
 * @date 2025-12-30 18:45:32
 * @since hello.world
 */
public class ChangelogAIStreamResponseListener implements AIStreamResponseListener {

    /**
     * 当前操作所在的项目实例
     *
     * @see Project
     */
    private final Project project;
    /**
     * 存储接收到的 AI 流式响应数据的缓冲区
     *
     * @see StringBuilder
     */
    private final StringBuilder buffer;
    /**
     * 用于控制线程同步的计数器
     * <p> 当计数值到达零且所有线程都已在此处等待时, 释放所有等待的线程.
     *
     * @see CountDownLatch
     */
    private final CountDownLatch latch;
    /**
     * 异常引用, 用于存储在处理流式响应过程中可能出现的异常信息.
     *
     * @see AtomicReference
     */
    private final AtomicReference<Exception> errorRef;

    /**
     * 构造函数, 初始化 ChangelogAIStreamResponseListener 对象
     * <p> 设置项目, 缓冲区, 计数器和异常引用
     *
     * @param project  项目对象
     * @param buffer   用于存储接收到的数据的字符串构建器
     * @param latch    用于同步的计数器
     * @param errorRef 用于存储错误信息的原子引用
     */
    public ChangelogAIStreamResponseListener(@NotNull Project project,
                                             @NotNull StringBuilder buffer,
                                             @NotNull CountDownLatch latch,
                                             @NotNull AtomicReference<Exception> errorRef) {
        this.project = project;
        this.buffer = buffer;
        this.latch = latch;
        this.errorRef = errorRef;
    }

    /**
     * 调用父类的 onStart 方法
     * <p> 此方法在 AI 流响应监听器启动时被调用, 主要用于初始化或执行一些启动前的操作.
     * <p>
     * 该方法没有具体的业务逻辑实现, 仅调用了父类的 onStart 方法.
     */
    @Override
    public void onStart() {
        AIStreamResponseListener.super.onStart();
    }

    /**
     * 处理接收到的流式响应数据块
     * <p> 将非空的数据块追加到缓冲区中
     *
     * @param chunk 接收到的数据块
     */
    @Override
    public void onChunk(@NotNull String chunk) {
        if (chunk.isEmpty()) {
            return;
        }
        buffer.append(chunk);
    }

    /**
     * 完成处理时调用的方法
     * <p> 该方法用于在处理完成后调用, 减少计数器 latch 的计数, 表示当前操作已完成.
     * <p>
     * 该方法没有参数, 也没有返回值, 且不会抛出异常.
     */
    @Override
    public void onComplete() {
        latch.countDown();
    }

    /**
     * 处理流式生成中的错误
     * <p> 当流式生成过程中出现错误时调用此方法. 记录错误信息, 并根据是否有异常设置错误引用,
     * 最后调用计数器减一以通知完成状态.
     *
     * @param error     错误信息
     * @param exception 可能关联的异常对象
     */
    @Override
    public void onError(@NotNull String error, @Nullable Throwable exception) {
        AIConsoleLoggerUtil.printError(project, "流式生成错误: " + error);
        if (exception != null) {
            errorRef.set(new Exception(error, exception));
        } else {
            errorRef.set(new Exception(error));
        }
        latch.countDown();
    }
}
