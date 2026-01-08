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
        log.debug("{}", actual);
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


    @Test
    void hyphenBetweenDigitsAndCjk1() {
        Pangu pangu = new Pangu();
        String input = """
            xxxx
            - 123213
            - 你好
            - hello
            """;
        String expected = """
            xxxx
            - 123213
            - 你好
            - hello
            """;
        assertThat(pangu.spacingText(input)).isEqualTo(expected);
    }

    @Test
    void hyphenBetweenDigitsAndCjk2() {
        Pangu pangu = new Pangu();
        String input = """
            feat(ai-provider): 添加 AI 请求的阻塞与流式执行器

            - 新增 BlockingRequestExecutor，支持发送阻塞式 HTTP 请求至 AI 服务
            - 支持基于配置自动校验 API 密钥和处理多种HTTP状态异常
            - 实现请求与响应日志记录与监听器回调机制
            - 解析 AI JSON 响应，提取内容和使用的 token 信息，支持验证模式响应解析
            - 新增 StreamRequestExecutor，支持流式请求发送并处理 SSE 流响应数据
            - 支持多种 AI 服务提供商的流解析器（OpenAI, Dashscope, Ollama, MiniMax）
            - 流式响应中区分思考内容和正文内容，支持动态选择对应解析策略
            - 完善异常处理，网络错误和响应错误通过监听器回调反馈
            - 提供详细注释说明类与方法用途，便于后续维护和扩展
            """;
        String expected = """
            feat(ai-provider): 添加 AI 请求的阻塞与流式执行器

            - 新增 BlockingRequestExecutor，支持发送阻塞式 HTTP 请求至 AI 服务
            - 支持基于配置自动校验 API 密钥和处理多种 HTTP 状态异常
            - 实现请求与响应日志记录与监听器回调机制
            - 解析 AI JSON 响应，提取内容和使用的 token 信息，支持验证模式响应解析
            - 新增 StreamRequestExecutor，支持流式请求发送并处理 SSE 流响应数据
            - 支持多种 AI 服务提供商的流解析器（OpenAI, Dashscope, Ollama, MiniMax）
            - 流式响应中区分思考内容和正文内容，支持动态选择对应解析策略
            - 完善异常处理，网络错误和响应错误通过监听器回调反馈
            - 提供详细注释说明类与方法用途，便于后续维护和扩展
            """;
        assertThat(pangu.spacingText(input)).isEqualTo(expected);
    }


}
