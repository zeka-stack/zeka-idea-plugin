package dev.dong4j.zeka.stack.idea.plugin.kit;

import org.junit.jupiter.api.Test;

import lombok.extern.slf4j.Slf4j;

/**
 * Notification Util Test
 *
 * @author dong4j
 * @version hello.world
 * @date 2026-01-02 15:23:08
 * @since hello.world
 */
@Slf4j
class NotificationUtilTest {

    @Test
    void test() throws Exception {
        log.info("{}", NotificationUtil.cleanPluginName("IntelliAI Stack"));
        log.info("{}", NotificationUtil.cleanPluginName("IntelliAI Stack"));
    }

}
