package dev.dong4j.zeka.stack.idea.plugin.repairer.adapter;

import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

import dev.dong4j.zeka.stack.idea.plugin.repairer.violation.CodeViolation;
import dev.dong4j.zeka.stack.idea.plugin.repairer.violation.SeverityMapper;

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
            Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xmlFile);
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
                    v.ruleId = violation.getAttribute("rule");
                    v.message = violation.getTextContent() != null ? violation.getTextContent().trim() : "";
                    v.startLine = parseInt(violation.getAttribute("beginline"));
                    v.endLine = parseInt(violation.getAttribute("endline"));
                    v.startColumn = 0;
                    v.endColumn = 0;
                    v.severity = SeverityMapper.fromPmdPriority(violation.getAttribute("priority"));
                    violations.add(v);
                }
            }
        } catch (Exception ignored) {
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
