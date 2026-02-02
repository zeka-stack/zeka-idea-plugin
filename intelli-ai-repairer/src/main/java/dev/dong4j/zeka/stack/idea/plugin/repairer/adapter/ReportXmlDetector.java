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
    public enum ReportType {
        CHECKSTYLE,
        PMD,
        UNKNOWN
    }

    private ReportXmlDetector() {
    }

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

    private static String stripPrefix(String tagName) {
        int colon = tagName.indexOf(':');
        return colon >= 0 ? tagName.substring(colon + 1) : tagName;
    }
}
