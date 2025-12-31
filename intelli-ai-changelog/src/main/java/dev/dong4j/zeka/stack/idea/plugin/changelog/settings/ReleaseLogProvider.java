package dev.dong4j.zeka.stack.idea.plugin.changelog.settings;

import org.jetbrains.annotations.NotNull;

/**
 * Release Log 生成方式枚举
 * <p>
 * 用于表示 Release Log 的生成方式，支持多种生成器。
 * 目前支持 AI 和 git-cliff 两种方式，后续可以扩展更多生成器。
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email mailto:zeka.stack@gmail.com
 * @date 2025.12.31
 * @since 1.0.0
 */
public enum ReleaseLogProvider {
    /**
     * 使用 AI 生成 Release Log
     * <p>
     * 通过 AI 服务（如 Qwen、Ollama、OpenAI 等）生成 Release Log。
     * 支持自定义提示词模板，生成更灵活、更符合项目风格的变更日志。
     */
    AI("ai"),

    /**
     * 使用 git-cliff 生成 Release Log
     * <p>
     * 使用 git-cliff 工具基于 Git 提交记录生成 Release Log。
     * 支持基于 Conventional Commits 规范的自动分类和格式化。
     */
    GIT_CLIFF("git-cliff");

    /**
     * 枚举值对应的字符串标识
     * <p>
     * 用于序列化和反序列化，保持与旧版本配置的兼容性。
     */
    private final String value;

    /**
     * 构造函数
     *
     * @param value 字符串标识
     */
    ReleaseLogProvider(@NotNull String value) {
        this.value = value;
    }

    /**
     * 获取字符串标识
     *
     * @return 字符串标识
     */
    @NotNull
    public String getValue() {
        return value;
    }

    /**
     * 根据字符串标识获取枚举值
     * <p>
     * 用于从配置文件中读取值时进行转换。
     * 如果无法识别，默认返回 AI。
     *
     * @param value 字符串标识
     * @return 对应的枚举值，如果无法识别则返回 AI
     */
    @NotNull
    public static ReleaseLogProvider fromValue(@NotNull String value) {
        for (ReleaseLogProvider provider : values()) {
            if (provider.value.equalsIgnoreCase(value)) {
                return provider;
            }
        }
        // 默认返回 AI，保持向后兼容
        return AI;
    }
}

