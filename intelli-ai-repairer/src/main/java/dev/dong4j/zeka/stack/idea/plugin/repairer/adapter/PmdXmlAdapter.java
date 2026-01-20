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
 * PMD XML 解析适配器.
 */
public class PmdXmlAdapter {
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

    private int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
