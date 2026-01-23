package dev.dong4j.zeka.stack.idea.plugin.kit;

/**
 * 站点内容常量类
 * <p> 该类用于定义项目中涉及的各类 API 地址, 资源链接及功能入口, 包括 GitHub 讨论, 问题追踪, 捐赠页面, 版本信息, Swagger 文档, 更新日志等, 便于统一管理和维护.
 * <p> 所有常量均为字符串类型, 用于在代码中引用外部资源或跳转链接, 避免硬编码, 提高可配置性和可维护性.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.20
 * @since 1.0.0
 */
public class SiteContents {

    /** 讨论 API 的访问地址, 用于反馈讨论相关操作, 指向插件后端的讨论接口服务 */
    public static final String DISCUSSIONS_API_URL = "https://zekastack.dong4j.site/api/plugin/feedback/discussion";
    /** 问题反馈 API 的地址, 用于提交或获取插件相关的问题反馈信息 */
    public static final String ISSUE_API_URL = "https://zekastack.dong4j.site/api/plugin/feedback/issue";
    /** GitHub 讨论链接 */
    public static final String GITHUB_DISCUSSIONS_URL = "https://github.com/zeka-stack/zeka-idea-plugin/discussions";
    /** 模型 API 基础地址 */
    public static final String MODEL_API_BASE_URL = "https://zekastack.dong4j.site/api/plugin/model";
    /** 文件上传的 API 地址, 用于将文件上传到指定的服务端接口 */
    public static final String UPLOAD_URL = "https://zekastack.dong4j.site/api/plugin/events";
    /** 隐私政策页面的 URL */
    public static final String ZEKA_STACK_HOME = "https://zekastack.dong4j.site/";
    /** 隐私政策页面的 URL */
    public static final String PRIVACY = "https://zekastack.dong4j.site/#/privacy";
    /** 数据页面访问地址 */
    public static final String DATAS = "https://zekastack.dong4j.site/#/datas";
    /** 捐赠页面链接, 用于引导用户支持项目开发 */
    public static final String DONATE = "https://zekastack.dong4j.site/#/donate";
    /** 版本检查 URL 地址 */
    public static final String VERSION_URL = "https://ideaplugin.dong4j.site/version";
    /** 引擎落地页 URL */
    public static final String ENGINE = "https://ideaplugin.dong4j.site/engine/landing.html";
    /** JavaDoc 文档链接 */
    public static final String JAVADOC = "https://ideaplugin.dong4j.site/javadoc/landing.html";
    /** 变更日志页面 URL */
    public static final String CHANGELOG = "https://ideaplugin.dong4j.site/changelog/landing.html";
    /** Tracer 跟踪页面 URL */
    public static final String TRACER = "https://ideaplugin.dong4j.site/tracer/landing.html";
    /** Swagger UI 页面地址 */
    public static final String SWAGGER = "https://ideaplugin.dong4j.site/swagger/landing.html";
    /** What's New 页面链接 */
    public static final String WHATSNEW = "https://ideaplugin.dong4j.site/whatsnew";
    /** 项目 GitHub 仓库链接 */
    public static final String GITHUB_LINK = "https://github.com/zeka-stack/zeka-idea-plugin";
    /** GitHub 问题提交链接, 用于用户提交插件相关问题或建议 */
    public static final String GITHUB_ISSUE_LINK = "https://github.com/zeka-stack/zeka-idea-plugin/issues/new";
    /** 支持页面链接, 用于引导用户获取插件支持与帮助 */
    public static final String SUPPORT_LINK = "https://plugins.jetbrains.com/plugin/29152";
    /** 捐赠者列表链接 */
    public static final String DONORS_LIST_LINK = "https://blog.dong4j.site/about";
    /** 市场链接 */
    public static final String MARKETPLACE_LINK = "https://plugins.jetbrains.com/plugin/29152";
    /** 插件市场评论链接, 用于引导用户查看插件在 JetBrains Marketplace 上的用户评价与反馈 */
    public static final String MARKETPLACE_REVIEWS_LINK = "https://plugins.jetbrains.com/vendor/9afaba35-91ea-4364-8ced-64db868dd23e";
    /** 终端图标资源地址 */
    public static final String TERMINAL_LOGO_URL = "https://cdn.dong4j.site/source/image/logo.png";
    /** 终端界面示例动图资源地址 */
    public static final String TERMINAL_GIF_URL = "https://cdn.dong4j.site/source/image/sample.gif";
    /** GitHub Releases API 地址, 用于获取 git-cliff 最新版本信息 */
    public static final String GITHUB_API_URL = "https://api.github.com/repos/orhun/git-cliff/releases/latest";
    /** GitHub 发布下载基础 URL */
    public static final String GITHUB_DOWNLOAD_BASE_URL = "https://github.com/orhun/git-cliff/releases/download";
}
