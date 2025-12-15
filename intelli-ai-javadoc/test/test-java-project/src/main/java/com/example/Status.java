package com.example;

/**
 * 状态枚举
 * <p>
 * 用于表示对象的生命周期状态, 包括待处理, 激活, 非激活和已删除等四种状态
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email "mailto:zeka.stack@gmail.com"
 * @date 2025.12.15
 * @since 1.0.0
 */
public enum Status {
    /** 用于存储待处理的任务数据 */
    PENDING,
    /** 当前活动状态 */
    ACTIVE,
    /** INACTIVE 状态标识 */
    INACTIVE,
    /** 已删除字段 */
    DELETED
}

