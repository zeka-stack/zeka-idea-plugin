package dev.dong4j.zeka.stack.idea.plugin.repairer.adapter;

import dev.dong4j.zeka.stack.idea.plugin.repairer.violation.CodeViolation;
import dev.dong4j.zeka.stack.idea.plugin.repairer.violation.SeverityMapper;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Checkstyle XML 解析适配器
 * <p> 用于将 Checkstyle 工具生成的 XML 格式静态代码分析报告解析为统一的 CodeViolation 对象列表, 便于后续处理或展示.
 * <p> 支持从指定的 XML 文件中提取所有代码违规信息, 包括文件路径, 规则 ID, 错误消息, 行号, 列号及严重等级.
 * <p> 解析过程使用标准的 DOM 解析器, 对 XML 结构进行遍历, 提取每个 &lt;file&gt; 下的 &lt;error&gt; 节点信息并封装成 CodeViolation 对象.
 * <p> 若解析过程中发生异常, 将静默忽略并返回空列表, 确保程序健壮性.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.20
 * @since 1.0.0
 */
public class CheckstyleXmlAdapter {
    /**
     * 解析 Checkstyle XML 格式的违规报告文件
     * <p> 该方法读取并解析 Checkstyle 工具生成的 XML 格式违规报告,
     * 将其中的违规信息提取为 {@link CodeViolation} 对象列表返回 </p>
     *
     * @param xmlFile 待解析的 Checkstyle XML 格式文件, 不能为 null
     * @return 包含所有代码违规信息的列表, 每个元素代表一个违规项
     */
    public List<CodeViolation> parse(@NotNull File xmlFile) {
        List<CodeViolation> violations = new ArrayList<>();
        try {
            // 配置 DocumentBuilderFactory 以提高解析健壮性
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            factory.setValidating(false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

            Document document = factory.newDocumentBuilder().parse(xmlFile);
            document.getDocumentElement().normalize();

            NodeList fileNodes = document.getElementsByTagName("file");
            for (int i = 0; i < fileNodes.getLength(); i++) {
                Element fileElement = (Element) fileNodes.item(i);
                String filePath = fileElement.getAttribute("name");
                NodeList errorNodes = fileElement.getElementsByTagName("error");
                for (int j = 0; j < errorNodes.getLength(); j++) {
                    Element error = (Element) errorNodes.item(j);
                    CodeViolation v = new CodeViolation();
                    v.tool = "CHECKSTYLE";
                    v.filePath = filePath;

                    // 改进 ruleId 解析，尝试从 source 属性中提取规则名
                    String source = error.getAttribute("source");
                    if (source != null && !source.isEmpty()) {
                        // 从 source 中提取规则名，例如从 "com.puppycrawl.tools.checkstyle.checks.blocks.EmptyBlockCheck" 中提取 "EmptyBlockCheck"
                        int lastDotIndex = source.lastIndexOf('.');
                        v.ruleId = lastDotIndex >= 0 ? source.substring(lastDotIndex + 1) : source;
                    } else {
                        v.ruleId = "Unknown";
                    }

                    v.message = error.getAttribute("message");
                    v.startLine = parseInt(error.getAttribute("line"));
                    v.startColumn = parseInt(error.getAttribute("column"));
                    v.endLine = v.startLine;
                    v.endColumn = v.startColumn;
                    v.severity = SeverityMapper.fromCheckstyle(error.getAttribute("severity"));
                    violations.add(v);
                }
            }
        } catch (Exception e) {
            // 打印异常信息，以便调试
            e.printStackTrace();
        }
        return violations;
    }

    /**
     * 将字符串转换为整数
     * <p> 尝试将输入的字符串解析为整数, 如果解析失败则返回 0
     *
     * @param value 需要转换的字符串
     * @return 转换后的整数值, 如果转换失败则返回 0
     */
    private int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
