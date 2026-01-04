package dev.dong4j.zeka.stack.idea.plugin.common.util;

import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.util.ui.TextTransferable;

/**
 * 平台工具类
 * <p> 提供一些与平台相关的实用方法, 包括设置剪贴板内容和判断当前系统是否为中国地区语言环境
 * <p> 可以用于在不同平台上进行通用的操作, 例如复制文本到剪贴板, 或者根据系统语言环境进行相应的处理
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.04
 * @since 1.0.0
 */
public class PlatformUtil {

    /**
     * 设置剪贴板内容
     * <p> 将指定的字符串内容设置到系统剪贴板中
     *
     * @param content 要设置到剪贴板中的字符串内容, 不能为空
     */
    public static void setClipboard(String content) {
        CopyPasteManager.getInstance().setContents(new TextTransferable(content));
    }

    /**
     * 判断当前系统是否处于中文环境
     * <p> 通过检查系统的默认区域设置来判断是否为中文环境
     * <p> 如果区域设置的语言为中文, 并且国家代码为空或为 "CN", 则认为是中文环境
     *
     * @return 如果是中文环境返回 true, 否则返回 false
     */
    public static boolean isChineseLocale() {
        java.util.Locale locale = java.util.Locale.getDefault();
        return java.util.Locale.CHINESE.getLanguage().equals(locale.getLanguage())
               && (java.util.Objects.equals(locale.getCountry(), "") || java.util.Objects.equals(locale.getCountry(), "CN"));
    }
}

