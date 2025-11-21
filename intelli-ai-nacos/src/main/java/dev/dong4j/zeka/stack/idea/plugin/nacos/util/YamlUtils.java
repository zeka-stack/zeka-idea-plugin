package dev.dong4j.zeka.stack.idea.plugin.nacos.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.Yaml;

import java.io.StringReader;
import java.util.Map;

/**
 * YAML 工具类
 * 提供 YAML 解析和处理功能
 *
 * @author dong4j
 * @since 1.0.0
 */
public class YamlUtils {

    private static final Yaml YAML = new Yaml();

    /**
     * 解析 YAML 字符串
     *
     * @param yamlContent YAML 内容
     * @return 解析后的 Map 对象
     */
    @Nullable
    public static Map<String, Object> parseYaml(@NotNull String yamlContent) {
        try {
            return YAML.load(new StringReader(yamlContent));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 将 Map 对象转换为 YAML 字符串
     *
     * @param data 数据 Map
     * @return YAML 字符串
     */
    @NotNull
    public static String toYaml(@NotNull Map<String, Object> data) {
        return YAML.dumpAsMap(data);
    }

    /**
     * 从 YAML 内容中提取应用名称
     *
     * @param yamlContent YAML 内容
     * @return 应用名称，如果未找到则返回 null
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public static String extractAppName(@NotNull String yamlContent) {
        Map<String, Object> yamlData = parseYaml(yamlContent);
        if (yamlData != null) {
            // 查找 spring.application.name
            Object springObj = yamlData.get("spring");
            if (springObj instanceof Map) {
                Map<String, Object> springMap = (Map<String, Object>) springObj;
                Object applicationObj = springMap.get("application");
                if (applicationObj instanceof Map) {
                    Map<String, Object> applicationMap = (Map<String, Object>) applicationObj;
                    Object nameObj = applicationMap.get("name");
                    if (nameObj instanceof String) {
                        return (String) nameObj;
                    }
                }
            }
        }
        return null;
    }

    /**
     * 从 YAML 内容中提取配置组
     *
     * @param yamlContent YAML 内容
     * @return 配置组，如果未找到则返回 null
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public static String extractConfigGroup(@NotNull String yamlContent) {
        Map<String, Object> yamlData = parseYaml(yamlContent);
        if (yamlData != null) {
            // 查找 fkh.app.config-group
            Object fkhObj = yamlData.get("fkh");
            if (fkhObj instanceof Map) {
                Map<String, Object> fkhMap = (Map<String, Object>) fkhObj;
                Object appObj = fkhMap.get("app");
                if (appObj instanceof Map) {
                    Map<String, Object> appMap = (Map<String, Object>) appObj;
                    Object configGroupObj = appMap.get("config-group");
                    if (configGroupObj instanceof String) {
                        return (String) configGroupObj;
                    }
                }
            }
        }
        return null;
    }

    /**
     * 根据文件名确定配置类型
     *
     * @param fileName 文件名
     * @return 配置类型
     */
    @NotNull
    public static String determineConfigType(@NotNull String fileName) {
        String lowerFileName = fileName.toLowerCase();
        if (lowerFileName.endsWith(".yml") || lowerFileName.endsWith(".yaml")) {
            return "yaml";
        } else if (lowerFileName.endsWith(".json")) {
            return "json";
        } else if (lowerFileName.endsWith(".xml")) {
            return "xml";
        } else if (lowerFileName.endsWith(".properties")) {
            return "properties";
        } else if (lowerFileName.endsWith(".html")) {
            return "html";
        } else {
            return "text";
        }
    }

    /**
     * 格式化 YAML 内容
     *
     * @param yamlContent YAML 内容
     * @return 格式化后的 YAML 内容
     */
    @NotNull
    public static String formatYaml(@NotNull String yamlContent) {
        Map<String, Object> yamlData = parseYaml(yamlContent);
        if (yamlData != null) {
            return toYaml(yamlData);
        }
        return yamlContent;
    }
}