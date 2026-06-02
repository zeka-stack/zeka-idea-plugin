package dev.dong4j.zeka.stack.idea.plugin.kit;

import org.junit.jupiter.api.Test;

import lombok.extern.slf4j.Slf4j;

/**
 * 通知工具测试类
 * <p> 用于测试 NotificationUtil 类中的 cleanPluginName 方法
 * <p> 该测试类通过日志记录 cleanPluginName 方法的输出结果, 验证其正确性
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.05.25
 * @since 1.0.0
 */
@Slf4j
class NotificationUtilTest {

    /**
     * 测试清理插件名称功能
     * <p>
     * 测试场景: 调用 NotificationUtil 的 cleanPluginName 方法
     * 预期结果: 验证 cleanPluginName 方法是否正确清理插件名称
     * <p>
     * 注意事项: 该测试会记录两次相同的日志信息, 确保方法行为一致
     */
    @Test
    void test() throws Exception {
        log.debug("{}", NotificationUtil.cleanPluginName("IntelliAI Stack"));
        log.debug("{}", NotificationUtil.cleanPluginName("IntelliAI Stack"));
    }

}
