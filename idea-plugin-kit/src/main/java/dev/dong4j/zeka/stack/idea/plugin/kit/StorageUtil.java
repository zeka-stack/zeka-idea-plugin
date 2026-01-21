package dev.dong4j.zeka.stack.idea.plugin.kit;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 插件存储路径工具
 * <p>为插件提供统一的本地缓存目录结构: ~/.zeka-stack/plugin/{pluginName}</p>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.21 18:56
 * @since 2025.3.1200
 */
public final class StorageUtil {

    /** 插件根目录标识, 用于构建插件存储路径的基准目录名 */
    private static final String ROOT_DIR = ".zeka-stack";
    /** 插件目录标识, 用于构建插件存储路径的子目录名 */
    private static final String PLUGIN_DIR = "plugin";

    /**
     * 私有构造函数, 防止外部实例化
     * <p> 该类为工具类, 所有方法均为静态, 禁止通过实例化方式调用 </p>
     */
    private StorageUtil() {
    }

    /**
     * 获取插件存储目录
     *
     * @param pluginName 插件名称/标识
     * @return 插件存储目录路径
     */
    public static Path getPluginStorageDir(String pluginName) {
        String safeName = sanitize(pluginName);
        Path base = Path.of(System.getProperty("user.home"), ROOT_DIR, PLUGIN_DIR, safeName);
        try {
            Files.createDirectories(base);
        } catch (IOException ignored) {
            // ignore
        }
        return base;
    }

    /**
     * 获取插件目录下的文件路径
     *
     * @param pluginName 插件名称/标识
     * @param fileName   文件名
     * @return 文件路径
     */
    public static Path resolve(String pluginName, String fileName) {
        return getPluginStorageDir(pluginName).resolve(fileName);
    }

    /**
     * 对输入字符串进行安全化处理, 用于生成插件存储路径中的目录或文件名
     * <p> 将空值或空字符串替换为 "unknown", 并移除路径分隔符, 多余空格及 "..", 替换为连字符 "-"</p>
     *
     * @param value 待处理的原始字符串
     * @return 安全化后的字符串, 若输入为 null 或空字符串则返回 "unknown"
     */
    private static String sanitize(String value) {
        if (value == null) {
            return "unknown";
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return "unknown";
        }
        return trimmed
            .replace("/", "-")
            .replace("\\", "-")
            .replace("..", "-")
            .replace(" ", "-");
    }
}
