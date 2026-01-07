package dev.dong4j.zeka.stack.idea.plugin.kit;

import org.junit.jupiter.api.Test;

import lombok.extern.slf4j.Slf4j;

/**
 *
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.07 20:52
 * @since x.x.x
 */
@Slf4j
class PanguTest {

    @Test
    void test() throws Exception {
        Pangu pangu = new Pangu();
        log.info("{}", pangu.spacingText("2026年01月05日-2026年01月11日 工作周报"));

    }

}
