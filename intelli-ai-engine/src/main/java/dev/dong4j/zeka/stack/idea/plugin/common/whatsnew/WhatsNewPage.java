package dev.dong4j.zeka.stack.idea.plugin.common.whatsnew;

/**
 * 提供获取版本信息和文件名的方法
 * <p> 该接口用于获取应用的版本信息和当前页面对应的文件名, 常用于页面展示或日志记录等场景
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2025.10.24
 * @since 1.0.0
 */
public interface WhatsNewPage {
    /**
     * 返回页面版本标签.
     *
     * @return 版本标签
     */
    String version();

    /**
     * 返回提供者基本路径下的 HTML 文件名.
     *
     * @return HTML 文件名
     */
    String fileName();
}
