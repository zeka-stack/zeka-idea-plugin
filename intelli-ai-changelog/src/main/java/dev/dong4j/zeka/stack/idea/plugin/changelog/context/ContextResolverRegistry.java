package dev.dong4j.zeka.stack.idea.plugin.changelog.context;

import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ServiceLoader;

/** 语言上下文解析器注册表, 用于管理并获取不同语言的上下文解析器 */
public final class ContextResolverRegistry {
    /** Java 插件的标识符, 用于检查插件是否已安装 */
    private static final String JAVA_PLUGIN_ID = "com.intellij.java";
    /**
     * Java 语言上下文解析器的类名
     * <p>
     * 该字段用于存储 Java 语言上下文解析器的类名, 以便在需要时动态加载解析器.
     *
     * @see JavaPsiContextResolver
     */
    private static final String JAVA_RESOLVER_CLASS =
        "dev.dong4j.zeka.stack.idea.plugin.changelog.context.JavaPsiContextResolver";
    /**
     * 上下文解析器列表
     * <p> 用于存储所有已注册的语言上下文解析器, 采用 volatile 保证多线程可见性 </p>
     * <p> 该列表在首次访问时初始化, 并通过 Collections.unmodifiableList 设置为不可修改 </p>
     */
    private static volatile List<LanguageContextResolver> resolvers;

    /**
     * 私有构造函数, 防止外部实例化
     * <p> 该类为工具类, 提供上下文解析器注册和查找功能, 不允许外部创建实例
     */
    private ContextResolverRegistry() {
        // 工具类，禁止实例化
    }

    /**
     * 获取语言上下文解析器列表
     * <p> 返回所有已注册的语言上下文解析器, 如果缓存中已存在解析器列表, 则直接返回缓存内容, 否则创建新的解析器列表并缓存
     *
     * @return 不可变的语言上下文解析器列表
     */
    @NotNull
    private static List<LanguageContextResolver> getResolvers() {
        List<LanguageContextResolver> cached = resolvers;
        if (cached != null) {
            return cached;
        }
        List<LanguageContextResolver> list = new ArrayList<>();
        list.add(new XmlContextResolver());
        LanguageContextResolver javaResolver = loadJavaResolverIfAvailable();
        if (javaResolver != null) {
            list.add(javaResolver);
        }
        loadResolversFromSpi(list);
        resolvers = Collections.unmodifiableList(list);
        return resolvers;
    }

    /**
     * 解析指定文件的上下文信息
     * <p> 遍历所有已注册的语言上下文解析器, 查找第一个支持该文件类型的解析器, 并尝试获取上下文内容.
     * 如果解析结果非空且非空白字符串, 则返回该上下文; 否则继续查找下一个解析器. 若所有解析器均未返回有效内容, 则返回 null.
     *
     * @param file          要解析上下文的文件对象, 不能为 null
     * @param preferredLine 优先使用的行号, 用于上下文提取
     * @param fallbackLine  当首选行不可用时使用的备用行号
     * @return 上下文字符串, 如果无法解析或结果为空白则返回 null
     */
    @Nullable
    public static String resolveContext(@NotNull VirtualFile file,
                                        int preferredLine,
                                        int fallbackLine) {
        for (LanguageContextResolver resolver : getResolvers()) {
            if (resolver.supports(file)) {
                String context = resolver.resolveContext(file, preferredLine, fallbackLine);
                if (context != null && !context.trim().isEmpty()) {
                    return context;
                }
            }
        }
        return null;
    }

    /**
     * 解析项目中的主要符号名称
     * <p> 遍历注册的语言上下文解析器, 找到支持给定文件的解析器, 并调用其方法获取主要符号名称
     * <p> 如果找到非空的主要符号名称, 则返回该名称; 否则返回 null
     *
     * @param project 项目对象
     * @param file    虚拟文件对象
     * @return 主要符号名称, 如果未找到或为空则返回 null
     */
    @Nullable
    public static String resolvePrimarySymbolName(@NotNull Project project, @NotNull VirtualFile file) {
        for (LanguageContextResolver resolver : getResolvers()) {
            if (resolver.supports(file)) {
                String name = resolver.resolvePrimarySymbolName(project, file);
                if (name != null && !name.trim().isEmpty()) {
                    return name;
                }
            }
        }
        return null;
    }

    /**
     * 解析 PSI 语义摘要
     * <p> 遍历注册的解析器，尝试生成结构化语义摘要，优先返回第一个有效结果。
     *
     * @param project       项目
     * @param file          文件
     * @param beforeContent 变更前内容
     * @param afterContent  变更后内容
     * @param fragments     diff 行片段
     * @return 语义摘要文本，若无法解析则返回 null
     */
    @Nullable
    public static String resolveSemanticSummary(@NotNull Project project,
                                                @NotNull VirtualFile file,
                                                @NotNull String beforeContent,
                                                @NotNull String afterContent,
                                                @NotNull List<com.intellij.diff.fragments.LineFragment> fragments) {
        for (LanguageContextResolver resolver : getResolvers()) {
            if (resolver.supports(file)) {
                String summary = resolver.resolveSemanticSummary(project, file, beforeContent, afterContent, fragments);
                if (summary != null && !summary.trim().isEmpty()) {
                    return summary;
                }
            }
        }
        return null;
    }

    /**
     * 加载可用的 Java 上下文解析器
     * <p> 检查是否安装了 Java 插件, 如果已安装则尝试加载并返回 Java 上下文解析器实例;
     * 如果未安装或加载失败, 则返回 null
     *
     * @return Java 上下文解析器实例, 如果未安装 Java 插件或加载失败则返回 null
     * @since 1.0
     */
    @Nullable
    private static LanguageContextResolver loadJavaResolverIfAvailable() {
        if (!PluginManagerCore.isPluginInstalled(PluginId.getId(JAVA_PLUGIN_ID))) {
            return null;
        }
        try {
            Class<?> resolverClass = Class.forName(JAVA_RESOLVER_CLASS);
            Object instance = resolverClass.getDeclaredConstructor().newInstance();
            if (instance instanceof LanguageContextResolver resolver) {
                return resolver;
            }
        } catch (Throwable ignored) {
            return null;
        }
        return null;
    }

    /**
     * 通过 SPI 机制加载可用的上下文解析器
     * <p> 允许外部模块通过 ServiceLoader 扩展语言解析能力。
     */
    private static void loadResolversFromSpi(@NotNull List<LanguageContextResolver> target) {
        try {
            ServiceLoader<LanguageContextResolver> loader = ServiceLoader.load(LanguageContextResolver.class,
                                                                               ContextResolverRegistry.class.getClassLoader());
            for (LanguageContextResolver resolver : loader) {
                if (!containsResolver(target, resolver)) {
                    target.add(resolver);
                }
            }
        } catch (Throwable ignored) {
            // SPI 加载失败时保持内置解析器可用
        }
    }

    /**
     * 判断是否已存在同类解析器，避免重复注册
     */
    private static boolean containsResolver(@NotNull List<LanguageContextResolver> target,
                                            @NotNull LanguageContextResolver candidate) {
        for (LanguageContextResolver resolver : target) {
            if (resolver.getClass().getName().equals(candidate.getClass().getName())) {
                return true;
            }
        }
        return false;
    }
}
