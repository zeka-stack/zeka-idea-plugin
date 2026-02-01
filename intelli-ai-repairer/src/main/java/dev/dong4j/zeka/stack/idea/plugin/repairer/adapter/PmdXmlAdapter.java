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
 * PMD XML 适配器类
 * <p> 用于解析 PMD 工具生成的 XML 格式代码违规报告文件, 并将其转换为 {@link CodeViolation} 对象列表.
 * 支持从 XML 文件中读取每个文件的违规信息, 包括规则 ID, 消息内容, 行号范围和严重程度等.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.20
 * @since 1.0.0
 */
public class PmdXmlAdapter {
    /**
     * 解析 PMD XML 格式的代码违规报告文件
     * <p> 从指定的 XML 文件中解析出所有代码违规信息, 包括文件路径, 规则 ID, 消息内容, 行号范围及严重等级等, 并封装为 {@code List<CodeViolation>} 返回.
     * <p> 若解析过程中发生异常, 将忽略异常并返回空列表.
     *
     * @param xmlFile PMD XML 报告文件, 不能为空
     * @return 解析得到的代码违规列表, 若解析失败或无违规则返回空列表
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

            // 使用更健壮的方式来查找元素，忽略命名空间
            NodeList fileNodes = document.getElementsByTagName("file");
            for (int i = 0; i < fileNodes.getLength(); i++) {
                Element fileElement = (Element) fileNodes.item(i);
                String filePath = fileElement.getAttribute("name");
                NodeList violationNodes = fileElement.getElementsByTagName("violation");
                for (int j = 0; j < violationNodes.getLength(); j++) {
                    Element violation = (Element) violationNodes.item(j);
                    CodeViolation v = new CodeViolation();
                    v.tool = "PMD";
                    v.filePath = filePath;

                    // 改进 ruleId 解析
                    String rule = violation.getAttribute("rule");
                    v.ruleId = rule != null && !rule.isEmpty() ? rule : "Unknown";

                    // 改进消息解析
                    String textContent = violation.getTextContent();
                    v.message = textContent != null ? textContent.trim() : "";

                    // 改进行号解析，尝试不同的属性名
                    v.startLine = parseInt(violation.getAttribute("beginline"));
                    if (v.startLine == 0) {
                        v.startLine = parseInt(violation.getAttribute("line"));
                    }

                    v.endLine = parseInt(violation.getAttribute("endline"));
                    if (v.endLine == 0) {
                        v.endLine = v.startLine;
                    }

                    // 尝试解析列号
                    v.startColumn = parseInt(violation.getAttribute("begincolumn"));
                    if (v.startColumn == 0) {
                        v.startColumn = parseInt(violation.getAttribute("column"));
                    }

                    v.endColumn = parseInt(violation.getAttribute("endcolumn"));
                    if (v.endColumn == 0) {
                        v.endColumn = v.startColumn;
                    }

                    v.severity = SeverityMapper.fromPmdPriority(violation.getAttribute("priority"));
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
     * 将字符串转换为整数, 如果转换失败则返回 0
     * <p> 尝试使用 Integer.parseInt 方法将输入字符串解析为整数, 若发生 NumberFormatException 异常则捕获并返回默认值 0
     *
     * @param value 待转换的字符串
     * @return 解析后的整数值, 若转换失败则返回 0
     */
    private int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
