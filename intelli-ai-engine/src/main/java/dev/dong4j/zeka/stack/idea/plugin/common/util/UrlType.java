package dev.dong4j.zeka.stack.idea.plugin.common.util;

import java.util.Objects;

import dev.dong4j.zeka.stack.idea.plugin.common.EngineContents;
import lombok.Getter;

/**
 * URL 类型枚举
 * <p> 定义了一组常用的 URL 类型及其对应的链接地址, 用于标识不同的资源访问路径.
 * <p> 每个枚举常量表示一种特定的 URL 类型, 包含唯一的 ID 和对应的 URL.
 * <p> 提供了静态方法 `of`, 可以根据给定的 ID 获取相应的 URL 类型实例.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.04
 * @since 1.0.0
 */
@Getter
public enum UrlType {

    /**
     * 默认 URL 类型 (站点主页)
     * <p> 表示站点的默认主页链接类型
     *
     * @see UrlType
     */
    DEFAULT(EngineContents.PLUGIN_ID + ".#DEFAULT", Urls.SUPPORT_LINK),

    /**
     * 分享链接 (插件市场)
     * <p> 用于指向插件市场的链接
     *
     * @see UrlType
     */
    SHARE(EngineContents.PLUGIN_ID + ".#SHARE", Urls.MARKETPLACE_LINK),

    /**
     * 捐赠链接
     * <p> 用于指向捐赠页面的 URL 类型
     *
     * @see UrlType
     */
    DONATE(EngineContents.PLUGIN_ID + ".#DONATE", Urls.DONATE_LINK),

    /**
     * 邮件链接
     * <p> 用于表示与电子邮件相关的 URL 类型
     *
     * @see UrlType
     */
    MAIL(EngineContents.PLUGIN_ID + ".#MAIL", "mailto:" + Urls.EMAIL_LINK);

    /**
     * URL 类型的唯一标识符
     * <p> 用于区分不同的 URL 类型
     */
    private final String id;
    /**
     * URL 地址
     * <p> 表示当前 URL 类型的完整地址
     *
     */
    private final String url;

    /**
     * 构造函数, 初始化 URL 类型的标识符和对应的 URL 地址
     *
     * @param id  URL 类型标识符, 例如 "plugin.#DEFAULT"
     * @param url 对应的 URL 地址
     */
    UrlType(String id, String url) {
        this.id = id;
        this.url = url;
    }

    /**
     * 根据给定的 ID 获取对应的 URL 类型
     * <p> 遍历所有的 URL 类型枚举值, 查找与指定 ID 匹配的类型. 如果未找到匹配项, 则返回默认类型 DEFAULT
     *
     * @param id URL 类型的唯一标识符, 不能为空
     * @return 匹配的 URL 类型, 如果未找到匹配项则返回 DEFAULT
     */
    public static UrlType of(String id) {
        for (UrlType value : values()) {
            if (Objects.equals(value.id, id)) {
                return value;
            }
        }
        return DEFAULT;
    }

}

