package dev.dong4j.zeka.stack.idea.plugin.changelog.service;

import java.util.Date;

/**
 * 更新日志提交模型类
 * <p>用于封装和表示版本控制系统的提交信息及其差异内容, 支持对提交哈希, 消息, 作者, 时间及差异文本的结构化存储.
 * <p>包含两个记录类:<code>CommitInfo</code> 用于存储单次提交的基本元数据,<code>DiffCommitInfo</code> 用于存储提交的差异文本内容.
 * <p>适用于版本控制系统 (如 Git) 的变更日志解析与展示场景, 可作为构建变更历史视图或自动化发布报告的数据模型.
 * <p>使用示例:
 * <pre>{@code
 * var commit = new CommitInfo("abc123", "修复登录 bug", "修复了用户登录时的权限校验逻辑", new Date(), "dong4j");
 * var diff = new DiffCommitInfo("abc123", new Date(), "diff --git a/src/Login.java b/src/Login.java\\nindex 123...456\\n--- a/src/Login.java\\n+++ b/src/Login.java\\n@@ -10,7 +10,7 @@\\n-    if (user == null) return false;\\n+    if (user == null) return true;");
 * }</pre>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.07
 * @since 1.0.0
 */
public final class ChangelogCommitModels {

    /**
     * 私有构造方法, 防止类被实例化
     * <p> 此类为工具类或只包含静态结构的类, 因此禁止外部创建其实例
     */
    private ChangelogCommitModels() {
        // 工具类，禁止实例化
    }

    /**
     * 提交信息的记录类
     *
     * @param hash         提交的哈希值
     * @param shortMessage 简短的消息描述
     * @param fullMessage  完整的消息描述
     * @param date         提交的时间
     * @param author       提交的作者
     */
    public record CommitInfo(String hash, String shortMessage, String fullMessage, Date date, String author) {
    }

    /**
     * 差异提交信息记录类
     *
     * @param hash     提交的哈希值
     * @param date     提交日期
     * @param diffText 差异文本内容
     */
    public record DiffCommitInfo(String hash, Date date, String diffText) {
    }
}
