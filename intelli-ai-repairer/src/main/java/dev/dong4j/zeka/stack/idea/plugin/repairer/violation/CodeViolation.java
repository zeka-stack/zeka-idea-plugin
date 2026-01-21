package dev.dong4j.zeka.stack.idea.plugin.repairer.violation;

/**
 * 代码违规信息类
 * <p> 用于表示代码静态分析工具检测到的代码违规或规范问题, 包含违规工具标识, 规则 ID, 消息内容, 严重程度及具体位置信息 (文件路径, 行列范围). 常用于代码质量检查, 静态分析报告生成等场景
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.20
 * @since 2025.3.1200
 */
public class CodeViolation {
    /** 检测工具名称 */
    public String tool;
    /**
     * 违规规则的唯一标识符
     *
     * @see CodeViolation
     */
    public String ruleId;
    /** 错误或违规信息的描述消息 */
    public String message;
    /** 严重程度, 取值范围为 0(忽略),1(信息),2(警告),3(错误) */
    public int severity;
    /**
     * 文件路径
     * <p>
     * 表示代码违规所在的文件路径.
     */
    public String filePath;
    /** 代码起始行号 */
    public int startLine;
    /** 起始列号 */
    public int startColumn;
    /** 结束行号 */
    public int endLine;
    /** 结束列号, 表示代码违规在文件中的结束列位置 */
    public int endColumn;
}
