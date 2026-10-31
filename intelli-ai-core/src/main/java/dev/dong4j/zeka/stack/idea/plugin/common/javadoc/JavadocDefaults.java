package dev.dong4j.zeka.stack.idea.plugin.common.javadoc;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Javadoc 默认模板入口
 * <p>
 * 集中提供 Javadoc 生成各场景的默认 prompt 模板, 资源文件位于 {@code classpath:prompts/},
 * 由插件宿主(SettingsState 默认值)与 MCP Server 宿主共用, 保证两个宿主的默认生成行为一致.
 * <p>
 * 模板占位符与插件版保持一致(如 {@code ${language}} / {@code $NAME$} 等),
 * 由各宿主的模板填充逻辑负责替换. 资源在首次读取后缓存, 后续调用零 I/O.
 *
 * @author dong4j
 * @version 1.0.0
 * @since 1.0.0
 */
public final class JavadocDefaults {

    /** 资源路径 -> 已缓存内容 */
    private static final Map<String, String> CACHE = new ConcurrentHashMap<>();

    /**
     * 私有构造函数, 防止实例化工具类
     */
    private JavadocDefaults() {
    }

    /**
     * 获取默认的类注释生成模板
     *
     * @return 默认类注释模板
     */
    public static @NotNull String getDefaultClassPromptTemplate() {
        return load("prompts/javadoc-class-prompt.txt");
    }

    /**
     * 获取默认的方法注释生成模板
     *
     * @return 默认方法注释模板
     */
    public static @NotNull String getDefaultMethodPromptTemplate() {
        return load("prompts/javadoc-method-prompt.txt");
    }

    /**
     * 获取默认的字段注释生成模板
     *
     * @return 默认字段注释模板
     */
    public static @NotNull String getDefaultFieldPromptTemplate() {
        return load("prompts/javadoc-field-prompt.txt");
    }

    /**
     * 获取默认的测试方法注释生成模板
     *
     * @return 默认测试方法注释模板
     */
    public static @NotNull String getDefaultTestPromptTemplate() {
        return load("prompts/javadoc-test-prompt.txt");
    }

    /**
     * 获取默认的系统提示词模板
     *
     * @return 默认系统提示词
     */
    public static @NotNull String getDefaultSystemPromptTemplate() {
        return load("prompts/javadoc-system-prompt.txt");
    }

    /**
     * 获取默认的修复 Javadoc 提示词模板
     *
     * @return 默认修复提示词模板
     */
    public static @NotNull String getDefaultFixJavadocPromptTemplate() {
        return load("prompts/javadoc-fix-prompt.txt");
    }

    /**
     * 读取 classpath 资源(带缓存)
     *
     * @param path 资源路径
     * @return 资源文本内容
     * @throws IllegalStateException 资源缺失时抛出(属于构建期错误, 不应静默降级为空模板)
     */
    private static @NotNull String load(@NotNull String path) {
        return CACHE.computeIfAbsent(path, p -> {
            try (InputStream is = JavadocDefaults.class.getClassLoader().getResourceAsStream(p)) {
                if (is == null) {
                    throw new IllegalStateException("缺少 core 模板资源: " + p);
                }
                try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    StringBuilder sb = new StringBuilder();
                    char[] buffer = new char[8192];
                    int n;
                    while ((n = reader.read(buffer)) != -1) {
                        sb.append(buffer, 0, n);
                    }
                    return sb.toString();
                }
            } catch (IOException e) {
                throw new IllegalStateException("读取 core 模板资源失败: " + p, e);
            }
        });
    }
}
