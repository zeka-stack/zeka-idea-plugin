package dev.dong4j.zeka.stack.idea.plugin.common.util;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * URL 常量与可达性验证工具类
 * <p> 提供与项目相关的各类链接常量, 以及网络可达性检测功能.
 * <p> 该类主要用于在运行时检测指定 URL 是否可达, 支持自定义超时时间.
 * <p> 使用示例:
 * <pre>{@code
 * if (Urls.isReachable(Urls.SUPPORT_LINK)) {*     System.out.println("支持链接可达");
 * } else {*     System.out.println("支持链接不可达");
 * }
 * }</pre>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.04
 * @since 1.0.0
 */
public class Urls {

    /**
     * 网络可达性标记
     * <p> 用于标识当前网络是否可达, 由插件内部线程进行更新
     */
    public static final AtomicBoolean reachableAtomic = new AtomicBoolean(false);

    /** GitHub 仓库链接 */
    public static final String GITHUB_LINK = "https://github.com/zeka-stack/zeka-idea-plugin";
    /** GitHub Issues 链接 */
    public static final String GITHUB_ISSUE_LINK = "https://github.com/zeka-stack/zeka-idea-plugin/issues/new";
    /** 支持页面链接 */
    public static final String SUPPORT_LINK = "https://plugins.jetbrains.com/plugin/29152";
    /**
     * 捐赠链接
     * <p> 提供一个链接以便用户可以通过此链接进行捐赠.
     */
    public static final String DONATE_LINK = "https://ideaplugin.dong4j.site/buy-me-a-coffee";
    /**
     * 捐赠者列表链接
     * <p> 指向包含捐赠者名单的博客页面.
     */
    public static final String DONORS_LIST_LINK = "https://blog.dong4j.site/about";
    /** 插件市场链接, 存储插件在 JetBrains 插件市场的 URL */
    public static final String MARKETPLACE_LINK = "https://plugins.jetbrains.com/plugin/29152";
    /** 插件市场评价链接 */
    public static final String MARKETPLACE_REVIEWS_LINK = "https://plugins.jetbrains.com/vendor/9afaba35-91ea-4364-8ced-64db868dd23e";
    /** 邮箱链接 */
    public static final String EMAIL_LINK = "dong4j@gmail.com";

    /**
     * 验证网络是否可达
     * <p> 在独立线程中验证指定的 URL(默认为支持页面链接) 是否可达, 并更新网络可达性状态
     *
     * @since 1.0.0
     */
    public static void verifyReachable() {
        new Thread(() -> reachableAtomic.getAndSet(isReachable(SUPPORT_LINK))).start();
    }

    /**
     * 获取网络可达性状态
     * <p> 返回当前网络是否可达的布尔值, 该状态由 {@link #verifyReachable()} 方法异步验证并更新
     *
     * @return 如果网络可达返回 true, 否则返回 false
     */
    public static boolean isReachable() {
        return reachableAtomic.get();
    }

    /**
     * 检查指定网络地址是否可达
     * <p> 调用内部方法检查网络地址是否可达, 使用默认超时时间 5000 毫秒
     *
     * @param url 网络地址
     * @return 如果网络可达返回 true, 否则返回 false
     */
    public static boolean isReachable(String url) {
        return isReachable(url, 5000);
    }

    /**
     * 验证指定网络地址是否在给定超时时间内可达
     * <p> 通过发送 HTTP GET 请求检查指定 URL 是否可访问, 若在指定时间内成功响应则返回 true, 否则返回 false.
     *
     * @param url     网络地址, 不能为空
     * @param timeout 超时时间 (毫秒), 默认为 5000 毫秒
     * @return 如果网络地址可达返回 true, 否则返回 false
     */
    public static boolean isReachable(String url, int timeout) {
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            RequestConfig requestConfig = RequestConfig.custom()
                // 设置连接超时时间
                .setConnectTimeout(timeout)
                // 设置读取超时时间
                .setSocketTimeout(timeout)
                .build();
            HttpGet request = new HttpGet(url);
            request.setConfig(requestConfig);
            httpClient.execute(request);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}

