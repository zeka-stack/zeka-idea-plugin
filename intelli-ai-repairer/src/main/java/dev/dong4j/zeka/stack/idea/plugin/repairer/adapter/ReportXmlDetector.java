package dev.dong4j.zeka.stack.idea.plugin.repairer.adapter;

import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;

import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Detect report type by XML structure.
 */
public final class ReportXmlDetector {
    /**
     * 报告类型枚举
     * <p> 用于标识不同类型的静态代码分析报告, 支持 CheckStyle 和 PMD 两种常见报告格式, 若无法识别则返回 UNKNOWN.
     * 该枚举常用于根据 XML 结构自动检测报告类型, 适用于代码质量分析工具的报告解析模块.
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.02.02
     * @since x.x.x
     */
    public enum ReportType {
        /** CheckStyle 报告类型 */
        CHECKSTYLE,
        /** PMD 工具对应的报告类型 */
        PMD,
        /**
         * 未知类型
         * <p> 表示无法识别或未定义的报告类型
         */
        UNKNOWN
    }

    /**
     * 私有构造函数, 防止外部实例化
     * <p> 此构造函数为私有, 确保类无法从外部实例化
     */
    private ReportXmlDetector() {
    }

    /**
     * 根据 XML 文件结构检测报告类型
     * <p> 该方法通过解析 XML 文件的根元素名称和内容, 判断其属于 Checkstyle,PMD 或未知类型.
     *
     * @param xmlFile 要分析的 XML 文件
     * @return 检测到的报告类型, 可能为 CHECKSTYLE,PMD 或 UNKNOWN
     */
    public static @NotNull ReportType detect(@NotNull File xmlFile) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            factory.setValidating(false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

            Document document = factory.newDocumentBuilder().parse(xmlFile);
            document.getDocumentElement().normalize();

            Element root = document.getDocumentElement();
            String rootName = stripPrefix(root.getTagName());
            if ("checkstyle".equalsIgnoreCase(rootName)) {
                return ReportType.CHECKSTYLE;
            }
            if ("pmd".equalsIgnoreCase(rootName)) {
                return ReportType.PMD;
            }

            NodeList errorNodes = document.getElementsByTagName("error");
            if (errorNodes != null && errorNodes.getLength() > 0) {
                return ReportType.CHECKSTYLE;
            }
            NodeList violationNodes = document.getElementsByTagName("violation");
            if (violationNodes != null && violationNodes.getLength() > 0) {
                return ReportType.PMD;
            }
        } catch (Exception ignored) {
            return ReportType.UNKNOWN;
        }
        return ReportType.UNKNOWN;
    }

    /**
     * 去除标签名中的前缀
     * <p> 如果标签名包含冒号 (:), 则返回冒号后的内容; 否则返回原始标签名 </p>
     *
     * @param tagName 要处理的标签名
     * @return 去除前缀后的标签名
     */
    private static String stripPrefix(String tagName) {
        int colon = tagName.indexOf(':');
        return colon >= 0 ? tagName.substring(colon + 1) : tagName;
    }
}
