package dev.dong4j.zeka.stack.idea.plugin.nacos.entity;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * 配置文件实体类
 * 代表一个 Nacos 配置文件
 *
 * @author dong4j
 * @since 1.0.0
 */
public class ConfigFile {
    private String namespace;
    private String group;
    private String dataId;
    private String content;
    private String type;
    private long lastModified;
    private String appName;

    public ConfigFile() {
    }

    public ConfigFile(String namespace, String group, String dataId) {
        this.namespace = namespace;
        this.group = group;
        this.dataId = dataId;
    }

    /**
     * 获取命名空间
     *
     * @return 命名空间
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * 设置命名空间
     *
     * @param namespace 命名空间
     */
    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    /**
     * 获取分组
     *
     * @return 分组
     */
    public String getGroup() {
        return group;
    }

    /**
     * 设置分组
     *
     * @param group 分组
     */
    public void setGroup(String group) {
        this.group = group;
    }

    /**
     * 获取数据 ID
     *
     * @return 数据 ID
     */
    public String getDataId() {
        return dataId;
    }

    /**
     * 设置数据 ID
     *
     * @param dataId 数据 ID
     */
    public void setDataId(String dataId) {
        this.dataId = dataId;
    }

    /**
     * 获取内容
     *
     * @return 内容
     */
    public String getContent() {
        return content;
    }

    /**
     * 设置内容
     *
     * @param content 内容
     */
    public void setContent(String content) {
        this.content = content;
    }

    /**
     * 获取配置类型
     *
     * @return 配置类型
     */
    public String getType() {
        return type;
    }

    /**
     * 设置配置类型
     *
     * @param type 配置类型
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * 获取最后修改时间
     *
     * @return 最后修改时间
     */
    public long getLastModified() {
        return lastModified;
    }

    /**
     * 设置最后修改时间
     *
     * @param lastModified 最后修改时间
     */
    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    /**
     * 获取应用名称
     *
     * @return 应用名称
     */
    public String getAppName() {
        return appName;
    }

    /**
     * 设置应用名称
     *
     * @param appName 应用名称
     */
    public void setAppName(String appName) {
        this.appName = appName;
    }

    /**
     * 生成配置文件的唯一标识符
     *
     * @return 唯一标识符
     */
    @NotNull
    public String getUniqueId() {
        return namespace + ":" + group + ":" + dataId;
    }

    /**
     * 从文件名解析配置文件信息
     *
     * @param fileName  文件名
     * @param namespace 命名空间
     * @return 配置文件对象
     */
    @Nullable
    public static ConfigFile fromFileName(@NotNull String fileName, @NotNull String namespace) {
        ConfigFile configFile = new ConfigFile();
        configFile.setNamespace(namespace);

        // 解析文件名格式: {appName}-{env}.yml 或 {appName}.yml
        String baseName = fileName;
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0) {
            baseName = fileName.substring(0, lastDotIndex);
        }

        // 确定配置类型
        configFile.setType(determineType(fileName));

        // 解析应用名和环境
        if (baseName.contains("-")) {
            int lastDashIndex = baseName.lastIndexOf("-");
            String appName = baseName.substring(0, lastDashIndex);
            String env = baseName.substring(lastDashIndex + 1);

            configFile.setAppName(appName);
            configFile.setDataId(appName + "." + configFile.getType());
            configFile.setGroup(env); // 简化处理，实际应该从配置中读取
        } else {
            // 没有环境后缀
            configFile.setAppName(baseName);
            configFile.setDataId(baseName + "." + configFile.getType());
            configFile.setGroup("DEFAULT_GROUP"); // 默认分组
        }

        return configFile;
    }

    /**
     * 根据文件名确定配置类型
     *
     * @param fileName 文件名
     * @return 配置类型
     */
    @NotNull
    private static String determineType(@NotNull String fileName) {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ConfigFile that = (ConfigFile) o;
        return Objects.equals(namespace, that.namespace) &&
               Objects.equals(group, that.group) &&
               Objects.equals(dataId, that.dataId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(namespace, group, dataId);
    }

    @Override
    public String toString() {
        return "ConfigFile{" +
               "namespace='" + namespace + '\'' +
               ", group='" + group + '\'' +
               ", dataId='" + dataId + '\'' +
               ", type='" + type + '\'' +
               ", appName='" + appName + '\'' +
               '}';
    }
}