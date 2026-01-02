package dev.dong4j.zeka.stack.idea.javadoc.settings;

import org.jetbrains.annotations.NotNull;

/**
 * 自定义 Javadoc 标签类
 * <p>
 * 用于表示和管理自定义的 Javadoc 标签信息, 包含标签名称和默认值两个属性,
 * 支持标签的创建, 获取, 设置等基本操作, 以及基于标签名称的相等性比较.
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email mailto:zeka.stack@gmail.com
 * @date 2025.11.30
 * @since 1.0.0
 */
public class CustomJavadocTag {
    /**
     * 标签名称
     * <p>
     * 不包含 @ 符号, 例如 "date","email".
     */
    @NotNull
    public String tagName;

    /**
     * 默认值
     * <p>
     * 标签的默认值, 例如 "yyyy.mm.dd".
     * 可以为空字符串.
     */
    @NotNull
    public String defaultValue;

    /**
     * 构造函数
     * <p>
     * 创建一个新的自定义 Javadoc 标签.
     *
     * @param tagName      标签名称
     * @param defaultValue 默认值
     */
    public CustomJavadocTag(@NotNull String tagName, @NotNull String defaultValue) {
        this.tagName = tagName;
        this.defaultValue = defaultValue;
    }

    /**
     * 无参构造函数
     * <p>
     * 用于 XML 序列化.
     */
    public CustomJavadocTag() {
        this.tagName = "";
        this.defaultValue = "";
    }

    /**
     * 获取标签名称
     *
     * @return 标签名称
     */
    @NotNull
    public String getTagName() {
        return tagName;
    }

    /**
     * 设置标签名称
     *
     * @param tagName 标签名称
     */
    public void setTagName(@NotNull String tagName) {
        this.tagName = tagName;
    }

    /**
     * 获取默认值
     *
     * @return 默认值
     */
    @NotNull
    public String getDefaultValue() {
        return defaultValue;
    }

    /**
     * 设置默认值
     *
     * @param defaultValue 默认值
     */
    public void setDefaultValue(@NotNull String defaultValue) {
        this.defaultValue = defaultValue;
    }

    /**
     * 返回当前对象的字符串表示形式
     * <p> 该方法重写自 Object 类的 toString 方法, 用于生成包含 tagName 和 defaultValue 属性值的字符串表示.
     *
     * @return 包含 tagName 和 defaultValue 属性值的字符串表示形式
     */
    @Override
    public String toString() {
        return "CustomJavaDocTag{" +
               "tagName='" + tagName + '\'' +
               ", defaultValue='" + defaultValue + '\'' +
               '}';
    }
}

