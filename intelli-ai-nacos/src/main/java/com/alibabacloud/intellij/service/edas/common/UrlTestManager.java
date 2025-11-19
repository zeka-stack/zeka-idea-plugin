//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.alibabacloud.intellij.service.edas.common;

import java.net.HttpURLConnection;
import java.net.URL;

@SuppressWarnings("All")
public class UrlTestManager {
    public static boolean testGetMethod(String url) {
        HttpURLConnection c = null;

        boolean var2;
        try {
            c = (HttpURLConnection) (new URL(url)).openConnection();
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
