package dev.dong4j.zeka.stack.idea.plugin.common.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * 验证 JCEF 探测在 classloader 缺少 JCEF 时也不会抛出链路错误.
 */
class JcefSupportTest {

    @Test
    void isSupportedMustNotThrow() {
        assertThatCode(JcefSupport::isSupported).doesNotThrowAnyException();
    }
}
