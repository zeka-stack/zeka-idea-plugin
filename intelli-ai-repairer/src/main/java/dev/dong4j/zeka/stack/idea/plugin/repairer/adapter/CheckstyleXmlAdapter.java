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
 * Checkstyle XML 解析适配器.
 */
public class CheckstyleXmlAdapter {
    public List<CodeViolation> parse(@NotNull File xmlFile) {
        List<CodeViolation> violations = new ArrayList<>();
        try {
            Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xmlFile);
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
                    v.ruleId = error.getAttribute("source");
                    v.message = error.getAttribute("message");
                    v.startLine = parseInt(error.getAttribute("line"));
                    v.startColumn = parseInt(error.getAttribute("column"));
                    v.endLine = v.startLine;
                    v.endColumn = v.startColumn;
                    v.severity = SeverityMapper.fromCheckstyle(error.getAttribute("severity"));
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
