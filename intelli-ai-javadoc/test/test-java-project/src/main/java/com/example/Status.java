package com.example;

/**
 * 状态枚举
 * <p> 用于表示资源或操作的当前状态, 包含待处理, 激活, 非激活和已删除四种状态
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email "mailto:zeka.stack@gmail.com"
 * @date 2025.12.15
 * @since 1.0.0
 */
public enum Status {
    /** 状态枚举值, 表示资源处于待处理状态 */
    PENDING,
    /** 状态码, 表示资源处于活动状态 */
    ACTIVE,
    /** 状态为非活跃, 表示该条目当前未启用或不可用 */
    INACTIVE,
    /** 状态表示删除 */
    DELETED
}

