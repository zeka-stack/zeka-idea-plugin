package dev.dong4j.zeka.stack.idea.plugin.task.parallel;

/**
 * 服务商状态枚举
 * <p>
 * 定义服务商的不同状态，用于管理服务商的可用性和线程生命周期。
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email mailto:zeka.stack@gmail.com
 * @date 2025.12.01
 * @since 1.0.0
 */
public enum ProviderStatus {
    /** 可用状态，可以正常处理任务 */
    AVAILABLE,

    /** 限流状态（429 错误），服务商不可用，需要销毁所有线程 */
    RATE_LIMITED,

    /** 错误状态，服务商出现其他错误 */
    ERROR
}

