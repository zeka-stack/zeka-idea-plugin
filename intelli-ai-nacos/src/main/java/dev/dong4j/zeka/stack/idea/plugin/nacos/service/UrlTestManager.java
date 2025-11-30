package dev.dong4j.zeka.stack.idea.plugin.nacos.service;

import java.net.HttpURLConnection;
import java.net.URI;

/**
 * URL 测试管理器
 * 用于测试 URL 的可访问性
 *
 * @author dong4j
 * @since 1.0.0
 */
@SuppressWarnings("All")
public class UrlTestManager {

    /**
     * 测试 URL 是否可通过 GET 方法访问
     *
     * @param url 测试的 URL
     * @return true 如果返回 200 状态码
     */
    public static boolean testGetMethod(String url) {
        HttpURLConnection c = null;

        boolean var2;
        try {
            c = (HttpURLConnection) (new URI(url).toURL()).openConnection();
            c.setRequestMethod("GET");
            c.setConnectTimeout(5000);
            c.setReadTimeout(5000);
            if (c.getResponseCode() != 200) {
                var2 = false;
                return var2;
            }

            var2 = true;
        } catch (Exception e) {
            e.printStackTrace();
            boolean var3 = false;
            return var3;
        } finally {
            if (c != null) {
                try {
                    c.disconnect();
                } catch (Exception var13) {
                }
            }
        }

        return var2;
    }
}
