package dev.dong4j.zeka.stack.idea.javadoc.util;

import com.intellij.openapi.vfs.VirtualFile;

import dev.dong4j.zeka.stack.idea.javadoc.PluginContents;
import dev.dong4j.zeka.stack.idea.javadoc.settings.SettingsState;

/**
 * 插件工具类
 * <p> 用于判断指定虚拟文件是否被当前插件支持, 主要根据文件扩展名进行判断.
 * <p> 支持的文件类型包括:
 * <ul>
 *   <li>Java 文件 (扩展名为 .java)</li>
 *   <li>Kotlin 文件 (扩展名为 .kt 或 .kts), 但需在设置中启用 Kotlin 语言支持 </li>
 * </ul>
 * <p> 使用示例:
 * <pre>{@code
 * VirtualFile file = ...; // 获取虚拟文件实例
 * boolean isSupported = PluginUtil.isSupportedFile(file);
 * }</pre>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.09
 * @since 1.0.0
 */
public class PluginUtil {

    /**
     * 判断给定文件是否为 Java 或 Kotlin 文件
     * <p>
     * 通过检查文件的扩展名是否为 "java" 或 "kt"(不区分大小写) 来判断文件类型
     *
     * @param file 要判断的文件对象
     * @return 如果文件是 Java 或 Kotlin 文件, 返回 true; 否则返回 false
     */
    public static boolean isSupportedFile(VirtualFile file) {
        String extension = file.getExtension();
        if (extension == null) {
            return false;
        }
        String extLower = extension.toLowerCase();

        // 检查是否为 Java 文件
        if (PluginContents.JAVA.equals(extLower)) {
            return true;
        }

        // 检查是否为 Kotlin 文件
        if (PluginContents.KOTLIN_EXTENSION.equals(extLower)) {
            SettingsState settings = SettingsState.getInstance();
            return settings.isLanguageSupported(PluginContents.KOTLIN);
        }

        return false;
    }

}
