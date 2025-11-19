//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.alibabacloud.intellij.service.edas.registry.custom;

import com.alibabacloud.intellij.service.edas.common.UrlTestManager;

public class CustomRegistryManager {
    public static boolean registryAvailable(String host) {
        return UrlTestManager.testGetMethod(String.format("http://%s:8848", host));
    }
}
