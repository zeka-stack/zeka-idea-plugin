package dev.dong4j.zeka.stack.idea.plugin.changelog.service;

import java.util.Date;

/** 变更日志提交信息模型 */
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
