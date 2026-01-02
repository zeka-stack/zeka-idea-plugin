package dev.dong4j.zeka.stack.idea.plugin.changelog.ai;

import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIStreamResponseListener;
import dev.dong4j.zeka.stack.idea.plugin.common.util.AIConsoleLoggerUtil;

/**
 * ChangelogAIStreamResponseListener 类
 * <p>实现了 AIStreamResponseListener 接口, 用于处理变更日志的流式响应.
 * <p>该类的主要职责是将接收到的流式响应数据追加到缓冲区中, 并在完成或发生错误时进行相应的处理.
 * <p>具体功能包括:
 * <ul>
 * <li>接收并处理每个数据块(chunk), 将其追加到缓冲区.</li>
 * <li>在接收到完整文本时, 调用计数信号量 (CountDownLatch) 以通知处理完成.</li>
 * <li>在发生错误时, 记录错误信息并设置异常引用(AtomicReference).</li>
 * </ul>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.02
 * @since 1.0.0
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
    public void onComplete(@NotNull String fullText) {
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
