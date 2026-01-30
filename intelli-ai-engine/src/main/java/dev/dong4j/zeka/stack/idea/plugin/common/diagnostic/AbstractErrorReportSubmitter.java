package dev.dong4j.zeka.stack.idea.plugin.common.diagnostic;

import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.openapi.application.ex.ApplicationInfoEx;
import com.intellij.openapi.diagnostic.ErrorReportSubmitter;
import com.intellij.openapi.diagnostic.IdeaLoggingEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.diagnostic.SubmittedReportInfo;
import com.intellij.openapi.extensions.PluginDescriptor;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.Consumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.Properties;

/**
 * 抽象错误报告提交器
 * <p> 该类用于抽象错误报告的提交逻辑, 提供通用的错误信息处理, 隐私通知文本生成以及报告提交功能. 子类需要实现具体的错误报告生成和提交方法.
 *
 * <p> 主要功能包括:
 * - 生成隐私通知文本, 包含联系信息指引和问题列表查看链接
 * - 提交错误报告并返回提交结果信息
 * - 构建完整的错误报告内容, 包括堆栈跟踪, 环境信息等
 * - 计算错误信息的 MD5 值以用于去重判断
 *
 * <pre>{@code
 * // 示例: 生成隐私通知文本
 * String privacyNotice = "Please include contact info in the description if you want a reply."
 *     + "After submitting, you can view the issue list at <a href='%s'>%s</a>."
 *     + "Example: <a href='%s'>%s</a>";
 * }</pre>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.20
 * @since 1.0.0
 */
public abstract class AbstractErrorReportSubmitter extends ErrorReportSubmitter {
    /** 应用日志记录器, 用于记录错误报告提交过程中的相关信息 */
    private static final Logger LOG = Logger.getInstance(AbstractErrorReportSubmitter.class);

    /**
     * 获取隐私通知文本内容
     * <p> 返回一条包含隐私政策说明的文本, 提示用户在提交问题时需提供联系方式, 并展示问题列表页面和示例链接.
     *
     * @return 隐私通知文本, 可能为 null
     */
    @Override
    public @Nullable String getPrivacyNoticeText() {
        return String.format(
            "Please include contact info in the description if you want a reply. "
            + "After submitting, you can view the issue list at <a href='%s'>%s</a>. "
            + "Example: <a href='%s'>%s</a>",
            getIssueListPageUrl(),
            getIssueListPageUrl(),
            generateUrlByIssueId(getExampleIssueId()),
            generateTextByIssueId(getExampleIssueId())
                            );
    }

    /**
     * 获取作者名称
     * <p> 返回固定的作者名称字符串 "zeka.stack.team"
     *
     * @return 作者名称
     */
    protected String getAuthorName() {
        return "zeka.stack.team";
    }

    /**
     * 获取报告操作按钮的显示文本
     * <p> 返回字符串 "Report to" 后接作者名称, 作者名称由 {@link #getAuthorName()} 方法提供
     *
     * @return 报告操作按钮的显示文本, 格式为 "Report to [作者名称]"
     * @since 1.0
     */
    @Override
    public @NotNull String getReportActionText() {
        return "Report to " + getAuthorName();
    }

    /**
     * 获取示例问题 ID
     * <p> 抽象方法, 需要由子类实现, 用于返回用于错误报告的示例问题 ID
     *
     * @return 示例问题 ID
     */
    protected abstract String getExampleIssueId();

    /**
     * 获取问题列表页面的 URL 地址
     * <p> 抽象方法, 用于获取错误报告问题列表页面的完整 URL 路径
     *
     * @return 问题列表页面的 URL 字符串
     */
    protected abstract String getIssueListPageUrl();

    /**
     * 生成针对给定问题标识符的文本描述.
     * <p> 此方法在提交或显示已提交报告信息时被调用, 用于生成可读的文本,
     * 例如问题链接或简要说明, 以便在 {@link SubmittedReportInfo} 中展示.
     *
     * @param issueId 目标问题的唯一标识符
     * @return 与该问题对应的文本内容; 返回值保证不为 {@code null}
     */
    protected abstract @NotNull String generateTextByIssueId(String issueId);

    /**
     * 根据问题 ID 生成对应的 URL
     *
     * @param issueId 问题 ID
     * @return 生成的 URL 字符串
     */
    protected abstract @NotNull String generateUrlByIssueId(String issueId);

    /**
     * 提交错误报告
     * <p> 根据提供的日志事件和附加信息, 查找或创建新的问题, 并将结果通过回调返回.
     *
     * @param events          日志事件数组, 当前仅使用第一个事件进行处理
     * @param additionalInfo  用户提供的附加信息, 可为 null
     * @param parentComponent 父组件, 用于 UI 上下文 (当前未直接使用)
     * @param consumer        用于接收提交结果的回调函数
     * @return 如果提交成功返回 true, 否则返回 false
     */
    @Override
    public boolean submit(@NotNull IdeaLoggingEvent @NotNull [] events,
                          @Nullable String additionalInfo,
                          @NotNull Component parentComponent,
                          @NotNull Consumer<? super SubmittedReportInfo> consumer) {
        return submitInternal(events, additionalInfo, parentComponent, consumer);
    }

    /**
     * 内部提交入口, 供手动触发提交流程复用, 避免直接调用 @ApiStatus.OverrideOnly 方法
     *
     * @param events          日志事件数组, 当前仅使用第一个事件进行处理
     * @param additionalInfo  用户提供的附加信息, 可为 null
     * @param parentComponent 父组件, 用于 UI 上下文 (当前未直接使用)
     * @param consumer        用于接收提交结果的回调函数
     * @return 如果提交成功返回 true, 否则返回 false
     */
    protected boolean submitInternal(@NotNull IdeaLoggingEvent @NotNull [] events,
                                     @Nullable String additionalInfo,
                                     @NotNull Component parentComponent,
                                     @NotNull Consumer<? super SubmittedReportInfo> consumer) {
        try {
            IdeaLoggingEvent event = events[0];
            String throwableText = event.getThrowableText();
            if (StringUtil.isEmptyOrSpaces(throwableText)) {
                return false;
            }

            String message = event.getMessage();
            if (StringUtil.isEmptyOrSpaces(message)) {
                int lineEnd = throwableText.indexOf('\n');
                message = lineEnd > 0 ? throwableText.substring(0, lineEnd).trim() : throwableText.trim();
            }

            String extraInfo = StringUtil.notNullize(additionalInfo);
            String issueId = findIssue(throwableText);
            SubmittedReportInfo reportInfo;
            if (issueId == null) {
                issueId = newIssue(throwableText, message, extraInfo);
                reportInfo = new SubmittedReportInfo(
                    generateUrlByIssueId(issueId),
                    generateTextByIssueId(issueId),
                    SubmittedReportInfo.SubmissionStatus.NEW_ISSUE
                );
            } else {
                reportInfo = new SubmittedReportInfo(
                    generateUrlByIssueId(issueId),
                    generateTextByIssueId(issueId),
                    SubmittedReportInfo.SubmissionStatus.DUPLICATE
                );
            }

            consumer.consume(reportInfo);
            return true;
        } catch (Exception e) {
            LOG.warn("Failed to submit issue", e);
            consumer.consume(new SubmittedReportInfo("", "error: " + e.getMessage(),
                                                     SubmittedReportInfo.SubmissionStatus.FAILED));
            return false;
        }
    }

    /**
     * 创建新问题并返回其 ID
     * <p> 根据异常文本, 消息和附加信息生成问题标题和正文, 计算 MD5 哈希值后调用抽象方法创建问题并返回问题 ID
     * <p> 内部调用 {@code buildIssueBody} 构建问题内容, 包含堆栈跟踪, 环境信息等
     *
     * @param throwableText  异常堆栈文本, 用于生成问题 MD5 哈希值和内容
     * @param message        问题摘要标题
     * @param additionalInfo 用户补充描述信息, 可为空
     * @return 新创建问题的 ID, 由抽象方法 {@code newIssueByTitleBody} 返回
     */
    protected String newIssue(String throwableText, String message, String additionalInfo) {
        String issueMd5 = md5Upper(throwableText);
        ApplicationInfoEx appInfo = ApplicationInfoEx.getInstanceEx();
        PluginDescriptor pluginDescriptor = getPluginDescriptor();
        Properties systemProperties = System.getProperties();

        String title = "[Report From IntelliJ] " + message;
        String body = buildIssueBody(throwableText, message, additionalInfo, issueMd5,
                                     appInfo, pluginDescriptor, systemProperties);
        return newIssueByTitleBody(title, body);
    }

    /**
     * 根据异常信息的 MD5 值查找已存在的问题 ID
     * <p> 该方法会先对传入的异常信息进行 MD5 哈希处理, 然后根据生成的哈希值调用 {@link #findIssueByMd5(String)} 方法查找对应的问题 ID.
     *
     * @param throwableText 异常信息文本内容
     * @return 已存在的问题 ID, 若未找到则返回 null
     */
    protected String findIssue(String throwableText) {
        String throwableMd5 = md5Upper(throwableText);
        return findIssueByMd5(throwableMd5);
    }

    /**
     * 根据标题和正文创建新问题
     * <p> 该方法用于在系统中创建一个新的问题条目, 由子类实现具体逻辑. 标题和正文内容将作为问题的初始内容提交.
     *
     * @param title 问题的标题
     * @param body  问题的正文内容
     * @return 新创建的问题 ID 或标识符, 非空字符串
     * @since 1.0
     */
    protected abstract @NotNull String newIssueByTitleBody(String title, String body);

    /**
     * 根据 MD5 值查找已存在的问题编号
     * <p> 该方法用于通过异常堆栈的 MD5 值在系统中查找是否已存在相同的问题记录, 若存在则返回对应的问题编号, 否则返回 null</p>
     * <p> 此方法为抽象方法, 由子类实现具体查找逻辑 </p>
     *
     * @param throwableMd5 异常堆栈内容的 MD5 值, 用于唯一标识问题
     * @return 已存在的问题编号, 若未找到则返回 null
     */
    protected abstract String findIssueByMd5(String throwableMd5);

    /**
     * 构建问题报告的详细信息文本
     * <p> 该方法生成一个包含问题摘要, 用户描述, 堆栈跟踪和环境信息的 Markdown 格式的字符串.
     * <p> 具体信息包括问题标题, 用户附加的信息, 堆栈跟踪,IDE 版本, 操作系统信息,JVM 版本, 插件信息等.
     *
     * @param throwableText    堆栈跟踪信息
     * @param message          问题摘要
     * @param additionalInfo   用户附加的信息
     * @param issueMd5         问题的 MD5 校验码
     * @param appInfo          应用程序信息
     * @param pluginDescriptor 插件描述符, 可以为 null
     * @param systemProperties 系统属性
     * @return 包含问题详细信息的 Markdown 格式的字符串
     */
    private String buildIssueBody(String throwableText,
                                  String message,
                                  String additionalInfo,
                                  String issueMd5,
                                  ApplicationInfoEx appInfo,
                                  @Nullable PluginDescriptor pluginDescriptor,
                                  Properties systemProperties) {
        StringBuilder builder = new StringBuilder(4096);
        builder.append("## Summary\n\n").append(message).append("\n\n");

        if (!StringUtil.isEmptyOrSpaces(additionalInfo)) {
            builder.append("## Description\n\n").append(additionalInfo).append("\n\n");
        }

        builder.append("## Stack Trace\n\n```").append(throwableText).append("\n```\n\n");
        builder.append("## Environment\n\n");
        builder.append("- IDE: ").append(ApplicationNamesInfo.getInstance().getFullProductName())
               .append(" ").append(appInfo.getFullVersion()).append("\n");
        builder.append("- OS: ").append(SystemInfo.getOsNameAndVersion()).append("\n");
        builder.append("- JVM: ").append(System.getProperty("java.runtime.version")).append("\n");
        builder.append("- Locale: ").append(Locale.getDefault()).append("\n");
        if (pluginDescriptor != null) {
            builder.append("- Plugin: ")
                   .append(pluginDescriptor.getName())
                   .append(" ")
                   .append(pluginDescriptor.getVersion())
                   .append("\n");
        }
        builder.append("- Issue MD5: ").append(issueMd5).append("\n\n");

        builder.append("## System Properties\n\n");
        for (String key : systemProperties.stringPropertyNames()) {
            builder.append(key).append("=").append(systemProperties.getProperty(key)).append("\n");
        }

        return builder.toString();
    }

    /**
     * 计算字符串的 MD5 值并返回大写字符串
     *
     * @param text 输入字符串
     * @return MD5 值, 大写字符串
     */
    private String md5Upper(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(text.getBytes(Charset.defaultCharset()));
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02X", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }
}
