package dev.dong4j.zeka.stack.idea.plugin.kit;

import org.junit.jupiter.api.Test;

import lombok.extern.slf4j.Slf4j;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pangu 测试类
 * <p> 用于验证 Pangu 类中 spacingText 方法的功能, 该方法用于对输入文本进行格式化或间距处理, 适用于工作周报等场景的文本美化.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.07
 * @since 1.0.0
 */
@Slf4j
class PanguTest {

    @Test
    void test() throws Exception {
        Pangu pangu = new Pangu();
        String input = "2026年01月05日-2026年01月11日 工作周报";
        String expected = "2026 年 01 月 05 日 - 2026 年 01 月 11 日 工作周报";
        String actual = pangu.spacingText(input);
        log.info("{}", actual);
        assertThat(actual).isEqualTo(expected);

    }

    @Test
    void hyphenWithAscii() {
        Pangu pangu = new Pangu();
        String input = "Version1-2-Test";
        String expected = "Version1-2-Test";
        assertThat(pangu.spacingText(input)).isEqualTo(expected);
    }

    @Test
    void hyphenAlreadySpaced() {
        Pangu pangu = new Pangu();
        String input = "2026 年 01 月 05 日 - 2026 年 01 月 11 日";
        assertThat(pangu.spacingText(input)).isEqualTo(input);
    }

    @Test
    void hyphenBetweenDigitsAndCjk() {
        Pangu pangu = new Pangu();
        String input = "第1周(2026-01-05)-第2周(2026年01月11日)";
        String expected = "第 1 周 (2026-01-05)- 第 2 周 (2026 年 01 月 11 日)";
        assertThat(pangu.spacingText(input)).isEqualTo(expected);
    }

}
