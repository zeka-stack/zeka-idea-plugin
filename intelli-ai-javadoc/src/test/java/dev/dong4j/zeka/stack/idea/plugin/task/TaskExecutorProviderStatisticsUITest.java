package dev.dong4j.zeka.stack.idea.plugin.task;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

/**
 * TaskExecutor ProviderStatistics 弹出框 UI 测试
 * <p>
 * 用于直接显示统计信息弹出框，方便查看和调整 HTML 样式
 * <p>
 * 这是一个独立的测试，不依赖 IDEA Platform
 *
 * @author Cursor AI Assistant
 * @version 1.0
 * @date 2025.01.17
 */
public class TaskExecutorProviderStatisticsUITest {

    /**
     * 测试显示统计信息弹出框
     * <p>
     * 创建模拟的统计数据，然后显示弹出框
     */
    @Test
    public void testShowProviderStatisticsDialog() throws Exception {
        // 创建模拟的统计数据
        Map<String, MockProviderStatistics> providerStats = createMockStatistics();

        // 生成 HTML
        String htmlContent = generateHtml(providerStats);

        // 在 UI 线程中显示对话框
        SwingUtilities.invokeAndWait(() -> showDialog(htmlContent));

        System.out.println("对话框已显示，请查看样式效果...");
        System.out.println("关闭对话框后，测试将完成");
    }

    /**
     * 创建模拟统计数据
     */
    private Map<String, MockProviderStatistics> createMockStatistics() {
        Map<String, MockProviderStatistics> stats = new HashMap<>();

        // 创建第一个提供商统计
        MockProviderStatistics provider1 = new MockProviderStatistics("QianWen 千问");
        provider1.incrementCompleted(5);
        provider1.incrementFailed(1);
        stats.put("QianWen 千问", provider1);
        //
        // // 创建第二个提供商统计
        // MockProviderStatistics provider2 = new MockProviderStatistics("Ollama (本地模型)");
        // provider2.incrementCompleted(2);
        // provider2.incrementSkipped(1);
        // stats.put("Ollama (本地模型)", provider2);
        //
        // // 创建第三个提供商统计
        // MockProviderStatistics provider3 = new MockProviderStatistics("Custom Provider");
        // provider3.incrementCompleted(1);
        // provider3.incrementFailed(2);
        // provider3.incrementSkipped(2);
        // stats.put("Custom Provider", provider3);

        return stats;
    }

    /**
     * 生成 HTML 内容
     */
    private String generateHtml(Map<String, MockProviderStatistics> providerStats) {
        StringBuilder htmlContent = new StringBuilder();
        htmlContent.append("<html><head><style>");
        htmlContent.append("body { font-family: 'Segoe UI', Arial, sans-serif; margin: 10px; font-size: 12px; }");
        htmlContent.append("h2 { color: #2E7D32; margin-bottom: 15px; font-size: 16px; }");
        htmlContent.append("h3 { color: #1976D2; margin-bottom: 10px; font-size: 14px; }");
        htmlContent.append("table { border-collapse: collapse; width: 100%; margin-bottom: 20px; font-size: 11px; border: 1px solid #ddd;" +
                           " }");
        htmlContent.append("th { background-color:rgb(122, 127, 131); color: white; padding: 8px; text-align: center; font-weight: bold; " +
                           "font-size: 11px; border: 1px solid #ddd; }");
        htmlContent.append("td { padding: 8px; text-align: center; font-size: 11px; border: 1px solid #ddd; }");
        htmlContent.append("td.provider-name { text-align: left; }");
        htmlContent.append("tr:nth-child(even) { background-color: #f8f9fa; }");
        htmlContent.append("tr:hover { background-color: #e3f2fd; }");
        htmlContent.append(".summary-row { background-color:rgb(41, 96, 123); color: white; font-weight: bold; }");
        htmlContent.append(".summary-row td { border: 1px solid #ddd; }");
        htmlContent.append("</style></head><body>");

        // 添加标题
        htmlContent.append("<h2>🚀 性能模式处理完成</h2>");

        // 创建表格
        htmlContent.append("<table>");
        htmlContent.append("<tr><th>服务商名称</th><th>完成数量</th><th>失败数量</th><th>跳过数量</th><th>耗时</th></tr>");

        int totalCompleted = 0;
        int totalFailed = 0;
        int totalSkipped = 0;
        long totalDuration = 0;

        for (MockProviderStatistics stats : providerStats.values()) {
            htmlContent.append("<tr>");
            htmlContent.append("<td class='provider-name'>").append(stats.getProviderName()).append("</td>");
            htmlContent.append("<td>").append(stats.getCompletedCount()).append("</td>");
            htmlContent.append("<td>").append(stats.getFailedCount()).append("</td>");
            htmlContent.append("<td>").append(stats.getSkippedCount()).append("</td>");
            htmlContent.append("<td>").append(String.format("%.1fs", stats.getDuration() / 1000.0)).append("</td>");
            htmlContent.append("</tr>");

            totalCompleted += stats.getCompletedCount();
            totalFailed += stats.getFailedCount();
            totalSkipped += stats.getSkippedCount();
            totalDuration += stats.getDuration();
        }

        // 添加总体统计
        htmlContent.append("<tr class='summary-row'>");
        htmlContent.append("<td>📊 总体统计</td>");
        htmlContent.append("<td>").append(totalCompleted).append("</td>");
        htmlContent.append("<td>").append(totalFailed).append("</td>");
        htmlContent.append("<td>").append(totalSkipped).append("</td>");
        htmlContent.append("<td>").append(String.format("%.1fs", totalDuration / 1000.0)).append("</td>");
        htmlContent.append("</tr>");

        htmlContent.append("</table>");
        htmlContent.append("</body></html>");

        return htmlContent.toString();
    }

    /**
     * 显示对话框
     */
    private void showDialog(String htmlContent) {
        JDialog dialog = new JDialog((java.awt.Frame) null, "性能模式处理完成", true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        JEditorPane editorPane = new JEditorPane();
        editorPane.setContentType("text/html");
        editorPane.setText(htmlContent);
        editorPane.setEditable(false);
        editorPane.setBackground(javax.swing.UIManager.getColor("Panel.background"));

        int calculatedHeight = 35 + (3 * 30) + 35 + 170;
        int minHeight = 200;
        int maxHeight = 500;
        int finalHeight = Math.max(minHeight, Math.min(maxHeight, calculatedHeight));

        JScrollPane scrollPane = new JScrollPane(editorPane);
        scrollPane.setPreferredSize(new java.awt.Dimension(800, finalHeight));

        JButton okButton = new JButton("确定");
        okButton.addActionListener(e -> dialog.dispose());

        javax.swing.JPanel buttonPanel = new javax.swing.JPanel();
        buttonPanel.add(okButton);

        dialog.setLayout(new java.awt.BorderLayout());
        dialog.add(scrollPane, java.awt.BorderLayout.CENTER);
        dialog.add(buttonPanel, java.awt.BorderLayout.SOUTH);

        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }

    /**
     * 模拟 ProviderStatistics
     */
    private static class MockProviderStatistics {
        private final String providerName;
        private int completedCount = 0;
        private int failedCount = 0;
        private int skippedCount = 0;
        private final long startTime = System.currentTimeMillis();
        private final long endTime;

        public MockProviderStatistics(String providerName) {
            this.providerName = providerName;
            this.endTime = System.currentTimeMillis() + 1000; // 1秒模拟
        }

        public void incrementCompleted(int count) {
            completedCount += count;
        }

        public void incrementFailed(int count) {
            failedCount += count;
        }

        public void incrementSkipped(int count) {
            skippedCount += count;
        }

        public String getProviderName() {return providerName;}

        public int getCompletedCount()  {return completedCount;}

        public int getFailedCount()     {return failedCount;}

        public int getSkippedCount()    {return skippedCount;}

        public long getDuration() {
            return endTime - startTime;
        }
    }
}
